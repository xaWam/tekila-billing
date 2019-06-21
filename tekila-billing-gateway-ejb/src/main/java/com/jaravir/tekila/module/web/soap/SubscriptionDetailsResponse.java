package com.jaravir.tekila.module.web.soap;

import com.jaravir.tekila.base.entity.Language;
import com.jaravir.tekila.module.sales.SalesPerson;
import com.jaravir.tekila.module.subscription.persistence.entity.SubscriptionDetails;
import javax.xml.bind.annotation.XmlElement;

/**
 *
 * @author khsadigov
 */
public class SubscriptionDetailsResponse extends BaseResponse {

    @XmlElement(nillable = true)
    private String city;
    @XmlElement(nillable = true)
    private String ats;
    @XmlElement(nillable = true)
    private String street;
    @XmlElement(nillable = true)
    private String building;
    @XmlElement(nillable = true)
    private String apartment;
    @XmlElement(nillable = true)
    private SalesPerson salesPerson;
    @XmlElement(nillable = true)
    private Language lang;
    @XmlElement(nillable = true)
    private String desc;
    @XmlElement(nillable = true)
    private String name;
    @XmlElement(nillable = true)
    private String surname;

    public SubscriptionDetailsResponse() {
    }

    public SubscriptionDetailsResponse(SubscriptionDetails entity) {

        this.city = entity.getCity();
        this.ats = entity.getAts();
        this.street = entity.getStreet();
        this.building = entity.getBuilding();
        this.apartment = entity.getApartment();
        this.salesPerson = entity.getSalesPerson();
        this.lang = entity.getLanguage();
        this.desc = entity.getDesc();
        this.name = entity.getName();
        this.surname = entity.getSurname();
    }

    /**
     * @return the city
     */
    public String getCity() {
        return city;
    }

    /**
     * @param city the city to set
     */
    public void setCity(String city) {
        this.city = city;
    }

    /**
     * @return the ats
     */
    public String getAts() {
        return ats;
    }

    /**
     * @param ats the ats to set
     */
    public void setAts(String ats) {
        this.ats = ats;
    }

    /**
     * @return the street
     */
    public String getStreet() {
        return street;
    }

    /**
     * @param street the street to set
     */
    public void setStreet(String street) {
        this.street = street;
    }

    /**
     * @return the building
     */
    public String getBuilding() {
        return building;
    }

    /**
     * @param building the building to set
     */
    public void setBuilding(String building) {
        this.building = building;
    }

    /**
     * @return the apartment
     */
    public String getApartment() {
        return apartment;
    }

    /**
     * @param apartment the apartment to set
     */
    public void setApartment(String apartment) {
        this.apartment = apartment;
    }

    /**
     * @return the salesPerson
     */
    public SalesPerson getSalesPerson() {
        return salesPerson;
    }

    /**
     * @param salesPerson the salesPerson to set
     */
    public void setSalesPerson(SalesPerson salesPerson) {
        this.salesPerson = salesPerson;
    }

    /**
     * @return the lang
     */
    public Language getLang() {
        return lang;
    }

    /**
     * @param lang the lang to set
     */
    public void setLang(Language lang) {
        this.lang = lang;
    }

    /**
     * @return the desc
     */
    public String getDesc() {
        return desc;
    }

    /**
     * @param desc the desc to set
     */
    public void setDesc(String desc) {
        this.desc = desc;
    }

    /**
     * @return the name
     */
    public String getName() {
        return name;
    }

    /**
     * @param name the name to set
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * @return the surname
     */
    public String getSurname() {
        return surname;
    }

    /**
     * @param surname the surname to set
     */
    public void setSurname(String surname) {
        this.surname = surname;
    }

}
