package spring.dto;

import com.jaravir.tekila.module.subscription.persistence.entity.SubscriptionStatus;

/**
 * Created by KamranMa on 25.12.2017.
 */
public class SubscriptionStatusResponse {
    public final String name;
    public final Integer id;

    private SubscriptionStatusResponse(final SubscriptionStatus status) {
        this.name = status.name();
        this.id = status.STATUS;
    }

    public static SubscriptionStatusResponse from(final SubscriptionStatus status) {
        return new SubscriptionStatusResponse(status);
    }
}
