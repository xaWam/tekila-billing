package com.jaravir.tekila.extern.tt.manager;

import com.jaravir.tekila.base.persistence.facade.AbstractPersistenceFacade;
import com.jaravir.tekila.extern.tt.TroubleTicket;
import com.jaravir.tekila.module.subscription.persistence.entity.Subscriber;
import com.jaravir.tekila.module.subscription.persistence.entity.Subscription;

import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

/**
 * Created by sajabrayilov on 12/16/2014.
 */
@Stateless
public class TroubleTicketPersistenceFacade extends AbstractPersistenceFacade<TroubleTicket> {

    @PersistenceContext
    private EntityManager em;

    public TroubleTicketPersistenceFacade() {
        super(TroubleTicket.class);
    }

    @Override
    protected EntityManager getEntityManager() {
        return em;
    }

    public List<TroubleTicket> findAllBySubscriber(Subscriber suscriber) {
        List<Subscription> subscriptions = suscriber.getSubscriptions();

        if (subscriptions == null || subscriptions.isEmpty()) {
            return null;
        }

        List<String> sbnIdList = new ArrayList<>();

        for (Subscription sbn : subscriptions) {
            sbnIdList.add(sbn.getAgreement());
        }

        return getEntityManager().createQuery("select t from TroubleTicket t where t.agreement   in :contracts")
                .setParameter("contracts", sbnIdList).getResultList();
    }

    public List<TroubleTicket> findByAgreementAndDates(String agreement, String startDate, String endDate) {

        DateTimeFormatter formatter = DateTimeFormat.forPattern("dd.MM.yyyy");
        Date start_date = formatter.parseDateTime(startDate).toDate();
        Date end_date = formatter.parseDateTime(endDate).toDate();

        return getEntityManager().createQuery("select t from TroubleTicket t "
                + "where t.agreement=:agreement and t.created>=:start_date and t.created<=:end_date")
                .setParameter("agreement", agreement)
                .setParameter("start_date", start_date)
                .setParameter("end_date", end_date)
                .getResultList();
    }
}
