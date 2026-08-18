package com.ai.base.starter.handler;

import com.ai.base.application.common.BizException;
import com.ai.base.starter.common.Result;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.MessageSource;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Locale;

@Slf4j
@RestControllerAdvice
@RequiredArgsConstructor
public class GlobalExceptionHandler {
    private final MessageSource messageSource;

    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ExceptionHandler(BizException.class)
    public Result<Void> handleBizException(BizException ex, Locale locale) {
        String message = messageSource.getMessage(ex.getErrorCode().getMessageKey(), ex.getArgs(),
                ex.getErrorCode().getDefaultMessage(), locale);
        return Result.error(ex.getErrorCode(), message);
    }

    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    @ExceptionHandler(Exception.class)
    public Result<Void> handleException(Exception ex) {
        log.error("[系统异常]", ex);
        return Result.error("系统异常，请稍后重试");
    }
}

