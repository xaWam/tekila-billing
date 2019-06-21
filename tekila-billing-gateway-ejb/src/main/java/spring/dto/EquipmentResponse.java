package spring.dto;

import com.jaravir.tekila.module.service.ServiceSettingType;
import com.jaravir.tekila.module.subscription.persistence.entity.Subscription;
import com.jaravir.tekila.module.subscription.persistence.entity.SubscriptionStatus;

public class EquipmentResponse {
    public final Long id;
    public final String equipment;
    public final String provider;
    public final String agreement;
    public final String subscriberName;
    public final String tariffPlan;
    public final SubscriptionStatus sbnStatus;
    public final String subscriberAddress;
    public final String phones;

    private EquipmentResponse(Subscription subscription) {
        this.id = subscription.getId();
        if (subscription.getSettingByType(ServiceSettingType.TV_EQUIPMENT) != null) {
            this.equipment = subscription.getSettingByType(ServiceSettingType.TV_EQUIPMENT).getValue();
        }else{this.equipment = "Empty"; }

        if (subscription.getSettingByType(ServiceSettingType.TV_EQUIPMENT) != null) {
            this.provider = subscription.getSettingByType(ServiceSettingType.TV_EQUIPMENT).getProperties().getProvider().getName();
        }else{

            this.provider = "UNKNOWN";
        }
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

    public static EquipmentResponse from(Subscription subscription) {
        return new EquipmentResponse(subscription);
    }
}
