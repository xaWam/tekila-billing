package com.jaravir.tekila.tools;

import java.io.Serializable;
import java.util.Objects;

/**
 * @author ElmarMa on 5/15/2018
 */
public class WrapperAgreementChangeBatch implements Serializable {


    public WrapperAgreementChangeBatch() {
    }

    private String name;
    private String surname;
    private String fatherName;
    private String mobile;
    private String agreement;
    private String oldAgreement;
    private String serviceName;
    private Boolean hasPromo;
    private String pinCode;
    private String idSerialNumber;
    private String homeNumber;
    private String message;
    private String address;

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getOldAgreement() {
        return oldAgreement;
    }

    public void setOldAgreement(String oldAgreement) {
        this.oldAgreement = oldAgreement;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getSurname() {
        return surname;
    }

    public void setSurname(String surname) {
        this.surname = surname;
    }

    public String getFatherName() {
        return fatherName;
    }

    public void setFatherName(String fatherName) {
        this.fatherName = fatherName;
    }

    public String getMobile() {
        return mobile;
    }

    public void setMobile(String mobile) {
        this.mobile = mobile;
    }

    public String getAgreement() {
        return agreement;
    }

    public void setAgreement(String agreement) {
        this.agreement = agreement;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getServiceName() {
        return serviceName;
    }

    public void setServiceName(String serviceName) {
        this.serviceName = serviceName;
    }

    public Boolean getHasPromo() {
        return hasPromo;
    }

    public void setHasPromo(Boolean hasPromo) {
        this.hasPromo = hasPromo;
    }

    public String getPinCode() {
        return pinCode;
    }

    public void setPinCode(String pinCode) {
        this.pinCode = pinCode;
    }

    public String getIdSerialNumber() {
        return idSerialNumber;
    }

    public void setIdSerialNumber(String idSerialNumber) {
        this.idSerialNumber = idSerialNumber;
    }

    public String getHomeNumber() {
        return homeNumber;
    }

    public void setHomeNumber(String homeNumber) {
        this.homeNumber = homeNumber;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof WrapperAgreementChangeBatch)) return false;
        WrapperAgreementChangeBatch that = (WrapperAgreementChangeBatch) o;
        return Objects.equals(agreement, that.agreement) &&
                Objects.equals(oldAgreement, that.oldAgreement);
    }

    @Override
    public int hashCode() {
        return Objects.hash(agreement, oldAgreement);
    }


    @Override
    public String toString() {
        return "WrapperAgreementChangeBatch{" +
                "name='" + name + '\'' +
                ", surname='" + surname + '\'' +
                ", fatherName='" + fatherName + '\'' +
                ", mobile='" + mobile + '\'' +
                ", agreement='" + agreement + '\'' +
                ", oldAgreement='" + oldAgreement + '\'' +
                ", serviceName='" + serviceName + '\'' +
                ", hasPromo=" + hasPromo +
                ", pinCode='" + pinCode + '\'' +
                ", idSerialNumber='" + idSerialNumber + '\'' +
                ", homeNumber='" + homeNumber + '\'' +
                ", message='" + message + '\'' +
                ", address='" + address + '\'' +
                '}';
    }
}
