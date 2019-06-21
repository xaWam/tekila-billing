/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package com.jaravir.tekila.module.service.persistence.manager;

import com.jaravir.tekila.base.entity.BaseEntity;
import com.jaravir.tekila.base.filter.Filterable;
import com.jaravir.tekila.base.filter.MatchingOperation;
import com.jaravir.tekila.base.persistence.facade.AbstractPersistenceFacade;
import com.jaravir.tekila.module.accounting.entity.Invoice;
import com.jaravir.tekila.module.accounting.manager.ChargePersistenceFacade;
import com.jaravir.tekila.module.accounting.manager.InvoicePersistenceFacade;
import com.jaravir.tekila.module.accounting.manager.TransactionPersistenceFacade;
import com.jaravir.tekila.module.service.ResourceBucketType;
import com.jaravir.tekila.module.service.VASSetting;
import com.jaravir.tekila.module.service.ValueAddedServiceType;
import com.jaravir.tekila.module.service.entity.Resource;
import com.jaravir.tekila.module.service.entity.ResourceBucket;
import com.jaravir.tekila.module.service.entity.ValueAddedService;
import com.jaravir.tekila.module.subscription.persistence.entity.Subscriber;
import com.jaravir.tekila.module.subscription.persistence.entity.Subscription;
import com.jaravir.tekila.module.subscription.persistence.management.SubscriptionPersistenceFacade;
import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import org.apache.log4j.Logger;
import org.joda.time.DateTime;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 *
 * @author sajabrayilov
 */
@Stateless
public class VASPersistenceFacade extends AbstractPersistenceFacade<ValueAddedService>{
    @PersistenceContext
    private EntityManager em;
    @EJB private SubscriptionPersistenceFacade subscriptionFacade;
    @EJB private TransactionPersistenceFacade transFacade;
    @EJB private ChargePersistenceFacade chargeFacade;
    @EJB private InvoicePersistenceFacade invoiceFacade;
    @EJB private ResourcePersistenceFacade resourceFacade;
    @EJB private ResourceBucketPersistenceFacade bucketFacade;

    private Subscriber subscriber;
    private final static Logger log = Logger.getLogger(VASPersistenceFacade.class);

    public enum Filter implements Filterable {
        PROVIDER("provider.id"),
        NAME("name"),
        ISACTIVE("isActive"),
        TYPE("code.type");

        private final String field;
        private MatchingOperation operation;

        Filter(String field) {
            this.field = field;
            this.operation = MatchingOperation.LIKE;
        }

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

    public VASPersistenceFacade() {
        super(ValueAddedService.class);
    }

    @Override
    protected EntityManager getEntityManager() {
        return em;
    }
    
    @Override
    public void save (ValueAddedService srv) {
        srv.setCreationDate(DateTime.now().toDate());
        super.save(srv);
    }    

    public void saveSetting (VASSetting setting) {
        em.persist(setting);
    }

    public void addService (ValueAddedService service, long subscriptionID, long rate, long userID, String desc) 
    throws Exception {
        try {
            Subscription subscription = subscriptionFacade.find(subscriptionID);        
            addService(service, subscription, rate, userID, desc);
        }
        catch (Exception ex) {
            log.error(String.format("Cannot find subscription", subscriptionID), ex);
        }
    }
    
    public void addService (ValueAddedService service, Subscription subscription, long rate, long userID, String desc) {
        Invoice invoice = invoiceFacade.findOrCreateForPostpaid(subscription);        
        invoiceFacade.addVasChargeToInvoice(invoice, subscription, rate, service, userID, desc);
    }
    
    public void findOrderedServicesBySubscriber(long subscriberID) {
        
    }

    public VASSetting findSettingByName (String name) {
        try {
            return em.createQuery("select v from VASSetting v where v.name = :name", VASSetting.class)
                    .setParameter("name", name).getSingleResult();
        }
        catch (Exception ex) {
            return null;
        }
    }
            
    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    private void addServiceToSubscription(ValueAddedService service, Subscription subscription) {
        
    }

    public void edit (ValueAddedService selectedVas, ResourceBucketType bucketType) {
        selectedVas = this.update(selectedVas);

        if (selectedVas.getSettings() != null && !selectedVas.getName().isEmpty()) {
            VASSetting stFound = null;

            Iterator<VASSetting> iterator = selectedVas.getSettings().iterator();
            VASSetting set = null;
            List<VASSetting> foundSettings = new ArrayList<>();

            while (iterator.hasNext()) {
                set = iterator.next();

                stFound = this.findSettingByName(set.getName());

                if (stFound != null) {
                    iterator.remove();
                    foundSettings.add(stFound);
                } else {
                    this.saveSetting(set);
                }
            }

            if (!foundSettings.isEmpty())
                selectedVas.getSettings().addAll(foundSettings);
        }

        if (selectedVas.getCode().getType() == ValueAddedServiceType.PERIODIC_STATIC) {
            Resource res = selectedVas.getResource();
            boolean addBucket = false;
            ResourceBucket buck = null;
            boolean adddResource = false;

            if (res == null) {
                res = new Resource();
                res.setName("Resource for " + selectedVas.getName());
                selectedVas.setResource(res);
                adddResource = true;
                addBucket = true;
            }

            else if (selectedVas.getResource().getBucketList() == null ||
                    selectedVas.getResource().getBucketList().size() == 0
                    || (buck = selectedVas.getResource().getBucketList().get(0)) == null
                    ) {
                addBucket = true;
            }

            if (addBucket) {
                buck = new ResourceBucket();
                buck.setType(bucketType);
                selectedVas.getResource().addBucket(buck);
                //bucketFacade.save(buck);
            }
            else if (buck.getType() != bucketType) {
                buck.setType(bucketType);
                buck = bucketFacade.update(buck);
            }

            if (adddResource) {
                //resourceFacade.save(res);
            }
            else {
                res = resourceFacade.update(res);
            }
        }
    }

    public ValueAddedService findEligibleVas(Subscription subscription) {
        try {
            return getEntityManager().createQuery("select v from ValueAddedService v where v.provider.id = :providerId and v.isStaticIp = TRUE", ValueAddedService.class)
                    .setParameter("providerId", subscription.getService().getProvider().getId())
                    .getSingleResult();
        } catch (Exception ex) {
            log.error("Error during static ip vas fetching from db: ", ex);
            return null;
        }
    }

    public ValueAddedService findEligibleSipVas(Subscription subscription) {
        try {
            return getEntityManager().createQuery("select v from ValueAddedService v where v.provider.id = :providerId and v.isSip = TRUE", ValueAddedService.class)
                    .setParameter("providerId", subscription.getService().getProvider().getId())
                    .getSingleResult();
        } catch (Exception ex) {
            log.error("Error during sip vas fetching from db: ", ex);
            return null;
        }
    }

    public List<ValueAddedService> findSuspensionVasListBySubscription(Subscription subscription) {
        try {
            return getEntityManager().createQuery("select v from ValueAddedService v where v.suspension = TRUE and v.provider.id = :providerId", ValueAddedService.class)
                    .setParameter("providerId", subscription.getService().getProvider().getId())
                    .getResultList();
        } catch (Exception ex) {
            log.error("Error during suspension vas fetching: ", ex);
            return null;
        }
    }
}
