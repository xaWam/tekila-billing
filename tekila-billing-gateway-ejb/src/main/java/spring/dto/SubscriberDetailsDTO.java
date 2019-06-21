package spring.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonView;
import com.jaravir.tekila.base.entity.Language;
import com.jaravir.tekila.jsonview.JsonViews;
import com.jaravir.tekila.module.subscription.persistence.entity.CompanyType;
import com.jaravir.tekila.module.subscription.persistence.entity.Gender;
import com.jaravir.tekila.module.subscription.persistence.entity.Subscriber;
import com.jaravir.tekila.module.subscription.persistence.entity.SubscriberType;

import java.util.Date;

/**
 * @author MusaAl
 * @date 4/2/2018 : 10:39 AM
 */
public class SubscriberDetailsDTO extends BaseDTO{

    /* individual
    general info */
    private SubscriberType type = SubscriberType.INDV;
    private Gender gender;
    private String firstName;
    private String middleName;
    private String surname;
    private String cityOfBirth;
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "dd.MM.yyyy HH:mm")
    private Date entryDate;

    private String comments;
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "dd.MM.yyyy HH:mm")
    private Date dateOfBirth;
    private String citizenship = "Azerbaijan";
    private Language lang;
    //passport info
    private String country = "Azerbaijan";
    private String passportSeries;
    private String passportNumber;
    private String passportAuthority;
    private String passportValidTill;
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "dd.MM.yyyy HH:mm")
    private Date passportIssueDate;
    private String email;
    private String phoneMobile;
    private String phoneMobileAlt;
    private String phoneLandline;
    //address
    private String city;
    private String ats;
    private String street;
    private String building;
    private String apartment;
    private long subscriberId;
    private String pinCode;
    /*Corporate
     * general info
     */
    private String companyName;
    private CompanyType companyType;
    private String bankAccount;

    public SubscriberType getType() {
        return type;
    }

    public void setType(SubscriberType type) {
        this.type = type;
    }

    public Gender getGender() {
        return gender;
    }

    public void setGender(Gender gender) {
        this.gender = gender;
    }

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getMiddleName() {
        return middleName;
    }

    public void setMiddleName(String middleName) {
        this.middleName = middleName;
    }

    public String getSurname() {
        return surname;
    }

    public void setSurname(String surname) {
        this.surname = surname;
    }

    public String getCityOfBirth() {
        return cityOfBirth;
    }

    public void setCityOfBirth(String cityOfBirth) {
        this.cityOfBirth = cityOfBirth;
    }

    public Date getEntryDate() {
        return entryDate;
    }

    public void setEntryDate(Date entryDate) {
        this.entryDate = entryDate;
    }

    public String getComments() {
        return comments;
    }

    public void setComments(String comments) {
        this.comments = comments;
    }

    public Date getDateOfBirth() {
        return dateOfBirth;
    }

    public void setDateOfBirth(Date dateOfBirth) {
        this.dateOfBirth = dateOfBirth;
    }

    public String getCitizenship() {
        return citizenship;
    }

    public void setCitizenship(String citizenship) {
        this.citizenship = citizenship;
    }

    public Language getLang() {
        return lang;
    }

    public void setLang(Language lang) {
        this.lang = lang;
    }

    public String getCountry() {
        return country;
    }

    public void setCountry(String country) {
        this.country = country;
    }

    public String getPassportSeries() {
        return passportSeries;
    }

    public void setPassportSeries(String passportSeries) {
        this.passportSeries = passportSeries;
    }

    public String getPassportNumber() {
        return passportNumber;
    }

    public void setPassportNumber(String passportNumber) {
        this.passportNumber = passportNumber;
    }

    public String getPassportAuthority() {
        return passportAuthority;
    }

    public void setPassportAuthority(String passportAuthority) {
        this.passportAuthority = passportAuthority;
    }

    public String getPassportValidTill() {
        return passportValidTill;
    }

    public void setPassportValidTill(String passportValidTill) {
        this.passportValidTill = passportValidTill;
    }

    public Date getPassportIssueDate() {
        return passportIssueDate;
    }

    public void setPassportIssueDate(Date passportIssueDate) {
        this.passportIssueDate = passportIssueDate;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPhoneMobile() {
        return phoneMobile;
    }

    public void setPhoneMobile(String phoneMobile) {
        this.phoneMobile = phoneMobile;
    }

    public String getPhoneMobileAlt() {
        return phoneMobileAlt;
    }

    public void setPhoneMobileAlt(String phoneMobileAlt) {
        this.phoneMobileAlt = phoneMobileAlt;
    }

    public String getPhoneLandline() {
        return phoneLandline;
    }

    public void setPhoneLandline(String phoneLandline) {
        this.phoneLandline = phoneLandline;
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

    public long getSubscriberId() {
        return subscriberId;
    }

    public void setSubscriberId(long subscriberId) {
        this.subscriberId = subscriberId;
    }

    public String getPinCode() {
        return pinCode;
    }

    public void setPinCode(String pinCode) {
        this.pinCode = pinCode;
    }

    public String getCompanyName() {
        return companyName;
    }

    public void setCompanyName(String companyName) {
        this.companyName = companyName;
    }

    public CompanyType getCompanyType() {
        return companyType;
    }

    public void setCompanyType(CompanyType companyType) {
        this.companyType = companyType;
    }

    public String getBankAccount() {
        return bankAccount;
    }

    public void setBankAccount(String bankAccount) {
        this.bankAccount = bankAccount;
    }

    @Override
    public String toString() {
        return "SubscriberDetailsDTO{" +
                "type=" + type +
                ", gender=" + gender +
                ", firstName='" + firstName + '\'' +
                ", middleName='" + middleName + '\'' +
                ", surname='" + surname + '\'' +
                ", cityOfBirth='" + cityOfBirth + '\'' +
                ", entryDate=" + entryDate +
                ", comments='" + comments + '\'' +
                ", dateOfBirth=" + dateOfBirth +
                ", citizenship='" + citizenship + '\'' +
                ", lang=" + lang +
                ", country='" + country + '\'' +
                ", passportSeries='" + passportSeries + '\'' +
                ", passportNumber='" + passportNumber + '\'' +
                ", passportAuthority='" + passportAuthority + '\'' +
                ", passportValidTill='" + passportValidTill + '\'' +
                ", passportIssueDate=" + passportIssueDate +
                ", email='" + email + '\'' +
                ", phoneMobile='" + phoneMobile + '\'' +
                ", phoneMobileAlt='" + phoneMobileAlt + '\'' +
                ", phoneLandline='" + phoneLandline + '\'' +
                ", city='" + city + '\'' +
                ", ats='" + ats + '\'' +
                ", street='" + street + '\'' +
                ", building='" + building + '\'' +
                ", apartment='" + apartment + '\'' +
                ", subscriberId=" + subscriberId +
                ", pinCode='" + pinCode + '\'' +
                ", companyName='" + companyName + '\'' +
                ", companyType=" + companyType +
                ", bankAccount='" + bankAccount + '\'' +
                '}';
    }
}
