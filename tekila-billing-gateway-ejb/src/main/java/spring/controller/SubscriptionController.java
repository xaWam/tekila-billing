package spring.controller;

import com.jaravir.tekila.base.filter.MatchingOperation;
import com.jaravir.tekila.equip.EquipmentPersistenceFacade;
import com.jaravir.tekila.module.accounting.entity.Bank;
import com.jaravir.tekila.module.accounting.entity.Charge;
import com.jaravir.tekila.module.accounting.entity.Payment;
import com.jaravir.tekila.module.accounting.entity.PaymentOption;
import com.jaravir.tekila.module.accounting.manager.BankPersistenceFacade;
import com.jaravir.tekila.module.accounting.manager.ChargePersistenceFacade;
import com.jaravir.tekila.module.accounting.manager.ChequeSequencePersistenceFacade;
import com.jaravir.tekila.module.accounting.manager.PaymentPersistenceFacade;
import com.jaravir.tekila.module.payment.PaymentOptionsPersistenceFacade;
import com.jaravir.tekila.module.service.ServiceSettingType;
import com.jaravir.tekila.module.service.ServiceType;
import com.jaravir.tekila.module.service.entity.*;
import com.jaravir.tekila.module.service.persistence.manager.ServicePropertyPersistenceFacade;
import com.jaravir.tekila.module.service.persistence.manager.ServiceProviderPersistenceFacade;
import com.jaravir.tekila.module.store.equip.Equipment;
import com.jaravir.tekila.module.subscription.persistence.entity.SubscriberType;
import com.jaravir.tekila.module.subscription.persistence.entity.Subscription;
import com.jaravir.tekila.module.subscription.persistence.entity.SubscriptionResourceBucket;
import com.jaravir.tekila.module.subscription.persistence.entity.SubscriptionSetting;
import com.jaravir.tekila.module.subscription.persistence.entity.SubscriptionStatus;
import com.jaravir.tekila.module.subscription.persistence.entity.external.StatusElementType;
import com.jaravir.tekila.module.subscription.persistence.management.SubscriptionPersistenceFacade;
import com.jaravir.tekila.module.web.service.provider.BillingServiceProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import spring.Filters;
import spring.dto.*;
import spring.mapper.subscription.ServicePropertyMapper;
import spring.mapper.subscription.SubscriptionMapper;
import spring.service.SubscriptionService;
import spring.service.customers.*;
import javax.ejb.EJB;
import javax.ejb.EJBs;
import javax.jws.WebParam;
import javax.persistence.NoResultException;
import javax.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static spring.util.Constants.INJECTION_POINT;

/**
 * Created by ElmarMa on 3/15/2018
 */
@RestController
public class SubscriptionController {

    private final Logger log = LoggerFactory.getLogger(SubscriptionController.class);

    @EJB(mappedName = INJECTION_POINT + "SubscriptionPersistenceFacade")
    private SubscriptionPersistenceFacade subscriptionPersistenceFacade;

    @EJB(mappedName = INJECTION_POINT + "ServiceProviderPersistenceFacade")
    private ServiceProviderPersistenceFacade providerPersistenceFacade;

    @EJB(mappedName = INJECTION_POINT + "ServicePropertyPersistenceFacade")
    private ServicePropertyPersistenceFacade servicePropertyPersistenceFacade;


    @EJB(mappedName = INJECTION_POINT + "ChequeSequencePersistenceFacade")
    private ChequeSequencePersistenceFacade chequeSequencePersistenceFacade;

    @EJB(mappedName = INJECTION_POINT + "PaymentOptionsPersistenceFacade")
    private PaymentOptionsPersistenceFacade paymentOptionsPersistenceFacade;

    @EJB(mappedName = INJECTION_POINT + "BankPersistenceFacade")
    private BankPersistenceFacade bankPersistenceFacade;

    @EJB(mappedName = INJECTION_POINT + "EquipmentPersistenceFacade")
    private EquipmentPersistenceFacade equipmentPersistenceFacade;

    @EJB(mappedName = INJECTION_POINT + "ChargePersistenceFacade")
    private ChargePersistenceFacade chargePersistenceFacade;

    @Autowired
    private SubscriptionMapper subscriptionMapper;

    @Autowired
    private ServicePropertyMapper servicePropertyMapper;

    @Autowired
    private AccountStatusService accountStatusService;

    @Autowired
    private ExternalSystemService externalSystemService;

    @Autowired
    private NotificationService notificationService;

    @Autowired
    private CampaignService campaignService;

    @Autowired
    private SettingService settingService;

    @Autowired
    private DetailService detailService;

    @Autowired
    private SubscriptionService subscriptionService;



