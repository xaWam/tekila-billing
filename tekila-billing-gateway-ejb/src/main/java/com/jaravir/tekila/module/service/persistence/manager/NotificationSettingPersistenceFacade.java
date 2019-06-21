package com.jaravir.tekila.module.service.persistence.manager;

import com.jaravir.tekila.base.persistence.facade.AbstractPersistenceFacade;
import com.jaravir.tekila.module.event.BillingEvent;
import com.jaravir.tekila.module.event.notification.channell.NotificationChannell;
import com.jaravir.tekila.module.service.NotificationSetting;
import org.apache.log4j.Logger;

import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import java.util.List;

/**
 * Created by sajabrayilov on 12/24/2014.
 */
@Stateless
public class NotificationSettingPersistenceFacade extends AbstractPersistenceFacade<NotificationSetting> {
    @PersistenceContext
    private EntityManager em;

    private final static Logger log = Logger.getLogger(NotificationSettingPersistenceFacade.class);

    public NotificationSettingPersistenceFacade () {
        super(NotificationSetting.class);
    }

    @Override
    protected EntityManager getEntityManager() {
        return em;
    }

    public void updateAndDelete(NotificationSetting setting) {
        delete(update(setting));
    }

    public NotificationSetting find (BillingEvent event, List<NotificationChannell> channellList) {
        if (channellList == null || channellList.isEmpty())
            throw new IllegalArgumentException("Notification list must not be null");

        List<NotificationSetting> notifList =  getEntityManager().createQuery("select n from NotificationSetting n where n.event = :ev", NotificationSetting.class)
                .setParameter("ev", event).getResultList();

        if (notifList != null && !notifList.isEmpty())
            for (NotificationSetting set : notifList)
                if (set.getChannelList() != null && set.getChannelList().size() == channellList.size() && set.getChannelList().equals(channellList))
                    return set;

        NotificationSetting setting = new NotificationSetting(event, channellList);
        save(setting);

        return setting;
    }
}
