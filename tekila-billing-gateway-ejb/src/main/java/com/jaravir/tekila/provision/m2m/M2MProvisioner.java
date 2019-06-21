package com.jaravir.tekila.provision.m2m;


import com.jaravir.tekila.base.filter.Filterable;
import com.jaravir.tekila.engines.ProvisioningEngine;
import com.jaravir.tekila.module.provisioning.hlr.*;
import com.jaravir.tekila.module.service.entity.*;
import com.jaravir.tekila.module.service.entity.ServiceProfile;
import com.jaravir.tekila.module.stats.external.ExternalStatusInformation;
import com.jaravir.tekila.module.stats.persistence.entity.OfflineBroadbandStats;
import com.jaravir.tekila.module.stats.persistence.entity.OnlineBroadbandStats;
import com.jaravir.tekila.module.store.nas.*;
import com.jaravir.tekila.module.subscription.persistence.entity.Subscription;
import com.jaravir.tekila.module.subscription.persistence.entity.SubscriptionVAS;
import com.jaravir.tekila.module.subscription.persistence.entity.external.TechnicalStatus;
import com.jaravir.tekila.provision.broadband.entity.BackProvisionDetails;
import com.jaravir.tekila.provision.broadband.entity.Usage;
import com.jaravir.tekila.provision.exception.ProvisioningException;
import org.apache.log4j.Logger;
import org.joda.time.DateTime;

import javax.annotation.PostConstruct;
import javax.ejb.*;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.Unmarshaller;
import javax.xml.namespace.QName;
import javax.xml.soap.MessageFactory;
import javax.xml.soap.SOAPMessage;
import javax.xml.transform.Source;
import javax.xml.transform.stream.StreamSource;
import javax.xml.ws.*;
import javax.xml.ws.Service;
import javax.xml.ws.soap.SOAPBinding;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * Created by sajabrayilov on 05.03.2015.
 */
@Stateless(name = "M2MProvisioner", mappedName = "M2MProvisioner")
/**  Tekila JOBS runs on tekila_jobs branch  */ // @Startup
public class M2MProvisioner implements ProvisioningEngine {
    private final static Logger log = Logger.getLogger(M2MProvisioner.class);
    private static long counter;

    private static String requestHeader;
    private static String requestContent;

    private enum RequestType {
        SEARCH_SUBSCRIBER,
        CREATE_SUBSCRIBER;
    }

    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public boolean ping() {
        return true;
    }

    @PostConstruct
    public void init() {
        try {
            requestContent = getXML("hlr_request.xml");
            searchRequest();
        } catch (Exception ex) {
            log.error("Cannot initialize", ex);
        }
        //testDispatch();
        //test();
    }

    public void searchRequest() {
        try {
            String requestBody = getXML("search_subscriber.xml");
            requestBody = requestBody.replace("%{ALIAS_NAME}%", "msisdn");
            requestBody = requestBody.replace("%{MOB_NUM}%", "994772011329");
            SOAPMessage response = sendRequest(requestBody);
            parseSearchSubscriberResponse(response);
        } catch (Exception ex) {
            log.error("Cannot perform SEARCH_REQUEST", ex);
        }
    }

    private String getXML(String name) throws Exception {
        File file = new File(M2MProvisioner.class.getResource(name).toURI());

        log.debug(String.format("getXML: file %s exists? %b", file.getCanonicalPath(), file.exists()));
        String res = new String(Files.readAllBytes(file.toPath()), StandardCharsets.ISO_8859_1);

        log.debug("getXML: read file contents=" + res);
        return res;
    }

    @Override
    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public boolean initService(Subscription subscription) //throws ProvisioningException
    {
        String fileName = "create_subscriber.xml";
        try {
            String contents = getXML(fileName);
            return true;
        } catch (Exception ex) {
            log.error("Cannot parse XML file: " + fileName, ex);
            return false;
        }
    }

    @Override
    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public boolean openService(Subscription subscription) //throws ProvisioningException;
    {
        return false;
    }

    @Override
    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public boolean openService(Subscription subscription, DateTime expDate) //throws ProvisioningException;
    {
        return false;
    }

    @Override
    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public boolean closeService(Subscription subscription) //throws ProvisioningException;
    {
        return false;
    }