    @EJB(mappedName = INJECTION_POINT+"PaymentPersistenceFacade")
    private PaymentPersistenceFacade paymentPersistenceFacade;


    @EJB(mappedName = INJECTION_POINT+"BillingServiceProvider")
    private BillingServiceProvider billingServiceProvider;

    @RequestMapping(method = RequestMethod.GET, value = "/subscriptions/{id}")
    public SubscriptionDTO getSubscription(@PathVariable Long id) {
//        Subscription subscription = subscriptionPersistenceFacade.find(id);
//        SubscriptionDTO subscriptionDTO = subscriptionMapper.toDto(subscription);
//
//        ServiceProperty property = null;
//        if (subscription.getService() != null &&
//                subscription.getSettingByType(ServiceSettingType.ZONE) != null &&
//                subscription.getSettingByType(ServiceSettingType.SERVICE_TYPE) != null) {
//            property = servicePropertyPersistenceFacade.find(
//                    subscription.getService(),
//                    subscription.getSettingByType(ServiceSettingType.ZONE));
//            subscriptionDTO.setServiceType(
//                    settingService.getNameFromId(subscription.getSettingByType(ServiceSettingType.SERVICE_TYPE),subscription));
//        }
//        subscriptionDTO.setServiceProperty(servicePropertyMapper.toDto(property));
//        return subscriptionDTO;


        return subscriptionService.getSubscription(id);

    }



    @RequestMapping(method = RequestMethod.GET, value = "/subsbyagreement/{agreement}")
    public SubscriberSmallDTO getSubscriptionByAgreement(@PathVariable String agreement) {
        if (agreement != null) {
            Subscription subscription = subscriptionPersistenceFacade.findByAgreementOrdinary(agreement);
        SubscriberSmallDTO subscriberSmallDTO = new SubscriberSmallDTO();
        subscriberSmallDTO.setName(subscription.getSubscriber().getDetails().getFirstName());
        subscriberSmallDTO.setSurname(subscription.getSubscriber().getDetails().getSurname());
        subscriberSmallDTO.setAgreement(subscription.getAgreement());
        subscriberSmallDTO.setSubscriptionID(subscription.getId());
        subscriberSmallDTO.setExpirationDate(subscription.getExpirationDate());
        subscriberSmallDTO.setServiceName(subscription.getService().getName());
        subscriberSmallDTO.setBalance(subscription.getBalance().getRealBalanceForView());


        if (subscription.getSettingByType(ServiceSettingType.TV_EQUIPMENT) != null &&
                subscription.getSettingByType(ServiceSettingType.TV_EQUIPMENT).getValue() != null) {
            log.debug("=======> "+subscription.getSettingByType(ServiceSettingType.TV_EQUIPMENT).getValue() );
            subscriberSmallDTO.setEquipmentId(subscription.getSettingByType(ServiceSettingType.TV_EQUIPMENT).getValue());
        }

            return subscriberSmallDTO;

        }else{
            return null;
        }

    }




    @PostMapping("/changeequipment")
    public int setNewEquipment(@RequestBody SubscriptionEquipmentUpdateDTO subscriptionEquipmentUpdateDTO) {

        log.debug("subscriptionEquipmentUpdateDTO.getAgreementId() "+subscriptionEquipmentUpdateDTO);

        if (subscriptionEquipmentUpdateDTO.getAgreementId() != null) {
            Subscription subscription = subscriptionPersistenceFacade.findSubscriptionByAgreement(subscriptionEquipmentUpdateDTO.getAgreementId());
            Equipment equipment = equipmentPersistenceFacade.findById(subscriptionEquipmentUpdateDTO.getEquipmentId());
            if (subscription.getSettingByType(ServiceSettingType.TV_EQUIPMENT) != null && equipment != null) {
                subscriptionPersistenceFacade.changeEquipmentForTV(subscription, equipment);
            }

            if (subscription.getBillingModel() != null && subscription.getExpirationDate() != null) {
                subscription.setExpirationDateWithGracePeriod(
                        subscription.getExpirationDate().plusDays(subscription.getBillingModel().getGracePeriodInDays()));
            }
            Subscription subscription1 =  subscriptionPersistenceFacade.update(subscription);
            log.debug("result subsciption  "+subscription1);

            return 0;

        }else{
            return 1;
        }

    }



