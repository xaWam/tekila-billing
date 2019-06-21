package spring.service.customers;

import com.jaravir.tekila.module.campaign.*;
import com.jaravir.tekila.module.subscription.persistence.entity.SubscriberType;
import com.jaravir.tekila.module.subscription.persistence.entity.Subscription;
import com.jaravir.tekila.module.subscription.persistence.management.SubscriptionPersistenceFacade;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import spring.dto.CampaignMinifiedDTO;
import spring.dto.CampaignRegisterDTO;
import spring.exceptions.CustomerOperationException;
import spring.mapper.subscription.CampaignMinifiedMapper;
import spring.mapper.subscription.CampaignRegisterMapper;

import javax.ejb.EJB;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;

import static spring.util.Constants.INJECTION_POINT;

/**
 * @author ElmarMa on 3/27/2018
 */
@Service
public class CampaignService {

    private static final Logger logger = Logger.getLogger(CampaignService.class);

    @EJB(mappedName = INJECTION_POINT + "SubscriptionPersistenceFacade")
    private SubscriptionPersistenceFacade subscriptionPersistenceFacade;

    @EJB(mappedName = INJECTION_POINT + "CampaignRegisterPersistenceFacade")
    private CampaignRegisterPersistenceFacade campaignRegisterPersistenceFacade;

    @EJB(mappedName = INJECTION_POINT + "CampaignPersistenceFacade")
    private CampaignPersistenceFacade campaignPersistenceFacade;

    @EJB(mappedName = INJECTION_POINT + "CampaignJoinerBean")
    private CampaignJoinerBean campaignJoinerBean;

    @Autowired
    private CampaignRegisterMapper campaignRegisterMapper;

    @Autowired
    private CampaignMinifiedMapper campaignMinifiedMapper;

    public List<CampaignRegisterDTO> getCampaignRegisters(Long subscriptionId) {
        Subscription subscription = subscriptionPersistenceFacade.find(subscriptionId);
        if (subscription == null)
            throw new CustomerOperationException("can not fetch campaign registers ,can not find subscription with id = " + subscriptionId);
        List<CampaignRegister> campaignRegisters = null;
        try {
            campaignRegisters = subscription.getCampaignRegisters();
            return campaignRegisters.stream().
                    map(campaignRegister -> campaignRegisterMapper.toDto(campaignRegister)).
                    collect(Collectors.toList());
        } catch (Exception e) {
            throw new CustomerOperationException(e.getMessage(), e);
        }
    }

    public void addNewCampaign(Long subscriptionId, CampaignMinifiedDTO campaignReq) {

        Subscription subscription = subscriptionPersistenceFacade.find(subscriptionId);
        if (subscription == null)
            throw new CustomerOperationException("can not add campaign ,can not find subscription with id = " + subscriptionId);

        Campaign campaign = campaignPersistenceFacade.find(campaignReq.getId());
        if (campaign == null)
            throw new CustomerOperationException("can not add campaign ,can not find campaign with id = " + campaignReq.getId());

        if (campaign.isActivateOnManualPaymentOnly())
            throw new CustomerOperationException("Please select a valid campaign ,This campaign should be activated through manual payment");

        if (subscription.getSubscriber().getDetails().getType().equals(SubscriberType.CORP)
                && !campaign.isCorporateEnabled())
            throw new CustomerOperationException("Cannot add this campaign to corporate clients ,This campaign should not be added to corporate clients");

        try {
            campaignJoinerBean.tryAddToCampaigns(subscription,
                    campaign,
                    true,
                    false,
                    campaignReq.getNotes());
        } catch (Exception e) {
            throw new CustomerOperationException("can not add campaign ," + e.getMessage(), e);
        }
    }

    public void activateCampaign(Long campaignRegisterId) {

        CampaignRegister campaignRegister = campaignRegisterPersistenceFacade.find(campaignRegisterId);
        if (campaignRegister == null)
            throw new CustomerOperationException("can not activate campaign ,can not find campaign register with id = " + campaignRegisterId);

        if (!campaignRegister.getCampaign().isActivateOnManualPaymentOnly()) {
            throw new CustomerOperationException("can not activate campaign");
        }

        try {
            campaignRegisterPersistenceFacade.tryActivateCampaign(campaignRegister);
        } catch (Exception e) {
            throw new CustomerOperationException("can not activate campaign ," + e.getMessage(), e);
        }
    }

    public void removeCampaign(Long campaignRegisterId) {
        CampaignRegister campaignRegister = campaignRegisterPersistenceFacade.find(campaignRegisterId);
        if (campaignRegister == null)
            throw new CustomerOperationException("can not remove campaign ,can not find campaign register with id = " + campaignRegisterId);

        try {
            if (campaignRegister.getCampaign().getTarget() == CampaignTarget.PAYMENT) {
                subscriptionPersistenceFacade.removePromo(campaignRegister);
            } else if (campaignRegister.getCampaign().getTarget() == CampaignTarget.EXPIRATION_DATE) {
                if (!subscriptionPersistenceFacade.rollbackBonusDate(campaignRegister)) {
                    throw new CustomerOperationException("No rollback date could be deducted");
                }
            }
            campaignRegisterPersistenceFacade.removeDetached(campaignRegister);
        } catch (Exception e) {
            throw new CustomerOperationException("can not remove campaign ," + e.getMessage(), e);
        }
    }


    public List<CampaignMinifiedDTO> getAvailableCampaignList(Long subscriptionId) {
        Subscription subscription = subscriptionPersistenceFacade.find(subscriptionId);
        if (subscription == null)
            throw new CustomerOperationException("can not fetch campaign registers ,can not find subscription with id = " + subscriptionId);

        List<CampaignRegister> regList = campaignRegisterPersistenceFacade.findActive(subscription);

        List<Campaign> cmpList = regList == null ?
                new ArrayList<>() :
                regList.stream().
                        map(campaignRegister -> campaignRegister.getCampaign()).
                        collect(Collectors.toList());

        List<Campaign> availableCampaignList = campaignPersistenceFacade.findAllActive(subscription.getService(), cmpList);

        filterAvailableCampaigns(availableCampaignList, subscription);
        return availableCampaignList.stream().map(campaign -> campaignMinifiedMapper.toDto(campaign)).collect(Collectors.toList());
    }

    private void filterAvailableCampaigns(List<Campaign> availableCampaignList, Subscription subscription) {
        List<CampaignRegister> campRegList = campaignRegisterPersistenceFacade.findAllBySubscription(subscription);
        boolean found = false;

        if (campRegList == null || campRegList.isEmpty()) {
            return;
        }

        Campaign cmp = null;
        Iterator<Campaign> it = availableCampaignList.iterator();

        while (it.hasNext()) {
            cmp = it.next();
            for (CampaignRegister reg : campRegList) {
                if (reg.getStatus().equals(CampaignStatus.PROCESSED)) {
                    continue;
                }
                if (reg.getCampaign().getId() == cmp.getId()) {
                    found = true;
                } else if (cmp.isCompound()) {
                    for (Campaign subCampaign : cmp.getCampaignList()) {
                        if (reg.getCampaign().getId() == subCampaign.getId()) {
                            found = true;
                        }
                    }
                }
                if (found) {
                    try {
                        it.remove();
                    } catch (Exception ex) {
                    }
                    found = false;
                }
            }
        }
    }


}
