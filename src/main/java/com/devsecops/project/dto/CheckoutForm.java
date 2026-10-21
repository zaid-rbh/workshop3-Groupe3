package com.devsecops.project.dto;

import java.math.BigDecimal;

/** Checkout form. VULNERABILITY #15: the total comes from the browser (hidden field). */
public class CheckoutForm {

    private String shippingAddress;
    private String cardNumber;
    private BigDecimal total;

    public String getShippingAddress() { return shippingAddress; }
    public void setShippingAddress(String shippingAddress) { this.shippingAddress = shippingAddress; }
    public String getCardNumber() { return cardNumber; }
    public void setCardNumber(String cardNumber) { this.cardNumber = cardNumber; }
    public BigDecimal getTotal() { return total; }
    public void setTotal(BigDecimal total) { this.total = total; }
}
