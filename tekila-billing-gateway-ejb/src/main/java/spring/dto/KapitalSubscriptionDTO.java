package spring.dto;

import java.io.Serializable;

/**
 * Created by ShakirG on 20/07/2018.
 */
public class KapitalSubscriptionDTO implements Serializable {

    public String agreement;
    public String name;
    public String surname;
    public String service;
    public String status;
    public String region;

    public String getAgreement() {
        return agreement;
    }

    public void setAgreement(String agreement) {
        this.agreement = agreement;
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

    public String getService() {
        return service;
    }

    public void setService(String service) {
        this.service = service;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getRegion() {
        return region;
    }

    public void setRegion(String region) {
        this.region = region;
    }

    @Override
    public String toString() {
        return "KapitalSubscriptionDTO{" +
                "agreement='" + agreement + '\'' +
                ", name='" + name + '\'' +
                ", surname='" + surname + '\'' +
                ", service='" + service + '\'' +
                ", status='" + status + '\'' +
                ", region='" + region + '\'' +
                '}';
    }
}
