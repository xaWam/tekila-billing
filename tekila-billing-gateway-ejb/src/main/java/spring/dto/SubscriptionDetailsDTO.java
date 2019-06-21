package spring.dto;

import com.jaravir.tekila.base.entity.Language;

import java.io.Serializable;

/**
 * @author MusaAl
 * @date 4/8/2018 : 4:18 PM
 */
public class SubscriptionDetailsDTO implements Serializable {

        private Long id;
        private String city;
        private String ats;
        private String street;
        private String building;
        private String apartment;
        private String entrance;
        private String floor;
        private Language lang;
        private String desc;
        private String name;
        private String surname;
        private String password;
        private String comments;

        public Long getId() {
            return id;
        }

        public void setId(Long id) {
            this.id = id;
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

        public String getEntrance() {
            return entrance;
        }

        public void setEntrance(String entrance) {
            this.entrance = entrance;
        }

        public String getFloor() {
            return floor;
        }

        public void setFloor(String floor) {
            this.floor = floor;
        }

        public Language getLang() {
            return lang;
        }

        public void setLang(Language lang) {
            this.lang = lang;
        }

        public String getDesc() {
            return desc;
        }

        public void setDesc(String desc) {
            this.desc = desc;
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

        public String getPassword() {
            return password;
        }

        public void setPassword(String password) {
            this.password = password;
        }

        public String getComments() {
            return comments;
        }

        public void setComments(String comments) {
            this.comments = comments;
        }


    @Override
    public String toString() {
        return "SubscriptionDetailsDTO{" +
                "id=" + id +
                ", city='" + city + '\'' +
                ", ats='" + ats + '\'' +
                ", street='" + street + '\'' +
                ", building='" + building + '\'' +
                ", apartment='" + apartment + '\'' +
                ", entrance='" + entrance + '\'' +
                ", floor='" + floor + '\'' +
                ", lang=" + lang +
                ", desc='" + desc + '\'' +
                ", name='" + name + '\'' +
                ", surname='" + surname + '\'' +
                ", password='" + password + '\'' +
                ", comments='" + comments + '\'' +
                '}';
    }
}
