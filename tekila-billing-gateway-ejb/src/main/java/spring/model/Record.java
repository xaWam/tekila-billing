package spring.model;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import java.util.Date;
/**
 * @author gurbanaz
 * @date 08.04.2019 / 13:52
 */

@XmlRootElement(name = "record")
@XmlAccessorType(XmlAccessType.FIELD)
public class Record {

    @XmlElement(name = "recordtype")
    private String recordType;
    @XmlElement(name = "referenceno")
    private String referenceNo;
    @XmlElement(name = "subscribertype")
    private String subscriberType;
    @XmlElement(name = "nationalid")
    private String nationalId;
    @XmlElement(name = "passportno")
    private String passportNo;
    @XmlElement(name = "taxid")
    private String taxId;
    @XmlElement(name = "name")
    private String name;
    @XmlElement(name = "surname")
    private String surname;
    @XmlElement(name = "nationality")
    private String nationality;
    @XmlElement(name = "birthdate")
    private Date birthdate;
    @XmlElement(name = "birthplace")
    private String birthPlace;
    @XmlElement(name = "fathername")
    private String fatherName;
    @XmlElement(name = "mothername")
    private String motherName;
    @XmlElement(name = "address")
    private String address;
    @XmlElement(name = "city")
    private String city;
    @XmlElement(name = "country")
    private String country;
    @XmlElement(name = "sid")
    private String sid;
    @XmlElement(name = "recorddate")
    private Date recordDate;

    public Record() {
        recordType = "-";
        referenceNo = "-";
        subscriberType = " A";
        nationalId = "-";
        passportNo = "-";
        taxId = "-";
        name = "-";
        surname = "-";
        nationality = "-";
        birthPlace = "-";
        fatherName = "-";
        motherName = "-";
        address = "-";
        city = "-";
        country = "-";
        sid = "-";
        recordDate = new Date();






    }

    public Record(String recordType,
                  String referenceNo,
                  String subscriberType,
                  String nationalId,
                  String passportNo,
                  String taxId,
                  String name,
                  String surname,
                  String nationality,
                  Date birthdate,
                  String birthPlace,
                  String fatherName,
                  String motherName,
                  String address,
                  String city,
                  String country,
                  String sid,
                  Date recordDate) {
        this.recordType = recordType;
        this.referenceNo = referenceNo;
        this.subscriberType = subscriberType;
        this.nationalId = nationalId;
        this.passportNo = passportNo;
        this.taxId = taxId;
        this.name = name;
        this.surname = surname;
        this.nationality = nationality;
        this.birthdate = birthdate;
        this.birthPlace = birthPlace;
        this.fatherName = fatherName;
        this.motherName = motherName;
        this.address = address;
        this.city = city;
        this.country = country;
        this.sid = sid;
        this.recordDate = recordDate;
    }

    public String getRecordType() {
        return recordType;
    }

    public void setRecordType(String recordType) {
        this.recordType = recordType;
    }

    public String getReferenceNo() {
        return referenceNo;
    }

    public void setReferenceNo(String referenceNo) {
        this.referenceNo = referenceNo;
    }

    public String getSubscriberType() {
        return subscriberType;
    }

    public void setSubscriberType(String subscriberType) {
        this.subscriberType = subscriberType;
    }

    public String getNationalId() {
        return nationalId;
    }

    public void setNationalId(String nationalId) {
        this.nationalId = nationalId;
    }

    public String getPassportNo() {
        return passportNo;
    }

    public void setPassportNo(String passportNo) {
        this.passportNo = passportNo;
    }

    public String getTaxId() {
        return taxId;
    }

    public void setTaxId(String taxId) {
        this.taxId = taxId;
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

    public String getNationality() {
        return nationality;
    }

    public void setNationality(String nationality) {
        this.nationality = nationality;
    }

    public Date getBirthdate() {
        return birthdate;
    }

    public void setBirthdate(Date birthdate) {
        this.birthdate = birthdate;
    }

    public String getBirthPlace() {
        return birthPlace;
    }

    public void setBirthPlace(String birthPlace) {
        this.birthPlace = birthPlace;
    }

    public String getFatherName() {
        return fatherName;
    }

    public void setFatherName(String fatherName) {
        this.fatherName = fatherName;
    }

    public String getMotherName() {
        return motherName;
    }

    public void setMotherName(String motherName) {
        this.motherName = motherName;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getCity() {
        return city;
    }

    public void setCity(String city) {
        this.city = city;
    }

    public String getCountry() {
        return country;
    }

    public void setCountry(String country) {
        this.country = country;
    }

    public String getSid() {
        return sid;
    }

    public void setSid(String sid) {
        this.sid = sid;
    }

    public Date getRecordDate() {
        return recordDate;
    }

    public void setRecordDate(Date recordDate) {
        this.recordDate = recordDate;
    }

    @Override
    public String toString() {
        return "Record{" +
                "recordType=" + recordType +
                ", referenceNo='" + referenceNo + '\'' +
                ", subscriberType='" + subscriberType + '\'' +
                ", nationalId='" + nationalId + '\'' +
                ", passportNo='" + passportNo + '\'' +
                ", taxId='" + taxId + '\'' +
                ", name='" + name + '\'' +
                ", surname='" + surname + '\'' +
                ", nationality='" + nationality + '\'' +
                ", birthdate=" + birthdate +
                ", birthPlace='" + birthPlace + '\'' +
                ", fatherName='" + fatherName + '\'' +
                ", motherName='" + motherName + '\'' +
                ", address='" + address + '\'' +
                ", city='" + city + '\'' +
                ", country='" + country + '\'' +
                ", sid='" + sid + '\'' +
                ", recordDate=" + recordDate +
                '}';
    }
}
