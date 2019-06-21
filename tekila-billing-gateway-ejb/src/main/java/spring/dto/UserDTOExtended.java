package spring.dto;

import com.jaravir.tekila.base.auth.persistence.User;

import java.io.Serializable;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * @author ElmarMa on 5/22/2018
 */
public class UserDTOExtended implements Serializable{

    private String login;
    private String email;
    private String firstName;
    private String middleName;
    private String lastName;
    private int status;
    private String roleName; // in our case this is Group(composition of multiple roles) name.
    private Set<String> providersName;

    public UserDTOExtended() {
    }

    public UserDTOExtended(User user) {
        this.email = user.getEmail();
        this.firstName = user.getFirstName();
        this.status=user.getStatus().ordinal();
        this.login=user.getUserName();
        this.middleName = user.getMiddleName();
        this.lastName = user.getSurname();
        this.roleName = user.getGroup() != null ? user.getGroup().getGroupName() : null;
        this.providersName = user.getGroup() != null
                ? user.getGroup().getRoles() != null
                ? user.getGroup().getRoles()
                      .stream()
                      .filter(role -> role.getProvider() != null)
                      .map(role -> role.getProvider().getName())
                      .collect(Collectors.toSet())
                 : null : null;
    }

    public String getLogin() {
        return login;
    }

    public void setLogin(String login) {
        this.login = login;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
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

    public int getStatus() {
        return status;
    }

    public void setStatus(int status) {
        this.status = status;
    }

    public String getRoleName() {
        return roleName;
    }

    public void setRoleName(String roleName) {
        this.roleName = roleName;
    }

    public Set<String> getProvidersName() {
        return providersName;
    }

    public void setProvidersName(Set<String> providersName) {
        this.providersName = providersName;
    }

    @Override
    public String toString() {
        return "UserDTOExtended{" +
                "login='" + login + '\'' +
                ", email='" + email + '\'' +
                ", firstName='" + firstName + '\'' +
                ", middleName='" + middleName + '\'' +
                ", lastName='" + lastName + '\'' +
                ", status=" + status +
                '}';
    }
}
