package spring.dto;

import com.jaravir.tekila.module.service.model.BillingPrinciple;
import com.jaravir.tekila.module.subscription.persistence.entity.SubscriberDetails;
import com.jaravir.tekila.module.subscription.persistence.entity.SubscriberType;
import com.jaravir.tekila.module.subscription.persistence.entity.Subscription;
import com.jaravir.tekila.module.subscription.persistence.entity.SubscriptionStatus;
import org.joda.time.DateTime;

/**
 * Created by KamranMa on 25.12.2017.
 */
public class SubscriptionResponse {
    public final long id;
    public final String agreement;
    public final String identifier;
    public final String subscriber;
    public final String registration;
    public final String tariffPlan;
    public final String billingModel;
    public final String balance;
    public final String vasList;
    public final SubscriptionStatus status;
    public final String fttbUsername;
    public final String campaigns;
    public final long providerId;
    public final String provider;
    public final DateTime expires;

    private SubscriptionResponse(Subscription subscription) {
        this.id = subscription.getId();
        this.agreement = subscription.getAgreement();
        this.identifier = subscription.getIdentifier();

        SubscriberDetails subscriberDetails = subscription.getSubscriber().getDetails();
        if (subscriberDetails.getType().equals(SubscriberType.INDV)) {
            this.subscriber = new StringBuilder().append(subscriberDetails.getFirstName()).
                    append(' ').append(subscriberDetails.getSurname() != null ? subscriberDetails.getSurname() : "").
                    append(' ').append(subscriberDetails.getMiddleName() != null ? subscriberDetails.getMiddleName() : "").toString();
            this.registration = subscriberDetails.getPassportNumber();
        } else {
            this.subscriber = subscriberDetails.getCompanyName();
            this.registration = "";
        }

        this.tariffPlan = subscription.getService().getName();
        this.billingModel = subscription.getBillingModel().getName();
        this.balance = subscription.getBalanceAsText();
        this.vasList = subscription.getSbnVASAsText();
        this.status = subscription.getStatus();
        this.fttbUsername = subscription.findRadiusUserName(subscription);
        this.campaigns = subscription.getActiveCampaignsStr();
        this.providerId = subscription.getService().getProvider().getId();
        this.provider = subscription.getService().getProvider().getName();
        this.expires = subscription.getBillingModel().getPrinciple().equals(BillingPrinciple.GRACE)
                ? subscription.getExpirationDateWithGracePeriod()
                : subscription.getExpirationDate();
    }

    public static SubscriptionResponse from(Subscription subscription) {
        return new SubscriptionResponse(subscription);
    }
}