    @RequestMapping(method = RequestMethod.GET, value = "/makePaymentBBTV")
    public boolean makeBBTVPayment(
            @RequestParam final Double amount,
            @RequestParam(required = false) final Long extUserID,
            @RequestParam(required = false) Long serviceID,
            @RequestParam(required = false) String accountID,
            @RequestParam final String contract,
            @RequestParam(required = false) String subscriberID,
            @RequestParam(required = false) String rid,
            @RequestParam(required = false) String rrn,
            @RequestParam(required = false) String sessID,
            @RequestParam(required = false) String dsc,
            @RequestParam(required = false) String currency,
            @RequestParam(required = false) String dateTime) {

        if (amount <= 0.0){
            log.debug(contract + "  amount is 0 or less");
            return false;
        }
        Subscription subscription = null;
        try {
                try {
                    subscription = subscriptionPersistenceFacade.findByAgreementOrdinary(contract);
                }catch(NoResultException nr){
                    log.debug("Subscription couln not find "+ nr);
                }


            Payment payment = null;
            try {
                payment = paymentPersistenceFacade.create(amount, extUserID, serviceID, subscription.getId(),
                        contract, subscription.getSubscriber().getId(), currency, rid, rrn, sessID, dsc, dateTime);
                log.info("makeExternalPayment: success. Payment after save: " + payment);

            }catch (NoResultException noz){

                log.debug(contract+ " Could not created payment ");
            }
            boolean result = billingServiceProvider.settlePayment(subscription.getSubscriber().getId(),
                    subscription.getId(), amount, payment.getId());
            log.debug(subscription.getAgreement()+"   Payment successfully settled");
            return result;
        } catch (Exception ex) {
            log.error("Cannot make external payment", ex);
            return false;
        }
    }


    @RequestMapping(method = RequestMethod.GET, value = "/subscription")
    public int isSubscriptionExist(@RequestParam String agreement, @RequestParam long provider_id) {
        try {
            Subscription subscription = subscriptionPersistenceFacade.findSubscription(agreement, provider_id);

            if (subscription != null) {
                log.debug("Subscription is: "+subscription.getAgreement());
                return 1;
            }
            log.debug("Subscription is empty for "+agreement);
            return 0;
        }catch (Exception ex){
            log.debug("Doesnt find subscription "+agreement  + " error:"+ex);
            return 0;
        }

    }



