package com.jaravir.tekila.module.subscription.persistence.management;

import com.jaravir.tekila.base.auth.persistence.User;
import com.jaravir.tekila.base.auth.persistence.manager.UserPersistenceFacade;
import com.jaravir.tekila.base.filter.Filterable;
import com.jaravir.tekila.base.filter.MatchingOperation;
import com.jaravir.tekila.base.persistence.facade.AbstractPersistenceFacade;
import com.jaravir.tekila.base.persistence.manager.BillingSettingsManager;
import com.jaravir.tekila.engines.EngineFactory;
import com.jaravir.tekila.engines.ProvisioningEngine;
import com.jaravir.tekila.equip.EquipmentPersistenceFacade;
import com.jaravir.tekila.module.accounting.InvoiceState;
import com.jaravir.tekila.module.accounting.entity.*;
import com.jaravir.tekila.module.accounting.manager.*;
import com.jaravir.tekila.module.queue.PersistentQueueManager;
import com.jaravir.tekila.module.service.NotificationSetting;
import com.jaravir.tekila.module.service.NotificationSettingRow;
import com.jaravir.tekila.module.service.ServiceSettingType;
import com.jaravir.tekila.module.service.ServiceType;
import com.jaravir.tekila.module.service.entity.ServiceSetting;
import com.jaravir.tekila.module.service.persistence.manager.NotificationSettingPersistenceFacade;
import com.jaravir.tekila.module.service.persistence.manager.ServicePersistenceFacade;
import com.jaravir.tekila.module.store.equip.Equipment;
import com.jaravir.tekila.module.subscription.exception.DuplicateAgreementException;
import com.jaravir.tekila.module.subscription.persistence.entity.*;
import com.jaravir.tekila.module.web.service.exception.NoSuchSubscriptionException;
import com.jaravir.tekila.provision.broadband.devices.MiniPop;
import com.jaravir.tekila.provision.broadband.devices.Port;
import com.jaravir.tekila.provision.broadband.devices.exception.NoFreePortLeftException;
import com.jaravir.tekila.provision.broadband.devices.exception.PortAlreadyReservedException;
import com.jaravir.tekila.provision.broadband.devices.manager.MiniPopPersistenceFacade;
import com.jaravir.tekila.provision.exception.ProvisionerNotFoundException;
import org.apache.log4j.Logger;
import org.joda.time.DateTime;
import org.joda.time.DateTimeFieldType;

import javax.annotation.Resource;
import javax.ejb.*;
import javax.faces.context.FacesContext;
import javax.persistence.*;
import javax.persistence.criteria.*;
import javax.servlet.http.HttpSession;
import java.util.Date;
import java.util.List;
import java.util.Map;

@Deprecated
@Stateless
@TransactionManagement(TransactionManagementType.CONTAINER)
@TransactionAttribute(TransactionAttributeType.REQUIRED)
public class SubscriptionPersistenceFacadeDeprected extends AbstractPersistenceFacade<Subscription>{
	//@PersistenceContext(unitName="tekila")
	@PersistenceContext
	private EntityManager em;
	@Resource
	private SessionContext ctx;
	private Subscriber subscriber;

    @EJB private BillingSettingsManager billingSettings;
    @EJB private InvoicePersistenceFacade invoiceFacade;
    @EJB private ChargePersistenceFacade chargeFacade;
    @EJB private TransactionPersistenceFacade transFacade;
    @EJB private ServicePersistenceFacade serviceFacade;
    @EJB private BillingSettingsManager billSettings;
    @EJB private PaymentPersistenceFacade paymentFacade;
    @EJB private EquipmentPersistenceFacade equipmentFacade;
    @EJB private UserPersistenceFacade userFacade;
    @EJB private NotificationSettingPersistenceFacade notifSettingFacade;
    @EJB private AccountingTransactionPersistenceFacade accTransFacade;
    @EJB private MiniPopPersistenceFacade miniPopFacade;
    @EJB private EngineFactory provisioningFactory;
    @EJB private PersistentQueueManager queueManager;

    private final static Logger log = Logger.getLogger(SubscriptionPersistenceFacadeDeprected.class);

    public enum Filter implements Filterable {
        AGREEMENT("agreement"),
        FIRSTNAME("firstName"),
        LASTNAME("surname"),
        MIDDLENAME("middleName"),
        CITY_OF_BIRTH("cityOfBirth"),
        CITIZENSHIP("citizenship"),
        COUNTRY("country"),
        PASSPORT_SERIES("passportSeries"),
        PASSPORT_NUMBER("passportNumber"),
        PASSPORT_AUTHORITY("passportAuthority"),
        PASSPORT_VALID("passportValidTill"),
        EMAIL("email"),
        PHONE_MOBILE("phoneMobile"),
        PHONE_ALT("phoneMobileAlt"),
        PHONE_LANDLINE("phoneLandline"),
        ADDRESS_CITY("city"),
        ADDRESS_ATS("ats"),
        ADDRESS_STREET("street"),
        ADDRESS_BUILDING("building"),
        ADDRESS_APARTMENT("apartment"),
        CORPORATE_COMPANY("companyName"),
        CORPORATE_COMPANY_TYPE("bankAccount"),
        DATE_OF_BIRTH("dateOfBirth"),
        CREATED_ON("creationDate"),
        ENTRY_DATE("entryDate");

        private final String field;
        private MatchingOperation operation;

        Filter(String field) {
            this.field = field;
            this.operation = MatchingOperation.LIKE;
        }

        public String getField() {
            return field;
        }

        @Override
        public MatchingOperation getOperation() {
            return operation;
        }

        public void setOperation(MatchingOperation operation) {
            this.operation = operation;
        }
    }