    @Override
    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public OnlineBroadbandStats collectOnlineStats(Subscription subscription) //throws ProvisioningException;
    {
        return null;
    }

    @Override
    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public List<OnlineBroadbandStats> collectOnlineStatswithPage(int start, int end) //throws ProvisioningException;
    {
        return null;
    }

    @Override
    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public TechnicalStatus getTechnicalStatus(Subscription subscription) {
        return null;
    }

    public void parseSearchSubscriberResponse(SOAPMessage response) throws Exception {

        SearchResponse resp = this.<SearchResponse>extractFromResponse(response, SearchResponse.class);
        log.debug("JAXB resp: " + resp.getSearchStatus());

        List<FirstClassObject> objectList = resp.getObjects();

        log.debug("SearchResponse nodes: " + objectList.toString());

        Subscriber sub = null;

        for (FirstClassObject ob : objectList) {
            sub = (Subscriber) ob;
            log.debug("Subscriber mscat: " + sub.getHlr().getMscat());
        }
    }

    private <T> T extractFromResponse(SOAPMessage response, Class<T> cl) throws Exception {
        ByteArrayOutputStream bs = new ByteArrayOutputStream();
        response.writeTo(bs);

        log.debug("SOAP response: " + bs.toString());

        JAXBContext ctx = JAXBContext.newInstance("com.jaravir.tekila.module.provisioning.hlr");
        Unmarshaller um = ctx.createUnmarshaller();
        JAXBElement<T> jaxbResp = um.unmarshal(response.getSOAPPart().getEnvelope().getBody().getFirstChild(), cl);
        T resp = jaxbResp.getValue();

        return resp;
    }

    public void parseCreateSubscriberResponse(SOAPMessage response) throws Exception {
        AddResponse resp = this.<AddResponse>extractFromResponse(response, AddResponse.class);

        log.debug("JAXB response result code: " + resp.getResult().value());

        Subscriber sub = (Subscriber) resp.getResultingObject();

        log.debug("Resulting subscriber: " + sub.getIdentifier()); //IMSI
    }

    public SOAPMessage sendRequest(String soapRequest) throws Exception {
        String xml = requestContent.replace("%{SOAP_REQUEST}%", soapRequest);

        Service service = Service.create(new QName("urn:siemens:names:prov:gw:SUBSCRIBER:1:0:wsdl", "SPMLSubscriber10"));

        service.addPort(new QName("urn:siemens:names:prov:gw:SUBSCRIBER:1:0:wsdl", "SPMLSubscriber10"), SOAPBinding.SOAP11HTTP_BINDING, "http://10.34.101.42:8081/ProvisioningGateway/services/SPMLSubscriber10Service");
        Dispatch<SOAPMessage> dispatch = service.createDispatch(new QName("urn:siemens:names:prov:gw:SUBSCRIBER:1:0:wsdl", "SPMLSubscriber10"), SOAPMessage.class, Service.Mode.MESSAGE);

        SOAPMessage message = MessageFactory.newInstance().createMessage();
        message.getSOAPPart().setContent((Source) new StreamSource(new StringReader(xml)));
        message.saveChanges();
        SOAPMessage response = dispatch.invoke(message);
        return response;
    }

