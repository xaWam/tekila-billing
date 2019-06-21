package com.jaravir.tekila.module.campaign;

import com.jaravir.tekila.base.filter.Filterable;
import com.jaravir.tekila.base.filter.MatchingOperation;
import com.jaravir.tekila.base.persistence.facade.AbstractPersistenceFacade;
import com.jaravir.tekila.engines.EngineFactory;
import com.jaravir.tekila.module.accounting.entity.Payment;
import com.jaravir.tekila.module.accounting.manager.PaymentPersistenceFacade;
import com.jaravir.tekila.module.service.ResourceBucketType;
import com.jaravir.tekila.module.service.ServiceType;
import com.jaravir.tekila.module.service.entity.ResourceBucket;
import com.jaravir.tekila.module.service.entity.Service;
import com.jaravir.tekila.module.service.entity.ValueAddedService;
import com.jaravir.tekila.module.subscription.persistence.entity.Subscription;
import com.jaravir.tekila.module.subscription.persistence.entity.SubscriptionStatus;
import com.jaravir.tekila.module.subscription.persistence.management.SubscriptionPersistenceFacade;
import com.jaravir.tekila.module.system.SystemEvent;
import com.jaravir.tekila.module.system.log.SystemLogger;
import com.jaravir.tekila.provision.ProvisioningFactory;
import org.apache.log4j.Logger;

import javax.ejb.*;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * Created by sajabrayilov on 5/29/2015.
 */
@Stateless
public class CampaignPersistenceFacade extends AbstractPersistenceFacade<Campaign> {
    @PersistenceContext
    private EntityManager entityManager;
    @EJB
    private PaymentPersistenceFacade paymentFacade;
    @EJB
    private SubscriptionPersistenceFacade subscriptionFacade;
    @EJB
    private SystemLogger systemLogger;
    @EJB
    private CampaignRegisterPersistenceFacade campaignRegisterFacade;
    @EJB
    private EngineFactory provisioningFactory;

    private final static Logger log = Logger.getLogger(CampaignPersistenceFacade.class);

    public CampaignPersistenceFacade() {
        super(Campaign.class);
    }

    public enum Filter implements Filterable {
        NAME("name");
        private final String field;
        private MatchingOperation operation;

        Filter(final String field) {
            this.field = field;
        }

        @Override
        public String getField() {
            return field;
        }

        @Override
        public MatchingOperation getOperation() {
            return operation;
        }

        public void setOperation(MatchingOperation operation) {
            this.operation = operation;
        }
    }

    @Override
    public EntityManager getEntityManager() {
        return entityManager;
    }

    public List<Campaign> findBySubscription(Subscription subscription) {
        return entityManager.createQuery("select c from Campaign c join c.serviceList s where s.id = :serviceID ")
                .setParameter("serviceID", subscription.getService().getId()).getResultList();
    }

    public void process(Subscription subscription, Campaign campaign, Long bonusAmount) {
        switch (campaign.getTarget()) {
            case PAYMENT:
                processPaymentCampaign(subscription, campaign, bonusAmount);
                break;
            case RESOURCE_INTERNET_BANDWIDTH:
                processResourceCampaign(subscription);
                break;
        }

    }

    private void processPaymentCampaign(Subscription subscription, Campaign campaign, long bonusAmount) {
        subscriptionFacade.usePromoBalance(subscription, bonusAmount);
    }

    private void processResourceCampaign(Subscription subscription) {
        if (subscription.getService().getServiceType() == ServiceType.BROADBAND) {
            ResourceBucket bucket = subscription.getService().getResourceBucketByType(ResourceBucketType.INTERNET_DOWN);

            if (bucket != null) {
                String capacity = bucket.getCapacity();
                //bonusAmount = Long.valueOf(bucket.getCapacity()) * register.getCampaign().getBonusCount().longValue();
                if (capacity != null) {
                    log.info(String.format("process: subscription agreement=%s, restoring initial bandwidth, new bandwidth=%s", subscription.getAgreement(), capacity));
                    subscription.setBucketCapacityByType(ResourceBucketType.INTERNET_DOWN, capacity);
                    subscription.setBucketCapacityByType(ResourceBucketType.INTERNET_UP, capacity);
                }
            }
        }
    }