	public SubscriptionPersistenceFacadeDeprected() {
		super(Subscription.class);
	}

	@Override
	protected EntityManager getEntityManager() {
		return this.em;
	}

    @Override
    public void save (Subscription sub) {
        sub.setCreationDate(DateTime.now().toDate());
        sub.synchronizeExpiratioDates();
        super.save(sub);
    }
        
    @Override
    public Subscription update(Subscription sub) {
        sub.synchronizeExpiratioDates();
        return super.update(sub);
    }

    public Subscription update (Subscription subscription, List<NotificationSettingRow> notificationSettings) {
        NotificationSetting setting = null;
        //srv.setNotificationSettings(null);

        for (NotificationSettingRow row : notificationSettings) {
            setting = subscription.getNotificationSettingByEvent(row.getEvent());

            if (setting != null) { //update setting
                if (row.getSelectedChannelList() != null && !row.getSelectedChannelList().isEmpty()) {
                    //setting.setChannelList(row.getSelectedChannelListAsChannels());
                    setting = notifSettingFacade.find(row.getEvent(), row.getSelectedChannelListAsChannels());
                    subscription.updateNotificationSetting(setting);
                }
                else { //none selected - remove the setting
                    subscription.getNotificationSettings().remove(setting);
                    //notifSettingFacade.updateAndDelete(setting);
                }
            }

            else {
                if (row.getSelectedChannelList() != null && !row.getSelectedChannelList().isEmpty()) {
                    setting = notifSettingFacade.find(row.getEvent(), row.getSelectedChannelListAsChannels());
                    subscription.addNotification(setting);
                }
            }
            setting = null;
        }

        return update(subscription);
    }

    public Subscription find (Long pk, LockModeType lockModeType) {
        return em.find(Subscription.class, pk, lockModeType);
    }
    @Override
    public Query getPaginatedQueryWithFilters() {
        Query query = null;
        CriteriaBuilder cb = getEntityManager().getCriteriaBuilder();
        CriteriaQuery criteriaQuery = null;
        if (getFilters().containsKey(Filter.AGREEMENT))
            criteriaQuery = cb.createQuery(Subscription.class);
        else
            criteriaQuery = cb.createQuery(Subscription.class);
            //criteriaQuery = cb.createQuery(SubscriberDetails.class);

        Root root = null;
        if (getFilters().containsKey(Filter.AGREEMENT)){
            root = criteriaQuery.from(Subscription.class);
        }

        else {
            root = criteriaQuery.from(SubscriberDetails.class);
            Join<Subscriber, Subscription> join = root.join("subscriber").join("subscriptions");
            criteriaQuery.select(join);
        }
       /* if (getFilters().containsKey(Filter.AGREEMENT) && !((String)getFilters().get(Filter.AGREEMENT)).isEmpty())
            predicate = cb.equal(root.get("agreement"), getFilters().get(Filter.AGREEMENT));
            */
        //if (getFilters().containsKey(Filter.FIRSTNAME) && !((String)getFilters().get(Filter.FIRSTNAME)).isEmpty())

        if (!getFilters().isEmpty()) {
            query = em.createQuery(criteriaQuery.where(getPredicateWithFilters(cb, root)).orderBy(cb.desc(root.get("id"))));
        }
        else {
            query = super.getPaginatedQueryWithFilters();
        }
        /*if (getFilters().containsKey(Filter.AGREEMENT) && !((String)getFilters().get(Filter.AGREEMENT)).isEmpty())
            query = getEntityManager().createQuery("select sbn from Subscription sbn where sbn.agreement = :agreement", Subscription.class)
                .setParameter(Filter.AGREEMENT.getField(), getFilters().get(Filter.AGREEMENT));
        else if (getFilters().containsKey(Filter.FIRSTNAME) && !((String)getFilters().get(Filter.FIRSTNAME)).isEmpty())
            query = getEntityManager().createQuery("select sbn from SubscriberDetails det join det.subscriber sub join sub.subscriptions sbn where det.firstName = :firstName", Subscription.class)
                    .setParameter("firstName", getFilters().get(Filter.FIRSTNAME));
        else if (getFilters().containsKey(Filter.LASTNAME) && !((String)getFilters().get(Filter.LASTNAME)).isEmpty())
            query = getEntityManager().createQuery("select sbn from SubscriberDetails det join det.subscriber sub join sub.subscriptions sbn where det.surname = :lastName", Subscription.class)
                    .setParameter(Filter.LASTNAME.getField(), getFilters().get(Filter.LASTNAME));
        else {
            query = super.getPaginatedQueryWithFilters();
        }*/
        return query;
    }

