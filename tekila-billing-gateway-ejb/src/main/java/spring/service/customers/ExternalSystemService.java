package spring.service.customers;

import com.jaravir.tekila.engines.EngineFactory;
import com.jaravir.tekila.engines.ProvisioningEngine;
import com.jaravir.tekila.module.stats.persistence.entity.OnlineBroadbandStats;
import com.jaravir.tekila.module.stats.persistence.manager.AuthRejectionReasonPersistenceFacade;
import com.jaravir.tekila.module.stats.persistence.manager.OfflineStatsPersistenceFacade;
import com.jaravir.tekila.module.stats.persistence.manager.OnlineStatsPersistenceFacade;
import com.jaravir.tekila.module.stats.persistence.manager.TechnicalStatusPersistenceFacade;
import com.jaravir.tekila.module.subscription.persistence.entity.Subscription;
import com.jaravir.tekila.module.subscription.persistence.entity.external.StatusElement;
import com.jaravir.tekila.module.subscription.persistence.entity.external.StatusElementType;
import com.jaravir.tekila.module.subscription.persistence.entity.external.TechnicalStatus;
import com.jaravir.tekila.module.subscription.persistence.management.SubscriptionPersistenceFacade;
import com.jaravir.tekila.module.system.SystemEvent;
import com.jaravir.tekila.module.system.log.SystemLogger;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import spring.dto.OfflineSessionDTO;
import spring.dto.OnlineSessionDTO;
import spring.exceptions.CustomerOperationException;
import spring.mapper.subscription.OfflineSessionMapper;
import spring.mapper.subscription.OnlineSessionMapper;

import javax.ejb.EJB;

import java.util.*;
import java.util.stream.Collectors;

import static spring.util.Constants.INJECTION_POINT;

/**
 * @author ElmarMa on 3/19/2018
 */
@Service
public class ExternalSystemService {

    private static final Logger logger = Logger.getLogger(ExternalSystemService.class);


    @EJB(mappedName = INJECTION_POINT + "TechnicalStatusPersistenceFacade")
    private TechnicalStatusPersistenceFacade technicalStatusPersistenceFacade;

    @EJB(mappedName = INJECTION_POINT + "SubscriptionPersistenceFacade")
    private SubscriptionPersistenceFacade subscriptionPersistenceFacade;

    @EJB(mappedName = INJECTION_POINT + "AuthRejectionReasonPersistenceFacade")
    private AuthRejectionReasonPersistenceFacade authRejectionReasonPersistenceFacade;

    @EJB(mappedName = INJECTION_POINT + "OnlineStatsPersistenceFacade")
    private OnlineStatsPersistenceFacade onlineStatsPersistenceFacade;

    @EJB(mappedName = INJECTION_POINT + "OfflineStatsPersistenceFacade")
    private OfflineStatsPersistenceFacade offlineStatsPersistenceFacade;

    @EJB(mappedName = INJECTION_POINT + "EngineFactory")
    private EngineFactory engineFactory;

    @EJB(mappedName = INJECTION_POINT + "SystemLogger")
    private SystemLogger systemLogger;

    @Autowired
    private OnlineSessionMapper onlineSessionMapper;

    @Autowired
    private OfflineSessionMapper offlineSessionMapper;


    public Map<StatusElementType, String> getTechincalStatusOfProvisioning(Long subscriptionId) {
        Subscription subscription = subscriptionPersistenceFacade.find(subscriptionId);
        if (subscription == null) {
            throw new CustomerOperationException("can not find subscription with id = " + subscriptionId);
        }

        TechnicalStatus technicalStatus = null;
        try {
            technicalStatus = technicalStatusPersistenceFacade.getTechnicalStatus(subscription);
            if (technicalStatus == null || technicalStatus.getElements().isEmpty())
                throw new CustomerOperationException("can not find out what is technical status");
        } catch (Exception e) {
            throw new CustomerOperationException("can not find out what is technical status ," + e.getMessage(), e);
        }

        /*
        * technicalStatus = new TechnicalStatus();

        StatusElement<String> element1 = new StatusElement<>();
        element1.setType(StatusElementType.BROADBAND_ACTIVE);
        element1.setValue("Veziyet guldu");

        StatusElement<String> element2 = new StatusElement<>();
        element2.setType(StatusElementType.BROADBAND_REDIRECT);
        element2.setValue("nemalaaalalalal");

        List<StatusElement> elements = new ArrayList<>();
        elements.add(element1);
        elements.add(element2);

        technicalStatus.setElements(elements);
        * */

        Map<StatusElementType, String> statuses = new HashMap<>();
        for (StatusElement se : technicalStatus.getElements()) {
            statuses.put(se.getType(), se.getValue().toString());
        }
        return statuses;
    }