    public List<Campaign> findVasOnly(Subscription subscription, ValueAddedService vas) {
        List<Campaign> campaignList = entityManager.createQuery("select c from Campaign c join c.serviceList sList join c.vasList vList " +
                                    "where sList.id = :serviceId and vList.id = :vasId and c.isVasOnly = TRUE and " +
                                    "c.expirationDate > CURRENT_TIMESTAMP order by c.id desc")
                                    .setParameter("serviceId", subscription.getService().getId())
                                    .setParameter("vasId", vas.getId())
                                    .getResultList();
        return campaignList;
    }

    public List<Campaign> findAllActive(Service service, CampaignTarget target) {
        List<Campaign> campaignList = entityManager.createQuery("select c from Campaign c join c.serviceList srv " +
                "where c.status = :status and srv.id = :srv_id and srv.provider.id = :prov and c.target = :target and c.expirationDate > CURRENT_TIMESTAMP order by c.id desc")
                .setParameter("srv_id", service.getId())
                .setParameter("status", CampaignStatus.ACTIVE)
                .setParameter("prov", service.getProvider().getId())
                .setParameter("target", target)
                .getResultList();
        return campaignList;
    }

    public List<Campaign> findAllActive(Service service) {
        return findAllActive(service, true);
    }

    public List<Campaign> findAllActive(Service service, boolean isAutomatic) {
        List<Campaign> campaignList = entityManager.createQuery("select c from Campaign c join c.serviceList srv " +
                "where c.status = :status and c.isAutomatic = :auto and srv.id = :srv_id and srv.provider.id = :prov and c.expirationDate > CURRENT_TIMESTAMP order by c.id desc")
                .setParameter("srv_id", service.getId())
                .setParameter("status", CampaignStatus.ACTIVE)
                .setParameter("auto", isAutomatic)
                .setParameter("prov", service.getProvider().getId())
                .getResultList();
        log.debug("campaignList size: " + campaignList.size());
        return campaignList;
    }

    public List<Campaign> findAllActive(Service service, List<Campaign> excludeList) {
        StringBuilder query = new StringBuilder("select c from Campaign c join c.serviceList srv ");

        query.append(" where c.status = :status and srv.id = :srv_id and srv.provider.id = :prov and c.expirationDate > " +
                "CURRENT_TIMESTAMP ");

        if (getFilters() != null && !getFilters().isEmpty()) {
            query.append(" and ");
            String operation = " like ";

            for (Map.Entry<Filterable, Object> entry : getFilters().entrySet()) {
                query.append(String.format("lower(c.%s) %s :%s and", entry.getKey().getField().toLowerCase(), operation, entry.getKey().getField()));
            }
        }

        if (query.lastIndexOf("and") != -1) {
            query.delete(query.lastIndexOf("and"), query.length());
        }

        // List<Long> idList = new ArrayList<>();
        //List<Campaign> idList = new ArrayList<>();

        if (excludeList != null && !excludeList.isEmpty()) {
            query.append(" and c.id not in (");

            for (Campaign campaign : excludeList) {
                //idList.add(campaign.getId());
                query.append(campaign.getId());
                query.append(", ");
            }

            if (query.lastIndexOf(",") != -1) {
                query.delete(query.lastIndexOf(","), query.length());
            }

            query.append(") ");
        }

        query.append(" order by c.id desc");

        Query q = entityManager.createQuery(query.toString())
                .setParameter("srv_id", service.getId())
                .setParameter("status", CampaignStatus.ACTIVE)
                .setParameter("prov", service.getProvider().getId());
/*
        if (!idList.isEmpty())
            q.setParameter("exclude", excludeList);
*/
        if (getFilters() != null && !getFilters().isEmpty()) {
            for (Map.Entry<Filterable, Object> entry : getFilters().entrySet()) {
                q.setParameter(entry.getKey().getField(), "%" + ((String) entry.getValue()).toLowerCase() + "%");
            }
        }

        log.debug(String.format("findAllActive: resulting query with filters %s", query.toString()));
        List<Campaign> campaignList = q.getResultList();

        return campaignList;
    }
}
