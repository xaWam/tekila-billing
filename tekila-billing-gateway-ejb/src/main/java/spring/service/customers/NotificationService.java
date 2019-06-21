package spring.service.customers;

import com.jaravir.tekila.module.event.BillingEvent;
import com.jaravir.tekila.module.event.notification.channell.NotificationChannell;
import com.jaravir.tekila.module.service.NotificationSetting;
import com.jaravir.tekila.module.subscription.persistence.entity.Subscription;
import com.jaravir.tekila.module.subscription.persistence.management.SubscriptionPersistenceFacade;
import org.apache.log4j.Logger;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;
import spring.dto.NotificationSettingDTO;
import spring.exceptions.CustomerOperationException;

import javax.ejb.EJB;

import java.util.ArrayList;
import java.util.List;

import static spring.util.Constants.INJECTION_POINT;

/**
 * @author ElmarMa on 3/27/2018
 */
@Service
public class NotificationService {

    private static final Logger logger = Logger.getLogger(NotificationService.class);


    @EJB(mappedName = INJECTION_POINT + "SubscriptionPersistenceFacade")
    private SubscriptionPersistenceFacade subscriptionPersistenceFacade;


    public List<NotificationSettingDTO> getNotifications(Long subscriptionId) {
        Subscription subscription = subscriptionPersistenceFacade.find(subscriptionId);
        if (subscription == null)
            throw new CustomerOperationException("can not fetch notification settings ,can not find subscription with id = " + subscriptionId);
        List<NotificationSettingDTO> notificationSettings = new ArrayList<>();
        if (subscription.getNotificationSettings() == null || subscription.getNotificationSettings().isEmpty()) {
            for (BillingEvent billingEvent : BillingEvent.values()) {
                notificationSettings.add(new NotificationSettingDTO(billingEvent));
            }
        } else {
            for (BillingEvent be : BillingEvent.values()) {
                NotificationSetting setting = subscription.getNotificationSettingByEvent(be);
                if (setting == null)
                    notificationSettings.add(new NotificationSettingDTO(be));
                else
                    notificationSettings.add(new NotificationSettingDTO(be,
                            setting.getChannelList().contains(NotificationChannell.SMS),
                            setting.getChannelList().contains(NotificationChannell.EMAIL),
                            setting.getChannelList().contains(NotificationChannell.SCREEN)));
            }
        }

        return notificationSettings;
    }

    public void updateNotificationSettings(Long subscriptionId, List<NotificationSettingDTO> settings) {
        Subscription subscription = subscriptionPersistenceFacade.find(subscriptionId);
        if (subscription == null)
            throw new CustomerOperationException("can not update notification settings ,can not find subscription with id = " + subscriptionId);

        List<NotificationSetting> changedSettings = new ArrayList<>();
        for (NotificationSettingDTO dto : settings) {
            NotificationSetting setting = new NotificationSetting(dto.getBillingEvent(), new ArrayList<>());
            if (dto.isSms()) {
                setting.getChannelList().add(NotificationChannell.SMS);
            }
            if (dto.isEmail()) {
                setting.getChannelList().add(NotificationChannell.EMAIL);
            }
            if (dto.isScreen()) {
                setting.getChannelList().add(NotificationChannell.SCREEN);
            }
            changedSettings.add(setting);
        }
        try {
            subscriptionPersistenceFacade.update(subscription, changedSettings);
            logger.debug("completed >>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>");
        } catch (Exception e) {
            throw new CustomerOperationException("can not update notification settings ," + e.getMessage(), e);
        }
    }


}
