package com.telegrambot.expection;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UserException extends Exception {
    private int code;
    private String message;


    public UserException(String message) {
        this.code = 400;
        this.message = message;
    }
}
