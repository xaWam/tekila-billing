/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package com.jaravir.tekila.module.service.persistence.manager;

import com.jaravir.tekila.base.filter.Filterable;
import com.jaravir.tekila.base.filter.MatchingOperation;
import com.jaravir.tekila.base.persistence.facade.AbstractPersistenceFacade;
import com.jaravir.tekila.engines.EngineFactory;
import com.jaravir.tekila.engines.ProvisioningEngine;
import com.jaravir.tekila.module.campaign.Campaign;
import com.jaravir.tekila.module.campaign.CampaignPersistenceFacade;
import com.jaravir.tekila.module.campaign.CampaignRegisterPersistenceFacade;
import com.jaravir.tekila.module.campaign.CampaignTarget;
import com.jaravir.tekila.module.service.ResourceBucketType;
import com.jaravir.tekila.module.service.ServiceType;
import com.jaravir.tekila.module.service.entity.*;
import com.jaravir.tekila.module.subscription.persistence.entity.Subscription;
import com.jaravir.tekila.module.subscription.persistence.entity.SubscriptionStatus;
import com.jaravir.tekila.module.subscription.persistence.entity.transition.StatusChangeRule;
import com.jaravir.tekila.module.system.SystemEvent;
import com.jaravir.tekila.module.system.log.SystemLogger;
import com.jaravir.tekila.provision.broadband.devices.manager.Providers;
import com.jaravir.tekila.provision.exception.ProvisionerNotFoundException;
import org.apache.log4j.Logger;

import java.util.List;
import javax.ejb.*;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

/**
 * @author sajabrayilov
 */
@Stateless
public class ServicePersistenceFacade extends AbstractPersistenceFacade<Service> {
    @PersistenceContext
    private EntityManager em;
    @javax.annotation.Resource
    private EJBContext ctx;
    private final static Logger log = Logger.getLogger(ServicePersistenceFacade.class);
    @EJB
    private SystemLogger systemLogger;
    @EJB
    private CampaignPersistenceFacade campaignFacade;
    @EJB
    private CampaignRegisterPersistenceFacade campaignRegisterFacade;
    @EJB
    private EngineFactory provisioningFactory;

    private final static long SERVICE_RATE_MULTIPLICATOR = 100000L;

    public enum Filter implements Filterable {
        ID("id"),
        NAME("name"),
        TYPE("serviceType"),
        PROVIDER_ID("provider.id"),
        ISACTIVE("isActive"),
        SUBGROUP_ID("subgroup.id"),
        SERVICE_ID("id");

        private final String field;
        private MatchingOperation operation;

