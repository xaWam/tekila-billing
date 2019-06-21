package com.jaravir.tekila.module.web.soap;

import com.jaravir.tekila.base.binding.xml.mapper.XmlSchemaNamespace;
import com.jaravir.tekila.module.subscription.persistence.entity.Subscription;
import com.jaravir.tekila.module.subscription.persistence.entity.SubscriptionStatus;
import com.jaravir.tekila.module.subscription.persistence.entity.SubscriptionType;
import com.jaravir.tekila.module.subscription.persistence.entity.external.TechnicalStatus;
import java.util.Date;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;

/**
 *
 * @author khsadigov
 */
@XmlType(namespace = XmlSchemaNamespace.SUBSCRIPTION_NS)
public class SubscriptionResponse extends BaseResponse {

    @XmlElement(nillable = true)
    public ServiceResponse service;
    @XmlElement(nillable = true)
    public BalanceResponse balance;
    @XmlElement(nillable = true)
    public String agreement;
    @XmlElement(nillable = true)
    public String identifier;
    @XmlElement(nillable = true)
    public SubscriptionStatus status;
    @XmlElement(nillable = true)
    public SubscriptionDetailsResponse details;
    @XmlElement(nillable = true)
    public SubscriptionType type;
    @XmlElement(nillable = true)
    public Date expirationDate;
    @XmlElement(nillable = true)
    public Date creationDate;
    @XmlElement(nillable = true)
    public Date lastStatusChangeDate;
    @XmlElement(nillable = true)
    public Date lastPaymentDate;
    @XmlElement(nillable = true)
    public Date billedUpToDate;
    @XmlElement(nillable = true)
    public Date lastBilledDate;
    @XmlElement(nillable = true)
    public Date expirationDateWithGracePeriod;
    @XmlElement(nillable = true)
    public Date activationDate;
//    public List<SubscriptionSetting> settings;
    @XmlElement(nillable = true)
    public int gracePeriodInDays;
    @XmlElement(nillable = true)
    public TechnicalStatus technicalStatus;
    @XmlElement(nillable = true)
    public double installationFeeRate;
    @XmlElement(nillable = true)
    public long serviceFeeRate;
    @XmlElement(nillable = true)
    public double unbilledAmount;

    public SubscriptionResponse() {
    }

    public SubscriptionResponse(Subscription entity) {

        this.service = new ServiceResponse(entity.getService());
        this.balance = new BalanceResponse(entity.getBalance());
        this.agreement = entity.getAgreement();
        this.identifier = entity.getIdentifier();
        this.status = entity.getStatus();

        try {
            InitialContext ctx = new InitialContext();
            SubscriptionDetailsResponseBuilder builder =
                    (SubscriptionDetailsResponseBuilder) ctx.lookup("java:module/SubscriptionDetailsResponseBuilder");
            this.details = builder.build(entity.getDetails());
        } catch (NamingException ex) {
            this.details = new SubscriptionDetailsResponse(entity.getDetails());
        }

        this.type = entity.getType();
        this.expirationDate = entity.getExpirationDate() != null ? entity.getExpirationDate().toDate() : null;
        this.creationDate = entity.getCreationDate();
        this.lastStatusChangeDate = entity.getLastStatusChangeDate() != null ? entity.getLastStatusChangeDate().toDate() : null;
        this.lastPaymentDate = entity.getLastPaymentDate() != null ? entity.getLastPaymentDate().toDate() : null;
        this.billedUpToDate = entity.getBilledUpToDate() != null ? entity.getBilledUpToDate().toDate() : null;
        this.lastBilledDate = entity.getLastBilledDate() != null ? entity.getLastBilledDate().toDate() : null;
        this.expirationDateWithGracePeriod = entity.getExpirationDateWithGracePeriod() != null ? entity.getExpirationDateWithGracePeriod().toDate() : null;
        this.activationDate = entity.getActivationDate() != null ? entity.getActivationDate().toDate() : null;
//        this.settings = entity.getSettings();
        this.gracePeriodInDays = entity.getBillingModel().getGracePeriodInDays();
        this.technicalStatus = entity.getTechnicalStatus();
        this.installationFeeRate = entity.getInstallationFeeInDouble() ;
        this.serviceFeeRate = entity.getServiceFeeRate();
        Double unbilledAmount = ((entity.calculateTotalCharge() -
                entity.getBalance().getRealBalance()) / 100000.0); //prepaid calculations minus real balance
        if (unbilledAmount < 0.0) {
            unbilledAmount = 0.0;
        }
        this.unbilledAmount = unbilledAmount;
    }
}