    @Override
    protected Predicate getPredicateWithFilters(CriteriaBuilder cb, From root) {

        Predicate predicate = null;

       /* if (getFilters().containsKey(Filter.AGREEMENT) && !((String)getFilters().get(Filter.AGREEMENT)).isEmpty())
            predicate = cb.equal(root.get("agreement"), getFilters().get(Filter.AGREEMENT));
            */
        //if (getFilters().containsKey(Filter.FIRSTNAME) && !((String)getFilters().get(Filter.FIRSTNAME)).isEmpty())

        if (!getFilters().isEmpty()) {
            for (Map.Entry<Filterable, Object> entry : getFilters().entrySet()) {
                if (entry.getValue() instanceof String) {
                    predicate = (predicate == null) ?
                        cb.like(cb.lower(root.get(((Filter) entry.getKey()).getField())),
                                new StringBuilder("%").append(((String) entry.getValue()).toLowerCase()).append("%").toString())
                        : cb.and(predicate, cb.like(cb.lower(root.get(((Filter) entry.getKey()).getField())),
                            new StringBuilder("%").append(((String) entry.getValue()).toLowerCase()).append("%").toString()));
                }
                else if (entry.getValue() instanceof Date) {
                    Date dt = (Date) entry.getValue();
                    DateTime start = new DateTime(dt).withTimeAtStartOfDay();
                    DateTime end = start.withTime(23,59,59,999);
                    log.debug(String.format("Filters: Start date=%s, end date=%s", start, end));
                    predicate = (predicate == null) ?
                            cb.between(root.get(((Filter) entry.getKey()).getField()), start.toDate(), end.toDate())
                        : cb.and(predicate, cb.between(root.get(((Filter) entry.getKey()).getField()), start.toDate(), end.toDate()));
                }
                else {
                    predicate = (predicate == null) ?
                        cb.equal(cb.lower(root.get(((Filter) entry.getKey()).getField())),
                                (Date) entry.getValue())
                        : cb.and(predicate, cb.equal(cb.lower(root.get(((Filter) entry.getKey()).getField())),
                        (Date) entry.getValue()));
                }
            }
        }
        return predicate;
    }

    @Override
    public Query countAllWithFilters() {
        Query query = null;

        CriteriaBuilder cb = getEntityManager().getCriteriaBuilder();
        CriteriaQuery<Long> countQuery = cb.createQuery(Long.class);
        Root root = null;

        if (getFilters().containsKey(Filter.AGREEMENT)) {
            root = countQuery.from(Subscription.class);
            countQuery.select(cb.count(root));
        }

        else {
            root = countQuery.from(SubscriberDetails.class);
            Join<Subscriber, Subscription> join = root.join("subscriber").join("subscriptions");
            countQuery.select(cb.count(join));
        }

        countQuery.where(getPredicateWithFilters(cb, root));

        query = getEntityManager().createQuery(countQuery);
        /*
        if (getFilters().containsKey(Filter.AGREEMENT) && !((String)getFilters().get(Filter.AGREEMENT)).isEmpty())
            query = getEntityManager().createQuery("select count(sbn) from Subscription sbn where sbn.agreement = :agreement", Long.class)
                    .setParameter(Filter.AGREEMENT.getField(), getFilters().get(Filter.AGREEMENT));
        else if (getFilters().containsKey(Filter.FIRSTNAME) && !((String)getFilters().get(Filter.FIRSTNAME)).isEmpty()) {
            log.info("Searching subscription by firstname: " + getFilters().get(Filter.FIRSTNAME));
            query = getEntityManager().createQuery("select count(sbn) from SubscriberDetails det  join det.subscriber sub join sub.subscriptions sbn where det.firstName = :firstName", Long.class)
            //query = getEntityManager().createNativeQuery("select count(*) from subscriber_details det where det.firstname =", Long.class)
                    .setParameter("firstName", getFilters().get(Filter.FIRSTNAME));
        }

        */
        /*else if (getFilters().containsKey(Filter.LASTNAME) && !((String)getFilters().get(Filter.LASTNAME)).isEmpty())
            getEntityManager().createQuery("select count(sbn) from SubscriberDetails det join det.subscriber sub join sub.subscriptions sbn where det.surname = :lastName", Long.class)
                    .setParameter(Filter.LASTNAME.getField(), getFilters().get(Filter.LASTNAME));
*/
        return query;
    }

	public Subscription findByCustomerIdentifier(String identifier) {
			
		return this.getEntityManager()
				.createQuery("select s from Subscription s where s.identifier = :ident", Subscription.class)
				.setParameter("ident", identifier).getSingleResult();		
			
	}
	
        public void updateWithResources(Subscription sub) {
            for (SubscriptionResource res : sub.getResources()) {
                for (SubscriptionResourceBucket buck : res.getBucketList()) {
                    em.persist(buck);
                }
                em.persist(res);                
            }
            this.update(sub);
        }
        
        @Override
        public List<Subscription> findAllPaginated (int first, int pageSize) {
            return (subscriber != null ? findAllPaginatedWithType(first, pageSize) : super.findAllPaginated(first, pageSize));
        }
        
        private List<Subscription> findAllPaginatedWithType(int first, int pageSize) {
            return em.createQuery("select s from Subscription s join s.subscriber sub where sub.id = :sub_id", Subscription.class)
                    .setParameter("sub_id", subscriber.getId())
                    .setFirstResult(first)
                    .setMaxResults(pageSize)
                    .getResultList();
        }
        
        public List<Subscription> findAllExpired() {
            return em.createQuery("select s from Subscription s where s.expirationDate < CURRENT_TIMESTAMP").getResultList();
        }
        
        public List<Subscription> findAllPostpaidForBilling() {
            return em.createQuery("select sub from Subscription sub join sub.subscriber owner "
                    + "where owner.lifeCycle =:lifeCycle and sub.status != :status "
                    + "and sub.activationDate IS NOT NULL "
                    + "and (sub.billedUpToDate <= CURRENT_TIMESTAMP or sub.billedUpToDate IS NULL)", 
                    Subscription.class)
                    .setParameter("lifeCycle", SubscriberLifeCycleType.POSTPAID)
                    .setParameter("status", SubscriptionStatus.FINAL)                    
                    .getResultList();
        }
        
        public List<Subscription> findAllPrepaidForBilling() {
            return em.createQuery("select sub from Subscription sub join sub.subscriber owner where owner.lifeCycle =:lifeCycle and sub.billedUpToDate between current_timestamp and :expireLimit and sub.status != :status", Subscription.class)
                    .setParameter("lifeCycle", SubscriberLifeCycleType.PREPAID)
                    .setParameter("expireLimit", DateTime.now().plusDays(billingSettings.getSettings().getPrepaidNumDaysToBillBeforeExpiration()))
                    .setParameter("status", SubscriptionStatus.FINAL)
                    .getResultList();
        }      

