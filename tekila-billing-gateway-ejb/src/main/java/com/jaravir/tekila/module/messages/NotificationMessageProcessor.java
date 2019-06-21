package com.jaravir.tekila.module.messages;

import com.azerfon.billing.narhome.verimatrix.OMIController;
import com.cloudhopper.commons.charset.CharsetUtil;
import com.cloudhopper.commons.util.windowing.WindowFuture;
import com.cloudhopper.smpp.SmppBindType;
import com.cloudhopper.smpp.SmppConstants;
import com.cloudhopper.smpp.SmppSession;
import com.cloudhopper.smpp.SmppSessionConfiguration;
import com.cloudhopper.smpp.impl.DefaultSmppClient;
import com.cloudhopper.smpp.impl.DefaultSmppSessionHandler;
import com.cloudhopper.smpp.pdu.*;
import com.cloudhopper.smpp.tlv.Tlv;
import com.jaravir.tekila.engines.EngineFactory;
import com.jaravir.tekila.module.accounting.entity.Invoice;
import com.jaravir.tekila.module.accounting.manager.InvoicePersistenceFacade;
import com.jaravir.tekila.module.event.BillingEvent;
import com.jaravir.tekila.module.event.notification.Notification;
import com.jaravir.tekila.module.event.notification.channell.NotificationChannelStatus;
import com.jaravir.tekila.module.event.notification.channell.NotificationChannell;
import com.jaravir.tekila.module.event.notification.parser.Parser;
import com.jaravir.tekila.module.notification.NotificationChannelPersistenceFacade;
import com.jaravir.tekila.module.notification.NotificationPersistenceFacade;
import com.jaravir.tekila.module.service.ServiceSettingType;
import com.jaravir.tekila.module.service.entity.ValueAddedService;
import com.jaravir.tekila.module.service.persistence.manager.VASPersistenceFacade;
import com.jaravir.tekila.module.subscription.persistence.entity.Subscriber;
import com.jaravir.tekila.module.subscription.persistence.entity.SubscriberDetails;
import com.jaravir.tekila.module.subscription.persistence.entity.SubscriberRuntimeDetails;
import com.jaravir.tekila.module.subscription.persistence.entity.Subscription;
import com.jaravir.tekila.module.subscription.persistence.management.SubscriptionPersistenceFacade;
import com.jaravir.tekila.module.system.SystemEvent;
import com.jaravir.tekila.module.system.log.SystemLogger;
import com.jaravir.tekila.provision.broadband.devices.manager.Providers;
import org.apache.log4j.Logger;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.annotation.Resource;
import javax.annotation.security.DeclareRoles;
import javax.annotation.security.RunAs;
import javax.ejb.EJB;
import javax.ejb.Singleton;
import javax.ejb.Startup;
import javax.mail.*;
import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by kmaharov on 19.09.2017.
 */
@DeclareRoles({"system"})
@RunAs("system")
@Singleton
/**  Tekila JOBS runs on tekila_jobs branch  */ // @Startup
public class NotificationMessageProcessor {
    @Resource(name = "mail/tekilaSession")
    private Session mailSession;

    @EJB
    private PersistentMessagePersistenceFacade messagePersistenceFacade;
    @EJB
    private SubscriptionPersistenceFacade subscriptionFacade;
    @EJB
    private EngineFactory engineFactory;
    @EJB
    private SystemLogger systemLogger;
    @EJB
    private InvoicePersistenceFacade invoiceFacade;
    @EJB
    private VASPersistenceFacade vasFacade;
    @EJB
    private NotificationPersistenceFacade notificationFacade;
    @EJB
    private NotificationChannelPersistenceFacade channelPersistenceFacade;

    private final static Parser parser = new Parser();
    private final static int MIN_PHONE_LENGTH = 7;
    private static Properties mailRelayProperties;
    private final static String MAIL_HOST = "";
    private final static String MAIL_PORT = "";

    private DefaultSmppClient smppClient;
    private SmppSession smppSession;

    private ExecutorService notificationExecutor;
    private volatile AtomicBoolean consumeQueues = new AtomicBoolean(true);

    private static final Logger log = Logger.getLogger(NotificationMessageProcessor.class);

    private class NotificationProcessor implements Runnable {
        private final List<PersistentMessage> messages;
        private final int messageIdx;

        public NotificationProcessor(final List<PersistentMessage> messages, final int messageIdx) {
            this.messages = messages;
            this.messageIdx = messageIdx;
        }

