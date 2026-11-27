package com.sprint.mission.sb15monewteam02.common.exception;


// 사용자를 찾지 못했을 때 예외처리,비밀번호가 같지 않을시 예외처리
public class UserNotFoundException extends BusinessException {
    public UserNotFoundException() {
        super(ErrorCode.USER_NOT_FOUND);
    }
}
