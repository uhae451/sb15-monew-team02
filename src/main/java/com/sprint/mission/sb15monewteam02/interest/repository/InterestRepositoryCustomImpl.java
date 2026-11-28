package com.sprint.mission.sb15monewteam02.interest.repository;

import com.sprint.mission.sb15monewteam02.interest.entity.Interest;
import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;
import org.springframework.data.domain.Sort;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;

// InterestRepositoryCustom의 실제 구현체
// Spring Data가 "InterestRepositoryCustom + Impl" 이름을 보고 자동으로 연결한다.
public class InterestRepositoryCustomImpl implements InterestRepositoryCustom {

    private final EntityManager em;

    public InterestRepositoryCustomImpl(EntityManager em) {
        this.em = em;
    }

    @Override
    public List<Interest> searchInterests(InterestSearchCondition condition) {
        String direction = condition.direction().name();
        String sortField = switch (condition.orderBy()) {
            case NAME -> "i.name";
            case SUBSCRIBER_COUNT -> "i.subscriberCount";
        };

        // 커서 조건에서 쓸 비교 연산자.
        // 오름차순이면 "다음 값"을 찾으므로 >, 내림차순이면 <를 쓴다.
        String op = condition.direction() == Sort.Direction.ASC ? ">" : "<";

        // 검색어가 실제 문자가 있을 때만 검색 조건을 적용한다. (공백만 있는 경우도 제외)
        boolean hasKeyword = StringUtils.hasText(condition.keyword());

        // 커서 기준이 되는 값과 id가 둘 다 있어야 "다음 페이지" 조건을 적용한다.
        boolean hasCursor = condition.cursor() != null && condition.after() != null;

        // 기본 SELECT 문. DISTINCT는 키워드 JOIN으로 같은 관심사가 여러 번 나오는 것을 막는다.
        StringBuilder jpql = new StringBuilder("SELECT DISTINCT i FROM Interest i");

        // WHERE 절에 들어갈 조건들을 모아 두었다가 마지막에 AND로 합친다.
        List<String> where = new ArrayList<>();

        if (hasKeyword) {

            // 키워드는 interest_keywords 테이블과 조인해서 비교, LEFT JOIN으로 키워드가 없는 관심사도 검색 가능
            jpql.append(" LEFT JOIN i.keywords k");

            // 이름/키워드 중 하나라도 검색어를 포함하면 조회한다.
            where.add("(i.name LIKE :keyword OR k LIKE :keyword)");
        }
        if (hasCursor) {
            // 커서 페이지네이션 조건

            //
            where.add("(" + sortField + " " + op + " :cursor"
                    + " OR (" + sortField + " = :cursor AND i.id " + op + " :after))");
        }

        // 조건이 하나라도 있으면 WHERE을 붙인다.
        if (!where.isEmpty()) {
            jpql.append(" WHERE ").append(String.join(" AND ", where));
        }

        // 정렬 기준으 같은 항목끼리는 id로 재정렬한다. (커서로 이어서 읽을 때 순서 보장)
        jpql.append(" ORDER BY ").append(sortField).append(" ").append(direction)
                .append(", i.id ").append(direction);

        // 완성된 JPQL 문자열로 쿼리 객체를 만든다.
        TypedQuery<Interest> query = em.createQuery(jpql.toString(), Interest.class);

        // %을 사용해 부분일치조건 생성
        if (hasKeyword) {
            query.setParameter("keyword", "%" + condition.keyword() + "%");
        }

        // 검색어 파라미터 (정렬 기준에 따라 문자열/숫자로 변환)
        if (hasCursor) {
            query.setParameter("cursor", cursorValue(condition));
            query.setParameter("after", condition.after());
        }

        // 가져올 최대 개수 (=> LIMIT)
        query.setMaxResults(condition.limit());     // 최대 limit개만 가져온다.
        return query.getResultList();
    }

    // 커서값은 문자열로 들어오기에 정렬 기준에 맞는 타입으로 변환시킨다. (구독자 수 = long, 이름 = 문자열)
    private Object cursorValue(InterestSearchCondition condition) {
        return condition.orderBy() == InterestOrderBy.SUBSCRIBER_COUNT
                ? Long.parseLong(condition.cursor())
                : condition.cursor();
    }
}