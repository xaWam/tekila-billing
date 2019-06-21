package spring.dto;

import com.jaravir.tekila.base.entity.Language;
import com.jaravir.tekila.module.service.ServiceType;
//import com.jaravir.tekila.module.service.entity.ServiceProperty;
import com.jaravir.tekila.module.service.entity.ServiceProvider;
import com.jaravir.tekila.module.subscription.persistence.entity.*;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

import java.io.Serializable;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.util.Date;

public class SubscriptionDTO implements Serializable {

    private Long id;
    private ServiceDTO service;
    private BalanceDTO balance;
    private SubscriptionDetailDTO details;
    private String agreement;
    private String identifier;
    private SubscriptionStatus status;
    private SubscriptionType type;
    private DateTime expirationDate;
    private Date creationDate;
    private DateTime lastStatusChangeDate;
    private DateTime lastPaymentDate;
    private DateTime billedUpToDate;
    private DateTime lastBilledDate;
    private DateTime expirationDateWithGracePeriod;
    private DateTime activationDate;
    private long installationFee;
    private ServicePropertyDTO serviceProperty;
    private String serviceType;
    private SubscriberDetailsSmallDTO subscriberDetails;
    private String billingModel;

    public static class BalanceDTO {

        private Long id;
        private long realBalance;
        private long promoBalance;

        public String getPromoBalanceForView() {
            double interm = promoBalance / 100000d;
            DecimalFormat df = new DecimalFormat();
            df.setRoundingMode(RoundingMode.FLOOR);
            return String.format("%.2f", interm);
        }

        public Double getPromoBalanceAsDouble() {
            double interm = promoBalance / 100000d;
            DecimalFormat df = new DecimalFormat();
            df.setRoundingMode(RoundingMode.FLOOR);
            return Double.valueOf(String.format("%.2f", interm));
        }

        public String getRealBalanceForView() {
            double interm = realBalance / 100000d;
            DecimalFormat df = new DecimalFormat();
            df.setRoundingMode(RoundingMode.FLOOR);
            return String.format("%.2f", interm);
        }

        public Double getRealBalanceAsNumber() {
            double interm = realBalance / 100000d;
            DecimalFormat df = new DecimalFormat();
            df.setRoundingMode(RoundingMode.FLOOR);
            return Double.valueOf(String.format("%.2f", interm));
        }


        public long getRealBalance() {
            return realBalance;
        }

        public void setRealBalance(long realBalance) {
            this.realBalance = realBalance;
        }

        public long getPromoBalance() {
            return promoBalance;
        }

        public void setPromoBalance(long promoBalance) {
            this.promoBalance = promoBalance;
        }

        public Long getId() {
            return id;
        }

        public void setId(Long id) {
            this.id = id;
        }
    }


    public static class ServiceDTO implements Serializable {
        private Long id;
        private String name;
        private long servicePrice;
        private ServiceType serviceType;
        private long installFee;
        private String dsc;
        private ServiceProvider provider;

        public Long getId() {
            return id;
        }

        public void setId(Long id) {
            this.id = id;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public long getServicePrice() {
            return servicePrice;
        }

        public void setServicePrice(long servicePrice) {
            this.servicePrice = servicePrice;
        }

        public ServiceType getServiceType() {
            return serviceType;
        }

        public void setServiceType(ServiceType serviceType) {
            this.serviceType = serviceType;
        }

        public long getInstallFee() {
            return installFee;
        }

        public void setInstallFee(long installFee) {
            this.installFee = installFee;
        }

        public String getDsc() {
            return dsc;
        }

        public void setDsc(String dsc) {
            this.dsc = dsc;
        }

        public ServiceProvider getProvider() {
            return provider;
        }

        public void setProvider(ServiceProvider provider) {
            this.provider = provider;
        }
    }

    public static class SubscriptionDetailDTO implements Serializable {
        private Long id;
        private String city;
        private String ats;
        private String street;
        private String building;
        private String apartment;
        private String entrance;
        private String floor;
        private Language lang;
        private String desc;
        private String name;
        private String surname;
        private String password;
        private String comments;
        private boolean isAvailableEcare;

        public boolean isAvailableEcare() {
            return isAvailableEcare;
        }

        public void setAvailableEcare(boolean availableEcare) {
            isAvailableEcare = availableEcare;
        }

        public Long getId() {
            return id;
        }

        public void setId(Long id) {
            this.id = id;
        }

        public String getCity() {
            return city;
        }

        public void setCity(String city) {
            this.city = city;
        }

        public String getAts() {
            return ats;
        }

        public void setAts(String ats) {
            this.ats = ats;
        }

        public String getStreet() {
            return street;
        }

        public void setStreet(String street) {
            this.street = street;
        }

        public String getBuilding() {
            return building;
        }

        public void setBuilding(String building) {
            this.building = building;
        }

        public String getApartment() {
            return apartment;
        }

        public void setApartment(String apartment) {
            this.apartment = apartment;
        }

        public String getEntrance() {
            return entrance;
        }