    //@Schedule(hour = "*", minute = "*/1")
    public void testDispatch() {
        counter++;
        log.debug("HLR request # " + counter);
        String mobNum = "994772011329";
        String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                "<soapenv:Envelope xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\" xmlns:xsd=\"http://www.w3.org/2001/XMLSchema\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\">" +
                /*"<soapenv:Header><ds:Signature xmlns:ds=\"http://www.w3.org/2000/09/xmldsig#\">\n" +
                "<ds:SignedInfo xmlns:ds=\"http://www.w3.org/2000/09/xmldsig#\">\n" +
                "<ds:CanonicalizationMethod Algorithm=\"http://www.w3.org/TR/2001/REC-xml-c14n-20010315\" xmlns:ds=\"http://www.w3.org/2000/09/xmldsig#\"/>\n" +
                "<ds:SignatureMethod Algorithm=\"http://www.w3.org/2000/09/xmldsig#rsa-sha1\" xmlns:ds=\"http://www.w3.org/2000/09/xmldsig#\"/>\n" +
                "<ds:Reference URI=\"#Body\" xmlns:ds=\"http://www.w3.org/2000/09/xmldsig#\">\n" +
                "<ds:Transforms xmlns:ds=\"http://www.w3.org/2000/09/xmldsig#\">\n" +
                "<ds:Transform Algorithm=\"http://www.w3.org/2000/09/xmldsig#enveloped-signature\" xmlns:ds=\"http://www.w3.org/2000/09/xmldsig#\"/>\n" +
                "<ds:Transform Algorithm=\"http://www.w3.org/TR/2001/REC-xml-c14n-20010315#WithComments\" xmlns:ds=\"http://www.w3.org/2000/09/xmldsig#\"/>\n" +
                "</ds:Transforms>\n" +
                "<ds:DigestMethod Algorithm=\"http://www.w3.org/2000/09/xmldsig#sha1\" xmlns:ds=\"http://www.w3.org/2000/09/xmldsig#\"/>\n" +
                "<ds:DigestValue xmlns:ds=\"http://www.w3.org/2000/09/xmldsig#\">XzGPbT33Uv4rxD1h/yvzA/W9/mI=</ds:DigestValue>\n" +
                "</ds:Reference>\n" +
                "</ds:SignedInfo>\n" +
                "<ds:SignatureValue xmlns:ds=\"http://www.w3.org/2000/09/xmldsig#\">\n" +
                "eXJagTXThsIy+T3DLVJ/gRi6CzZDirgWBQklkwCB97YaWby8uqfmo+xPTUrc4XLHQpAetByZmBQy\n" +
                "G6QAVVchs+J+Y+4vxEdmSMYCDxREKciJd0trwDgABUEVhl0mD1Mn+xH+UDvxkkzzE6aBvO8TvGJc\n" +
                "NeJd/GITUC2AECklEjs=\n" +
                "</ds:SignatureValue>\n" +
                "<ds:KeyInfo xmlns:ds=\"http://www.w3.org/2000/09/xmldsig#\">\n" +
                "<ds:X509Data xmlns:ds=\"http://www.w3.org/2000/09/xmldsig#\">\n" +
                "<ds:X509Certificate xmlns:ds=\"http://www.w3.org/2000/09/xmldsig#\">\n" +
                "MIIClzCCAgCgAwIBAgICAR4wDQYJKoZIhvcNAQEFBQAwTzELMAkGA1UEBhMCUlUxDzANBgNVBAgT\n" +
                "Bm1vc2NvdzEPMA0GA1UEBxMGbW9zY293MQ4wDAYDVQQKEwVjYm9zczEOMAwGA1UECxMFbnRobHIw\n" +
                "HhcNMTIwMzI3MTQwMzMyWhcNMjIwMzI1MTQwMzMyWjBWMQswCQYDVQQGEwJSVTEPMA0GA1UECBMG\n" +
                "bW9zY293MQ4wDAYDVQQKEwVjYm9zczEOMAwGA1UECxMFbnRobHIxFjAUBgNVBAMTDWNib3NzMi5Q\n" +
                "cm92R1cwgZ8wDQYJKoZIhvcNAQEBBQADgY0AMIGJAoGBAKSieubzP0vJJoPCmyR8B/yaPvS6sukq\n" +
                "mvH2VSsaAJM5L7MRpOy4wojAmnwVmJFBWEyeS1pblsvkVVxMn0oEhSEdM63o7qwFpUvU/0EIegl4\n" +
                "HIf+v+XkZEHfmikeG3g4d4YIFJ6zynvdyRvS/fqwLzDgX90V9op2reS8thyEOgg3AgMBAAGjezB5\n" +
                "MAkGA1UdEwQCMAAwLAYJYIZIAYb4QgENBB8WHU9wZW5TU0wgR2VuZXJhdGVkIENlcnRpZmljYXRl\n" +
                "MB0GA1UdDgQWBBTyz9NWo9TL1CLsYBXFKSxlu5yHxDAfBgNVHSMEGDAWgBTQpOlm/5eLt9mBrSlX\n" +
                "md590JJFYjANBgkqhkiG9w0BAQUFAAOBgQCrxa8MY9ZJxKkHAgF+7PdXl+VavOIi0BKjGhJgi0xf\n" +
                "wLO4VuIb4flcajzCEX8vMX4lUgXCSpuOJu4J3TKNGPg4D++ow1bVPP5xva289o5v2qzEtA/cM0KY\n" +
                "t7CM4MnbqufRA9bSbKqRzbZS6XKg9VyAOYKD+eN+dFpB2l4h27pKUw==\n" +
                "</ds:X509Certificate>\n" +
                "</ds:X509Data>\n" +
                "<ds:KeyValue xmlns:ds=\"http://www.w3.org/2000/09/xmldsig#\">\n" +
                "<ds:RSAKeyValue xmlns:ds=\"http://www.w3.org/2000/09/xmldsig#\">\n" +
                "<ds:Modulus xmlns:ds=\"http://www.w3.org/2000/09/xmldsig#\">\n" +
                "pKJ65vM/S8kmg8KbJHwH/Jo+9Lqy6Sqa8fZVKxoAkzkvsxGk7LjCiMCafBWYkUFYTJ5LWluWy+RV\n" +
                "XEyfSgSFIR0zrejurAWlS9T/QQh6CXgch/6/5eRkQd+aKR4beDh3hggUnrPKe93JG9L9+rAvMOBf\n" +
                "3RX2inat5Ly2HIQ6CDc=\n" +
                "</ds:Modulus>\n" +
                "<ds:Exponent xmlns:ds=\"http://www.w3.org/2000/09/xmldsig#\">AQAB</ds:Exponent>\n" +
                "</ds:RSAKeyValue>\n" +
                "</ds:KeyValue>\n" +
                "</ds:KeyInfo>\n" +
                "</ds:Signature>" +
                "</soapenv:Header>" +*/
                "<soapenv:Body Id=\"Body\">" +
                "%{SOAP_REQUEST}%" +
                "</soapenv:Body></soapenv:Envelope>\n";
        String soapRequest = "<spml:searchRequest xmlns:spml=\"urn:siemens:names:prov:gw:SPML:2:0\" language=\"en_us\"><version xmlns:nsr=\"urn:siemens:names:prov:gw:HLR_NSR:2:1\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\">SUBSCRIBER_v10</version><base><objectclass>Subscriber</objectclass><alias name=\"msisdn\" value=\"%{MOB_NUM}%\"/></base></spml:searchRequest>";
        //String soapRequest = "<searchRequest><version xmlns:nsr=\"urn:siemens:names:prov:gw:HLR_NSR:2:1\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\">SUBSCRIBER_v10</version><base><objectclass>Subscriber</objectclass><alias name=\"msisdn\" value=\"%{MOB_NUM}%\"/></base></searchRequest>";
        soapRequest = soapRequest.replace("%{MOB_NUM}%", "994772011329");

        xml = xml.replace("%{SOAP_REQUEST}%", soapRequest);

        Service service = Service.create(new QName("urn:siemens:names:prov:gw:SUBSCRIBER:1:0:wsdl", "SPMLSubscriber10"));

        service.addPort(new QName("urn:siemens:names:prov:gw:SUBSCRIBER:1:0:wsdl", "SPMLSubscriber10"), SOAPBinding.SOAP11HTTP_BINDING, "http://10.34.101.42:8081/ProvisioningGateway/services/SPMLSubscriber10Service");
        Dispatch<SOAPMessage> dispatch = service.createDispatch(new QName("urn:siemens:names:prov:gw:SUBSCRIBER:1:0:wsdl", "SPMLSubscriber10"), SOAPMessage.class, Service.Mode.MESSAGE);
        try {
            SOAPMessage message = MessageFactory.newInstance().createMessage();
            message.getSOAPPart().setContent((Source) new StreamSource(new StringReader(xml)));
            message.saveChanges();
            SOAPMessage response = dispatch.invoke(message);

            ByteArrayOutputStream bs = new ByteArrayOutputStream();
            response.writeTo(bs);

            log.debug("SOAP response: " + bs.toString());

            JAXBContext ctx = JAXBContext.newInstance("com.jaravir.tekila.module.provisioning.hlr");
            Unmarshaller um = ctx.createUnmarshaller();
            JAXBElement<SearchResponse> jaxbResp = um.unmarshal(response.getSOAPPart().getEnvelope().getBody().getFirstChild(), SearchResponse.class);
            SearchResponse resp = jaxbResp.getValue();

            log.debug("JAXB resp" + resp.getSearchStatus());

            List<FirstClassObject> objectList = resp.getObjects();

            log.debug("SearchResponse nodes: " + objectList.toString());

            Subscriber sub = null;

            for (FirstClassObject ob : objectList) {
                sub = (Subscriber) ob;
                log.debug("Subscriber mscat: " + sub.getHlr().getMscat());
            }

        } catch (Exception ex) {
            log.error("Cannot make SOAP request", ex);
        }
    }

