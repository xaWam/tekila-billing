package com.jaravir.tekila.provision.tv;

import com.azerfon.billing.narhome.verimatrix.OMIController;
import com.azerfon.billing.narhome.verimatrix.VerimatrixIntegrationException;
import com.jaravir.tekila.base.filter.Filterable;
import com.jaravir.tekila.engines.ProvisioningEngine;
import com.jaravir.tekila.module.service.ServiceSettingType;
import com.jaravir.tekila.module.service.entity.Service;
import com.jaravir.tekila.module.service.entity.ServiceProfile;
import com.jaravir.tekila.module.service.entity.ValueAddedService;
import com.jaravir.tekila.module.stats.external.ExternalStatusInformation;
import com.jaravir.tekila.module.stats.persistence.entity.OfflineBroadbandStats;
import com.jaravir.tekila.module.stats.persistence.entity.OnlineBroadbandStats;
import com.jaravir.tekila.module.store.nas.Attribute;
import com.jaravir.tekila.module.store.nas.Nas;
import com.jaravir.tekila.module.subscription.persistence.entity.Subscription;
import com.jaravir.tekila.module.subscription.persistence.entity.SubscriptionStatus;
import com.jaravir.tekila.module.subscription.persistence.entity.SubscriptionVAS;
import com.jaravir.tekila.module.subscription.persistence.entity.external.TechnicalStatus;
import com.jaravir.tekila.provision.broadband.entity.BackProvisionDetails;
import com.jaravir.tekila.provision.broadband.entity.Usage;
import com.jaravir.tekila.provision.exception.ProvisioningException;
import org.apache.log4j.Logger;
import org.joda.time.DateTime;

import javax.ejb.Local;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import java.util.*;

/**
 * Created by sajabrayilov on 24.01.2015.
 */
@Stateless(name = "BBTVProvisioner", mappedName = "BBTVProvisioner")
@Local(ProvisioningEngine.class)
public class BBTVProvisioner implements ProvisioningEngine {
    private final static String networkID = "network_dvb";
    private final static String packageID = "standart_package";
    private final static String networkContentID = "";
    private final static String smartCard = "STB_DVB_SC";
    private final static String smartlessDevice = "STB_DVB_NSC_2";
    private final static OMIController verimatrix = new OMIController();
    private final static Logger log = Logger.getLogger(BBTVProvisioner.class);
    private final static String resultSuccess = "0";
    private final static String resultDuplicateEntitlement = "207";
    private final static String resultMissingEntitlement = "223";
    private final static String resultError = "error";
    private final static String entitlementType = "DEVICE";

    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public boolean ping() {
        return true;
    }

    @Override
    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public boolean initService(Subscription subscription) //throws ProvisioningException
    {
        log.debug("initService Subs: " + subscription);
        final String logHeader = String.format("initService: agreement=%s", subscription.getAgreement());
        log.debug(logHeader);

        log.debug("partNumber: " + subscription.getSettingByType(
                        ServiceSettingType.TV_EQUIPMENT)
                        .getValue());

        String partNumber =
                processPartNumber(
                        subscription.getSettingByType(
                                ServiceSettingType.TV_EQUIPMENT)
                                .getValue());

        log.info(String.format("%s: equipment partnumber=%s", logHeader, partNumber));

        try {
            log.info(String.format("%s calling addDevice (%s, %s, %s, %s}", logHeader, partNumber, networkID, smartlessDevice, partNumber));

            Map<String, String> res = verimatrix.addDevice(Arrays.asList(partNumber), Arrays.asList(networkID), Arrays.asList(smartlessDevice), Arrays.asList(partNumber));
            log.debug(String.format("%s: result map: ", res.entrySet()));
            return processResult(res, partNumber);
        } catch (VerimatrixIntegrationException ex) {
            log.error(String.format("%s: Cannot provision: ", ex));
            return false;
        }
    }

    @Override
    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public boolean openService(Subscription subscription) //throws ProvisioningException;
    {
        log.debug("Starting open service on BBTV, agreement=" + subscription.getAgreement());

        final String logHeader = String.format("openService: agreement=%s", subscription.getAgreement());
        String partNumber = processPartNumber(subscription.getSettingByType(ServiceSettingType.TV_EQUIPMENT).getValue());
        String entitlementID = subscription.getAgreement();

        try {
            Map<String, String> res = verimatrix.addEntitlements(Arrays.asList(partNumber), packageID, Arrays.asList(entitlementID), entitlementType);
            log.debug(String.format("%s: result map for subscription %s", res.entrySet(), subscription.getId()));
            return processResult(res, entitlementID);
        } catch (VerimatrixIntegrationException ex) {
//            log.error(String.format("%s: cannot provision: ", ex));
            log.error("Error occurs BBTV open service provisioning for subscription "+subscription.getId(), ex);
            return false;
        }
    }

    @Override
    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public boolean openService(Subscription subscription, DateTime expDate) //throws ProvisioningException;
    {
        return true;
    }