    @RequestMapping(method = RequestMethod.GET, value = "/search/subscriptions")
    public List<SubscriptionResponse> search(
            @RequestParam(required = false) String agreement,
            @RequestParam(required = false) Integer statusId,
            @RequestParam(required = false) Integer providerId,
            @RequestParam(required = false) Boolean isExact,
            @RequestParam(required = false) String identifier,
            @RequestParam(required = false) String taxID,
            @RequestParam(required = false) String companyName,
            @RequestParam(required = false) String subscriberFirstName,
            @RequestParam(required = false) String subscriberLastName,
            @RequestParam(required = false) String subscriberMiddleName,
            @RequestParam(required = false) String cityOfBirth,
            @RequestParam(required = false) String citizenship,
            @RequestParam(required = false) String country,
            @RequestParam(required = false) String passport_series,
            @RequestParam(required = false) String passport_number,
            @RequestParam(required = false) String issuedBy,
            @RequestParam(required = false) String valid_till,
            @RequestParam(required = false) String email,
            @RequestParam(required = false) String phone_mobile,
            @RequestParam(required = false) String phone_alt,
            @RequestParam(required = false) String phone_landline,
            @RequestParam(required = false) String city,
            @RequestParam(required = false) String userName,
            @RequestParam(required = false) String ats,
            @RequestParam(required = false) String street,
            @RequestParam(required = false) String building,
            @RequestParam(required = false) String apartment,
            @RequestParam Integer pageId,
            @RequestParam(required = false, defaultValue = "10") Integer rowsPerPage) {
        Filters filters = new Filters();
        filters.clearFilters();
        log.debug("-----------search() start----------");
        log.debug(String.format("search() method. agreement = %s, statusId = %d, providerId = %d, isExact = %s, pageId = %d, rowsPerPage = %d",
                agreement, statusId, providerId, String.valueOf(isExact), pageId, rowsPerPage));

        if (agreement != null && !agreement.isEmpty()) {
            if (isExact != null && isExact) {
                SubscriptionPersistenceFacade.Filter agreementFilter = SubscriptionPersistenceFacade.Filter.AGREEMENT;
                agreementFilter.setOperation(MatchingOperation.EQUALS);
                filters.addFilter(agreementFilter, agreement);
            } else {
                SubscriptionPersistenceFacade.Filter agreementFilter = SubscriptionPersistenceFacade.Filter.AGREEMENT;
                agreementFilter.setOperation(MatchingOperation.LIKE);
                filters.addFilter(agreementFilter, agreement);
            }
        }

        if (statusId != null) {
            filters.addFilter(SubscriptionPersistenceFacade.Filter.STATUS,
                    SubscriptionStatus.convertFromCode(statusId));
        }

        if (providerId != null) {
            filters.addFilter(SubscriptionPersistenceFacade.Filter.PROVIDER,
                    providerPersistenceFacade.find(providerId));
        }

        if (identifier != null && !identifier.isEmpty()) {
            filters.addFilter(SubscriptionPersistenceFacade.Filter.IDENTIFIER, identifier);
        }

        if (taxID != null && !taxID.isEmpty()) {
            filters.addFilter(SubscriptionPersistenceFacade.Filter.PASSPORT_NUMBER, taxID);
            filters.addFilter(SubscriptionPersistenceFacade.Filter.SUBSCRIBER_TYPE, SubscriberType.CORP);
        }

        if (companyName != null && !companyName.isEmpty()) {
            filters.addFilter(SubscriptionPersistenceFacade.Filter.CORPORATE_COMPANY, companyName);
        }

        if (subscriberFirstName != null && !subscriberFirstName.isEmpty()) {
            filters.addFilter(SubscriptionPersistenceFacade.Filter.FIRSTNAME, subscriberFirstName);
        }

        if (subscriberLastName != null && !subscriberLastName.isEmpty()) {
            filters.addFilter(SubscriptionPersistenceFacade.Filter.LASTNAME, subscriberLastName);
        }

        if (subscriberMiddleName != null && !subscriberMiddleName.isEmpty()) {
            filters.addFilter(SubscriptionPersistenceFacade.Filter.MIDDLENAME, subscriberMiddleName);
        }

        if (cityOfBirth != null && !cityOfBirth.isEmpty()) {
            filters.addFilter(SubscriptionPersistenceFacade.Filter.CITY_OF_BIRTH, cityOfBirth);
        }

        if (citizenship != null && !citizenship.isEmpty()) {
            filters.addFilter(SubscriptionPersistenceFacade.Filter.CITIZENSHIP, citizenship);
        }

        if (country != null && !country.isEmpty()) {
            filters.addFilter(SubscriptionPersistenceFacade.Filter.COUNTRY, country);
        }

        if (passport_series != null && !passport_series.isEmpty()) {
            filters.addFilter(SubscriptionPersistenceFacade.Filter.PASSPORT_SERIES, passport_series);
        }

        if (passport_number != null && !passport_number.isEmpty()) {
            filters.addFilter(SubscriptionPersistenceFacade.Filter.PASSPORT_NUMBER, passport_number);
        }

        if (issuedBy != null && !issuedBy.isEmpty()) {
            filters.addFilter(SubscriptionPersistenceFacade.Filter.PASSPORT_AUTHORITY, issuedBy);
        }

        if (valid_till != null && !valid_till.isEmpty()) {
            filters.addFilter(SubscriptionPersistenceFacade.Filter.PASSPORT_VALID, valid_till);
        }

        if (email != null && !email.isEmpty()) {
            filters.addFilter(SubscriptionPersistenceFacade.Filter.EMAIL, email);
        }

        if (phone_mobile != null && !phone_mobile.isEmpty()) {
            filters.addFilter(SubscriptionPersistenceFacade.Filter.PHONE_MOBILE, phone_mobile);
        }

        if (phone_alt != null && !phone_alt.isEmpty()) {
            filters.addFilter(SubscriptionPersistenceFacade.Filter.PHONE_ALT, phone_alt);
        }

        if (phone_landline != null && !phone_landline.isEmpty()) {
            filters.addFilter(SubscriptionPersistenceFacade.Filter.PHONE_LANDLINE, phone_landline);
        }

        if (city != null && !city.isEmpty()) {
            filters.addFilter(SubscriptionPersistenceFacade.Filter.ADDRESS_CITY, city);
        }

        if (userName != null && !userName.isEmpty()) {
            filters.addFilter(SubscriptionPersistenceFacade.Filter.USERNAME, userName);
        }

        if (ats != null && !ats.isEmpty()) {
            filters.addFilter(SubscriptionPersistenceFacade.Filter.ADDRESS_ATS, ats);
        }

        if (street != null && !street.isEmpty()) {
            filters.addFilter(SubscriptionPersistenceFacade.Filter.ADDRESS_STREET, street);
        }

        if (building != null && !building.isEmpty()) {
            filters.addFilter(SubscriptionPersistenceFacade.Filter.ADDRESS_BUILDING, building);
        }

        if (apartment != null && !apartment.isEmpty()) {
            filters.addFilter(SubscriptionPersistenceFacade.Filter.ADDRESS_APARTMENT, apartment);
        }

        log.debug("-----------search end()----------");
        return subscriptionPersistenceFacade.findAllPaginated((pageId - 1) * rowsPerPage, rowsPerPage, filters).stream().
                map(SubscriptionResponse::from).collect(Collectors.toList());
    }

