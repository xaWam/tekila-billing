/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.jaravir.tekila.module.stats.persistence.manager;

import com.jaravir.tekila.base.filter.Filterable;
import com.jaravir.tekila.base.filter.MatchingOperation;
import com.jaravir.tekila.base.persistence.facade.AbstractPersistenceFacade;
import com.jaravir.tekila.module.service.entity.ServiceProvider;
import com.jaravir.tekila.module.stats.persistence.entity.OfflineBroadbandStats;
import java.text.ParseException;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;

import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.persistence.PersistenceContext;
import javax.persistence.TemporalType;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

/**
 *
 * @author sajabrayilov
 */
@Stateless
public class OfflineStatsPersistenceFacade extends AbstractPersistenceFacade<OfflineBroadbandStats> {

    @PersistenceContext
    private EntityManager em;

    public enum Filter implements Filterable {

        ACCOUNT_ID("accountID"),
        START_DATE("startTime"),
        END_TIME("endTime"),
        FRAMED_ADDRESS("framedAddress"),
        NAS_IP_ADDRESS("nasIpAddress"),
        USERNAME("user"),
        TERMINATION_CAUSE("terminationCause"),
        CALLING_STATION("callingStationID"),
        PROVIDER("provider.id");

        private String field;
        private MatchingOperation operation;

        Filter(String field) {
            this.field = field;
            this.operation = MatchingOperation.LIKE;
        }

        @Override
        public String getField() {
            return field;
        }

        public void setField(String field) {
            this.field = field;
        }

        @Override
        public MatchingOperation getOperation() {
            return operation;
        }

        public void setOperation(MatchingOperation operation) {
            this.operation = operation;
        }
    }

    public OfflineStatsPersistenceFacade() {
        super(OfflineBroadbandStats.class);
    }

    @Override
    protected EntityManager getEntityManager() {
        return em;
    }

    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public OfflineBroadbandStats create(StatsRecord statsRecord, ServiceProvider provider) {
        OfflineBroadbandStats stats = new OfflineBroadbandStats();
        stats.setRadAccountID(statsRecord.getId());
        stats.setAccountSessionID(statsRecord.getAccountSessionID());
        stats.setUser(statsRecord.getUser());
        stats.setUp(statsRecord.getUp());
        stats.setDown(statsRecord.getDown());
        stats.setNasIpAddress(statsRecord.getNasIpAddress());
        stats.setFramedAddress(statsRecord.getFramedAddress());
        stats.setCallingStationID(statsRecord.getCallingStationID());
        stats.setStartTime(statsRecord.getStartDate());
        stats.setEndTime(statsRecord.getStopDate());
        stats.setProvider(provider);
        stats.setSessionDuration(statsRecord.getSessionDuration());
        stats.setTerminationCause(statsRecord.getTerminationCause());
        stats.setAccountID(statsRecord.getAccountID());
        stats.setDslamIpAddress(statsRecord.getDslamAddress());

        save(stats);
        return stats;
    }

    public long getLastIdByProvider(long providerId) {
        try {
                return em.createQuery("select max(s.radAccountID) from OfflineBroadbandStats s" +
                    " where s.provider.id = :providerId", Long.class).setParameter("providerId", providerId).getSingleResult();
        } catch (NoResultException ex) {
            return 0L;
        }
    }

    public Date getLastDateByProvider(long providerId) {
        Date lastDate = null;
        try {
            lastDate = em.createQuery("select max(s.endTime) from OfflineBroadbandStats s" +
                    " where s.provider.id = :providerId", Date.class).setParameter("providerId", providerId).getSingleResult();
        } catch (NoResultException ex) {
        }
        if (lastDate == null) {
            lastDate = new Date(0);
        }
        return lastDate;
    }

    public long getLastId() {
        return em.createQuery("select max(s.radAccountID) from OfflineBroadbandStats s", Long.class).getSingleResult();
    }

    public List<OfflineBroadbandStats> findByAgreement(String agreement, String startDate, String endDate) throws ParseException {

        DateTimeFormatter formatter = DateTimeFormat.forPattern("dd.MM.yyyy");
        Date start_date = formatter.parseDateTime(startDate).toDate();
        Date end_date = formatter.parseDateTime(endDate).toDate();

        return em.createQuery("select s from OfflineBroadbandStats s "
                + "where s.accountID=:agreement and s.startTime>=:startDate and s.startTime<=:endDate",
                OfflineBroadbandStats.class).
                setParameter("agreement", agreement).
                setParameter("startDate", start_date).
                setParameter("endDate", end_date).
                getResultList();
    }

    public List<OfflineBroadbandStats> getRecentSessions(String agreement) {
        return em.createQuery("select s from OfflineBroadbandStats s "
                        + "where s.accountID=:agreement order by s.startTime desc",
                OfflineBroadbandStats.class).
                setParameter("agreement", agreement).
                setFirstResult(0).
                setMaxResults(5).
                getResultList();
    }
}