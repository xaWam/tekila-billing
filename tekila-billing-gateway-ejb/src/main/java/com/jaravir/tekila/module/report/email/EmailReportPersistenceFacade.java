package com.jaravir.tekila.module.report.email;

import com.jaravir.tekila.base.persistence.facade.AbstractPersistenceFacade;
import com.jaravir.tekila.module.report.EmailReport;
import org.joda.time.DateTime;

import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import java.util.Date;
import java.util.List;

/**
 * Created by kmaharov on 26.01.2017.
 */
@Stateless
public class EmailReportPersistenceFacade extends AbstractPersistenceFacade<EmailReport> {
    @PersistenceContext
    private EntityManager em;

    public EmailReportPersistenceFacade() {
        super(EmailReport.class);
    }

    @Override
    protected EntityManager getEntityManager() {
        return em;
    }

    public void saveReport(Date fromDate, Date toDate, String email, String selectedProviderId, String externalUserName) {
        EmailReport report = new EmailReport();
        report.setFromDate(new DateTime(fromDate));
        report.setToDate(new DateTime(toDate));
        report.setEmail(email);
        report.setSent(false);
        report.setLastUpdateDate();
        report.setSelectedProviderId(selectedProviderId);
        report.setExternalUserName(externalUserName);
        save(report);
    }

    public List<EmailReport> findNotSent() {
        return em.createQuery("SELECT emailReport FROM EmailReport emailReport "
                + "WHERE emailReport.sent = FALSE", EmailReport.class)
                .getResultList();
    }
}