        private void notifyByEmail(Subscriber subscriber, String subject, String notification1) {
            //Session session = Session.getDefaultInstance(mailRelayProperties, null);
            //session.setDebug(true);
            if (subscriber.getDetails().getEmail() == null || subscriber.getDetails().getEmail().isEmpty() || !validateEmail(subscriber.getDetails().getEmail())) {
                log.error("Email not valid for subscriber: " + subscriber);
                return;
            }

            MimeMessage message = new MimeMessage(mailSession);
            Address recipient = null;
            try {
           /* Language language = (subscriber.getDetails().getLanguage() == null
                || subscriber.getDetails().getLanguage().isEmpty()) ? subscriber.getDetails().getLanguage() :
            Notification notification = notificationFacade.findNotificationByEventAndChannelAndLanguage(
                    BillingEvent.PAYMENT, NotificationChannell.EMAIL, ()
            )*/
                Address recipientAddress = new InternetAddress(subscriber.getDetails().getEmail());
                message.setFrom(new InternetAddress("no-reply@narhome.az", "NarHome"));
                message.setRecipient(Message.RecipientType.TO, recipientAddress);
                message.setSentDate(new Date());
                message.setSubject(subject);
                message.setText(notification1);

                Transport.send(message);
            } catch (AddressException ex) {
                log.error("Cannot parse address: ", ex);
            } catch (UnsupportedEncodingException ex) {
                log.error("Cannot parse From address: ", ex.getCause());
            } catch (MessagingException ex) {
                log.error("Cannot send notification", ex.getCause());
            }
        }



        private void notifyBySMSGateway(Subscription subscription, String text) throws Exception {
            String phone = subscription.getSubscriber().getDetails().getPhoneMobile();
            //String phone = "994702011390";
            if (phone == null || phone.length() <= MIN_PHONE_LENGTH) {
                log.info(String.format("phone not specified for agreement = %s. not sending sms.", subscription.getAgreement()));
                return;
            }
            URL url = new URL("http://gw.maradit.net/api/xml/reply/Submit");
            //Proxy proxy = new Proxy(Proxy.Type.HTTP, new InetSocketAddress("10.10.1.98", 12321));
            //conn = new URL(urlString).openConnection(proxy);
            HttpURLConnection con = (HttpURLConnection) url.openConnection();

            //add reuqest header
            con.setRequestMethod("POST");
            con.setDoOutput(true);
            con.setRequestProperty("Content-Type", "application/xml");

            String providerName = ((subscription.getService().getProvider().getId() == Providers.CITYNET.getId()) ? "Citynet" : "Uninet");

            String data = String.format("<Submit xmlns:i=\"http://www.w3.org/2001/XMLSchema-instance\" xmlns=\"http://schemas.maradit.net/api/types\">\n" +
                    "  <Credential>\n" +
                    "    <Password>superonline02</Password>\n" +
                    "    <Username>superonline1</Username>\n" +
                    "  </Credential>\n" +
                    "  <DataCoding>Default</DataCoding>\n" +
                    "  <Header>\n" +
                    "    <From>%s</From>\n" +
                    "    <Route>0</Route>\n" +
                    "    <ScheduledDeliveryTime>0001-01-01T00:00:00</ScheduledDeliveryTime>\n" +
                    "    <ValidityPeriod>0</ValidityPeriod>\n" +
                    "  </Header>\n" +
                    "  <Message>%s</Message>\n" +
                    "  <To xmlns:d2p1=\"http://schemas.microsoft.com/2003/10/Serialization/Arrays\">\n" +
                    "    <d2p1:string>%s</d2p1:string>\n" +
                    "  </To>\n" +
                    "</Submit>", providerName, text, phone);

            // Send post request
            con.setRequestProperty("Content-Length", String.valueOf(data.length()));
            OutputStream outputStream = con.getOutputStream();
            //con.getOutputStream().write(data.getBytes());
            outputStream.write(data.getBytes());
            outputStream.flush();
            outputStream.close();

            int responseCode = con.getResponseCode();
            System.out.println("\nSending 'POST' request to URL : " + url);
            //System.out.println("Post parameters : " + urlParameters);
            System.out.println("Response Code : " + responseCode);

            BufferedReader in = new BufferedReader(
                    new InputStreamReader(con.getInputStream()));
            String inputLine;
            StringBuffer response = new StringBuffer();

            while ((inputLine = in.readLine()) != null) {
                response.append(inputLine + "\n");
            }
            in.close();

            //print result
            System.out.println(response.toString());
        }