    public void test() {
        SPMLSubscriber10 sub = new SPMLSubscriber10();
        AliasType type = new AliasType();
        type.setName("msisdn");
        type.setValue("994772011329");

        SearchBase base = new SearchBase();
        base.setObjectclass("Subscriber");
        base.setAlias(type);

        SearchRequest request = new SearchRequest();
        request.setBase(base);
        request.setVersion("SUBSCRIBER_v10");

        Object ob = sub.getSPMLSubscriber10().receiveRequest(request); //receiveRequest(request);
        log.debug("search result: " + ob);
    }

    @Override
    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public boolean openVAS(Subscription subscription, ValueAddedService vas) {
        return true;
    }

    @Override
    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public boolean openVAS(Subscription subscription, ValueAddedService vas, SubscriptionVAS sbnVas) {
        return false;
    }

    @Override
    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public boolean closeVAS(Subscription subscription, ValueAddedService vas, SubscriptionVAS sbnVas) {
        return false;
    }

    @Override
    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public boolean closeVAS(Subscription subscription, ValueAddedService vas) {
        return true;
    }

    @Override
    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public boolean changeEquipment(Subscription subscription, String newValue) throws ProvisioningException {
        return false;
    }

    @Override
    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public boolean disconnect(Subscription subscription) throws ProvisioningException {
        return false;
    }

