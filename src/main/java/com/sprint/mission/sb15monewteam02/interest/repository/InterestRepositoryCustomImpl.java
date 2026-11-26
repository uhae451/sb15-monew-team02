package com.sprint.mission.sb15monewteam02.interest.repository;

import com.sprint.mission.sb15monewteam02.interest.entity.Interest;
import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;
import org.springframework.data.domain.Sort;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;

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
        String op = condition.direction() == Sort.Direction.ASC ? ">" : "<";

        boolean hasKeyword = StringUtils.hasText(condition.keyword());
        boolean hasCursor = condition.cursor() != null && condition.after() != null;

        StringBuilder jpql = new StringBuilder("SELECT DISTINCT i FROM Interest i");
        List<String> where = new ArrayList<>();

        if (hasKeyword) {
            jpql.append(" LEFT JOIN i.keywords k");
            where.add("(i.name LIKE :keyword OR k LIKE :keyword)");
        }
        if (hasCursor) {
            where.add("(" + sortField + " " + op + " :cursor"
                    + " OR (" + sortField + " = :cursor AND i.id " + op + " :after))");
        }
        if (!where.isEmpty()) {
            jpql.append(" WHERE ").append(String.join(" AND ", where));
        }
        jpql.append(" ORDER BY ").append(sortField).append(" ").append(direction)
                .append(", i.id ").append(direction);

        TypedQuery<Interest> query = em.createQuery(jpql.toString(), Interest.class);
        if (hasKeyword) {
            query.setParameter("keyword", "%" + condition.keyword() + "%");
        }
        if (hasCursor) {
            query.setParameter("cursor", cursorValue(condition));
            query.setParameter("after", condition.after());
        }
        query.setMaxResults(condition.limit());
        return query.getResultList();
    }

    private Object cursorValue(InterestSearchCondition condition) {
        return condition.orderBy() == InterestOrderBy.SUBSCRIBER_COUNT
                ? Long.parseLong(condition.cursor())
                : condition.cursor();
    }
}