    @RequestMapping(method = RequestMethod.GET, value = "/search/subscriptions/count")
    public long count(
            @RequestParam String agreement,
            @RequestParam(required = false) Integer statusId,
            @RequestParam(required = false) Integer providerId,
            @RequestParam(required = false) Boolean isExact,
            @RequestParam(required = false) String identifier,
            @RequestParam(required = false) String taxID,
            @RequestParam(required = false) String companyName,
            @RequestParam(required = false) String subscriberFirstName,
            @RequestParam(required = false) String subscriberLastName,
            @RequestParam(required = false) String subscriberMiddleName,
            @RequestParam(required = false) String cityOfBirth,
            @RequestParam(required = false) String citizenship,
            @RequestParam(required = false) String country,
            @RequestParam(required = false) String passport_series,
            @RequestParam(required = false) String passport_number,
            @RequestParam(required = false) String issuedBy,
            @RequestParam(required = false) String valid_till,
            @RequestParam(required = false) String email,
            @RequestParam(required = false) String phone_mobile,
            @RequestParam(required = false) String phone_alt,
            @RequestParam(required = false) String phone_landline,
            @RequestParam(required = false) String city,
            @RequestParam(required = false) String userName,
            @RequestParam(required = false) String ats,
            @RequestParam(required = false) String street,
            @RequestParam(required = false) String building,
            @RequestParam(required = false) String apartment
    ) {
        Filters filters = new Filters();
        filters.clearFilters();
        log.debug("-----------count() start----------");
        log.debug(String.format("count() method. agreement = %s, statusId = %d, providerId = %d, isExact = %s",
                agreement, statusId, providerId, String.valueOf(isExact)));

        if (agreement != null && !agreement.isEmpty()) {
            if (isExact != null && isExact) {
                SubscriptionPersistenceFacade.Filter agreementFilter = SubscriptionPersistenceFacade.Filter.AGREEMENT;
                agreementFilter.setOperation(MatchingOperation.EQUALS);
                filters.addFilter(agreementFilter, agreement);
            } else {
                SubscriptionPersistenceFacade.Filter agreementFilter = SubscriptionPersistenceFacade.Filter.AGREEMENT;
                agreementFilter.setOperation(MatchingOperation.LIKE);
                filters.addFilter(agreementFilter, agreement);
            }
        }

        if (statusId != null) {
            filters.addFilter(SubscriptionPersistenceFacade.Filter.STATUS,
                    SubscriptionStatus.convertFromCode(statusId));
        }

        if (providerId != null) {
            filters.addFilter(SubscriptionPersistenceFacade.Filter.PROVIDER,
                    providerPersistenceFacade.find(providerId));
        }

        if (identifier != null && !identifier.isEmpty()) {
            filters.addFilter(SubscriptionPersistenceFacade.Filter.IDENTIFIER, identifier);
        }

        if (taxID != null && !taxID.isEmpty()) {
            filters.addFilter(SubscriptionPersistenceFacade.Filter.PASSPORT_NUMBER, taxID);
            filters.addFilter(SubscriptionPersistenceFacade.Filter.SUBSCRIBER_TYPE, SubscriberType.CORP);
        }

        if (companyName != null && !companyName.isEmpty()) {
            filters.addFilter(SubscriptionPersistenceFacade.Filter.CORPORATE_COMPANY, companyName);
        }

        if (subscriberFirstName != null && !subscriberFirstName.isEmpty()) {
            filters.addFilter(SubscriptionPersistenceFacade.Filter.FIRSTNAME, subscriberFirstName);
        }

        if (subscriberLastName != null && !subscriberLastName.isEmpty()) {
            filters.addFilter(SubscriptionPersistenceFacade.Filter.LASTNAME, subscriberLastName);
        }

        if (subscriberMiddleName != null && !subscriberMiddleName.isEmpty()) {
            filters.addFilter(SubscriptionPersistenceFacade.Filter.MIDDLENAME, subscriberMiddleName);
        }

        if (cityOfBirth != null && !cityOfBirth.isEmpty()) {
            filters.addFilter(SubscriptionPersistenceFacade.Filter.CITY_OF_BIRTH, cityOfBirth);
        }

        if (citizenship != null && !citizenship.isEmpty()) {
            filters.addFilter(SubscriptionPersistenceFacade.Filter.CITIZENSHIP, citizenship);
        }

        if (country != null && !country.isEmpty()) {
            filters.addFilter(SubscriptionPersistenceFacade.Filter.COUNTRY, country);
        }

        if (passport_series != null && !passport_series.isEmpty()) {
            filters.addFilter(SubscriptionPersistenceFacade.Filter.PASSPORT_SERIES, passport_series);
        }

        if (passport_number != null && !passport_number.isEmpty()) {
            filters.addFilter(SubscriptionPersistenceFacade.Filter.PASSPORT_NUMBER, passport_number);
        }

        if (issuedBy != null && !issuedBy.isEmpty()) {
            filters.addFilter(SubscriptionPersistenceFacade.Filter.PASSPORT_AUTHORITY, issuedBy);
        }

        if (valid_till != null && !valid_till.isEmpty()) {
            filters.addFilter(SubscriptionPersistenceFacade.Filter.PASSPORT_VALID, valid_till);
        }

        if (email != null && !email.isEmpty()) {
            filters.addFilter(SubscriptionPersistenceFacade.Filter.EMAIL, email);
        }

        if (phone_mobile != null && !phone_mobile.isEmpty()) {
            filters.addFilter(SubscriptionPersistenceFacade.Filter.PHONE_MOBILE, phone_mobile);
        }

        if (phone_alt != null && !phone_alt.isEmpty()) {
            filters.addFilter(SubscriptionPersistenceFacade.Filter.PHONE_ALT, phone_alt);
        }

        if (phone_landline != null && !phone_landline.isEmpty()) {
            filters.addFilter(SubscriptionPersistenceFacade.Filter.PHONE_LANDLINE, phone_landline);
        }

        if (city != null && !city.isEmpty()) {
            filters.addFilter(SubscriptionPersistenceFacade.Filter.ADDRESS_CITY, city);
        }

        if (userName != null && !userName.isEmpty()) {
            filters.addFilter(SubscriptionPersistenceFacade.Filter.USERNAME, userName);
        }

        if (ats != null && !ats.isEmpty()) {
            filters.addFilter(SubscriptionPersistenceFacade.Filter.ADDRESS_ATS, ats);
        }

        if (street != null && !street.isEmpty()) {
            filters.addFilter(SubscriptionPersistenceFacade.Filter.ADDRESS_STREET, street);
        }

        if (building != null && !building.isEmpty()) {
            filters.addFilter(SubscriptionPersistenceFacade.Filter.ADDRESS_BUILDING, building);
        }

        if (apartment != null && !apartment.isEmpty()) {
            filters.addFilter(SubscriptionPersistenceFacade.Filter.ADDRESS_APARTMENT, apartment);
        }

        long cnt = subscriptionPersistenceFacade.count(filters);
        log.debug(String.format("count() method. count = %d", cnt));
        log.debug("-----------count end()----------");
        return cnt;
    }


