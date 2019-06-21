package com.jaravir.tekila.engines;

import com.jaravir.tekila.base.filter.Filterable;
import com.jaravir.tekila.module.service.entity.Service;
import com.jaravir.tekila.module.service.entity.ServiceProfile;
import com.jaravir.tekila.module.service.entity.ValueAddedService;
import com.jaravir.tekila.module.stats.external.ExternalStatusInformation;
import com.jaravir.tekila.module.stats.persistence.entity.OfflineBroadbandStats;
import com.jaravir.tekila.module.stats.persistence.entity.OnlineBroadbandStats;
import com.jaravir.tekila.module.store.nas.Attribute;
import com.jaravir.tekila.module.store.nas.Nas;
import com.jaravir.tekila.module.subscription.persistence.entity.Subscription;
import com.jaravir.tekila.module.subscription.persistence.entity.SubscriptionVAS;
import com.jaravir.tekila.module.subscription.persistence.entity.external.TechnicalStatus;
import com.jaravir.tekila.provision.broadband.entity.BackProvisionDetails;
import com.jaravir.tekila.provision.broadband.entity.Usage;
import com.jaravir.tekila.provision.exception.ProvisioningException;
import org.joda.time.DateTime;

import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * Created by khsadigov on 5/16/2017.
 */
public interface ProvisioningEngine extends BaseEngine {
    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    boolean ping();

    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    boolean initService(Subscription subscription); //throws ProvisioningException

    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    boolean openService(Subscription subscription); //throws ProvisioningException;

    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    boolean openService(Subscription subscription, DateTime expDate); //throws ProvisioningException;

    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    boolean closeService(Subscription subscription); //throws ProvisioningException;

    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    OnlineBroadbandStats collectOnlineStats(Subscription subscription); //throws ProvisioningException;

    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    List<OnlineBroadbandStats> collectOnlineStatswithPage(int start, int end); //throws ProvisioningException;


    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    TechnicalStatus getTechnicalStatus(Subscription subscription);

    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    boolean openVAS(Subscription subscription, ValueAddedService vas);

    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    boolean openVAS(Subscription subscription, ValueAddedService vas, SubscriptionVAS sbnVas);

    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    boolean closeVAS(Subscription subscription, ValueAddedService vas, SubscriptionVAS sbnVas);

    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    boolean closeVAS(Subscription subscription, ValueAddedService vas);

    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    boolean changeEquipment(Subscription subscription, String newValue) throws ProvisioningException;

    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    boolean disconnect(Subscription subscription) throws ProvisioningException;

    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    String checkRadiusState(Subscription subscription) throws ProvisioningException;

    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    boolean changeService(Subscription subscription, Service targetService) throws ProvisioningException;

    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    ExternalStatusInformation collectExternalStatusInformation(Subscription subscription);

    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    boolean reprovision(Subscription subscription);

    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    boolean reprovisionWithEndDate(Subscription subscription, DateTime endDate);

    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    boolean updateByService(Service service);

    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    boolean createServiceProfile(Service service);

    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    boolean updateServiceProfile(ServiceProfile serviceProfile, int oper);

    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    boolean createServiceProfile(ServiceProfile serviceProfile);

    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    boolean createNas(Nas nas);

    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    boolean updateNas(Nas nas);

    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    boolean updateAttribute(Attribute attribute);

    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    List<OfflineBroadbandStats> offlineBroadbandStats(Subscription subscription, Map<Filterable, Object> filters);

    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    DateTime getActivationDate(Subscription subscription);

    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    boolean updateAccount(Subscription subscription);

    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    boolean removeAccount(Subscription subscription);

    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    boolean removeFD(Subscription subscription);

    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    Usage getUsage(Subscription subscription, Date startDate, Date endDate);

    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    List<String> getAuthRejectionReasons(Subscription subscription);

    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    boolean provisionIptv(Subscription subscription);

    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    BackProvisionDetails getBackProvisionDetails(Subscription subscription);

    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    void provisionNewAgreements(String oldAgreement,String newAgreement) throws Exception;

}