        public List<Subscription> findAllExpiredPostpaid() {
            return em.createQuery("select sub from Subscription sub join sub.subscriber owner "
            + "where owner.lifeCycle =:lifeCycle and sub.status = :status and "
                    + "sub.expirationDateWithGracePeriod <= CURRENT_TIMESTAMP", Subscription.class)
            .setParameter("lifeCycle", SubscriberLifeCycleType.POSTPAID)
            .setParameter("status", SubscriptionStatus.ACTIVE)             
            .getResultList();
        }   
        
        public List<Subscription> findAllExpiredPrepaid() {
            return em.createQuery("select sub from Subscription sub join sub.subscriber owner "
            + "where owner.lifeCycle =:lifeCycle and sub.status = :status and "
                    + "sub.expirationDateWithGracePeriod <= CURRENT_TIMESTAMP", Subscription.class)
            .setParameter("lifeCycle", SubscriberLifeCycleType.PREPAID)
            .setParameter("status", SubscriptionStatus.ACTIVE)
            .getResultList();
        }           
      /*  @Override
        public List<Subscription> findAllPaginated (int first, int pageSize) {
            return em.createQuery("select s from Subscription s order by s.id desc", Subscription.class)
                    .setFirstResult(first)
                    .setMaxResults(pageSize)
                    .getResultList();
        }
        */
        
        public void create (Subscriber selectedSubscriber, Subscription subscription, String serviceId, MiniPop miniPop,
                boolean isUseStockEquipment, Equipment equipment, List<NotificationSettingRow> notificationSettings, HttpSession session, User user)
                throws NoFreePortLeftException, PortAlreadyReservedException {
            Balance balance = new Balance();
            balance.setRealBalance(0L);
            balance.setPromoBalance(0L);
            
            if (selectedSubscriber.getLifeCycle() == SubscriberLifeCycleType.POSTPAID) 
                balance.setVirtualBalance(0);
            
            selectedSubscriber.getSubscriptions().add(subscription);
            subscription.setUser(user);
            subscription.setSubscriber(selectedSubscriber);
            subscription.setService(serviceFacade.find(Long.valueOf(serviceId)));
            subscription.setStatus(SubscriptionStatus.INITIAL);

            log.debug("the created subscription: " + subscription);

            subscription.setBalance(balance);      
            subscription.setIdentifier(subscription.getAgreement());

            List<com.jaravir.tekila.module.service.entity.Resource> resList = subscription.getService().getResourceList();

            List<ServiceSetting> settings = subscription.getService().getSettings();
            subscription.copySettingsFromService(settings);
            if (isUseStockEquipment && equipment != null) {
                subscription.setSettingByType(ServiceSettingType.TV_EQUIPMENT, equipment.getPartNumber());
                equipment.reserve();
            }
            if (subscription.getService().getServiceType() == ServiceType.BROADBAND) {
                //MiniPop miniPop = miniPopFacade.find(miniPopId);
                Port port = miniPopFacade.getAvailablePort(miniPop);
                //if (port != null) return;
                subscription.getSettingByType(ServiceSettingType.USERNAME).setValue(
                        //"Ethernet0/0/5:4846-fbe9-b9f0" + DateTime.now().toString(DateTimeFormat.forPattern("yyyyMMddHHmmss")) +"@narhome");
                        "Ethernet0/0/" + String.valueOf(port.getNumber()) + ":" + miniPop.getMac()
                );
                subscription.getSettingByType(ServiceSettingType.PASSWORD).setValue("-");
                subscription.getSettingByType(ServiceSettingType.BROADBAND_SWITCH).setValue(miniPop.getSwitch_id());
                subscription.getSettingByType(ServiceSettingType.BROADBAND_SWITCH_PORT).setValue(String.valueOf(port.getNumber()));
             }
             //log.debug("service: " + subscription.getService() + ", resource list: " + resList);
             if (resList.size() > 0) {                   
                 for (com.jaravir.tekila.module.service.entity.Resource res : resList) {
                     SubscriptionResource subResource = new SubscriptionResource(res);
                     /*if (subscription.getService().getServiceType().equals(ServiceType.BROADBAND)) {
                         subResource.getBucketByType(ResourceBucketType.INTERNET_SWITCH).setCapacity("test switch");
                         subResource.getBucketByType(ResourceBucketType.INTERNET_SWITCH_PORT).setCapacity("test port");
                         subResource.getBucketByType(ResourceBucketType.INTERNET_USERNAME).setCapacity("test switch:testport:" + DateTime.now().toString(DateTimeFormat.forPattern("yyyyMMddHHmmss")) +"@narhome");
                     }*/
                     subscription.addResource(subResource);                                 
                 }
             }
             log.debug("created sub: " + subscription);
             this.save(subscription);
             
             int totalCharge = 0;
             
             if (selectedSubscriber.getLifeCycle() == SubscriberLifeCycleType.PREPAID) {
                Invoice invoice = null;
                List<Invoice> openInvoiceList = invoiceFacade.findOpenBySubscriber(subscription.getSubscriber().getId());

                log.debug("Invoice search result: " + openInvoiceList);

                boolean isNewInvoice = false;

                if (openInvoiceList == null || openInvoiceList.isEmpty()) {
                    invoice = new Invoice();
                    isNewInvoice = true;
                }
                else {
                    invoice = openInvoiceList.get(0);
                }

                invoice.setSubscriber(selectedSubscriber);
                invoice.setState(InvoiceState.OPEN);
                //charge for installation fee if > 0
               if (subscription.getService().getInstallationFee() > 0) {
                    //balance.debitReal(subscription.getService().getInstallationFee() * 100000);
                   Charge instFeeCharge = new Charge();
                   instFeeCharge.setService(subscription.getService());
                   instFeeCharge.setAmount(subscription.getService().getInstallationFee() );
                   instFeeCharge.setSubscriber(subscription.getSubscriber());
                   instFeeCharge.setUser_id((Long)session.getAttribute("userID"));
                   instFeeCharge.setDsc("Charge for installation fee");
                   instFeeCharge.setDatetime(DateTime.now());
                   chargeFacade.save(instFeeCharge);

                   Transaction transDebitInstFee = new Transaction(
                            TransactionType.DEBIT, 
                            subscription, 
                            subscription.getService().getInstallationFee() ,
                            "Charged for installation fee");
                    transDebitInstFee.execute();
                    transFacade.save(transDebitInstFee);

                    invoice.addChargeToList(instFeeCharge);
                    
                    /*if (selectedSubscriber.getLifeCycle() == SubscriberLifeCycleType.POSTPAID)
                        totalCharge += instFeeCharge.getAmount();    */                
               }

               if (isUseStockEquipment && equipment != null) {
                   invoiceFacade.addEquipmentChargeToInvoice(invoice, equipment, subscription,(Long) (session.getAttribute("userID")));
               }

               long rate = subscription.getService().getServicePrice();

               //charge for service fee if > 0
               if(rate > 0) {
                   Charge servFeeCharge = new Charge();
                   servFeeCharge.setService(subscription.getService());
                   servFeeCharge.setAmount(rate);
                   servFeeCharge.setSubscriber(subscription.getSubscriber());
                   servFeeCharge.setUser_id((Long)session.getAttribute("userID"));
                   servFeeCharge.setDsc("Charge for service fee");
                   servFeeCharge.setDatetime(DateTime.now());
                   chargeFacade.save(servFeeCharge);
                   //balance.debitReal(rate * 100000);
                   Transaction transDebitServiceFee = new Transaction(
                           TransactionType.DEBIT,
                           subscription, 
                           rate,
                           "Charged for service fee"
                   );
                   transDebitServiceFee.execute();
                   transFacade.save(transDebitServiceFee); 

                   invoice.addChargeToList(servFeeCharge);
                    
                   /*if (selectedSubscriber.getLifeCycle() == SubscriberLifeCycleType.POSTPAID)
                        totalCharge += servFeeCharge.getAmount();    */                
                   
               }

               //this.update(subscription);

               if (isNewInvoice) {
                   invoiceFacade.save(invoice);
               }
               
                if (billSettings.getSettings().getPrepaidlifeCycleLength() == 30) {
                    subscription.setBilledUpToDate(DateTime.now().plusMonths(1));                    
                }
                else {
                    subscription.setBilledUpToDate(DateTime.now().plusDays(
                        billSettings.getSettings().getPrepaidlifeCycleLength()
                    ));
                }
                
                //subscription.synchronizeExpiratioDates();
              /* else {
                   invoiceFacade.update(invoice);
               }*/
            } // END IF PREPAID

            if (isUseStockEquipment && equipment != null)
                equipmentFacade.update(equipment);

            if (notificationSettings != null && !notificationSettings.isEmpty()) {
                NotificationSetting setting = null;

                for (NotificationSettingRow row : notificationSettings) {
                    if (row.getSelectedChannelList() != null && !row.getSelectedChannelList().isEmpty()) {
                        setting = notifSettingFacade.find(row.getEvent(), row.getSelectedChannelListAsChannels());

                        subscription.addNotification(setting);
                        setting = null;
                    }
                }
            }

            initService(subscription);
        }

