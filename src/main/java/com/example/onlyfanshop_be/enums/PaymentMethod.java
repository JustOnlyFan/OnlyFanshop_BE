package com.example.onlyfanshop_be.enums;

public enum PaymentMethod {
    CASH_ON_DELIVERY("Cash on Delivery", "COD"),
    CREDIT_CARD("Credit Card", "CREDIT"),
    DEBIT_CARD("Debit Card", "DEBIT"),
    BANK_TRANSFER("Bank Transfer", "TRANSFER"),
    E_WALLET("E-Wallet", "EWALLET"),
    PAYPAL("PayPal", "PAYPAL");

    private final String displayName;
    private final String code;

    PaymentMethod(String displayName, String code) {
        this.displayName = displayName;
        this.code = code;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getCode() {
        return code;
    }
}