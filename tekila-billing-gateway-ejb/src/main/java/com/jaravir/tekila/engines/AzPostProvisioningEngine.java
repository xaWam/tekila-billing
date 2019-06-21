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

import javax.ejb.Local;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import java.io.Serializable;
import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * Created by sgulmammadov on 6/13/2017.
 */
@Stateless(name = "AzPostProvisioningEngine", mappedName = "AzPostProvisioningEngine")
@Local(ProvisioningEngine.class)
public class AzPostProvisioningEngine implements ProvisioningEngine, Serializable {
    @Override
    public boolean ping() {
        return false;
    }

    @Override
    public boolean initService(Subscription subscription) {
        return false;
    }

    @Override
    public boolean openService(Subscription subscription) {
        return false;
    }

    @Override
    public boolean openService(Subscription subscription, DateTime expDate) {
        return false;
    }

    @Override
    public boolean closeService(Subscription subscription) {
        return false;
    }

    @Override
    public OnlineBroadbandStats collectOnlineStats(Subscription subscription) {
        return null;
    }

    @Override
    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public List<OnlineBroadbandStats> collectOnlineStatswithPage(int start, int end) //throws ProvisioningException;
    {
        return null;
    }

    @Override
    public TechnicalStatus getTechnicalStatus(Subscription subscription) {
        return null;
    }

    @Override
    public boolean openVAS(Subscription subscription, ValueAddedService vas) {
        return false;
    }

    @Override
    public boolean openVAS(Subscription subscription, ValueAddedService vas, SubscriptionVAS sbnVas) {
        return false;
    }

    @Override
    public boolean closeVAS(Subscription subscription, ValueAddedService vas, SubscriptionVAS sbnVas) {
        return false;
    }

    @Override
    public boolean closeVAS(Subscription subscription, ValueAddedService vas) {
        return false;
    }

    @Override
    public boolean changeEquipment(Subscription subscription, String newValue) throws ProvisioningException {
        return false;
    }

    @Override
    public boolean disconnect(Subscription subscription) throws ProvisioningException {
        return false;
    }

    @Override
    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public String checkRadiusState(Subscription subscription) throws ProvisioningException {
        return "OFFLINE";
    }

    @Override
    public boolean changeService(Subscription subscription, Service targetService) throws ProvisioningException {
        return false;
    }

    @Override
    public ExternalStatusInformation collectExternalStatusInformation(Subscription subscription) {
        return null;
    }

    @Override
    public boolean reprovision(Subscription subscription) {
        return false;
    }

    @Override
    public boolean reprovisionWithEndDate(Subscription subscription, DateTime endDate) {
        return false;
    }

    @Override
    public boolean updateByService(Service service) {
        return false;
    }

    @Override
    public boolean createServiceProfile(Service service) {
        return false;
    }

    @Override
    public boolean updateServiceProfile(ServiceProfile serviceProfile, int oper) {
        return false;
    }

    @Override
    public boolean createServiceProfile(ServiceProfile serviceProfile) {
        return false;
    }

    @Override
    public boolean createNas(Nas nas) {
        return false;
    }

    @Override
    public boolean updateNas(Nas nas) {
        return false;
    }

    @Override
    public boolean updateAttribute(Attribute attribute) {
        return false;
    }

    @Override
    public List<OfflineBroadbandStats> offlineBroadbandStats(Subscription subscription, Map<Filterable, Object> filters) {
        return null;
    }

    @Override
    public DateTime getActivationDate(Subscription subscription) {
        return null;
    }

    @Override
    public boolean updateAccount(Subscription subscription) {
        return false;
    }

    @Override
    public boolean removeAccount(Subscription subscription) {
        return false;
    }

    public boolean removeFD(Subscription subscription) {
        return false;
    }

    @Override
    public Usage getUsage(Subscription subscription, Date startDate, Date endDate) {
        return null;
    }

    @Override
    public List<String> getAuthRejectionReasons(Subscription subscription) {
        return null;
    }

    @Override
    public boolean provisionIptv(Subscription subscription) {
        return false;
    }

    @Override
    public BackProvisionDetails getBackProvisionDetails(Subscription subscription) {
        return null;
    }

    @Override
    public void provisionNewAgreements(String oldAgreement, String newAgreement) {

    }
}
