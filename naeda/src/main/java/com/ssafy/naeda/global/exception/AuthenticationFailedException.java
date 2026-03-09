package com.ssafy.naeda.global.exception;

public class AuthenticationFailedException extends RuntimeException{

    public AuthenticationFailedException(String message){
        super(message);
    }
}
