package com.jaravir.tekila.module.web.soap;

import com.jaravir.tekila.module.store.street.StreetPersistenceFacade;
import com.jaravir.tekila.module.subscription.persistence.entity.Streets;
import com.jaravir.tekila.module.subscription.persistence.entity.SubscriptionDetails;

import javax.ejb.EJB;
import javax.ejb.Stateless;

/**
 * Created by kmaharov on 22.09.2016.
 */
@Stateless
public class SubscriptionDetailsResponseBuilder {
    @EJB
    private StreetPersistenceFacade streetPersistenceFacade;

    public SubscriptionDetailsResponseBuilder() {
    }

    public SubscriptionDetailsResponse build(SubscriptionDetails details) {
        SubscriptionDetailsResponse detailsResponse = new SubscriptionDetailsResponse(details);
        Streets strObj;

        try {
            Long streetId = Long.parseLong(detailsResponse.getStreet());
            strObj = streetPersistenceFacade.find(streetId);
            detailsResponse.setStreet(strObj.getName());
        } catch (Exception e) {
        }
        return detailsResponse;
    }
}