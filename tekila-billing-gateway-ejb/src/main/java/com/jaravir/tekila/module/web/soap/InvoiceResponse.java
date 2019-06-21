package com.jaravir.tekila.module.web.soap;

import com.jaravir.tekila.module.accounting.AccountingStatus;
import com.jaravir.tekila.module.accounting.InvoiceState;
import com.jaravir.tekila.module.accounting.entity.Charge;
import com.jaravir.tekila.module.accounting.entity.Invoice;
import com.jaravir.tekila.module.accounting.entity.Payment;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import javax.xml.bind.annotation.XmlElement;

/**
 *
 * @author khsadigov
 */
public class InvoiceResponse {

    @XmlElement(nillable = true)
    public InvoiceState state;
    @XmlElement(nillable = true)
    public Long sumPaid;
    @XmlElement(nillable = true)
    public Long sumCharged;
    @XmlElement(nillable = true)
    public Long balance;
    @XmlElement(nillable = true)
    public Date creationDate;
    @XmlElement(nillable = true)
    public Date closeDate;
    @XmlElement(nillable = true)
    public Date lastChargeDate;
    @XmlElement(nillable = true)
    public Date lastPaymentDate;
    @XmlElement(nillable = true)
    public List<ChargeResponse> charges;
    @XmlElement(nillable = true)
    public List<PaymentResponse> payments;
    @XmlElement(nillable = true)
    public String subscription;
    @XmlElement(nillable = true)
    public String user;
    @XmlElement(nillable = true)
    public AccountingStatus status;

    public InvoiceResponse() {
    }

    public InvoiceResponse(Invoice entity) {
        this.state = entity.getState();
        this.sumPaid = entity.getSumPaid();
        this.sumCharged = entity.getSumCharged();
        this.balance = entity.getBalance();
        this.creationDate = entity.getCreationDate();
        this.closeDate = entity.getCloseDate() != null ? entity.getCloseDate().toDate() : null;
        this.lastChargeDate = entity.getLastChargeDate() != null ? entity.getLastChargeDate().toDate() : null;
        this.lastPaymentDate = entity.getLastPaymentDate() != null ? entity.getLastPaymentDate().toDate() : null;

        this.charges = new ArrayList<>();
        for (Charge c : entity.getCharges()) {
            this.charges.add(new ChargeResponse(c));
        }

        this.payments = new ArrayList<>();
        for (Payment c : entity.getPayments()) {
            this.payments.add(new PaymentResponse(c));
        }

        if (entity.getSubscription() != null) {
            this.subscription = entity.getSubscription().getAgreement();
        }
        try {
            this.user = entity.getUser().getUserName();
        } catch (NullPointerException n) {
            this.user = null;
        }

        this.status = entity.getStatus();
    }
}
