package spring.controller;

import com.fasterxml.jackson.annotation.JsonView;
import com.jaravir.tekila.jsonview.JsonViews;
import com.jaravir.tekila.module.subscription.persistence.entity.Subscriber;
import com.jaravir.tekila.module.subscription.persistence.entity.Subscription;
import com.jaravir.tekila.module.subscription.persistence.management.SubscriberPersistenceFacade;
import com.jaravir.tekila.module.subscription.persistence.management.SubscriptionPersistenceFacade;
import org.apache.log4j.Logger;
import org.springframework.web.bind.annotation.*;
import spring.Filters;
import spring.dto.SubscriberResponse;

import javax.ejb.EJB;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Created by KamranMa on 25.12.2017.
 */
@RestController
public class SubscriberController {
    private static final Logger log = Logger.getLogger(SubscriberController.class);

    @EJB(mappedName = "java:global/tekila-billing-gateway-ear-0.0.1/tekila-billing-gateway-ejb-0.0.1/SubscriberPersistenceFacade")
    private SubscriberPersistenceFacade subscriberPersistenceFacade;
    @EJB(mappedName = "java:global/tekila-billing-gateway-ear-0.0.1/tekila-billing-gateway-ejb-0.0.1/SubscriptionPersistenceFacade")
    private SubscriptionPersistenceFacade subscriptionPersistenceFacade;

    @RequestMapping(method = RequestMethod.GET, value = "/search/subscribers")
    public List<SubscriberResponse> search(
            @RequestParam(required = false) String subscriberId,
            @RequestParam(required = false) String passportNumber,
            @RequestParam(required = false) String subscriberName,
            @RequestParam(required = false) String subscriberSurname,
            @RequestParam Integer pageId,
            @RequestParam(required = false, defaultValue = "10") Integer rowsPerPage) {
        Filters filters = new Filters();
        filters.clearFilters();
        log.debug("-----------search() start----------");
        log.debug(String.format("search() method. subscriberId = %s, passportNumber = %s, subscriberName = %s, subscriberSurname = %s, pageId = %d, rowsPerPage = %d",
                subscriberId, passportNumber, subscriberName, subscriberSurname, pageId, rowsPerPage));

        if (subscriberId != null && !subscriberId.isEmpty()) {
            filters.addFilter(SubscriberPersistenceFacade.Filter.ID, subscriberId);
        }

        if (passportNumber != null && !passportNumber.isEmpty()) {
            filters.addFilter(SubscriberPersistenceFacade.Filter.PASSPORTNUMBER, passportNumber);
        }

        if (subscriberName != null && !subscriberName.isEmpty()) {
            filters.addFilter(SubscriberPersistenceFacade.Filter.NAME, subscriberName);
        }

        if (subscriberSurname != null && !subscriberSurname.isEmpty()) {
            filters.addFilter(SubscriberPersistenceFacade.Filter.SURNAME, subscriberSurname);
        }

        log.debug("-----------search end()----------");
        return subscriberPersistenceFacade.findAllPaginated((pageId - 1) * rowsPerPage, rowsPerPage, filters).stream().
                map(subscriber -> {
                    List<Subscription> subs = subscriptionPersistenceFacade.findBySubscriberId(subscriber.getId());
                    subscriber.setSubscriptions(subs);
                    return SubscriberResponse.from(subscriber);
                }).collect(Collectors.toList());
    }

    @RequestMapping(method = RequestMethod.GET, value = "/search/subscribers/count")
    public long count(
            @RequestParam(required = false) String subscriberId,
            @RequestParam(required = false) String passportNumber,
            @RequestParam(required = false) String subscriberName,
            @RequestParam(required = false) String subscriberSurname
    ) {
        Filters filters = new Filters();
        filters.clearFilters();
        log.debug("-----------count() start----------");
        log.debug(String.format("count() method. subscriberId = %s, passportNumber = %s, subscriberName = %s, subscriberSurname = %s",
                subscriberId, passportNumber, subscriberName, subscriberSurname));

        if (subscriberId != null && !subscriberId.isEmpty()) {
            filters.addFilter(SubscriberPersistenceFacade.Filter.ID, subscriberId);
        }

        if (passportNumber != null && !passportNumber.isEmpty()) {
            filters.addFilter(SubscriberPersistenceFacade.Filter.PASSPORTNUMBER, passportNumber);
        }

        if (subscriberName != null && !subscriberName.isEmpty()) {
            filters.addFilter(SubscriberPersistenceFacade.Filter.NAME, subscriberName);
        }

        if (subscriberSurname != null && !subscriberSurname.isEmpty()) {
            filters.addFilter(SubscriberPersistenceFacade.Filter.SURNAME, subscriberSurname);
        }

        long cnt = subscriberPersistenceFacade.count(filters);
        log.debug(String.format("count() method. count = %d", cnt));
        log.debug("-----------count end()----------");
        return cnt;
    }

    @JsonView(JsonViews.Subscriber.class)
    @RequestMapping(method = RequestMethod.GET, value = "/subscribers/subscription/{id}")
    public Subscriber getSubscription(@PathVariable Long id) {
        return subscriptionPersistenceFacade.find(id).getSubscriber();
    }
}