    @RequestMapping(method = RequestMethod.PUT, value = "/customer/details")
    public ResponseEntity<Void> updateCustomerDetail(@RequestParam Long sid, @RequestBody CustomerDetailRequestDTO requestDTO) {
        detailService.updateSubscriptionDeatils(sid, requestDTO);
        return ResponseEntity.status(HttpStatus.ACCEPTED).build();
    }


    @RequestMapping(method = RequestMethod.PUT, value = "/customer/change-status")
    public ResponseEntity<Void> changeSubscriptionStatus(@RequestBody StatusChangeRequestDTO requestDTO) {
        accountStatusService.changeStatus(requestDTO.getSubscriptionId(),
                requestDTO.getStatusId(),
                requestDTO.getStartDate(),
                requestDTO.getEndDate(),
                requestDTO.isApplyReversal());
        return ResponseEntity.ok().build();
    }


    @RequestMapping(method = RequestMethod.GET, value = "/customer/provisioning-status")
    public ResponseEntity<Map<StatusElementType, String>> getProvisioningStatus(@RequestParam Long sid) {
        return ResponseEntity.ok(externalSystemService.getTechincalStatusOfProvisioning(sid));
    }

    @RequestMapping(method = RequestMethod.GET, value = "/customer/rejection-messages")
    public ResponseEntity<List<String>> getRejectionMessages(@RequestParam Long sid) {
        return ResponseEntity.ok(externalSystemService.getAuthenticationProblems(sid));
    }

    @RequestMapping(method = RequestMethod.GET, value = "/customer/online-sessions")
    public ResponseEntity<List<OnlineSessionDTO>> getOnlineSessions(@RequestParam Long sid) {
        return ResponseEntity.ok(externalSystemService.getOnlineBroadbandStatuses(sid));
    }

    @RequestMapping(method = RequestMethod.GET, value = "/customer/offline-sessions")
    public ResponseEntity<List<OfflineSessionDTO>> getOfflineSessions(@RequestParam Long sid) {
        return ResponseEntity.ok(externalSystemService.getOfflineBroadbandStatuses(sid));
    }

