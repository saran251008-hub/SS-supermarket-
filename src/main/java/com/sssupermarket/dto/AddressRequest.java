package com.sssupermarket.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public class AddressRequest {
    @NotBlank @Size(max = 100) private String fullName;
    @NotBlank @Pattern(regexp = "\\d{10}") private String mobileNumber;
    @NotBlank @Size(max = 250) private String addressLine1;
    @Size(max = 250) private String addressLine2;
    @NotBlank @Size(max = 100) private String city;
    @NotBlank @Size(max = 100) private String state;
    @NotBlank @Pattern(regexp = "\\d{5,10}") private String pincode;
    @NotBlank @Size(max = 80) private String country;
    private boolean defaultAddress;
    public String getFullName(){return fullName;} public void setFullName(String v){fullName=v;}
    public String getMobileNumber(){return mobileNumber;} public void setMobileNumber(String v){mobileNumber=v;}
    public String getAddressLine1(){return addressLine1;} public void setAddressLine1(String v){addressLine1=v;}
    public String getAddressLine2(){return addressLine2;} public void setAddressLine2(String v){addressLine2=v;}
    public String getCity(){return city;} public void setCity(String v){city=v;}
    public String getState(){return state;} public void setState(String v){state=v;}
    public String getPincode(){return pincode;} public void setPincode(String v){pincode=v;}
    public String getCountry(){return country;} public void setCountry(String v){country=v;}
    public boolean isDefaultAddress(){return defaultAddress;} public void setDefaultAddress(boolean v){defaultAddress=v;}
}
