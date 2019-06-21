package spring.security.wrapper;

import javax.validation.constraints.Size;
import java.io.Serializable;
import java.time.Instant;
import java.util.List;

/**
 * A DTO for the SysUser entity. Use case is where User need to transfer via token.
 */
public class Subject implements Serializable {

    private Long id;

    private Integer status;

    private String firstName;

    private String middleName;

    private String lastName;

    private String login;

    private String kacapetxana;

    private String email;

    @Size(min = 2, max = 6)
    private String langKey;

    private String phone;

    private Boolean notifyMe;

    private String activationKey;

    private Instant activatedOn;

    private List<SubjectProviderDTO> providers;

    private List<SubjectApplicationDTO> applications;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
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

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public String getLogin() {
        return login;
    }

    public void setLogin(String login) {
        this.login = login;
    }

    public String getKacapetxana() {
        return kacapetxana;
    }

    public void setKacapetxana(String kacapetxana) {
        this.kacapetxana = kacapetxana;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getLangKey() {
        return langKey;
    }

    public void setLangKey(String langKey) {
        this.langKey = langKey;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public Boolean getNotifyMe() {
        return notifyMe;
    }

    public void setNotifyMe(Boolean notifyMe) {
        this.notifyMe = notifyMe;
    }

    public String getActivationKey() {
        return activationKey;
    }

    public void setActivationKey(String activationKey) {
        this.activationKey = activationKey;
    }

    public Instant getActivatedOn() {
        return activatedOn;
    }

    public void setActivatedOn(Instant activatedOn) {
        this.activatedOn = activatedOn;
    }

    public List<SubjectProviderDTO> getProviders() {
        return providers;
    }

    public void setProviders(List<SubjectProviderDTO> providers) {
        this.providers = providers;
    }

    public List<SubjectApplicationDTO> getApplications() {
        return applications;
    }

    public void setApplications(List<SubjectApplicationDTO> applications) {
        this.applications = applications;
    }

    @Override
    public String toString() {
        return "Subject{" +
            "id=" + id +
            ", status=" + status +
            ", firstName='" + firstName + '\'' +
            ", middleName='" + middleName + '\'' +
            ", lastName='" + lastName + '\'' +
            ", login='" + login + '\'' +
            ", email='" + email + '\'' +
            ", langKey='" + langKey + '\'' +
            ", phone='" + phone + '\'' +
            ", notifyMe=" + notifyMe +
            ", activationKey='" + activationKey + '\'' +
            ", activatedOn=" + activatedOn +
            ", providers=" + providers +
            ", applications=" + applications +
            '}';
    }
}
