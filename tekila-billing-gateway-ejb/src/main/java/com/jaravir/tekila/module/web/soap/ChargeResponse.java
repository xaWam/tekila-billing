package com.jaravir.tekila.module.web.soap;

import com.jaravir.tekila.module.accounting.AccountingStatus;
import com.jaravir.tekila.module.accounting.entity.Charge;
import java.util.Date;
import javax.xml.bind.annotation.XmlElement;

/**
 *
 * @author khsadigov
 */
public class ChargeResponse {

    @XmlElement(nillable = true)
    public String service;
    @XmlElement(nillable = true)
    public long amount;
    @XmlElement(nillable = true)
    public Date datetime;
    @XmlElement(nillable = true)
    public String dsc;
    @XmlElement(nillable = true)
    public String user;
    @XmlElement(nillable = true)
    public Long user_id;
    @XmlElement(nillable = true)
    public String vas;
    @XmlElement(nillable = true)
    public String subscription;
    @XmlElement(nillable = true)
    public AccountingStatus status;

    public ChargeResponse() {
    }

    public ChargeResponse(Charge entity) {

        this.service = entity.getService().getName();
        this.amount = entity.getAmount();
        this.datetime = entity.getDatetime() != null ? entity.getDatetime().toDate() : null;
        this.dsc = entity.getDsc();
        try {
            this.user = entity.getUser().getUserName();
        } catch (NullPointerException n) {
            this.user = null;
        }
        this.user_id = entity.getUser_id();

        try {
            this.vas = entity.getVas().getName();
        } catch (NullPointerException n) {
            this.vas = null;
        }

        if (entity.getSubscription() != null) {
            this.subscription = entity.getSubscription().getAgreement();
        }
        this.status = entity.getStatus();
    }
}
