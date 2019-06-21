package spring.dto;

import com.jaravir.tekila.module.subscription.persistence.entity.Subscription;
import com.jaravir.tekila.module.subscription.persistence.entity.SubscriptionStatus;

public class IpResponse {
    public final Long id;
    public final String staticIp;
    public final String agreement;
    public final String subscriberName;
    public final String tariffPlan;
    public final SubscriptionStatus sbnStatus;
    public final String subscriberAddress;
    public final String phones;

    private IpResponse(Subscription subscription, String ip) {
        this.id = subscription.getId();
        this.staticIp = subscription.getIP(ip);
        this.agreement = subscription.getAgreement();
        this.subscriberName = subscription.getSubscriber().getDetails().getFirstName() + " " +
                subscription.getSubscriber().getDetails().getSurname() + " " +
                subscription.getSubscriber().getDetails().getMiddleName();
        this.tariffPlan = subscription.getService().getName();
        this.sbnStatus = subscription.getStatus();
        this.subscriberAddress = Utils.merge(
                subscription.getSubscriber().getDetails().getCity(),
                subscription.getSubscriber().getDetails().getStreet(),
                subscription.getSubscriber().getDetails().getBuilding(),
                subscription.getSubscriber().getDetails().getApartment());
        this.phones = Utils.merge(
                subscription.getSubscriber().getDetails().getPhoneMobile(),
                subscription.getSubscriber().getDetails().getPhoneMobileAlt(),
                subscription.getSubscriber().getDetails().getPhoneLandline());
    }

    public static IpResponse from(Subscription subscription, String ip) {
        return new IpResponse(subscription, ip);
    }
}