    @RequestMapping(method = RequestMethod.PUT, value = "/customer/disconnect")
    public ResponseEntity<Void> disconnectSession(@RequestParam Long sid) {
        externalSystemService.disconnectSession(sid);
        return ResponseEntity.ok().build();
    }

    @RequestMapping(method = RequestMethod.GET, value = "/customer/provision/reasons")
    public ResponseEntity<List<String>> getReasons() {
        return ResponseEntity.ok(externalSystemService.getReasons());
    }

    @RequestMapping(method = RequestMethod.PUT, value = "/customer/provision")
    public ResponseEntity<Void> reprovision(@RequestParam Long sid, @RequestBody String reason) {
        externalSystemService.reprovision(sid, reason);
        return ResponseEntity.ok().build();
    }

    @RequestMapping(method = RequestMethod.GET, value = "/customer/notification-settings")
    public ResponseEntity<List<NotificationSettingDTO>> getNotifications(@RequestParam Long sid) {
        return ResponseEntity.ok(notificationService.getNotifications(sid));
    }

    @RequestMapping(method = RequestMethod.PUT, value = "/customer/notification-settings")
    public ResponseEntity<List<NotificationSettingDTO>> getNotifications(@RequestParam Long sid, @RequestBody List<NotificationSettingDTO> changedSettings) {
        notificationService.updateNotificationSettings(sid, changedSettings);
        return ResponseEntity.ok().build();
    }

    @RequestMapping(method = RequestMethod.GET, value = "/customer/campaigns")
    public ResponseEntity<List<CampaignRegisterDTO>> getCampaigns(@RequestParam Long sid) {
        return ResponseEntity.ok(campaignService.getCampaignRegisters(sid));
    }

    @RequestMapping(method = RequestMethod.GET, value = "/customer/campaigns/available")
    public ResponseEntity<List<CampaignMinifiedDTO>> getAvailableCampaigns(@RequestParam Long sid) {
        return ResponseEntity.ok(campaignService.getAvailableCampaignList(sid));
    }

    @RequestMapping(method = RequestMethod.PUT, value = "/customer/campaign/activate")
    public ResponseEntity<Void> activateCampaign(@RequestParam Long cid) {
        campaignService.activateCampaign(cid);
        return ResponseEntity.ok().build();
    }

    @RequestMapping(method = RequestMethod.DELETE, value = "/customer/campaign/remove")
    public ResponseEntity<Void> removeCampaign(@RequestParam Long cid) {
        campaignService.removeCampaign(cid);
        return ResponseEntity.ok().build();
    }

    @RequestMapping(method = RequestMethod.POST, value = "/customer/campaign/add")
    public ResponseEntity<Void> addCampaign(@RequestParam Long sid, @RequestBody CampaignMinifiedDTO requestedCampaign) {
        campaignService.addNewCampaign(sid, requestedCampaign);
        return ResponseEntity.ok().build();
    }

    @RequestMapping(method = RequestMethod.GET, value = "/customer/settings")
    public ResponseEntity<List<SubscriptionSettingDTO>> getSubscriptionSettings(@RequestParam Long sid) {
        return ResponseEntity.ok(settingService.getSubscriptionSettings(sid));
    }

    @RequestMapping(method = RequestMethod.PUT, value = "/customer/settings/update")
    public ResponseEntity<Void> updateSubscriptionSettings(@RequestParam Long sid, @RequestBody EditSettingsRequestDTO editSettingsRequest) {
        settingService.updateServiceAndSettings(sid, editSettingsRequest);
        return ResponseEntity.ok().build();
    }


    @RequestMapping(method = RequestMethod.PUT, value = "/customer/settings/static-ip/update")
    public ResponseEntity<Void> mergeSubscriptionStaticIp(@RequestParam Long sid, @RequestBody(required = false) String staticIp) {
        settingService.mergeStaticIP(sid, staticIp, 1);
        return ResponseEntity.ok().build();
    }

    @RequestMapping(method = RequestMethod.GET, value = "/customer/settings/services")
    public ResponseEntity<List<ServiceDropdownDTO>> getSuitableServices(@RequestParam Long sid) {
        return ResponseEntity.ok(settingService.getServicesForSelectBox(sid));
    }

    @RequestMapping(method = RequestMethod.GET, value = "/customer/settings/service-types")
    public ResponseEntity<List<SubscriptionServiceType>> getServiceTypes(@RequestParam Long pid) {
        return ResponseEntity.ok(settingService.getServiceTypesForSelectBox(pid));
    }

