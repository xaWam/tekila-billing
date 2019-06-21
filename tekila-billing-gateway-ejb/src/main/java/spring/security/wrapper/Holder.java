package spring.security.wrapper;


import org.springframework.security.core.userdetails.User;

/**
 * @author MusaAl
 * @date 1/12/2018 : 5:34 PM
 */
public class Holder {

    private User springUser;

    private Subject user;

    public User getSpringUser() {
        return springUser;
    }

    public void setSpringUser(User springUser) {
        this.springUser = springUser;
    }

    public Subject getUser() {
        return user;
    }

    public void setUser(Subject user) {
        this.user = user;
    }
}
