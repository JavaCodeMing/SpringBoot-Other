package com.example.currentlimiting.handler;

import com.example.currentlimiting.domain.ResponseBo;
import com.example.currentlimiting.exception.LimitAccessException;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;

/**
 * @author dengzhiming
 * @date 2020/2/29 18:14
 */
@ControllerAdvice
@Order(Ordered.HIGHEST_PRECEDENCE)
public class GlobalExceptionHandler {
    @ExceptionHandler(LimitAccessException.class)
    @ResponseBody
    public ResponseBo handleLimitAccessException(LimitAccessException e){
        return ResponseBo.overClocking(e.getMessage());
    }
}
