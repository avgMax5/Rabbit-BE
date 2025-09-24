package team.avgmax.rabbit.global.exception;

import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import jakarta.servlet.http.HttpServletRequest;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.access.AccessDeniedException;

import team.avgmax.rabbit.global.dto.ErrorResponse;
import team.avgmax.rabbit.funding.exception.FundingException;
import team.avgmax.rabbit.bunny.exception.BunnyException;
import team.avgmax.rabbit.user.exception.UserException;

@RestControllerAdvice
public class GlobalExceptionHandler {
    // UserException 처리
    @ExceptionHandler(UserException.class)
    public ResponseEntity<ErrorResponse> handleUserException(UserException ex) {
        return ErrorResponse.toResponseEntity(ex.getError());
    }

    // BunnyException 처리
    @ExceptionHandler(BunnyException.class)
    public ResponseEntity<ErrorResponse> handleBunnyException(BunnyException ex) {
        return ErrorResponse.toResponseEntity(ex.getError());
    }

    // FundingException 처리
    @ExceptionHandler(FundingException.class)
    public ResponseEntity<ErrorResponse> handleFundingException(FundingException ex) {
        return ErrorResponse.toResponseEntity(ex.getError());
    }

    // IllegalStateException 처리
    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<Object> handleIllegalStateException(IllegalStateException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ex.getMessage());
    }

   // Unauthentication 처리
   @ExceptionHandler(AuthenticationException.class)
   public ResponseEntity<Object> handleAuthenticationException(AuthenticationException ex) {
       return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(ex.getMessage());
   }

   // Authorization 처리
   @ExceptionHandler(AccessDeniedException.class)
   public ResponseEntity<Object> handleAccessDeniedException(AccessDeniedException ex) {
       return ResponseEntity.status(HttpStatus.FORBIDDEN).body(ex.getMessage());
   }

    // 기타 모든 예외 처리 (500 서버 에러)
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Object> handleException(HttpServletRequest req, Exception ex) {
         if (req.getRequestURI().startsWith("/v3/api-docs")
            || req.getRequestURI().startsWith("/swagger-ui")) {
            throw new RuntimeException(ex);
        }

        return ResponseEntity
            .status(HttpStatus.INTERNAL_SERVER_ERROR)
            .body(ex.getMessage());
    }
}
