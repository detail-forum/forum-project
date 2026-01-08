package com.pgh.api_practice.global;

import com.pgh.api_practice.dto.ApiResponse;
import com.pgh.api_practice.dto.ValidationErrorDTO;
import com.pgh.api_practice.exception.*;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.NoHandlerFoundException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalApiResponseHandler {

    // 400: DTO @Valid 검증 실패
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<String> handleValidation(MethodArgumentNotValidException ex) {
        Map<String, String> errors = new HashMap<>();

        for (var error : ex.getBindingResult().getAllErrors()) {
            String field = error instanceof FieldError
                    ? ((FieldError) error).getField()
                    : error.getObjectName();
            String msg = error.getDefaultMessage();
            errors.put(field, msg);
        }

        ValidationErrorDTO dto = ValidationErrorDTO.builder()
                .message("요청 값이 올바르지 않습니다.")
                .errors(errors)
                .build();

        // 문자열 형태로 반환 (toString() 사용)
        return ResponseEntity.badRequest().body(dto.toString());
    }


    // 400: JSON 파싱/타입 오류 등
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiResponse<Void>> handleNotReadable(HttpMessageNotReadableException ex) {
        return ResponseEntity.badRequest().body(ApiResponse.fail("요청 본문을 읽을 수 없습니다."));
    }

    // 404: 서비스 계층에서 존재하지 않는 리소스 등
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiResponse<Void>> handleIllegalArgument(IllegalArgumentException ex) {
        return ResponseEntity.status(404).body(ApiResponse.fail(ex.getMessage() != null ? ex.getMessage() : "대상을 찾을 수 없습니다."));
    }

    // 400: 커스텀 이미 유저있다
    @ExceptionHandler(UserAlreadyExistException.class)
    public ResponseEntity<ApiResponse<Void>> handleUserAlreadyExist(UserAlreadyExistException ex) {
        return ResponseEntity.badRequest().body(ApiResponse.fail(ex.getMessage()));
    }

    // 500: NullPointerException 처리
    @ExceptionHandler(NullPointerException.class)
    public ResponseEntity<ApiResponse<Void>> handleNullPointer(NullPointerException ex) {
        ex.printStackTrace(); // 디버깅을 위한 스택 트레이스 출력
        return ResponseEntity.status(401).body(ApiResponse.fail("인증이 필요합니다."));
    }

    // 500: IllegalStateException 처리 (예상치 못한 상태)
    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<ApiResponse<Void>> handleIllegalState(IllegalStateException ex) {
        ex.printStackTrace(); // 디버깅을 위한 스택 트레이스 출력
        return ResponseEntity.status(500).body(ApiResponse.fail(ex.getMessage() != null ? ex.getMessage() : "서버 내부 오류가 발생했습니다."));
    }

    // 500: 기타 모든 예외
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleException(Exception ex) {
        // 로그는 여기서 찍으세요 (ex.getMessage(), stacktrace 등)
        ex.printStackTrace(); // 디버깅을 위한 스택 트레이스 출력
        return ResponseEntity.internalServerError().body(ApiResponse.fail("서버 내부 오류가 발생했습니다: " + ex.getMessage()));
    }

    //리프레시 토큰 만료시 처리
    @ExceptionHandler(RefreshTokenExpiredException.class)
    public ResponseEntity<ApiResponse<Void>> handleRefreshTokenExpiredException(RefreshTokenExpiredException ex) {
        return ResponseEntity.status(401).body(ApiResponse.fail(ex.getMessage()));//401
    }
    //토큰검증 실패시 처리
    @ExceptionHandler(TokenNotValidateException.class)
    public ResponseEntity<ApiResponse<Void>> handleTokenNotValidateException(TokenNotValidateException ex) {
       //추후 보안 관제로깅등 추가가능
        return ResponseEntity.status(401).body(ApiResponse.fail(ex.getMessage()));//401
    }
    //
    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ApiResponse<Void>> handleResourceNotFoundException(ResourceNotFoundException ex) {
        return ResponseEntity.status(404).body(ApiResponse.fail(ex.getMessage()));
    }
    @ExceptionHandler(ApplicationUnauthorizedException.class)
    public ResponseEntity<ApiResponse<Void>> handleApplicationUnauthorizedException(ApplicationUnauthorizedException ex) {
        return ResponseEntity.status(403).body(ApiResponse.fail(ex.getMessage()));
    }
    
    @ExceptionHandler(ApplicationBadRequestException.class)
    public ResponseEntity<ApiResponse<Void>> handleApplicationBadRequestException(ApplicationBadRequestException ex) {
        return ResponseEntity.badRequest().body(ApiResponse.fail(ex.getMessage()));
    }

    // 401: Spring Security 인증 실패 (아이디/비밀번호 틀림)
    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<ApiResponse<Void>> handleBadCredentials(BadCredentialsException ex) {
        return ResponseEntity.status(401).body(ApiResponse.fail("아이디 또는 비밀번호가 올바르지 않습니다."));
    }

    // 401: 기타 인증 예외
    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ApiResponse<Void>> handleAuthenticationException(AuthenticationException ex) {
        String message = "인증에 실패했습니다.";
        if (ex.getMessage() != null && ex.getMessage().contains("자격 증명")) {
            message = "아이디 또는 비밀번호가 올바르지 않습니다.";
        }
        return ResponseEntity.status(401).body(ApiResponse.fail(message));
    }

    // 401: 인증 자격 증명 없음
    @ExceptionHandler(AuthenticationCredentialsNotFoundException.class)
    public ResponseEntity<ApiResponse<Void>> handleAuthenticationCredentialsNotFound(AuthenticationCredentialsNotFoundException ex) {
        return ResponseEntity.status(401).body(ApiResponse.fail("인증 정보가 없습니다."));
    }

    // 404: 핸들러를 찾을 수 없음 (NoHandlerFoundException)
    @ExceptionHandler(NoHandlerFoundException.class)
    public ResponseEntity<ApiResponse<Void>> handleNoHandlerFound(NoHandlerFoundException ex) {
        ex.printStackTrace();
        return ResponseEntity.status(404).body(ApiResponse.fail("요청한 경로를 찾을 수 없습니다: " + ex.getRequestURL()));
    }

    // 404: 정적 리소스를 찾을 수 없음 (NoResourceFoundException) - 컨트롤러 매핑 실패 시 발생
    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ApiResponse<Void>> handleNoResourceFound(NoResourceFoundException ex) {
        ex.printStackTrace();
        String resourcePath = ex.getResourcePath();
        // /api/로 시작하는 경로는 API 엔드포인트이므로 더 명확한 메시지 제공
        if (resourcePath != null && resourcePath.startsWith("/api/")) {
            return ResponseEntity.status(404).body(ApiResponse.fail("API 엔드포인트를 찾을 수 없습니다: " + resourcePath));
        }
        return ResponseEntity.status(404).body(ApiResponse.fail("요청한 리소스를 찾을 수 없습니다: " + resourcePath));
    }

    // 500: RuntimeException 처리
    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<ApiResponse<Void>> handleRuntimeException(RuntimeException ex) {
        ex.printStackTrace();
        // ResourceNotFoundException은 이미 처리되므로 여기서는 제외
        if (ex instanceof ResourceNotFoundException) {
            return ResponseEntity.status(404).body(ApiResponse.fail(ex.getMessage()));
        }
        return ResponseEntity.status(500).body(ApiResponse.fail("서버 내부 오류가 발생했습니다: " + ex.getMessage()));
    }
}
