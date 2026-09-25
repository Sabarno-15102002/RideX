package com.ridex.payment.exception;

public class PaymentProviderException extends RuntimeException{

    public PaymentProviderException(String message){
        super(message);
    }
}