    @Override
    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public String checkRadiusState(Subscription subscription) throws ProvisioningException {
        return "OFFLINE";
    }

    @Override
    public boolean changeService(Subscription subscription, com.jaravir.tekila.module.service.entity.Service targetService) throws ProvisioningException {
        return false;
    }

    @Override
    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public ExternalStatusInformation collectExternalStatusInformation(Subscription subscription) {
        return null;
    }

    @Override
    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public boolean reprovision(Subscription subscription) {
        return true;
    }

    @Override
    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public boolean reprovisionWithEndDate(Subscription subscription, DateTime endDate) {
        return false;
    }

    @Override
    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public boolean createNas(Nas nas) {
        return false;
    }

    @Override
    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public boolean updateNas(Nas nas) {
        return false;
    }

    @Override
    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public boolean updateAttribute(com.jaravir.tekila.module.store.nas.Attribute attribute) {
        return false;
    }

    @Override
    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public List<OfflineBroadbandStats> offlineBroadbandStats(Subscription subscription, Map<Filterable, Object> filters) {
        return null;
    }

    @Override
    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public boolean updateAccount(Subscription subscription) {
        return false;
    }

    @Override
    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public DateTime getActivationDate(Subscription subscription) {
        return null;
    }

    @Override
    public boolean updateByService(com.jaravir.tekila.module.service.entity.Service service) {
        return false;
    }

    @Override
    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public boolean createServiceProfile(com.jaravir.tekila.module.service.entity.Service service) {
        return false;
    }

    @Override
    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public boolean updateServiceProfile(ServiceProfile serviceProfile, int oper) {
        return false;
    }

    @Override
    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public boolean createServiceProfile(ServiceProfile serviceProfile) {
        return false;
    }

    @Override
    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public boolean removeAccount(Subscription subscription) {
        return false;
    }

    public boolean removeFD(Subscription subscription) {
        return false;
    }

    @Override
    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public Usage getUsage(Subscription subscription, Date startDate, Date endDate) {
        return null;
    }

    @Override
    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public List<String> getAuthRejectionReasons(Subscription subscription) {
        return null;
    }

    @Override
    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public boolean provisionIptv(Subscription subscription) {
        return true;
    }

    @Override
    public BackProvisionDetails getBackProvisionDetails(Subscription subscription) {
        return null;
    }

    @Override
    public void provisionNewAgreements(String oldAgreement, String newAgreement) {

    }
}