        private Filter(String code) {
            this.field = code;
            this.operation = MatchingOperation.EQUALS;
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

    public ServicePersistenceFacade() {
        super(Service.class);
    }

    @Override
    protected EntityManager getEntityManager() {
        return em;
    }

    public List<Service> findAll() {
        return em.createQuery("select s from Service s order by s.id desc", Service.class)
                .getResultList();
    }

    public List<Service> findAll(Long providerID, ServiceGroup group) {
        return em.createQuery("select s from Service s where s.provider.id = :prov_id and s.group = :group order by s.id desc")
                .setParameter("prov_id", providerID)
                .setParameter("group", group)
                .getResultList();
    }

    public List<Service> findAll(Long providerID) {
        return em.createQuery("select s from Service s where s.provider.id = :prov_id and s.isActive = 1 order by s.id")
                .setParameter("prov_id", providerID)
                .getResultList();
    }

    public Service addRule(Service service, SubscriptionStatus initStatus, SubscriptionStatus finalStatus, ValueAddedService vas) {
        String message = String.format("Adding rule service id=%d, initStatus=%s, finalStatus=%s, vas id=%d", service.getId(), initStatus, finalStatus, vas.getId());
        log.debug("addRule: " + message);

        service = update(service);
        StatusChangeRule rule = new StatusChangeRule(initStatus, finalStatus, vas);

        em.persist(rule);

        if (service.addRule(rule)) {
            log.debug("ADD RULE finished successfully. Rules: " + service.getStatusChangeRules());
            systemLogger.success(SystemEvent.SERVICE_ADD_STATUS_CHANGE_RULE, null, message);
        } else {
            log.debug("ADD RULE finished with error: " + ": rule already exists");
            systemLogger.error(SystemEvent.SERVICE_ADD_STATUS_CHANGE_RULE, null, message + ": rule already exists");
        }
        return service;
    }

    public Service removeRule(Service service, StatusChangeRule rule) {
        String message = String.format("Removing rule service id=%d, rule=%s", service.getId(), rule);
        log.debug("removeRule: " + message);

        service = update(service);
        rule = em.merge(rule);

        if (service.removeRule(rule)) {
            em.remove(rule);
            log.debug("REMOVE RULE finished successfully. Rules: " + service.getStatusChangeRules());
            systemLogger.success(SystemEvent.SERVICE_REMOVE_STATUS_CHANGE_RULE, null, message);
        } else {
            log.debug("REMOVE RULE finished with error: " + ": rule not found");
            systemLogger.error(SystemEvent.SERVICE_REMOVE_STATUS_CHANGE_RULE, null, message + ": rule not found");
        }
        return service;
    }

    public String getBucketCapacity(Subscription subscription) {
        String logHeader = String.format("getBucketCapacity: subscription agreement=%s, id=%d", subscription.getAgreement(), subscription.getId());
        Campaign campaign = null;
        List<Campaign> campaignList = campaignFacade.findAllActive(subscription.getService(), CampaignTarget.RESOURCE_INTERNET_BANDWIDTH);

        if (campaignList != null && !campaignList.isEmpty()) {
            log.info(String.format("%s found campaigns: %d", logHeader, campaignList.size()));

            Campaign resourceCampaign = campaignList.get(0);
            log.info(String.format("%s processing campaign id: %d", logHeader, resourceCampaign.getId()));

            Double count = resourceCampaign.getBonusCount();
            Service service = subscription.getService();
            ResourceBucket bucket = service.getResourceBucketByType(ResourceBucketType.INTERNET_DOWN);

            if (bucket != null) {
                String bandwidth = String.valueOf(Long.valueOf(bucket.getCapacity()) * count.longValue());
                log.info(String.format("%s bandwidth returned: %s", logHeader, bandwidth));

                return bandwidth;
            } else {
                return null;
            }
        } else {
            log.info(String.format("%s no campaigns found", logHeader));
            return null;
        }
    }

    @Deprecated
    public Long getServiceRateOld(Subscription subscription) {
        String logHeader = String.format("getServiceRate: subscription agreement=%s, id=%d", subscription.getAgreement(), subscription.getId());
        Campaign campaign = null;
        List<Campaign> campaignList = campaignFacade.findAllActive(subscription.getService(), CampaignTarget.SERVICE_RATE);

        if (campaignList != null && !campaignList.isEmpty()) {
            log.info(String.format("%s found campaigns: %d", logHeader, campaignList.size()));

            Campaign resourceCampaign = campaignList.get(0);
            log.info(String.format("%s processing campaign id: %d", logHeader, resourceCampaign.getId()));

            int multiplicator = resourceCampaign.getBonusCount().intValue();

            long rate = subscription.getService().getServicePrice() * multiplicator * SERVICE_RATE_MULTIPLICATOR;
            log.info(String.format("%s service rate returned: %d", logHeader, rate));

            return rate;
        } else {
            log.info(String.format("%s no campaigns found", logHeader));
            return null;
        }
    }

    public Long getServiceRate(Subscription subscription) {
        String logHeader = String.format("getServiceRate: subscription agreement=%s, id=%d", subscription.getAgreement(), subscription.getId());
        Campaign campaign = null;
        List<Campaign> campaignList = campaignFacade.findAllActive(subscription.getService(), CampaignTarget.SERVICE_RATE);

        if (campaignList != null && !campaignList.isEmpty()) {
            log.info(String.format("%s found campaigns: %d", logHeader, campaignList.size()));

            Campaign resourceCampaign = campaignList.get(0);
            log.info(String.format("%s processing campaign id: %d", logHeader, resourceCampaign.getId()));

            int multiplicator = resourceCampaign.getBonusCount().intValue();

            long rate = subscription.getService().getServicePrice() * multiplicator * SERVICE_RATE_MULTIPLICATOR;
            log.info(String.format("%s service rate returned: %d", logHeader, rate));

            return rate;
        } else {
            log.info(String.format("%s no campaigns found", logHeader));
            return null;
        }
    }

    public void createServiceProfile(Service service) {
        boolean res = false;
        try {
            res = provisioningFactory.getProvisioningEngine(service.getProvider()).createServiceProfile(service);
        } catch (ProvisionerNotFoundException e) {
            e.printStackTrace();
        }
        log.debug("Provisioning: " + res);
    }

    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public Service getNewCreated(Service service) {
        try {
            return this.getEntityManager().merge(service);
        } catch (Exception ex) {
            log.debug("Cannot find new created Service");
            return null;
        }
    }

    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public Service update(Service service) {
        service = this.getEntityManager().merge(service);
        if (service.getProvider().getId() == Providers.CITYNET.getId()) {
            ProvisioningEngine provisioner = null;
            try {
                provisioner = provisioningFactory.getProvisioningEngine(service.getProvider());
            } catch (ProvisionerNotFoundException e) {
                e.printStackTrace();
            }
            provisioner.updateByService(service);
        }
        return service;
    }

    public List<Service> findServicesByFilters(ServiceType type, long id, ServiceSubgroup subgroup, ServiceSubgroup subgroupAll, long providerId) {
        try {
            return em.createQuery("select s from Service s where s.provider.id = :prov_id and (s.subgroup = :subgroup or s.subgroup = :subgroupAll) and s.serviceType = :serviceType and s.id != :id and s.isActive = :isActive order by s.id desc")
                    .setParameter("prov_id", providerId)
                    .setParameter("subgroup", subgroup)
                    .setParameter("subgroupAll", subgroupAll)
                    .setParameter("serviceType", type)
                    .setParameter("id", id)
                    .setParameter("isActive", true)
                    .getResultList();
        } catch (Exception ex) {
            log.debug("findServiceByFilters: " + ex);
            return null;
        }
    }

    public List<Service> findServicesByFilters(ServiceType type, long id, long providerId) {
        try {
            return em.createQuery("select s from Service s where s.provider.id = :prov_id and s.serviceType = :serviceType and s.id != :id and s.isActive = :isActive order by s.id desc")
                    .setParameter("prov_id", providerId)
                    .setParameter("serviceType", type)
                    .setParameter("id", id)
                    .setParameter("isActive", true)
                    .getResultList();
        } catch (Exception ex) {
            log.debug("findServiceByFilters: " + ex);
            return null;
        }
    }

    public Service findServiceByNameAndProvider(String name, long providerId) {
        try {
            return em.createQuery("select s from Service s where s.provider.id = :prov_id and s.name = :name and s.isActive = :isActive",Service.class)
                    .setParameter("prov_id", providerId)
                    .setParameter("name", name)
                    .setParameter("isActive", true)
                    .getSingleResult();
        } catch (Exception ex) {
            log.debug("findServiceByFilters: " + ex);
            return null;
        }
    }

    public Long getServiceRate(Long serviceId) {
        try {
            return em.createQuery("select s.servicePrice from Service s where s.id = :id", Long.class)
                    .setParameter("id", serviceId)
                    .getSingleResult();
        }catch (Exception ex){
            log.error("Error occurs when getting service rate for service id "+serviceId, ex);
            return null;
        }
    }
}
