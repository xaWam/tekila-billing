package com.jaravir.tekila.module.web.soap;

import com.jaravir.tekila.module.accounting.entity.TaxationCategory;
import com.jaravir.tekila.module.subscription.persistence.entity.Subscriber;
import com.jaravir.tekila.module.subscription.persistence.entity.SubscriberFunctionalCategory;
import com.jaravir.tekila.module.subscription.persistence.entity.SubscriberLifeCycleType;
import com.jaravir.tekila.module.subscription.persistence.entity.Subscription;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import javax.xml.bind.annotation.XmlElement;

/**
 *
 * @author khsadigov
 */
public class SubscriberResponse extends BaseResponse {

    @XmlElement(nillable = true)
    public Long master_account;
    @XmlElement(nillable = true)
    public SubscriberDetailsResponse details;
    @XmlElement(nillable = true)
    public List<SubscriptionResponse> subscriptions;
    @XmlElement(nillable = true)
    public boolean isBilledByLifeCycle;
    @XmlElement(nillable = true)
    public SubscriberLifeCycleType lifeCycle;
    @XmlElement(nillable = true)
    public Date lastPaymentDate;
    @XmlElement(nillable = true)
    public Date creationDate;
    @XmlElement(nillable = true)
    public TaxationCategory taxCategory;
    @XmlElement(nillable = true)
    public SubscriberFunctionalCategory fnCategory;

    public SubscriberResponse() {
    }

    public SubscriberResponse(Subscriber entity) {
        this.master_account = entity.getMasterAccount();
        this.details = new SubscriberDetailsResponse(entity.getDetails());

        this.subscriptions = new ArrayList<>();
        for (Subscription sub : entity.getSubscriptions()) {
            this.subscriptions.add(new SubscriptionResponse(sub));
        }

        this.isBilledByLifeCycle = entity.getBilledByLifeCycle();
        this.lifeCycle = entity.getLifeCycle();
        this.lastPaymentDate = entity.getLastPaymentDate() != null ? entity.getLastPaymentDate().toDate() : null;
        this.creationDate = entity.getCreationDate();
        this.taxCategory = entity.getTaxCategory();
        this.fnCategory = entity.getFnCategory();
    }

}