    @TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
    private void initService(Subscription subscription) {
        try {
            ProvisioningEngine provisioner = provisioningFactory.getProvisioningEngine(subscription);
            provisioner.initService(subscription);
        }
        catch (ProvisionerNotFoundException ex) {
            log.error("Error initializing service", ex);
        }
    }
	public void createSubscriber () {
		
		Balance b = new Balance();
		b.setRealBalance(100);
		
		/*Rate rt = new Rate();
		rt.setName("Default Rate");
		rt.setPrice(10000);
		rt.setUsePromoResources(true);
		
		RateProfile rp = new RateProfile();
		rp.setName("RP for ADSL");
		rp.addRate(rt);
		
		Service s = new Service();		
		s.setName("service-test");
		s.setServiceType(ServiceType.ADSL);
		s.setIsBillByLifeCycle(true);
		s.setRateProfile(rp);
		
		String ident = "1-ident";
		
		Subscription sub = new Subscription();
		sub.setAgreement("agrem");
		sub.setIdentifier(ident);		
		sub.setBalance(b);
		sub.setService(s);
		this.save(sub);
		*/
		//this.getEntityManager().persist(b);
		
	}
        
        public void chargePostpaidVirtuallyOnActivate (Subscription sub)  {
            //virtual charge for installation fee
            sub.getBalance().debitVirtual(sub.getService().getInstallationFee() );
            long rate = sub.getService().getServicePrice();
            if (rate > 0) { //virtual charge for service fee
                rate = getPartialRateForPostpaid(sub, rate);
                sub.getBalance().debitVirtual(rate);  
            }                      
        }      
        
