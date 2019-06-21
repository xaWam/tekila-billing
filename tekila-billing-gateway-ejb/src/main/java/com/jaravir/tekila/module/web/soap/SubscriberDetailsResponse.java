package com.jaravir.tekila.module.web.soap;

import com.jaravir.tekila.base.entity.Language;
import com.jaravir.tekila.module.subscription.persistence.entity.CompanyType;
import com.jaravir.tekila.module.subscription.persistence.entity.Gender;
import com.jaravir.tekila.module.subscription.persistence.entity.Subscriber;
import com.jaravir.tekila.module.subscription.persistence.entity.SubscriberDetails;
import com.jaravir.tekila.module.subscription.persistence.entity.SubscriberType;
import java.util.Date;
import javax.xml.bind.annotation.XmlElement;

/**
 *
 * @author khsadigov
 */
public class SubscriberDetailsResponse extends BaseResponse {

    public SubscriberType type = SubscriberType.INDV;
    public String country = "Azerbaijan";
    public String citizenship = "Azerbaijan";
    @XmlElement(nillable = true)
    public Gender gender;
    @XmlElement(nillable = true)
    public String firstName;
    @XmlElement(nillable = true)
    public String middleName;
    @XmlElement(nillable = true)
    public String surname;
    @XmlElement(nillable = true)
    public String cityOfBirth;
    @XmlElement(nillable = true)
    public Date entryDate;
    @XmlElement(nillable = true)
    public Date dateOfBirth;
    @XmlElement(nillable = true)
    public Language lang;
    @XmlElement(nillable = true)
    public String passportSeries;
    @XmlElement(nillable = true)
    public String passportNumber;
    @XmlElement(nillable = true)
    public String passportAuthority;
    @XmlElement(nillable = true)
    public String passportValidTill;
    @XmlElement(nillable = true)
    public Date passportIssueDate;
    @XmlElement(nillable = true)
    public String email;
    @XmlElement(nillable = true)
    public String phoneMobile;
    @XmlElement(nillable = true)
    public String phoneMobileAlt;
    @XmlElement(nillable = true)
    public String phoneLandline;
    @XmlElement(nillable = true)
    public String city;
    @XmlElement(nillable = true)
    public String ats;
    @XmlElement(nillable = true)
    public String street;
    @XmlElement(nillable = true)
    public String building;
    @XmlElement(nillable = true)
    public String apartment;
    @XmlElement(nillable = true)
    public String pinCode;
    @XmlElement(nillable = true)
    public String companyName;
    @XmlElement(nillable = true)
    public CompanyType companyType;
    @XmlElement(nillable = true)
    public String bankAccount;

    public SubscriberDetailsResponse() {
    }

    public SubscriberDetailsResponse(SubscriberDetails entity) {

        this.gender = entity.getGender();
        this.firstName = entity.getFirstName();
        this.middleName = entity.getMiddleName();
        this.surname = entity.getSurname();
        this.cityOfBirth = entity.getCityOfBirth();
        this.entryDate = entity.getEntryDate();
        this.dateOfBirth = entity.getDateOfBirth();
        this.lang = entity.getLanguage();
        this.passportSeries = entity.getPassportSeries();
        this.passportNumber = entity.getPassportNumber();
        this.passportAuthority = entity.getPassportAuthority();
        this.passportValidTill = entity.getPassportValidTill();
        this.passportIssueDate = entity.getPassportIssueDate();
        this.email = entity.getEmail();
        this.phoneMobile = entity.getPhoneMobile();
        this.phoneMobileAlt = entity.getPhoneMobileAlt();
        this.phoneLandline = entity.getPhoneLandline();
        this.city = entity.getCity();
        this.ats = entity.getAts();
        this.street = entity.getStreet();
        this.building = entity.getBuilding();
        this.apartment = entity.getApartment();
        this.pinCode = entity.getPinCode();
        this.companyName = entity.getCompanyName();
        this.companyType = entity.getCompanyType();
        this.bankAccount = entity.getBankAccount();

    }

}
