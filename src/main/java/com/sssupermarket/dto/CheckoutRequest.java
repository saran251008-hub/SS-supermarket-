package com.sssupermarket.dto;

import com.sssupermarket.entity.PaymentMethod;
import jakarta.validation.constraints.NotNull;

public class CheckoutRequest {
    @NotNull private Long addressId;
    @NotNull private PaymentMethod paymentMethod;
    public Long getAddressId(){return addressId;} public void setAddressId(Long v){addressId=v;}
    public PaymentMethod getPaymentMethod(){return paymentMethod;} public void setPaymentMethod(PaymentMethod v){paymentMethod=v;}
}