    public List<String> getAuthenticationProblems(Long subscriptionId) {
        Subscription subscription = subscriptionPersistenceFacade.find(subscriptionId);
        if (subscription == null)
            throw new CustomerOperationException("can not load authentication messages ,can not find subscription with id = " + subscriptionId);
        List<String> rejectionMessages = null;
        try {
            rejectionMessages = authRejectionReasonPersistenceFacade.getAuthRejectionReasons(subscription);
            if (rejectionMessages == null)
                return new ArrayList<>();
        } catch (Exception e) {
            throw new CustomerOperationException(e.getMessage(), e);
        }
        return rejectionMessages;
    }

    public List<OnlineSessionDTO> getOnlineBroadbandStatuses(Long subscriptionId) {
        Subscription subscription = subscriptionPersistenceFacade.find(subscriptionId);
        if (subscription == null)
            throw new CustomerOperationException("can not load online sessions ,can not find subscription with id = " + subscriptionId);

        OnlineBroadbandStats onlineBroadbandStats = null;
        try {
            onlineBroadbandStats = onlineStatsPersistenceFacade.getOnlineSession(subscription);
            if (onlineBroadbandStats == null)
                return new ArrayList<>();
            else
                return Arrays.asList(onlineSessionMapper.toDto(onlineBroadbandStats));
        } catch (Exception e) {
            throw new CustomerOperationException("can not load online sessions ," + e.getMessage(), e);
        }

    }

    public List<OfflineSessionDTO> getOfflineBroadbandStatuses(Long subscriptionId) {
        Subscription subscription = subscriptionPersistenceFacade.find(subscriptionId);
        if (subscription == null)
            throw new CustomerOperationException("can not load offline sessions ,can not find subscription with id = " + subscriptionId);
        List<OfflineSessionDTO> offlineSessions = null;
        try {
            offlineSessions = offlineStatsPersistenceFacade.getRecentSessions(subscription.getAgreement()).
                    stream().
                    map(offlineBroadbandStats -> offlineSessionMapper.toDto(offlineBroadbandStats)).
                    collect(Collectors.toList());
            if (offlineSessions == null)
                return new ArrayList<>();
            else
                return offlineSessions;
        } catch (Exception e) {
            throw new CustomerOperationException("can not find out offline sessions ," + e.getMessage(), e);
        }
    }

    public void disconnectSession(Long subscriptionId) {
        Subscription subscription = subscriptionPersistenceFacade.find(subscriptionId);
        if (subscription == null)
            throw new CustomerOperationException("can not disconnect ,can not find subscription with id = " + subscriptionId);
        try {
            subscriptionPersistenceFacade.disconnectSession(subscription);
        } catch (Exception e) {
            throw new CustomerOperationException("can not disconnect" + e.getMessage(), e);
        }
    }

    public List<String> getReasons() {
        String reasons[] = {
                "Channels or Internet did not open after payment",
                "Channels ot Internet unexpectedly stopped working",
                "Other reason"
        };
        return Arrays.asList(reasons);
    }

    public void reprovision(Long subscriptionId, String reason) {
        Subscription subscription = subscriptionPersistenceFacade.find(subscriptionId);
        if (subscription == null)
            throw new CustomerOperationException("can not re-provision, can not find subscription with id = " + subscriptionId);

        reprovision(subscription, reason);
    }

    public void reprovision(Subscription subscription, String reason) {
        if (reason == null || reason.isEmpty())
            throw new CustomerOperationException("can not re-provision, reason is not present");

        ProvisioningEngine provisioningEngine = null;

        try {
            provisioningEngine = engineFactory.getProvisioningEngine(subscription);
            if (provisioningEngine == null)
                throw new CustomerOperationException("can not find provisioner for customer " + subscription.getId());
            if (provisioningEngine.reprovision(subscription)) {
                logger.info(String.format("reprovision: subscription id=%d, agreement=%s, status=%s, result=%s, reprovisionReason=%s reprovisioning successfull",
                        subscription.getId(), subscription.getAgreement(), subscription.getStatus(), "succeed", reason));
                systemLogger.success(SystemEvent.REPROVISION, subscription, String.format("status=%s, reason=%s", subscription.getStatus(), reason));
            } else {
                logger.error(String.format("reprovision: subscription id=%d, agreement=%s, result=%s, reprovisionReason=%s reprovisioning failed",
                        subscription.getId(), subscription.getAgreement(), "Failed", reason));
                systemLogger.error(SystemEvent.REPROVISION, subscription, String.format("status=%s, reason=%s", subscription.getStatus(), reason));
                throw new CustomerOperationException("re-provision is unsuccessful");
            }
        } catch (Exception e) {
            throw new CustomerOperationException(e.getMessage(), e);
        }
    }

}