        public void setEntrance(String entrance) {
            this.entrance = entrance;
        }

        public String getFloor() {
            return floor;
        }

        public void setFloor(String floor) {
            this.floor = floor;
        }

        public Language getLang() {
            return lang;
        }

        public void setLang(Language lang) {
            this.lang = lang;
        }

        public String getDesc() {
            return desc;
        }

        public void setDesc(String desc) {
            this.desc = desc;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getSurname() {
            return surname;
        }

        public void setSurname(String surname) {
            this.surname = surname;
        }

        public String getPassword() {
            return password;
        }

        public void setPassword(String password) {
            this.password = password;
        }

        public String getComments() {
            return comments;
        }

        public void setComments(String comments) {
            this.comments = comments;
        }
    }


    public SubscriberDetailsSmallDTO getSubscriberDetails() {
        return subscriberDetails;
    }

    public void setSubscriberDetails(SubscriberDetailsSmallDTO subscriberDetails) {
        this.subscriberDetails = subscriberDetails;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public ServiceDTO getService() {
        return service;
    }

    public void setService(ServiceDTO service) {
        this.service = service;
    }

    public BalanceDTO getBalance() {
        return balance;
    }

    public void setBalance(BalanceDTO balance) {
        this.balance = balance;
    }

    public SubscriptionDetailDTO getDetails() {
        return details;
    }

    public void setDetails(SubscriptionDetailDTO details) {
        this.details = details;
    }

    public String getAgreement() {
        return agreement;
    }

    public void setAgreement(String agreement) {
        this.agreement = agreement;
    }

    public String getIdentifier() {
        return identifier;
    }

    public void setIdentifier(String identifier) {
        this.identifier = identifier;
    }

    public int getStatusCode() {
        return status.STATUS;
    }

    public SubscriptionStatus getStatus() {
        return status;
    }

    public void setStatus(SubscriptionStatus status) {
        this.status = status;
    }

    public SubscriptionType getType() {
        return type;
    }

    public void setType(SubscriptionType type) {
        this.type = type;
    }

    public DateTime getExpirationDate() {
        return expirationDate;
    }

    public String getExpirationDateWithoutTime() {
        return getExpirationDate() != null ? getExpirationDate().toString(DateTimeFormat.forPattern("dd-MM-yyyy")) : "N/A";
    }

    public void setExpirationDate(DateTime expirationDate) {
        this.expirationDate = expirationDate;
    }

    public Date getCreationDate() {
        return creationDate;
    }

    public void setCreationDate(Date creationDate) {
        this.creationDate = creationDate;
    }

    public DateTime getLastStatusChangeDate() {
        return lastStatusChangeDate;
    }

    public void setLastStatusChangeDate(DateTime lastStatusChangeDate) {
        this.lastStatusChangeDate = lastStatusChangeDate;
    }

    public DateTime getLastPaymentDate() {
        return lastPaymentDate;
    }

    public void setLastPaymentDate(DateTime lastPaymentDate) {
        this.lastPaymentDate = lastPaymentDate;
    }

    public DateTime getBilledUpToDate() {
        return billedUpToDate;
    }

    public String getBilledUpToDateWithoutTime() {
        return getBilledUpToDate() != null ? getBilledUpToDate().toString(DateTimeFormat.forPattern("dd-MM-yyyy")) : "N/A";
    }

    public void setBilledUpToDate(DateTime billedUpToDate) {
        this.billedUpToDate = billedUpToDate;
    }

    public DateTime getLastBilledDate() {
        return lastBilledDate;
    }

    public void setLastBilledDate(DateTime lastBilledDate) {
        this.lastBilledDate = lastBilledDate;
    }

    public DateTime getExpirationDateWithGracePeriod() {
        return expirationDateWithGracePeriod;
    }

    public String getExpirationDateWithGracePeriodWithoutTime() {
        return getExpirationDateWithGracePeriod() != null ?
                getExpirationDateWithGracePeriod().toString(DateTimeFormat.forPattern("dd-MM-yyyy")) :
                "N/A";
    }

    public void setExpirationDateWithGracePeriod(DateTime expirationDateWithGracePeriod) {
        this.expirationDateWithGracePeriod = expirationDateWithGracePeriod;
    }

    public DateTime getActivationDate() {
        return activationDate;
    }

    public void setActivationDate(DateTime activationDate) {
        this.activationDate = activationDate;
    }

    public long getInstallationFee() {
        return installationFee;
    }


    public void setInstallationFee(long installationFee) {
        this.installationFee = installationFee;
    }

    public ServicePropertyDTO getServiceProperty() {
        return serviceProperty;
    }

    public void setServiceProperty(ServicePropertyDTO serviceProperty) {
        this.serviceProperty = serviceProperty;
    }

    public String getServiceType() {
        return serviceType;
    }

    public void setServiceType(String serviceType) {
        this.serviceType = serviceType;
    }

    public String getBillingModel() {
        return billingModel;
    }

    public void setBillingModel(String billingModel) {
        this.billingModel = billingModel;
    }
}