    @Override
    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public boolean closeService(Subscription subscription) //throws ProvisioningException;
    {
        final String logHeader = String.format("closeService: agreement=%s", subscription.getAgreement());
        String partNumber = processPartNumber(subscription.getSettingByType(ServiceSettingType.TV_EQUIPMENT).getValue());
        String entitlementID = subscription.getAgreement();

        try {
            Map<String, String> res = verimatrix.removeEntitlements(Arrays.asList(partNumber), packageID, Arrays.asList(entitlementID), entitlementType);
            log.debug(String.format("%s: result map: ", res.entrySet()));
            return processResult(res, entitlementID);
        } catch (VerimatrixIntegrationException ex) {
            log.error(String.format("%s: cannot provision: ", ex));
            return false;
        }
    }

    @Override
    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public OnlineBroadbandStats collectOnlineStats(Subscription subscription) {
        return null;
    }//throws ProvisioningException;

    @Override
    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public List<OnlineBroadbandStats> collectOnlineStatswithPage(int start, int end) //throws ProvisioningException;
    {
        return null;
    }

    @Override
    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public TechnicalStatus getTechnicalStatus(Subscription subscription) {
        return null;
    }

    @Override
    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public ExternalStatusInformation collectExternalStatusInformation(Subscription subscription) //throws ProvisioningException;
    {
        final String logHeader = String.format("collectOnlineStats: agreement=%s", subscription.getAgreement());
        String partNumber = processPartNumber(subscription.getSettingByType(ServiceSettingType.TV_EQUIPMENT).getValue());
        String entitlementID = subscription.getAgreement();
        ExternalStatusInformation info = new ExternalStatusInformation();

        try {
            Map<String, String> res = verimatrix.getDeviceEntitlements(partNumber);
            log.debug(String.format("%s: result map: ", res.entrySet()));

            if (res.containsKey("resultCode") && res.get("resultCode").equals("0")) {
                for (Map.Entry<String, String> entry : res.entrySet()) {
                    info.addProperty(entry.getKey(), entry.getValue());
                }
            }
        } catch (VerimatrixIntegrationException ex) {
            log.error(String.format("%s: cannot provision: ", ex));
            return null;
        }

        return info;
    }

    @Override
    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public boolean openVAS(Subscription subscription, ValueAddedService vas) {
        return true;
    }

    @Override
    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public boolean openVAS(Subscription subscription, ValueAddedService vas, SubscriptionVAS sbnVas) {
        return false;
    }

    @Override
    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public boolean closeVAS(Subscription subscription, ValueAddedService vas, SubscriptionVAS sbnVas) {
        return false;
    }

    @Override
    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public boolean closeVAS(Subscription subscription, ValueAddedService vas) {
        return true;
    }

    @Override
    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public boolean changeEquipment(Subscription subscription, String newValue) throws ProvisioningException {
        return initService(subscription);
    }

    @Override
    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
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

    public String processPartNumber(String partNumber) {
        String res = partNumber.replaceAll("\\s+", "").replaceFirst("N", "");

        if (res.length() > 10) {
            res = new StringBuilder(res).deleteCharAt(res.length() - 1).toString();
        }

        return res;
    }

    private boolean processResult(Map<String, String> resultMap, String key) {
        return resultMap.get(key).toLowerCase().equals(resultSuccess)
                || resultMap.get(key).toLowerCase().equals(resultDuplicateEntitlement)
                || resultMap.get(key).toLowerCase().equals(resultMissingEntitlement);
    }

    @Override
    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public boolean reprovision(Subscription subscription) {

        log.debug("Starting reprovisoning on BBTV, agreement=" + subscription.getAgreement());

        boolean res = false;
        boolean tmp = false;

        res = initService(subscription);
        tmp = closeService(subscription);

        if (!res)
            res = tmp;

        if (subscription.getStatus() == SubscriptionStatus.ACTIVE)
            return openService(subscription);
        else {
            return res;
        }
    }

    @Override
    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public boolean reprovisionWithEndDate(Subscription subscription, DateTime endDate) {
        return false;
    }

    @Override
    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public boolean createNas(Nas nas) {
        return false;
    }

    @Override
    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public boolean updateNas(Nas nas) {
        return false;
    }

    @Override
    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public boolean updateAttribute(Attribute attribute) {
        return false;
    }

    @Override
    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public List<OfflineBroadbandStats> offlineBroadbandStats(Subscription subscription, Map<Filterable, Object> filters) {
        return null;
    }

    @Override
    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public boolean updateAccount(Subscription subscription) {
        return false;
    }

    @Override
    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public DateTime getActivationDate(Subscription subscription) {
        return null;
    }

    @Override
    public boolean updateByService(Service service) {
        return false;
    }

    @Override
    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public boolean createServiceProfile(Service service) {
        return false;
    }

    @Override
    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public boolean updateServiceProfile(ServiceProfile serviceProfile, int oper) {
        return false;
    }

    @Override
    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public boolean createServiceProfile(ServiceProfile serviceProfile) {
        return false;
    }

    @Override
    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public boolean removeAccount(Subscription subscription) {
        return false;
    }

    public boolean removeFD(Subscription subscription) {
            return false;
    }


    @Override
    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public Usage getUsage(Subscription subscription, Date startDate, Date endDate) {
        return null;
    }

    @Override
    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public List<String> getAuthRejectionReasons(Subscription subscription) {
        return null;
    }

    @Override
    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public boolean provisionIptv(Subscription subscription) {
        return true;
    }

    @Override
    public BackProvisionDetails getBackProvisionDetails(Subscription subscription) {
        return null;
    }

    @Override
    public void provisionNewAgreements(String oldAgreement, String newAgreement) {

    }
}
