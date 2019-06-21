package spring.service;

import com.jaravir.tekila.module.subscription.persistence.entity.SubscriberDetails;
import com.jaravir.tekila.module.subscription.persistence.management.SubscriberDetailsPeristenceFacade;
import com.jaravir.tekila.module.subscription.persistence.management.SubscriptionPersistenceFacade;
import com.jcraft.jsch.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import spring.exceptions.BadRequestAlertException;
import spring.model.Record;
import spring.model.RecordType;
import spring.model.Report;
import spring.util.MarshallingUtils;
import javax.ejb.EJB;
import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import static spring.util.Constants.INJECTION_POINT;

/**
 * @author GurbanAz
 * @date 11/04/2019
 */

@Service
@EnableScheduling
public class XDMXService {


    private static final Logger log = LoggerFactory.getLogger(XDMXService.class);

    @EJB(mappedName = INJECTION_POINT + "SubscriberDetailsPeristenceFacade")
    private SubscriberDetailsPeristenceFacade subscriberDetailsPeristenceFacade;

    @EJB(mappedName = INJECTION_POINT + "SubscriptionPersistenceFacade")
    private SubscriptionPersistenceFacade subscriptionPersistenceFacade;

    public List<File> getAllReport() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
        long count = count();
        log.info("~~~~~" + count + "~~~~~");

        int loop = 1;
        int size = 3000;
        int records = 3000;
        File file = null;
        List<File> fileList = new ArrayList<>();

        while (records < count) {
            List<SubscriberDetails> list = subscriberDetailsPeristenceFacade.findAllPaginated((loop - 1) * size, size);
            log.info("SubscriberDetailsRecords from: " + (loop - 1) * size + " to: " + (loop * size));
            Report report = createReport(list);
            report.setEndDate(sdf.format(new Date()));
            file = MarshallingUtils.subscriptionToXML(sdf.format(new Date()) + "-" + loop, report);
            log.debug("~~~~~~~" + file.getName());
            fileList.add(file);
            records += 3000;
            loop++;
            upload(file.getName());
        }

        return fileList;
    }


    /**
     * Tekila JOBS run on tekila_jobs branch*/
    //@Scheduled(cron = "0 18 01 * * *")
    public File createXmlForUpdatedSubscribers(){
        log.debug("~~~Job starts for creating file for updated subscribers");
        List<SubscriberDetails> list = subscriberDetailsPeristenceFacade.getAllUpdatesNative();
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
        if(list == null && list.isEmpty()){
            log.debug("~~~No Added or updates Subscribers found...");
            throw new BadRequestAlertException("~~~No Added or updates Subscribers found...");
        }
        log.debug("~~~Creating records for report starts...");
        Report report = createReport(list);
        report.setEndDate(sdf.format(new Date()));
        report.setEndDate(sdf.format(new Date()));
        File file = null;
        file = MarshallingUtils.subscriptionToXML(sdf.format(new Date()), report);
        upload(file.getName());
        log.debug("~~~~~~~" + file.getName());

        return file;
    }


    public static void upload(String fileName) {
        JSch jSch = new JSch();
        Session session = null;
        try{
            session = jSch.getSession("azertelecom", "10.2.0.8", 22);
            session.setConfig("StrictHostKeyChecking", "no");
            session.setPassword("A.66T&com");
            session.connect();

            Channel channel = session.openChannel("sftp");
            channel.connect();
            ChannelSftp sftpChannel = (ChannelSftp) channel;
            String localDestination = "/opt/payara 4.1.2.173/glassfish/domains/tekila/config/";
            String remoteDestination = "/dr/sdr_isp/";
            sftpChannel.put(localDestination + fileName, remoteDestination + fileName);
            sftpChannel.exit();
        }catch (JSchException  | SftpException e){
            e.printStackTrace();
        }
    }




    private Report createReport(List<SubscriberDetails> subscriberDetailsList){
        if(subscriberDetailsList == null || subscriberDetailsList.isEmpty()){
            throw new BadRequestAlertException("Report cannot be created without subscriber");
        }

        Report report = new Report();
        report.setRecords(new ArrayList<>());
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy:MM:dd");
        report.setStartDate(sdf.format(new Date()));
        report.setReportingEntity("System");

        for(SubscriberDetails sd : subscriberDetailsList){
            Record record = new Record();
            record.setRecordType(RecordType.A.toString());
            if(sd.getSubscriber() != null && sd.getSubscriber().getSubscriptions() != null && sd.getSubscriber().getSubscriptions().size() > 0){
                record.setReferenceNo(sd.getSubscriber().getSubscriptions().get(0).getAgreement());
                record.setSid(sd.getSubscriber().getSubscriptions().get(0).getAgreement());
            }else if(sd.getSubscriber() != null && sd.getSubscriber().getSubscriptions() == null && sd.getSubscriber().getSubscriptions().isEmpty()){
                record.setReferenceNo(String.valueOf(sd.getId()));
                record.setSid(String.valueOf(sd.getId()));
            }
            record.setSubscriberType(sd.getType().toString());
            record.setNationalId(sd.getPinCode());
            record.setPassportNo(sd.getPassportNumber());
            record.setTaxId(" ");
            if(sd.getFirstName() != null || sd.getSurname() != null){
                record.setName(sd.getFirstName());
                record.setSurname(sd.getSurname());
            }else if(sd.getSubscriber() != null && sd.getSubscriber().getSubscriptions() != null && sd.getSubscriber().getSubscriptions().size() > 0){
                if(sd.getFirstName() == null || sd.getSurname() == null){
                    record.setName(sd.getSubscriber().getSubscriptions().get(0).getDetails().getName());
                    record.setName(sd.getSubscriber().getSubscriptions().get(0).getDetails().getSurname());
                }
            }
            record.setNationality(sd.getCitizenship());
            record.setBirthdate(sd.getDateOfBirth());
            record.setBirthPlace(sd.getCityOfBirth());
            record.setFatherName(sd.getMiddleName());
            record.setMotherName(" ");
            record.setAddress(sd.getStreet());
            record.setCity(sd.getCity());
            record.setCountry(sd.getCountry());
            record.setRecordDate(sd.getEntryDate());
            report.getRecords().add(record);
        }
        log.info("Report Model created with " + subscriberDetailsList.size() + " records");
        return report;
    }

    public Long count() {
        return subscriptionPersistenceFacade.count();
    }




}
