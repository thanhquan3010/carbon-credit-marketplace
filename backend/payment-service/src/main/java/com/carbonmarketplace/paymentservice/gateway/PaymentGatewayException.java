package com.carbonmarketplace.paymentservice.gateway;

public class PaymentGatewayException extends Exception {
    
    private final String errorCode;
    private final String gatewayResponse;
    
    public PaymentGatewayException(String message) {
        super(message);
        this.errorCode = null;
        this.gatewayResponse = null;
    }
    
    public PaymentGatewayException(String message, Throwable cause) {
        super(message, cause);
        this.errorCode = null;
        this.gatewayResponse = null;
    }
    
    public PaymentGatewayException(String message, String errorCode, String gatewayResponse) {
        super(message);
        this.errorCode = errorCode;
        this.gatewayResponse = gatewayResponse;
    }
    
    public PaymentGatewayException(String message, String errorCode, String gatewayResponse, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
        this.gatewayResponse = gatewayResponse;
    }
    
    public String getErrorCode() {
        return errorCode;
    }
    
    public String getGatewayResponse() {
        return gatewayResponse;
    }
}
