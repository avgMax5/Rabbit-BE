package team.avgmax.rabbit.funding.exception;

import org.springframework.http.HttpStatus;

import team.avgmax.rabbit.global.dto.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum FundingError implements ErrorCode {
    BUNNY_NAME_REQUIRED(HttpStatus.BAD_REQUEST, "버니 이름은 필수입니다."),
    BUNNY_NAME_DUPLICATE(HttpStatus.BAD_REQUEST, "이미 존재하는 버니 이름입니다."),
    BUNNY_NAME_INVALID_LENGTH(HttpStatus.BAD_REQUEST, "버니 이름은 3자 이상 20자 이하여야 합니다."),
    BUNNY_NAME_INVALID_HYPHEN_START_END(HttpStatus.BAD_REQUEST, "버니 이름은 하이픈으로 시작하거나 끝날 수 없습니다."),
    BUNNY_NAME_INVALID_CONSECUTIVE_HYPHEN(HttpStatus.BAD_REQUEST, "연속된 하이픈은 사용할 수 없습니다."),
    BUNNY_NAME_INVALID_CHARACTER(HttpStatus.BAD_REQUEST, "버니 이름은 영어 소문자, 숫자, 하이픈만 사용할 수 있습니다."),
    BUNNY_TYPE_REQUIRED(HttpStatus.BAD_REQUEST, "버니 타입은 필수입니다."),
    BUNNY_TYPE_INVALID(HttpStatus.BAD_REQUEST, "지원하지 않는 버니 타입입니다. A, B, C 중 하나를 선택해주세요."),
    FUND_BUNNY_NOT_FOUND(HttpStatus.NOT_FOUND, "존재하지 않는 펀드버니입니다."),
    BNY_NOT_POSITIVE(HttpStatus.BAD_REQUEST, "BNY는 양수여야 합니다."),
    ALREADY_FUND_BUNNY_USER(HttpStatus.FORBIDDEN, "이미 심사 중인 유저입니다."),
    BNY_OVER_REMAINING(HttpStatus.BAD_REQUEST, "잔량을 초과한 BNY 요청입니다."),
    BNY_OVER_50(HttpStatus.FORBIDDEN, "최대 50%까지 지분을 차지할 수 있습니다.");

    private final HttpStatus status;
    private final String message;

}
