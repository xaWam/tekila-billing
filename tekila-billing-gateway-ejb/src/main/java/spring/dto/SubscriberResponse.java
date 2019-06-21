package spring.dto;

import com.jaravir.tekila.module.subscription.persistence.entity.Subscriber;
import com.jaravir.tekila.module.subscription.persistence.entity.Subscription;
import org.joda.time.DateTime;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Created by KamranMa on 25.12.2017.
 */
public class SubscriberResponse {
    public final Long id;
    public final String firstName;
    public final String surname;
    public final String middleName;
    public final String passportNumber;
    public final String city;
    public final String street;
    public final String phoneMobile;
    public final String email;
    public final List<SubscriptionResponse> subscriptions;

    private SubscriberResponse(Subscriber subscriber) {
        this.id = subscriber.getId();
        this.firstName = subscriber.getDetails().getFirstName();
        this.surname = subscriber.getDetails().getSurname();
        this.middleName = subscriber.getDetails().getMiddleName();
        this.passportNumber = subscriber.getDetails().getPassportNumber();
        this.city = subscriber.getDetails().getCity();
        this.street = subscriber.getDetails().getStreet();
        this.phoneMobile = subscriber.getDetails().getPhoneMobile();
        this.email = subscriber.getDetails().getEmail();
        subscriptions = new ArrayList<>();
        for (Subscription s : subscriber.getSubscriptions()) {
            subscriptions.add(new SubscriptionResponse(s));
        }
    }


    private class SubscriptionResponse {

        public SubscriptionResponse(Subscription subscription) {
            this.id = subscription.getId();
            this.agreement = subscription.getAgreement();
            this.balance = subscription.getBalance().getRealBalance();
            this.created = subscription.getCreationDate();
            this.expiration = subscription.getExpirationDate();
            this.service = subscription.getService().getName();
            this.provider = subscription.getService().getProvider().getName();
            this.providerId = subscription.getService().getProvider().getId();
        }

        private Long id;
        private String agreement;
        private double balance;
        private Date created;
        private DateTime expiration;
        private String service;
        private String provider;
        private Long providerId;

        public Long getId() {
            return id;
        }

        public void setId(Long id) {
            this.id = id;
        }

        public String getAgreement() {
            return agreement;
        }

        public void setAgreement(String agreement) {
            this.agreement = agreement;
        }

        public double getBalance() {
            return balance;
        }

        public void setBalance(double balance) {
            this.balance = balance;
        }

        public Date getCreated() {
            return created;
        }

        public void setCreated(Date created) {
            this.created = created;
        }

        public DateTime getExpiration() {
            return expiration;
        }

        public void setExpiration(DateTime expiration) {
            this.expiration = expiration;
        }

        public String getService() {
            return service;
        }

        public void setService(String service) {
            this.service = service;
        }

        public String getProvider() {
            return provider;
        }

        public void setProvider(String provider) {
            this.provider = provider;
        }

        public Long getProviderId() {
            return providerId;
        }

        public void setProviderId(Long providerId) {
            this.providerId = providerId;
        }
    }

    public static SubscriberResponse from(Subscriber subscriber) {
        return new SubscriberResponse(subscriber);
    }
}