        public long getPartialRateForPostpaid(Subscription sub, long rate) {
                
               int numOfDaysUsed = 0;               
               int dayInMonth = sub.getActivationDate().get(DateTimeFieldType.dayOfMonth());               
               
               if (dayInMonth >= sub.getActivationDate().dayOfMonth().getMaximumValue()) {                    
                   numOfDaysUsed = 1;
               }
               else {    
                   numOfDaysUsed = billSettings.getSettings().getPospaidLifeCycleLength() - (dayInMonth - 1); 
                }
               
               if (numOfDaysUsed < billSettings.getSettings().getPospaidLifeCycleLength()) {
                    rate = (rate / billSettings.getSettings().getPospaidLifeCycleLength()) * numOfDaysUsed;
               }
               
               return rate;
        }
        
        public void chargePostPaidOnActivate(Subscription subscription) {
                Invoice newInvoice = null;

                Subscriber subscriber = subscription.getSubscriber();
                for (Invoice inv : subscriber.getInvoices()) {
                    if (inv.getState() == InvoiceState.OPEN
                             && new DateTime(inv.getCreationDate()).isAfter(subscription.getBilledUpToDate())
                        ) {                   
                        newInvoice = inv;
                    }
                }
                //new subscription - never billed                        
                if (newInvoice == null) {
                    newInvoice = invoiceFacade.createInvoiceForSubscriber(subscriber);
                }

                long rate = subscription.getService().getServicePrice();;
                long startBalance = subscription.getBalance().getRealBalance();
                long totalCharged = 0;

                //subscription is new - never billed
                if (subscription.getBilledUpToDate() == null) {
                    //calculate the rate for partial billing
                    int numOfDaysUsed = 0;

                    int dayInMonth = DateTime.now().get(DateTimeFieldType.dayOfMonth());               
                    //calculate 
                    if (dayInMonth >= subscription.getActivationDate().dayOfMonth().getMaximumValue()) {                    
                        numOfDaysUsed = 1;
                    }
                    else {    
                        numOfDaysUsed = billSettings.getSettings().getPospaidLifeCycleLength() - (dayInMonth - 1); 
                    }

                    if (numOfDaysUsed < billSettings.getSettings().getPospaidLifeCycleLength()) {
                        rate = (rate / billSettings.getSettings().getPospaidLifeCycleLength()) * numOfDaysUsed;
                    }

                    log.info(String.format("Partial rate for %d days is %d", numOfDaysUsed, rate));

                    //charge for installation fee
                     if (subscription.getService().getInstallationFee() > 0) {
                         //balance.debitReal(subscription.getService().getInstallationFee() * 100000);
                        Charge instFeeCharge = new Charge();
                        instFeeCharge.setService(subscription.getService());
                        instFeeCharge.setAmount(subscription.getService().getInstallationFee());
                        instFeeCharge.setSubscriber(subscription.getSubscriber());
                        instFeeCharge.setUser_id(20000L);
                        instFeeCharge.setDsc("Autocharged  for installation fee upon manual activation");
                        instFeeCharge.setDatetime(DateTime.now());
                        chargeFacade.save(instFeeCharge);

                        Transaction transDebitInstFee = new Transaction(
                                TransactionType.DEBIT, 
                                subscription, 
                                subscription.getService().getInstallationFee() ,
                                "Autocharged  for installation fee upon manual activation");
                        transDebitInstFee.execute();
                        transFacade.save(transDebitInstFee);

                        newInvoice.addChargeToList(instFeeCharge);

                        totalCharged += instFeeCharge.getAmount();
                    }               
                }                      
                //charge for service fee
                if (rate > 0) {                     
                    Charge servFeeCharge = new Charge();
                    servFeeCharge.setService(subscription.getService());
                    servFeeCharge.setAmount(rate);
                    servFeeCharge.setSubscriber(subscriber);
                    servFeeCharge.setUser_id(20000L);
                    servFeeCharge.setDsc("Autocharged  for service fee upon manual activation");
                    servFeeCharge.setDatetime(DateTime.now());
                    chargeFacade.save(servFeeCharge);

                    Transaction transDebitServiceFee = new Transaction(
                            TransactionType.DEBIT,
                            subscription, 
                            rate,
                            "Autocharged for service fee upon manual activation"
                    );
                    transDebitServiceFee.execute();
                    transFacade.save(transDebitServiceFee); 
                    newInvoice.addChargeToList(servFeeCharge);
                    totalCharged += servFeeCharge.getAmount();               
                } // end if for rate

               //adjust invoice debt to subscription's balance
                long endBalance = subscription.getBalance().getRealBalance();
               //newInvoice.addDebt(totalCharged);

             /* if (startBalance <= 0) {
                  newInvoice.addDebt(totalCharged);                    
              } */               
                if (startBalance >= 0
                    && endBalance < 0
                    ) {
                  //newInvoice.addDebt(Math.abs(endBalance));
                  //newInvoice.addDebt(totalCharged);
                   newInvoice.reduceDebt(startBalance);
                }
                else if (endBalance >= 0) {
                  //newInvoice.addDebt(totalCharged);
                    newInvoice.reduceDebt(totalCharged);
                    if (newInvoice.getBalance() >=0) {
                        newInvoice.setState(InvoiceState.CLOSED);
                        newInvoice.setCloseDate(DateTime.now());
                }
                  //subscriber paid out the debt - extend subscription date
                if (billSettings.getSettings().getPospaidLifeCycleLength() == 30) {
                      /*sub.setBilledUpToDate(DateTime.now().plusMonths(1).plusDays(                        
                         billSettings.getSettings().getPostpaidDefaultGracePeriod()));
                      */
                    subscription.setExpirationDate(DateTime.now().plusMonths(1));
                    subscription.setExpirationDateWithGracePeriod(subscription.getExpirationDate().plusDays(                        
                    billSettings.getSettings().getPostpaidDefaultGracePeriod()));                
                }
                else {
                      /*sub.setBilledUpToDate(DateTime.now().plusDays(
                          billSettings.getSettings().getPospaidLifeCycleLength()
                          +  billSettings.getSettings().getPostpaidDefaultGracePeriod()));
                      */
                    subscription.setExpirationDate(DateTime.now().plusDays(
                    billSettings.getSettings().getPospaidLifeCycleLength()
                        )
                    );
                    subscription.setExpirationDateWithGracePeriod(subscription.getExpirationDate().plusDays(                            
                          billSettings.getSettings().getPostpaidDefaultGracePeriod()));                        
                  }                    
                  subscription.synchronizeExpiratioDates();
               } // end if for charging
                
                //adjust last billing date
                DateTime baseDate = DateTime.now().plusMonths(1).withDayOfMonth(1).withTimeAtStartOfDay().minusSeconds(1);
                if (billSettings.getSettings().getPospaidLifeCycleLength() == 30) {
                    subscription.setBilledUpToDate(baseDate.plusMonths(1));               
                }
                else {
                    subscription.setBilledUpToDate(baseDate.plusDays(
                        billSettings.getSettings().getPospaidLifeCycleLength())
                    );                              
                }  
        }

    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public Payment settlePayment (long subscriptionID, Long amount, double fAmount, long paymentID) throws NoSuchSubscriptionException, Exception {
        log.info(String.format("Settling payment. Parameters: subscriptionID=%d, amount=%d, fAmount=%f, paymentID=%d", subscriptionID, amount, fAmount, paymentID));

        Payment payment = null;
        try {
            em.setProperty("javax.persistence.lock.timeout", 10000);
            Subscription subscription = this.find(subscriptionID,LockModeType.PESSIMISTIC_WRITE );
            payment = paymentFacade.find(paymentID);

            accTransFacade.makePayment(payment, subscription, amount, userFacade.findByUserName(ctx.getCallerPrincipal().getName()));

            if (payment.getSubscriber_id() == null || payment.getSubscriber_id() == 0)
                payment.setSubscriber_id(subscription.getSubscriber().getId());

            //paymentFacade.update(payment);

            List<Invoice> openInvoiceList = invoiceFacade.findOpenBySubscriberForPayment(subscription.getSubscriber().getId());
               /* if (openInvoiceList == null || openInvoiceList.isEmpty()) {
                    throw new NoInvoiceFoundException(String.format("No invoice exists for subscriber %d", subscriber.getId()));
                }
                */
            long residualValue = 0;

            log.info("Found open invoices: " + openInvoiceList);

            if (openInvoiceList != null && !openInvoiceList.isEmpty()) {
                for (Invoice invoice : openInvoiceList) {
                    residualValue = invoice.addPaymentToList(payment, residualValue);
                    if (residualValue <= 0)
                        break;

                    log.debug("Invoice after payment" + invoice);
                }
                //invoiceFacade.update(invoice);
                ///invoiceFacade.save(invoice);
            }
            return payment;

            //this.em.merge(subscription.getBalance());
            //subscription.setLastPaymentDate(payment.getLastUpdateDate());
            //this.update(subscription);
            //Subscription sbn = this.findWithoutRefresh(subscriptionID);
            //log.debug(String.format("Subscription upon retrieval: %s", sbn));
        } catch (NoResultException ex) {
            log.error(String.format("No subscription retrieved for ID %d", subscriptionID), ex);
            ctx.setRollbackOnly();
            throw new NoSuchSubscriptionException(String.format("subscriptionID %d not found"));
        }
    }
        @Deprecated
        @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
        public Payment settlePaymentOld (long subscriptionID, Long amount, double fAmount, long paymentID) throws NoSuchSubscriptionException {
            log.info(String.format("Settling payment. Parameters: subscriptionID=%d, amount=%d, fAmount=%f, paymentID=%d", subscriptionID, amount, fAmount, paymentID));

            Payment payment = null;
            try {
                 em.setProperty("javax.persistence.lock.timeout", 10000);
                 Subscription subscription = this.find(subscriptionID,LockModeType.PESSIMISTIC_WRITE );
                                
                transFacade.createTransation(
                        TransactionType.PAYMENT, subscription, 
                        amount, 
                        String.format("Payment of %f AZN for Subscription %s of Subscriber %s", 
                            fAmount, subscription.getAgreement(), subscription.getSubscriber().getMasterAccount()));

                log.debug(String.format("Subscription %d after payment: %s", subscription.getId(), subscription));
                //subscription.getBalance().creditVirtual(amount);       
                subscription.setLastPaymentDate(DateTime.now());
                subscription.getSubscriber().setLastPaymentDate(DateTime.now());
               
                payment = paymentFacade.find(paymentID);
                payment.setProcessed(1);
                
                if (payment.getSubscriber_id() == null || payment.getSubscriber_id() == 0)
                    payment.setSubscriber_id(subscription.getSubscriber().getId());
                
                //paymentFacade.update(payment);

                List<Invoice> openInvoiceList = invoiceFacade.findOpenBySubscriberForPayment(subscription.getSubscriber().getId());
               /* if (openInvoiceList == null || openInvoiceList.isEmpty()) {
                    throw new NoInvoiceFoundException(String.format("No invoice exists for subscriber %d", subscriber.getId()));
                }
                */
                long residualValue = 0;

                log.info("Found open invoices: " + openInvoiceList);

                if (openInvoiceList != null && !openInvoiceList.isEmpty()) {
                    for (Invoice invoice : openInvoiceList) {
                        residualValue = invoice.addPaymentToList(payment, residualValue);                        
                        if (residualValue <= 0)
                            break;
                        
                        log.debug("Invoice after payment" + invoice);
                    }
                    //invoiceFacade.update(invoice);
                    ///invoiceFacade.save(invoice);
                }
                
                //this.em.merge(subscription.getBalance());
                //subscription.setLastPaymentDate(payment.getLastUpdateDate());
                //this.update(subscription);
                //Subscription sbn = this.findWithoutRefresh(subscriptionID);
                //log.debug(String.format("Subscription upon retrieval: %s", sbn));
            }
            catch (NoResultException ex) {
                log.error(String.format("No subscription retrieved for ID %d", subscriptionID), ex);      
                ctx.setRollbackOnly();
                throw new NoSuchSubscriptionException(String.format("subscriptionID %d not found"));            
            }
            return payment;
        }
        
