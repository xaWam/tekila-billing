package spring.dto;

import org.joda.time.DateTime;

/**
 * @author ShakirG on 01/30/2019
 */
public class SubscriberSmallDTO {

    String agreement;
    long subscriptionID;
    String name;
    String surname;
    String equipmentId;
    String  balance;
    DateTime expirationDate;
    String serviceName;


    public String getBalance() {
        return balance;
    }

    public void setBalance(String balance) {
        this.balance = balance;
    }

    public String getServiceName() {
        return serviceName;
    }

    public void setServiceName(String serviceName) {
        this.serviceName = serviceName;
    }

    public DateTime getExpirationDate() {
        return expirationDate;
    }

    public void setExpirationDate(DateTime expirationDate) {
        this.expirationDate = expirationDate;
    }


    public String getAgreement() {
        return agreement;
    }

    public void setAgreement(String agreement) {
        this.agreement = agreement;
    }

    public long getSubscriptionID() {
        return subscriptionID;
    }

    public void setSubscriptionID(long subscriptionID) {
        this.subscriptionID = subscriptionID;
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

    public String getEquipmentId() {
        return equipmentId;
    }

    public void setEquipmentId(String equipmentId) {
        this.equipmentId = equipmentId;
    }




    @Override
    public String toString() {
        return "SubscriberSmallDTO{" +
                "agreement='" + agreement + '\'' +
                ", subscriptionID=" + subscriptionID +
                ", name='" + name + '\'' +
                ", surname='" + surname + '\'' +
                ", equipmentId='" + equipmentId + '\'' +
                '}';
    }
}
