package spring.dto;

import com.jaravir.tekila.module.store.scratchcard.persistence.entity.ScratchCardSession;
import java.util.Date;

public class ScratchCardResponse {
    public final Long id;
    public final String agreement;
    public final Long amount;
    public final int wrongAttemp;
    public final String service;
    public final Date date;

    private ScratchCardResponse(final ScratchCardSession session) {
        this.id = session.getId();
        this.agreement = session.getSubscription().getAgreement();
        this.amount = (session.getScratchCard() != null) ? session.getScratchCard().getAmount() : null;
        this.wrongAttemp = session.getwrongAttempt();
        this.service = (session.getSubscription().getService() != null) ? session.getSubscription().getService().getName() : "";
        this.date = session.getLastUpdateDateAsDate();
    }

    public static ScratchCardResponse from(final ScratchCardSession session) {
        return new ScratchCardResponse(session);
    }
}