        public Subscription findWithoutRefresh(long pk) {
            return em.find(Subscription.class, pk);
        }
        
        public void transferBalance (Long paymentID, Long targetID) throws Exception {
            Payment payment = null;
            Subscription targetSubscription = null;
            
            try {                
                payment = paymentFacade.find(paymentID);
                targetSubscription = find(targetID);
                transFacade.transferPaymentToSubscription(payment, targetSubscription);
                payment.setDsc(String.format("%s\nPayment transfered from subscription %s", (payment.getDsc() != null ? payment.getDsc() : ""), payment.getAccount().getAgreement()));               
                payment.setAccount(targetSubscription);
                payment.setSubscriber_id(targetID);
                payment.setServiceId(targetSubscription.getService());
            }
            catch (Exception ex) {
                StringBuilder sb = new StringBuilder("Cannot transfer payment=");
                sb.append(payment);
                sb.append("to subscription=");                
                sb.append(targetSubscription);
                String message = sb.toString();
                log.error(message);
                throw new Exception(message, ex);
            }
        }

    public void transferPaymentForFinance (Long paymentID, Long targetID) throws Exception {
        Payment payment = null;
        Subscription targetSubscription = null;

        try {
            HttpSession session = (HttpSession) FacesContext.getCurrentInstance().getExternalContext().getSession(false);
            User user = userFacade.find((Long) session.getAttribute("userID"));
            payment = paymentFacade.find(paymentID);
            targetSubscription = find(targetID);
            //transFacade.transferPaymentToSubscription(payment, targetSubscription);
            AccountingTransaction trans = accTransFacade.transferPayment(targetSubscription, payment.getAccount(), payment, user);
            payment.setDsc(String.format("%s\nPayment amount transfered to subscription %s, accountingTransaction=%d", (payment.getDsc() != null ? payment.getDsc() : ""), targetSubscription.getAgreement(), trans.getId()));
            //payment.setAccount(targetSubscription);
           // payment.setSubscriber_id(targetID);
            //payment.setServiceId(targetSubscription.getService());
        }
        catch (Exception ex) {
            StringBuilder sb = new StringBuilder("Cannot transfer payment=");
            sb.append(payment);
            sb.append("to subscription=");
            sb.append(targetSubscription);
            String message = sb.toString();
            log.error(message);
            throw new Exception(message, ex);
        }
    }

