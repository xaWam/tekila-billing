package com.jaravir.tekila.tools;

import com.jaravir.tekila.module.subscription.persistence.entity.Subscription;

import javax.xml.bind.annotation.XmlRootElement;


@XmlRootElement
public class WrapperBatch {


    // 0 - data not found, 1 - ok, 2 - error
    private int status;
    private String batchAgreement;
    private Integer batchStatus;
    private Subscription subscription;
    private String msg;

    public WrapperBatch(){}

    public int getStatus() {
        return status;
    }

    public void setStatus(int status) {
        this.status = status;
    }

    public String getBatchAgreement() {
        return batchAgreement;
    }

    public void setBatchAgreement(String batchAgreement) {
        this.batchAgreement = batchAgreement;
    }

    public Integer getBatchStatus() {
        return batchStatus;
    }

    public void setBatchStatus(Integer batchStatus) {
        this.batchStatus = batchStatus;
    }

    public Subscription getSubscription() {
        return subscription;
    }

    public void setSubscription(Subscription subscription) {
        this.subscription = subscription;
    }

    public String getMsg() {
        return msg;
    }

    public void setMsg(String msg) {
        this.msg = msg;
    }
}
