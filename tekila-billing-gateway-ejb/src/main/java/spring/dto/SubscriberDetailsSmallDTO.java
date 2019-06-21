package spring.dto;

/**
 * @author Gurban Azimli
 * @date 25/02/2019 11:35 AM
 */
public class SubscriberDetailsSmallDTO extends BaseDTO {

    private String firstName;
    private String middleName;
    private String surname;
    private String city;
    private String street;
    private String building;
    private String appartment;
    private String ats;

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

    public String getCity() {
        return city;
    }

    public void setCity(String city) {
        this.city = city;
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

    public String getAppartment() {
        return appartment;
    }

    public void setAppartment(String appartment) {
        this.appartment = appartment;
    }

    public String getAts() {
        return ats;
    }

    public void setAts(String ats) {
        this.ats = ats;
    }

    @Override
    public String toString() {
        return "SubscriberDetailsSmallDTO{" +
                "firstName='" + firstName + '\'' +
                ", middleName='" + middleName + '\'' +
                ", surname='" + surname + '\'' +
                ", city='" + city + '\'' +
                ", street='" + street + '\'' +
                ", building='" + building + '\'' +
                ", appartment='" + appartment + '\'' +
                ", ats='" + ats + '\'' +
                ", id=" + id +
                ", lastUpdateDate=" + lastUpdateDate +
                '}';
    }
}
