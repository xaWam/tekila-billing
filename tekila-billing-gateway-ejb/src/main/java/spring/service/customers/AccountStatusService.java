
package spring.service.customers;

import com.jaravir.tekila.module.service.persistence.manager.ServicePersistenceFacade;
import com.jaravir.tekila.module.subscription.persistence.entity.Subscription;
import com.jaravir.tekila.module.subscription.persistence.entity.SubscriptionStatus;
import com.jaravir.tekila.module.subscription.persistence.management.SubscriptionPersistenceFacade;
import com.jaravir.tekila.provision.broadband.devices.manager.Providers;
import org.apache.log4j.Logger;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.springframework.stereotype.Service;
import spring.controller.SubscriptionController;
import spring.exceptions.CustomerOperationException;
import spring.util.Constants;

import javax.annotation.Resource;
import javax.ejb.EJB;
import javax.ejb.SessionContext;

import static spring.util.Constants.INJECTION_POINT;

/**
 * @author ElmarMa on 3/16/2018
 */
@Service
public class AccountStatusService {
    private static final Logger logger = Logger.getLogger(AccountStatusService.class);

    @EJB(mappedName = INJECTION_POINT + "SubscriptionPersistenceFacade")
    private SubscriptionPersistenceFacade subscriptionFacade;



    public void changeStatus(Long subscriptionId, Integer status, String startDateAsText, String endDateAsText, boolean applyReversal) {
        Subscription subscription = null;

        SubscriptionStatus subscriptionStatus = null;

        DateTime startDate = null;

        DateTime endDate = null;

        try {

            subscriptionStatus = SubscriptionStatus.convertFromCode(status);
            if (subscriptionStatus == null)
                throw new RuntimeException("can not identify target status of subscription");

            logger.debug(subscriptionStatus);

            subscription = subscriptionFacade.find(subscriptionId);
            if (subscription == null)
                throw new RuntimeException("can not find subscription");

            logger.debug(subscription.getId());

            // make additional check
            check(subscription, subscriptionStatus);

            DateTimeFormatter formatter = DateTimeFormat.forPattern("dd/MM/yyyy HH:mm:ss");
            startDate = formatter.parseDateTime(startDateAsText);
            if (applyReversal)
                endDate = formatter.parseDateTime(endDateAsText);



            subscription = subscriptionFacade.addChangeStatusJob(subscription, subscriptionStatus, startDate, endDate);
            logger.debug("Successfuly changed");
            logger.debug(subscription.getStatus());
        } catch (Exception e) {
            throw new CustomerOperationException(e.getMessage(), e);
        }

    }

    public void check(Subscription subscription, SubscriptionStatus targetStatus) {

        if (targetStatus == SubscriptionStatus.BLOCKED)
            throw new CustomerOperationException("This status is not available for manual assignation");

        if (subscription.getService().findRule(subscription.getStatus(), targetStatus) == null)
            throw new CustomerOperationException("no rule founds for from:" + subscription.getStatus() +
                    " to:" + targetStatus);

        if (targetStatus == SubscriptionStatus.FINAL)
            if (subscription.getBalance().getRealBalance() < 0 && subscription.getService().getProvider().getId() != Providers.UNINET.getId())
                throw new CustomerOperationException("Subscription cannot be FINALIZED before all debt is paid up");

    }


}
