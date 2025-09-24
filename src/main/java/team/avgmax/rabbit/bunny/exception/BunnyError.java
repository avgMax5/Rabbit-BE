package team.avgmax.rabbit.bunny.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import team.avgmax.rabbit.global.dto.ErrorCode;

@Getter
@RequiredArgsConstructor
public enum BunnyError implements ErrorCode {
    BUNNY_NOT_FOUND(HttpStatus.NOT_FOUND, "해당 버니를 찾을 수 없습니다."),
    INSUFFICIENT_BALANCE(HttpStatus.BAD_REQUEST, "보유 캐럿이 부족합니다."),
    INSUFFICIENT_HOLDING(HttpStatus.BAD_REQUEST, "보유 수량이 부족합니다."),
    ORDER_NOT_FOUND(HttpStatus.NOT_FOUND, "해당 주문을 찾을 수 없습니다."),
    FORBIDDEN(HttpStatus.FORBIDDEN, "해당 주문을 취소할 권한이 없습니다."),
    ORDER_ALREADY_FILLED(HttpStatus.CONFLICT, "이미 체결이 완료된 주문은 취소할 수 없습니다."),
    NEGATIVE_HOLDING(HttpStatus.CONFLICT, "보유 수량이 마이너스 입니다."),
    UNSUPPORTED_ORDER_TYPE(HttpStatus.BAD_REQUEST, "지원하지 않는 주문 타입입니다."),
    ALREADY_LIKED(HttpStatus.CONFLICT, "이미 좋아요를 추가한 버니입니다."),
    ALREADY_UNLIKED(HttpStatus.CONFLICT, "이미 좋아요를 취소한 버니입니다.");

    private final HttpStatus status;
    private final String message;
}
