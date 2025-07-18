package com.xielaoban.aiagent.execption;

import lombok.Getter;

@Getter
public class BussinesException extends RuntimeException {

    /**
     * 错误码
     */
    private final int code;

    public BussinesException(int code, String message) {
        super(message);
        this.code = code;
    }

    public BussinesException(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.code = errorCode.getCode();
    }

    public BussinesException(ErrorCode errorCode, String message) {
        super(message);
        this.code = errorCode.getCode();
    }

}

