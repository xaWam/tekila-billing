package com.jaravir.tekila.module.web.soap;

import com.jaravir.tekila.module.accounting.PaymentPurpose;
import com.jaravir.tekila.module.accounting.entity.Payment;
import com.jaravir.tekila.module.accounting.entity.PaymentOption;

import java.util.Calendar;
import java.util.Date;
import javax.xml.bind.annotation.XmlElement;

/**
 *
 * @author khsadigov
 */
public class PaymentResponse {

    @XmlElement(nillable = true)
    public String rrn;
    @XmlElement(nillable = true)
    public String rid;
    @XmlElement(nillable = true)
    public String sessid;
    @XmlElement(nillable = true)
    public String contract;
    @XmlElement(nillable = true)
    public String service;
    @XmlElement(nillable = true)
    public Double amount;
    @XmlElement(nillable = true)
    public String currency;
    @XmlElement(nillable = true)
    public String datetime;
    @XmlElement(nillable = true)
    public String dsc;
    @XmlElement(nillable = true)
    public String user;
    @XmlElement(nillable = true)
    public int status;
    @XmlElement(nillable = true)
    public String ext_user;
    @XmlElement(nillable = true)
    public int processed;
    @XmlElement(nillable = true)
    public long user_id;
    @XmlElement(nillable = true)
    public PaymentOption method;
    @XmlElement(nillable = true)
    public String chequeID;
    @XmlElement(nillable = true)
    public Date fd;
    @XmlElement(nillable = true)
    public Long subscriber_id;
    @XmlElement(nillable = true)
    public PaymentPurpose purpose;
    @XmlElement(nillable = true)
    public Date timestampDate;

    public PaymentResponse() {
    }

    public PaymentResponse(Payment entity) {

        this.rrn = entity.getRrn();
        this.rid = entity.getRid();
        this.sessid = entity.getSessid();
        this.contract = entity.getContract();
        this.service = entity.getService().getName();
        this.amount = entity.getAmount();
        this.currency = entity.getCurrency();
        this.datetime = entity.getDateTime();
        this.dsc = entity.getDsc();
        this.processed = entity.getProcessed();
        this.user_id = entity.getUser_id();
        this.method = entity.getMethod();
        this.chequeID = entity.getChequeID();
        this.fd = entity.getFd();
        this.subscriber_id = entity.getSubscriber_id();
        this.purpose = entity.getPurpose();
        try {
            this.user = entity.getUser().getUserName();
        } catch (NullPointerException n) {
            this.user = null;
        }
        this.status = entity.getStatus();

        try {
            this.ext_user = entity.getExtUser().getUsername();
        } catch (NullPointerException n) {
            this.ext_user = null;
        }
        this.status = entity.getStatus();
        Calendar calendar = entity.getTimestamp();
        if (calendar != null) {
            this.timestampDate = calendar.getTime();
        }
    }
}
