package com.example.currentlimiting.exception;

/**
 * @author dengzhiming
 * @date 2020/2/29 16:50
 */
public class LimitAccessException extends RuntimeException {
    public LimitAccessException(String message){
        super(message);
    }
}