        private void notifyBySMSGatewaySOFTLINE(Subscription subscription, String text) throws Exception {
        log.info("notifyBySMSGatewaySOFTLINE is started");
            String phone = subscription.getSubscriber().getDetails().getPhoneMobile();
            //String phone = "994702011390";
        log.info(subscription.getAgreement()+" phone number is "+phone);
            if (phone == null || phone.length() <= MIN_PHONE_LENGTH) {
                log.info(String.format("phone not specified for agreement = %s. not sending sms.", subscription.getAgreement()));
                return;
            }
            URL url = new URL("https://gw.soft-line.az/sendsms");
            //Proxy proxy = new Proxy(Proxy.Type.HTTP, new InetSocketAddress("10.10.1.98", 12321));
            //conn = new URL(urlString).openConnection(proxy);
            HttpURLConnection con = (HttpURLConnection) url.openConnection();
            log.info("url is "+url.toString());
            //add reuqest header
            con.setRequestMethod("POST");
            con.setDoOutput(true);
            con.setRequestProperty("Content-Type", "application/xml");

            String providerName = ((subscription.getService().getProvider().getId() == Providers.CITYNET.getId()) ? "Citynet" : "Uninet");

            String data = String.format("<?xml version=\"1.0\" encoding=\"utf-8\"?>\n" +
                    "<SMS-InsRequest>\n" +
                    "<CLIENT user=\"uninetapi\" pwd=\"bMB74gYq\" from=\"%s\"/>\n" +
                    "<INSERTMSG text=\"%s\">\n" +
                    "<TO>%s</TO>\n" +
                    "</INSERTMSG>\n" +
                    "</SMS-InsRequest>",providerName,  text, phone);
            log.info("API body is "+data);
            // Send post request
            con.setRequestProperty("Content-Length", String.valueOf(data.length()));
            OutputStream outputStream = con.getOutputStream();
            //con.getOutputStream().write(data.getBytes());
            outputStream.write(data.getBytes());
            outputStream.flush();
            outputStream.close();

            int responseCode = con.getResponseCode();
            log.info(" Agreeement "+ subscription.getAgreement()+" Sending 'POST' request to URL : " + url);
            //System.out.println("Post parameters : " + urlParameters);
            log.info(subscription.getAgreement()+" Response Code : " + responseCode);

            BufferedReader in = new BufferedReader(
                    new InputStreamReader(con.getInputStream()));
            String inputLine;
            StringBuffer response = new StringBuffer();

            while ((inputLine = in.readLine()) != null) {
                response.append(inputLine + "\n");
            }
            in.close();

            //print result
            log.info(response.toString());
        }

        private void notifyByDataplusSMSGatewaySOFTLINE(Subscription subscription, String text) throws Exception {
            String phone = subscription.getSubscriber().getDetails().getPhoneMobile();
            //String phone = "994702011390";
            String providerName = ((subscription.getService().getProvider().getId() == Providers.CITYNET.getId()) ? "Citynet" : "Uninet");
            if (phone == null || phone.length() <= MIN_PHONE_LENGTH) {
                log.info(String.format("phone not specified for agreement = %s. not sending sms.", subscription.getAgreement()));
                return;
            }
            URL url = new URL(
                    String.format("http://gw.soft-line.az/sendsms?user=uninetapi&password=bMB74gYq&gsm=%s&from=%s&text=%s", phone, providerName, text).
//                    String.format("http://api.msm.az/sendsms?user=dataplusapi&password=KcPjoXX0&gsm=%s&from=Data Plus&text=%s", phone, text).
                            replaceAll(" ", "%20"));
log.info("GET API is "+url.toString());
            //Proxy proxy = new Proxy(Proxy.Type.HTTP, new InetSocketAddress("10.10.1.98", 12321));
            //conn = new URL(urlString).openConnection(proxy);
            HttpURLConnection con = (HttpURLConnection) url.openConnection();

            //add reuqest header
            con.setRequestMethod("GET");
            con.setDoOutput(true);
            //con.setRequestProperty( "Content-Type", "application/xml" );

            int responseCode = con.getResponseCode();
            log.info("Sending 'GET' request to URL : " + url);
            //System.out.println("Post parameters : " + urlParameters);
            log.info("Response Code : " + responseCode);

            BufferedReader in = new BufferedReader(
                    new InputStreamReader(con.getInputStream()));
            String inputLine;
            StringBuffer response = new StringBuffer();

            while ((inputLine = in.readLine()) != null) {
                response.append(inputLine + "\n");
            }
            in.close(); 

            //print result
            log.info(response.toString());
        }

