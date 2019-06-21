package spring.dto;

import com.jaravir.tekila.module.accounting.entity.Payment;

import java.util.Date;

/**
 * Created by KamranMa on 25.12.2017.
 */
public class PaymentResponse {
    public final Long id;
    public final String chequeId;
    public final String rrn;
    public final String agreement;
    public final Double amount;
    public final int status;
    public final String service;
    public final Date date;

    private PaymentResponse(Payment payment) {
        this.id = payment.getId();
        this.chequeId = payment.getChequeID() != null ? payment.getChequeID() : "";
        this.rrn = payment.getRrn() != null ? payment.getRrn() : "";
        this.agreement = payment.getContract();
        this.amount = payment.getAmount();
        this.status = payment.getStatus();
        this.service = (payment.getAccount() != null) ? payment.getAccount().getService().getName() : null;
        this.date = (payment.getFd() != null) ? payment.getFd() : payment.getTimestamp().getTime();
    }

    public static PaymentResponse from(Payment payment) {
        return new PaymentResponse(payment);
    }
}
