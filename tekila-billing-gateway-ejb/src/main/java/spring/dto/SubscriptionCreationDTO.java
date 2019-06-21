package spring.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.jaravir.tekila.module.subscription.persistence.entity.PaymentTypes;
import com.jaravir.tekila.module.subscription.persistence.entity.SubscriptionStatus;
import com.jaravir.tekila.module.subscription.persistence.entity.SubscriptionType;
import org.joda.time.DateTime;
import spring.controller.vm.SubscriptionVASSettingsVM;

import java.util.Date;

public class SubscriptionCreationDTO extends BaseDTO{

    private String agreement;
    private String identifier;
//    private SubscriptionStatus status;
//    private SubscriptionType type;
//    private DateTime expirationDate;
//    private Date creationDate;
//    private DateTime lastStatusChangeDate;
//    private DateTime lastPaymentDate;
//    private DateTime billedUpToDate;
//    private DateTime lastBilledDate;
//    private DateTime expirationDateWithGracePeriod;
//    private DateTime activationDate;
    private long installationFee;
    private SubscriptionDetailsDTO details;
    @JsonProperty private boolean isDiscountEnabled;
    @JsonProperty private boolean isTaxFreeEnabled;
    @JsonProperty private boolean isUseEquipmentFromStock;
    @JsonProperty private boolean isAvailableOnEcareSystem;
    @JsonProperty private boolean isUseSubscriberAddress;
    private long discountPercentage;
    private PaymentTypes paymentType;


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

    public SubscriptionDetailsDTO getDetails() {
        return details;
    }
    public void setDetails(SubscriptionDetailsDTO details) {
        this.details = details;
    }

    public long getDiscountPercentage() { return discountPercentage; }
    public void setDiscountPercentage(long discountPercentage) { this.discountPercentage = discountPercentage; }

    public PaymentTypes getPaymentType() { return paymentType; }
    public void setPaymentType(PaymentTypes paymentType) { this.paymentType = paymentType; }

    public boolean isDiscountEnabled() { return isDiscountEnabled; }
    public void setDiscountEnabled(boolean discountEnabled) { isDiscountEnabled = discountEnabled; }

    public boolean isTaxFreeEnabled() { return isTaxFreeEnabled; }
    public void setTaxFreeEnabled(boolean taxFreeEnabled) { isTaxFreeEnabled = taxFreeEnabled; }

    public boolean isUseEquipmentFromStock() {
        return isUseEquipmentFromStock;
    }
    public void setUseEquipmentFromStock(boolean useEquipmentFromStock) {
        isUseEquipmentFromStock = useEquipmentFromStock;
    }

    public boolean isAvailableOnEcareSystem() {
        return isAvailableOnEcareSystem;
    }
    public void setAvailableOnEcareSystem(boolean availableOnEcareSystem) {
        isAvailableOnEcareSystem = availableOnEcareSystem;
    }

    public boolean isUseSubscriberAddress() {
        return isUseSubscriberAddress;
    }
    public void setUseSubscriberAddress(boolean useSubscriberAddress) {
        isUseSubscriberAddress = useSubscriberAddress;
    }


    public long getInstallationFee() {
        return installationFee;
    }
    public void setInstallationFee(long installationFee) {
        this.installationFee = installationFee;
    }

    @Override
    public String toString() {
        return "SubscriptionCreationDTO{" +
                "agreement='" + agreement + '\'' +
                ", identifier='" + identifier + '\'' +
                ", installationFee=" + installationFee +
                ", details=" + details +
                ", isDiscountEnabled=" + isDiscountEnabled +
                ", isTaxFreeEnabled=" + isTaxFreeEnabled +
                ", isUseEquipmentFromStock=" + isUseEquipmentFromStock +
                ", isAvailableOnEcareSystem=" + isAvailableOnEcareSystem +
                ", isUseSubscriberAddress=" + isUseSubscriberAddress +
                ", discountPercentage=" + discountPercentage +
                ", paymentType=" + paymentType +
                '}';
    }
}

