package spring.model.helper;

import javax.validation.constraints.NotNull;

/**
 * @author MushfigM on 03.04.2019
 */
public class MailDetails {
    @NotNull
    private String users;
    @NotNull
    private String subject;
    @NotNull
    private String body;


    public String getUsers() {
        return users;
    }

    public void setUsers(String users) {
        this.users = users;
    }

    public String getSubject() {
        return subject;
    }

    public void setSubject(String subject) {
        this.subject = subject;
    }

    public String getBody() {
        return body;
    }

    public void setBody(String body) {
        this.body = body;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("MailDetails{");
        sb.append("users='").append(users).append('\'');
        sb.append(", subject='").append(subject).append('\'');
        sb.append(", body='").append(body).append('\'');
        sb.append('}');
        return sb.toString();
    }
}
