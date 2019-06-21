package com.jaravir.tekila.module.accounting.periodic;

import com.jaravir.tekila.module.report.EmailReport;
import com.jaravir.tekila.module.report.email.EmailReportPersistenceFacade;
import com.jaravir.tekila.module.report.export.xlsx.ExcelExporter;
import com.jaravir.tekila.module.service.entity.ServiceProvider;
import com.jaravir.tekila.module.service.persistence.manager.ServiceProviderPersistenceFacade;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.activation.DataHandler;
import javax.activation.DataSource;
import javax.activation.FileDataSource;
import javax.annotation.Resource;
import javax.annotation.security.DeclareRoles;
import javax.annotation.security.RolesAllowed;
import javax.annotation.security.RunAs;
import javax.ejb.EJB;
import javax.ejb.Schedule;
import javax.ejb.Singleton;
import javax.ejb.Startup;
import javax.mail.Message;
import javax.mail.Multipart;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;
import java.io.FileOutputStream;
import java.util.List;

/**
 * Created by kmaharov on 26.01.2017.
 */
@DeclareRoles({"system"})
@RunAs("system")
/**  Tekila JOBS runs on tekila_jobs branch  */ // @Startup
@Singleton
//@Stateless
public class EmailReporter {
    private final static Logger log = LoggerFactory.getLogger(EmailReporter.class);
    @EJB
    private EmailReportPersistenceFacade emailReportPersistenceFacade;
    @EJB
    private ExcelExporter excelExporter;
    @EJB
    private ServiceProviderPersistenceFacade serviceProviderPersistenceFacade;
    @Resource(name="mail/tekilaSession")
    private Session mailSession;

    @RolesAllowed("system")
    //@PostConstruct
    /**  Tekila JOBS runs on tekila_jobs branch  */ // @Schedule(hour = "*", minute = "*/10")
    //@Schedule(hour = "*", minute = "*/1")
//    @Schedule(hour = "*", minute = "*/5")
//    @Schedule(hour = "*", minute = "*")
    public void reportEmail() {
        List<EmailReport> reports = emailReportPersistenceFacade.findNotSent();

        long start = System.currentTimeMillis();
        for (EmailReport report : reports) {
            if (report.isSent()) {
                continue;
            }
            report.setSent(true);
            report = emailReportPersistenceFacade.update(report);
            String fileName = "report_" + System.currentTimeMillis() + ".xls";
            log.info("report filename = " + fileName + ", requested user email = " + report.getEmail());

            try {
                FileOutputStream fos = new FileOutputStream(fileName);
                excelExporter.generatePaymentReport(
                        fos,
                        report.getFromDate().toDate(),
                        report.getToDate().toDate(),
                        report.getSelectedProviderId(),
                        report.getExternalUserName());
                fos.close();
            } catch (Exception ex) {
                log.error("Exception on email report = " + ex);
                continue;
            }


            Message msg = new MimeMessage(mailSession);

            try {
                msg.setFrom(new InternetAddress("no-reply@narhome.az", "NarHome"));
                //msg.setFrom(new InternetAddress("//my gmail", "Sr."));
                msg.addRecipient(Message.RecipientType.TO,
                        new InternetAddress(report.getEmail()));
                StringBuilder subjectBuilder = new StringBuilder(String.format(
                        "Payment report from %s till %s",
                        report.getFromDate().toString(),
                        report.getToDate().toString()));
                if (report.getExternalUserName() != null && !report.getExternalUserName().equals("-1")) {
                    subjectBuilder.append(", external user name = " + report.getExternalUserName());
                }
                if (report.getSelectedProviderId() != null && !report.getSelectedProviderId().equals("-1")) {
                    ServiceProvider provider = serviceProviderPersistenceFacade.find(Integer.parseInt(report.getSelectedProviderId()));
                    subjectBuilder.append(", provider = " + provider.getName());
                }
                msg.setSubject(subjectBuilder.toString());
                //TO,From and all the mail details goes here

                DataSource fds = new FileDataSource(fileName);

                MimeBodyPart mbp1 = new MimeBodyPart();
                mbp1.setText("Please find report in an attached file");

                MimeBodyPart mbp2 = new MimeBodyPart();
                mbp2.setDataHandler(new DataHandler(fds));
                mbp2.setFileName(
                        String.format(
                                "payment_report_%s-%s.xls",
                                report.getFromDate().toString(),
                                report.getToDate().toString())
                );

                Multipart mp = new MimeMultipart();
                mp.addBodyPart(mbp1);
                mp.addBodyPart(mbp2);
                msg.setContent(mp);
                msg.saveChanges();

                // Set the Date: header
                msg.setSentDate(new java.util.Date());

                Transport.send(msg);
            } catch (Exception ex) {
                log.error("Exception on reportEmail : {}", ex);
            }
            emailReportPersistenceFacade.update(report);
        }

        log.info("reportEmail  elapsed time : {}", (System.currentTimeMillis()-start)/1000);
    }
}