        private void notifyBySMS(Subscriber subscriber, String text) throws Exception {
            if (smppSession == null) {
                log.error("notifyBySMS: SMPP session not found. Aborting...");
                return;
            }

            String phoneNumber = subscriber.getDetails().getPhoneMobile();
            if (phoneNumber == null || phoneNumber.isEmpty()) {
                log.error(String.format("Phone %s not valid for subscriber %s", phoneNumber, subscriber));
                return;
            }

            log.debug(String.format("Sending sms. PhoneNumber=%s", phoneNumber));

            try {
                // and then optionally choose to pick when we wait for it
                WindowFuture<Integer, PduRequest, PduResponse> future0 = smppSession.sendRequestPdu(new EnquireLink(), 10000, true);
                if (!future0.await()) {
                    log.error("Failed to receive enquire_link_resp within specified time");
                } else if (future0.isSuccess()) {
                    EnquireLinkResp enquireLinkResp2 = (EnquireLinkResp) future0.getResponse();
                    log.info("enquire_link_resp #2: commandStatus [" + enquireLinkResp2.getCommandStatus() + "=" + enquireLinkResp2.getResultMessage() + "]");
                } else {
                    log.error("Failed to properly receive enquire_link_resp: " + future0.getCause());
                }

                //String text160 = "\u20AC Lorem [ipsum] dolor sit amet, consectetur adipiscing elit. Proin feugiat, leo id commodo tincidunt, nibh diam ornare est, vitae accumsan risus lacus sed sem metus.";

                //byte[] textBytes = CharsetUtil.encode(text, CharsetUtil.CHARSET_UCS_2);
                byte[] textBytes = CharsetUtil.encode(text, CharsetUtil.CHARSET_GSM);

                SubmitSm submit0 = new SubmitSm();

                // add delivery receipt
                //submit0.setRegisteredDelivery(SmppConstants.REGISTERED_DELIVERY_SMSC_RECEIPT_REQUESTED);
                //Tlv tlv = new Tlv(SmppConstants.TAG_MESSAGE_PAYLOAD, textBytes, "message_payload");
                //submit0.setOptionalParameter(tlv);
                //submit0.setShortMessage(new byte[0]);
                //submit0.addOptionalParameter(new Tlv((short)0x0424, textBytes)); //0x0424 is an optional parameter code for payload
                submit0.setOptionalParameter(new Tlv(SmppConstants.TAG_MESSAGE_PAYLOAD, textBytes));
                submit0.setShortMessage("".getBytes());
                submit0.setSourceAddress(new com.cloudhopper.smpp.type.Address((byte) 0x05, (byte) 0x00, "NarHome"));
                submit0.setDestAddress(new com.cloudhopper.smpp.type.Address((byte) 0x01, (byte) 0x01, phoneNumber));
                log.debug("Destination address is " + submit0.getDestAddress().getAddress());

                SubmitSmResp submitResp = smppSession.submit(submit0, 10000);

                log.info(String.format("sendWindow.size: %s", smppSession.getSendWindow().getSize()));
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }

        private void notifyByDataplusSMSGateway(Subscription subscription, String text) throws Exception {
            String phone = subscription.getSubscriber().getDetails().getPhoneMobile();
            String providerName = "Data Plus";
            //String phone = "994702011390";
            log.debug(subscription.getAgreement()+ " phone "+phone);
            if (phone == null || phone.length() <= MIN_PHONE_LENGTH) {
                log.info(String.format("phone not specified for agreement = %s. not sending sms.", subscription.getAgreement()));
                return;
            }
            if (subscription.getService().getProvider().getId() == 454111){
                providerName = "Data Plus";
            }else if (subscription.getService().getProvider().getId() == 454112){
                providerName = "Global Networks";
            }else{
                providerName = "Pilot";
            }
            log.debug("providerName "+providerName);
            URL url = new URL(
                    String.format("http://api.msm.az/sendsms?user=dataplusapi&password=KcPjoXX0&gsm=%s&from=%s&text=%s", phone, providerName, text).
                            replaceAll(" ", "%20"));
            log.debug("SMS url "+url.toString());
            //Proxy proxy = new Proxy(Proxy.Type.HTTP, new InetSocketAddress("10.10.1.98", 12321));
            //conn = new URL(urlString).openConnection(proxy);
            HttpURLConnection con = (HttpURLConnection) url.openConnection();

            //add reuqest header
            con.setRequestMethod("GET");
            con.setDoOutput(true);
            //con.setRequestProperty( "Content-Type", "application/xml" );

            int responseCode = con.getResponseCode();
            log.debug("Sending 'GET' request to URL : " + url);
            //System.out.println("Post parameters : " + urlParameters);
            log.debug("Response Code : " + responseCode);

            BufferedReader in = new BufferedReader(
                    new InputStreamReader(con.getInputStream()));
            String inputLine;
            StringBuffer response = new StringBuffer();

            while ((inputLine = in.readLine()) != null) {
                response.append(inputLine + "\n");
            }
            in.close();

            //print result
            System.out.println(response.toString());
        }

        private void notifyByGlobalSMSGateway(Subscription subscription, String text) throws Exception {
            String phone = subscription.getSubscriber().getDetails().getPhoneMobile();
            //String phone = "994702011390";
            log.debug(subscription.getAgreement()+ " phone "+phone);
            if (phone == null || phone.length() <= MIN_PHONE_LENGTH) {
                log.info(String.format("phone not specified for agreement = %s. not sending sms.", subscription.getAgreement()));
                return;
            }


            URL url = new URL(
                    String.format("http://www.poctgoyercini.com/api_http/sendsms.asp?user=qlobalSMPP&password=qlobal123&gsm=%s&text=%s", phone, text)
                            .replaceAll(" ", "%20"));
            log.debug("SMS url "+url.toString());
            //Proxy proxy = new Proxy(Proxy.Type.HTTP, new InetSocketAddress("10.10.1.98", 12321));
            //conn = new URL(urlString).openConnection(proxy);
            HttpURLConnection con = (HttpURLConnection) url.openConnection();

            //add reuqest header
            con.setRequestMethod("GET");
            con.setDoOutput(true);
            //con.setRequestProperty( "Content-Type", "application/xml" );
            log.debug("Sending SMS to "+ subscription.getAgreement() + " url "+  url);
            int responseCode = con.getResponseCode();
            log.debug("Sending GET request to URL : " + url);
            //System.out.println("Post parameters : " + urlParameters);
            log.debug("Response Code : " + responseCode);

            BufferedReader in = new BufferedReader(
                    new InputStreamReader(con.getInputStream()));
            String inputLine;
            StringBuffer response = new StringBuffer();

            while ((inputLine = in.readLine()) != null) {
                response.append(inputLine + "\n");
            }
            in.close();

            //print result
            System.out.println(response.toString());
        }

        private void notifyByCNCSMSGateway(Subscription subscription, String text) throws Exception {
            String phone = subscription.getSubscriber().getDetails().getPhoneMobile();
            //String phone = "994702011390";
            log.debug(subscription.getAgreement()+ " phone "+phone);
            if (phone == null || phone.length() <= MIN_PHONE_LENGTH) {
                log.info(String.format("phone not specified for agreement = %s. not sending sms.", subscription.getAgreement()));
                return;
            }


            URL url = new URL(
                    String.format("http://www.poctgoyercini.com/api_http/sendsms.asp?user=pilot_smpp&password=pilot852&gsm=%s&text=%s", phone, text)
                            .replaceAll(" ", "%20"));
            log.debug("SMS url "+url.toString());
            //Proxy proxy = new Proxy(Proxy.Type.HTTP, new InetSocketAddress("10.10.1.98", 12321));
            //conn = new URL(urlString).openConnection(proxy);
            HttpURLConnection con = (HttpURLConnection) url.openConnection();

            //add reuqest header
            con.setRequestMethod("GET");
            con.setDoOutput(true);
            //con.setRequestProperty( "Content-Type", "application/xml" );
            log.debug("Sending SMS to "+ subscription.getAgreement() + " url "+  url);
            int responseCode = con.getResponseCode();
            log.debug("Sending GET request to URL : " + url);
            //System.out.println("Post parameters : " + urlParameters);
            log.debug("Response Code : " + responseCode);

            BufferedReader in = new BufferedReader(
                    new InputStreamReader(con.getInputStream()));
            String inputLine;
            StringBuffer response = new StringBuffer();

            while ((inputLine = in.readLine()) != null) {
                response.append(inputLine + "\n");
            }
            in.close();

            //print result
            System.out.println(response.toString());
        }



        private void notifyByOnScreenMessage(Subscription subscription, int messageID, String text) {
            final String networkID = "network_dvb";
            final String packageID = "standart_package";
            final String networkContentID = "";
            final String smartCard = "STB_DVB_SC";
            final String smartlessDevice = "STB_DVB_NSC_2";
            final String resultSuccess = "0";
            final String resultDuplicateEntitlement = "207";
            final String resultError = "error";
            final String entitlementType = "DEVICE";


            log.debug(String.format("Sending onScreenMessage to device of subscription=%s", subscription.getAgreement()));

            try {
                String partNumber = processPartNumber(subscription.getSettingByType(ServiceSettingType.TV_EQUIPMENT).getValue());
                //String partNumber = subscription.getSettingByType(ServiceSettingType.TV_EQUIPMENT).getValue();


                OMIController omiController = new OMIController();

                // Bu hisse sabit deyerlerle yox, deyishe bilen shekilde edilmelidir.
                // BBTVProvisioner icherisinde de eyni problem var. Deyishdirilmelidir.
                omiController.generateMessage(partNumber, text, messageID, networkID, entitlementType);
                log.info(String.format("sendWindow.size: %s", smppSession.getSendWindow().getSize()));
                log.info("NotificationMessageProcessor messageId = " + messageID);
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }

        public String processPartNumber(String partNumber) {
            String res = partNumber.replaceAll("\\s+", "").replaceFirst("N", "");

            if (res.length() > 10) {
                res = new StringBuilder(res).deleteCharAt(res.length() - 1).toString();
            }

            return res;
        }

        private void executeMessage(PersistentMessage message) {
            message.setStatus(MessageStatus.EXECUTED);
            messagePersistenceFacade.update(message);
        }

        @Override
        public void run() {
            PersistentMessage message = messages.get(messageIdx);
            log.debug("NotificationMessageProcessor|Message received: " + message);

            try {
                //String notification = textMessage.getText();
                Long subscriptionID = Long.parseLong(message.getProperty("subscriptionID"));
                BillingEvent event = BillingEvent.convertFromCode(message.getProperty("event"));
                //String subject = (String) textMessage.getObjectProperty("subject");

                Subscription subscription = subscriptionFacade.find(subscriptionID);

                Double amount = null;
                Double debt = null;
                Invoice invoice = null;
                ValueAddedService vas = null;
                Integer messageID = null;

                if (message.getProperty("payment") != null) {
                    amount = Double.parseDouble(message.getProperty("payment"));
                }

                if (message.getProperty("invoiceID") != null) {
                    invoice = invoiceFacade.find(Long.parseLong(message.getProperty("invoiceID")));
                }

                if (message.getProperty("vasID") != null) {
                    vas = vasFacade.find(Long.parseLong(message.getProperty("vasID")));
                }

                if (message.getProperty("messageID") != null) {
                    messageID = Integer.parseInt(message.getProperty("messageID"));
                }


                ///Bu commentin yerini deyishdikde notification ishlemeyecek/ishleyecek.
                List<Notification> notificationList = null;
                if (subscription != null)
                    notificationList = notificationFacade.findNotification(event, subscription);
                //List<Notification> notificationList = null;

                log.debug("Notifications to be sent" + notificationList);
                log.debug("Subscription to be notified: " + subscription);

                String msg = null;

                if (notificationList == null) {
                    executeMessage(message);
                    return;
                }



log.info("NotificationMessageProcessor continues ");
                //send only Citynet SMS.
                if (!(event == BillingEvent.CITYNET_SERVICE_CHANGE
                        || event == BillingEvent.SOON_PARTIAL_BLOCKED
                        || event == BillingEvent.SOON_BLOCKED_GRACE
                        || event == BillingEvent.SOON_BLOCKED_CONTINUOUS)) {
                    executeMessage(message);
                    return;
                }
                for (Notification notification : notificationList) {
                    NotificationChannell channel = notification.getChannell();
                    NotificationChannelStatus status = channelPersistenceFacade.getChannelStatus(channel);
                    if (status != null && !status.isActive()) {
                        continue;
                    }
                    String text = null;
                    if (event != BillingEvent.CITYNET_SERVICE_CHANGE) {
                        text = parser.parseText(notification.getNotification(), subscription, amount, invoice, vas);
                    }
                    log.info(String.format("Sending %s to subscription id = %d", text, subscription.getId()));
                    try {
                        if (channel == NotificationChannell.EMAIL && subscription.getSubscriber().getDetails().getEmail() != null) {
                            String subject = parser.parseText(notification.getSubject(), subscription, amount, invoice, vas);
                            notifyByEmail(subscription.getSubscriber(), subject, text);
                        }
                        if (channel == NotificationChannell.SMS) {
                            if (subscription.getService().getProvider().getId() == Providers.CITYNET.getId() ||
                                    subscription.getService().getProvider().getId() == Providers.UNINET.getId()) {
                                log.info(String.format("sending sms to %s subscription. subscription id = %d",
                                        ((subscription.getService().getProvider().getId() == Providers.CITYNET.getId()) ? "Citynet" : "Uninet"),
                                        subscription.getId()));
                                if (event == BillingEvent.CITYNET_SERVICE_CHANGE) {
                                    SubscriberRuntimeDetails runtimeDetails = subscription.getSubscriber().getRuntimeDetails();
                                    if (runtimeDetails.getSmsCode() == null ||
                                            runtimeDetails.getSmsCode().equals(SubscriberRuntimeDetails.DEFAULT_SMS_CODE)) {
                                        String smsCode = String.valueOf(ThreadLocalRandom.current().nextInt(100000, 1000000));
                                        runtimeDetails.setSmsCode(smsCode);
                                    }
                                    text = parser.parseText(notification.getNotification(), subscription, amount, invoice, vas);
                                }
//                                notifyBySMSGateway(subscription, text);
                                notifyBySMSGatewaySOFTLINE(subscription, text);
//                                notifyByDataplusSMSGatewaySOFTLINE(subscription, text);
                            } else if (subscription.getService().getProvider().getId() == Providers.DATAPLUS.getId()
                                    ) {
                                log.info(String.format("sending sms to Dataplus subscription. subscription id = %d",
                                        subscription.getId()));
                                notifyByDataplusSMSGateway(subscription, text);
                            } else if (subscription.getService().getProvider().getId() == Providers.GLOBAL.getId()  )
                            {
                                log.info(String.format("sending sms to Global subscription. subscription id = %d",
                                        subscription.getId()));
                                notifyByGlobalSMSGateway(subscription, text);
                            } else if(subscription.getService().getProvider().getId() == Providers.CNC.getId()){
                                log.info(String.format("sending sms to CNC subscription. subscription id = %d",
                                        subscription.getId()));
                                notifyByCNCSMSGateway(subscription, text);

                            }
                            else {
                                notifyBySMS(subscription.getSubscriber(), text);
                            }
                        }
                        if (channel == NotificationChannell.SCREEN) {
                            notifyByOnScreenMessage(subscription, messageID, text);
                        }


                        msg = String.format("subscription id=%d, event=%s, notification id=%d", subscription.getId(), event, notification.getId());
                        systemLogger.success(SystemEvent.NOTIFICATION_SENT, subscription, msg);
                        log.info("Notification sent: " + msg);
                    } catch (Exception ex) {
                        msg = String.format("subscription id=%d, event=%s, notification id=%d", subscription.getId(), event, notification.getId());
                        systemLogger.error(SystemEvent.NOTIFICATION_SENT, subscription, msg);
                        log.error(ex+" Cannot send notification " + msg, ex);
                    }
                    msg = null;
                }
                executeMessage(message);
                log.debug("Message sending finished "+subscription.getId()+ " message state: "+message);
                //notifyByEmail(subscriber, subject, notification);
            } catch (Exception ex) {
                log.error("Cannot parse message: message=" + message, ex);
                log.info("Bus exited at: " + new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS").format(new Date()));
            }
        }
    }

    private class NotificationExecutor implements Runnable {
        @Override
        public void run() {
            log.info("NotificationMessageProcessor|NotificationExecutor started");
            ExecutorService executorService = Executors.newFixedThreadPool(5);

            while (consumeQueues.get()) {
                List<PersistentMessage> messages = messagePersistenceFacade.findNewNotificationMessages();

                try {
                    Thread.sleep(5000);
                } catch (InterruptedException e) {
                    log.error(e);
                }

                if (messages != null && !messages.isEmpty()) {
                    log.info("NotificationMessageProcessor|NotificationExecutor iteration");
                    for (PersistentMessage message : messages) {
                        log.info(String.format("NotificationMessageProcessor|NotificationExecutor message id = %d is being scheduled", message.getId()));
                        message.setStatus(MessageStatus.SCHEDULED);
                        messagePersistenceFacade.update(message);
                    }
                    for (int i = 0; i < messages.size(); ++i) {
                        executorService.execute(new NotificationProcessor(messages, i));
                    }
                }
            }
            executorService.shutdown();
            while (!executorService.isTerminated()) {
            }
            log.info("NotificationMessageProcessor|NotificationExecutor end");
        }
    }

    private class NotificationReprocessor implements Runnable {
        @Override
        public void run() {
            log.info("NotificationMessageProcessor|NotificationReprocessor started");
            ExecutorService executorService = Executors.newFixedThreadPool(3);

            while (consumeQueues.get()) {
                List<PersistentMessage> messages = messagePersistenceFacade.findFailedNotificationMessages();
                if (messages != null && !messages.isEmpty()) {
                    log.info("NotificationMessageProcessor|NotificationReprocessor iteration");
                    for (PersistentMessage message : messages) {
                        log.info(String.format("NotificationMessageProcessor|NotificationReprocessor message id = %d is being scheduled %s", message.getId(), message));
                        message.setStatus(MessageStatus.SCHEDULED);
                        message.setLastUpdateDate();
                        messagePersistenceFacade.update(message);
                    }
                    for (int i = 0; i < messages.size(); ++i) {
                        log.debug(i+ " message: "+messages.get(i));
                        executorService.execute(new NotificationProcessor(messages, i));
                    }
                }

                try {
                    Thread.sleep(5000);
                } catch (InterruptedException e) {
                    log.error(e);
                }
            }
            executorService.shutdown();
            while (!executorService.isTerminated()) {
            }
            log.info("NotificationMessageProcessor|NotificationReprocessor end");
        }
    }

    @PostConstruct
    public void signalUp() {
        log.info("NotificationMessageProcessor|signalUp started");
        smppClient = new DefaultSmppClient();
        DefaultSmppSessionHandler sessionHandler = new DefaultSmppSessionHandler();

        SmppSessionConfiguration config0 = new SmppSessionConfiguration();
        config0.setWindowSize(1);
        config0.setName("Tester.Session.0");
        config0.setType(SmppBindType.TRANSCEIVER);
        config0.setHost("10.13.166.105");
        config0.setPort(2775);
        config0.setConnectTimeout(10000);
        config0.setSystemId("tekila_bps");
        config0.setPassword("#tek1l@_!!");
        config0.getLoggingOptions().setLogBytes(true);
        // to enable monitoring (request expiration)
        config0.setRequestExpiryTimeout(30000);
        config0.setWindowMonitorInterval(15000);
        config0.setCountersEnabled(true);

        try {
            smppSession = smppClient.bind(config0, sessionHandler);
        } catch (Exception ex) {
            log.error("Cannot bind SMPP session", ex);
        }

        notificationExecutor = Executors.newFixedThreadPool(2);
        consumeQueues.set(true);
        notificationExecutor.execute(new NotificationExecutor());
        notificationExecutor.execute(new NotificationReprocessor());
        log.info("NotificationMessageProcessor|signalUp finished");
    }

    @PreDestroy
    public void signalShutdown() {
        log.info("NotificationMessageProcessor|signalShutdown started");
        if (smppSession != null && !smppSession.isClosed()) {
            smppSession.unbind(5000);
            smppSession.close();
            smppSession.destroy();
        }

        if (smppClient != null)
            smppClient.destroy();

        consumeQueues.set(false);
        notificationExecutor.shutdown();
        log.info("NotificationMessageProcessor|signalShutdown finished");
    }

    public void testInit() {
        mailRelayProperties = new Properties();
        mailRelayProperties.put("mail.smtp.host", MAIL_HOST);
        mailRelayProperties.put("mail.smtp.port", MAIL_PORT);
        testNotifyByEmail();
    }

    private void testNotifyByEmail() {
        log.debug("Mail test");
        try {
            InitialContext ctx = new InitialContext();

            Subscriber subscriber = new Subscriber();
            subscriber.setDetails(new SubscriberDetails());
            subscriber.getDetails().setEmail("jaravir@gmail.com");

            notifyByEmail(subscriber, mailSession, "NARHOME TEST", "TEST NOTIFICATION");
            log.debug("Mail sent successfully");
        } catch (NamingException e) {
            e.printStackTrace();
        }
    }

    private void notifyByEmail(Subscriber subscriber, Session session, String subject, String notification) {
        if (subscriber.getDetails().getEmail() == null || subscriber.getDetails().getEmail().isEmpty() || !validateEmail(subscriber.getDetails().getEmail())) {
            log.error("Email not valid for subscriber: " + subscriber);
            return;
        }

        MimeMessage message = new MimeMessage(session);
        Address recipient = null;
        try {
            new InternetAddress(subscriber.getDetails().getEmail());

            //message.setFrom(mailSenderAddress);
            //message.setRecipient(Message.RecipientType.TO, recipient);
            message.setRecipient(Message.RecipientType.TO, new InternetAddress(subscriber.getDetails().getEmail()));
            message.setSentDate(new Date());
            message.setSubject(subject);
            message.setText(notification);

            Transport.send(message);
        } catch (AddressException ex) {
            log.error("Cannot create address: ", ex);
        } catch (MessagingException ex) {
            log.error("Cannot send notification", ex.getCause());
        }
    }

    private boolean validateEmail(String email) {
        Pattern pat = Pattern.compile("[^@]+@([^\\.]+\\.)+[a-zA-Z]{2,3}");
        Matcher mat = pat.matcher(email);
        return mat.find();
    }
}