    @RequestMapping(method = RequestMethod.GET, value = "/customer/settings/zones")
    public ResponseEntity<List<Zone>> getZones() {
        return ResponseEntity.ok(settingService.getZones());
    }

    @RequestMapping(method = RequestMethod.GET, value = "/customer/settings/ats-list")
    public ResponseEntity<List<AtsDTO>> getAtsList(@RequestParam Long sid) {
        return ResponseEntity.ok(settingService.getAtsForDropDown(sid));
    }

    @RequestMapping(method = RequestMethod.GET, value = "/customer/settings/minipops")
    public ResponseEntity<List<MinipopDropdownDTO>> getMinipops(@RequestParam String atsName, @RequestParam Long stid) {
        return ResponseEntity.ok(settingService.getMinipopsForSelectBox(atsName, stid));
    }

    @RequestMapping(method = RequestMethod.GET, value = "/customer/settings/resellers")
    public ResponseEntity<List<ResellerDropdownDTO>> getResellers() {
        return ResponseEntity.ok(settingService.getResellersForSelectBox());
    }

    @RequestMapping(method = RequestMethod.GET, value = "/customer/settings/ports")
    public ResponseEntity<List<PortDTO>> getPorts(@RequestParam Long mid, @RequestParam Long stid) {
        return ResponseEntity.ok(settingService.getPorts(mid, stid));
    }

    @RequestMapping(method = RequestMethod.PUT, value = "/customer/prolong/onetime")
    public ResponseEntity<Void> prolongOneTime(@RequestParam Long sid) {
        subscriptionService.prolongPrepaidSubscriptionOneTime(sid);
        return ResponseEntity.ok().build();
    }

//    @RequestMapping(method = RequestMethod.GET, value = "/customer/settings/minipops/all")
//    public ResponseEntity<List<MiniPopDTO>> findAllMiniPopsByProvider(@RequestParam Long id){
//        log.info("Request for Minipops by Provider id: {}" + id);
//        return ResponseEntity.ok(subscriptionService.findAllMiniPopsByProvider(id));
//    }


    @RequestMapping(method = RequestMethod.GET, value = "/customer/payment/chequeid")
    public long getChequeId() {
        long chequeId = chequeSequencePersistenceFacade.findAndUpdateChequeNum();
        return chequeId;
    }


    @RequestMapping(method = RequestMethod.GET, value = "/customer/payment/paymentMethod")
    public List<PaymentOption> getPaymentMethods() {
        List<PaymentOption> optionList = paymentOptionsPersistenceFacade.findAllAscending();
        for (PaymentOption paymentOption: optionList
        ) {

            log.debug(paymentOption.toString());
        }
        return optionList;
    }


    @RequestMapping(method = RequestMethod.GET, value = "/customer/payment/banks")
    public List<Bank> getBanks() {
        List<Bank> bankList = bankPersistenceFacade.findAll();

        for (Bank bank: bankList
        ) {
            log.debug("bank "+bank.toString());
        }

        return bankList;
    }


    @RequestMapping(method = RequestMethod.PUT, value = "/subscription/ecare/update")
    public void updateSubscriptionEcare(@RequestParam Long subscriptionId, @RequestParam Boolean isAvailableOnEcare){
        log.info("~~~Rest Request for updating eCare for subscription with id : {}", subscriptionId);
        subscriptionService.updateSubscriptionEcare(subscriptionId, isAvailableOnEcare);
    }

    @GetMapping("/subscription/service/agreement")
    public String generateSubscriptionAgreementByServiceId(@RequestParam Long serviceId){
        log.info("~~~Rest Request for agreement generation by serviceId: {}", serviceId);
        return subscriptionService.generateSubscriptionAgreementByServiceId(serviceId);
    }

    @GetMapping("/subscription/service/revert")
    public String revertCharges(@RequestParam long chargeId){

        try {
            log.debug("starting cancel ");
//            chargePersistenceFacade.cancelCharge(chargeId);
            List<Object[]> doubleCharges  = chargePersistenceFacade.cityNetDoubleCharge();

            for (Object[] objects: doubleCharges
                 ) {
                log.debug("Double charge is "+objects[0]+ " - "+objects[1]+" - "+objects[2]+" - "+objects[3]);
                try {
                    chargePersistenceFacade.cancelCharge(Integer.parseInt(objects[0].toString()));

                    chargePersistenceFacade.updatecityNetDoubleCharge(Integer.parseInt(objects[0].toString()));
                } catch (Exception ex){
                    log.debug("agreement "+objects[1]+ " occured exception on cancelling "+ex );
                }
                log.debug(objects[0] +"sucessfully cancelled ");
            }


        } catch (Exception e) {
            log.debug("Error on cancelCharge "+e);
        }

        return null;
    }

}