    public void transferBalanceForFinance (Long subscriptionID, Long targetID) throws Exception {
       Subscription subscription = null;
        Subscription targetSubscription = null;

        try {
            HttpSession session = (HttpSession) FacesContext.getCurrentInstance().getExternalContext().getSession(false);
            User user = userFacade.find((Long) session.getAttribute("userID"));
            subscription = find(subscriptionID);
            targetSubscription = find(targetID);
            //transFacade.transferPaymentToSubscription(payment, targetSubscription);
            accTransFacade.transferBalance(targetSubscription, subscription, user);
        }
        catch (Exception ex) {
            StringBuilder sb = new StringBuilder("Cannot transfer balance from subscription=");
            sb.append(subscription);
            sb.append("to subscription=");
            sb.append(targetSubscription);
            String message = sb.toString();
            log.error(message);
            throw new Exception(message, ex);
        }
    }

    public void transferBalanceForFinanceForFinance (Long paymentID, Long targetID) throws Exception {
        Payment payment = null;
        Subscription targetSubscription = null;

        try {
            payment = paymentFacade.find(paymentID);
            targetSubscription = find(targetID);
            transFacade.transferPaymentToSubscription(payment, targetSubscription);
            payment.setDsc(String.format("%s\nPayment transfered from subscription %s", (payment.getDsc() != null ? payment.getDsc() : ""), payment.getAccount().getAgreement()));
            payment.setAccount(targetSubscription);
            payment.setSubscriber_id(targetID);
            payment.setServiceId(targetSubscription.getService());
        }
        catch (Exception ex) {
            StringBuilder sb = new StringBuilder("Cannot transfer payment=");
            sb.append(payment);
            sb.append("to subscription=");
            sb.append(targetSubscription);
            String message = sb.toString();
            log.error(message);
            throw new Exception(message, ex);
        }
    }

    public void findByAgreement (String agreement) throws DuplicateAgreementException {
        CriteriaBuilder cb = getEntityManager().getCriteriaBuilder();
        CriteriaQuery<Long> query = cb.createQuery(Long.class);
        Root root = query.from(Subscription.class);
        query.select(cb.count(root));
        query.where(cb.equal(root.get("agreement"), agreement));
        Long numberOfSbns = getEntityManager().createQuery(query).getSingleResult();

        if (numberOfSbns > 0)
            throw new DuplicateAgreementException();

    }

    public List<Subscription> findAllEquipmentPaginated () {
        return em.createQuery("select distinct sbn from Subscription sbn join SubscriptionSetting st where st.value like '%1%'", Subscription.class)
                .getResultList();
    }
}
