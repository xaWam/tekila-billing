package spring.dto;

import com.jaravir.tekila.module.event.BillingEvent;

import java.io.Serializable;

/**
 * @author ElmarMa on 3/27/2018
 */
public class NotificationSettingDTO implements Serializable {
    private Long id;
    private BillingEvent billingEvent;
    private boolean sms;
    private boolean email;
    private boolean screen;

    public NotificationSettingDTO(BillingEvent billingEvent, boolean sms, boolean email, boolean screen) {
        this.billingEvent = billingEvent;
        this.sms = sms;
        this.email = email;
        this.screen = screen;
    }

    public NotificationSettingDTO() {
        this.billingEvent = billingEvent;
    }

    public NotificationSettingDTO(BillingEvent billingEvent) {
        this.billingEvent = billingEvent;
    }

    public BillingEvent getBillingEvent() {
        return billingEvent;
    }

    public void setBillingEvent(BillingEvent billingEvent) {
        this.billingEvent = billingEvent;
    }

    public boolean isSms() {
        return sms;
    }

    public void setSms(boolean sms) {
        this.sms = sms;
    }

    public boolean isEmail() {
        return email;
    }

    public void setEmail(boolean email) {
        this.email = email;
    }

    public boolean isScreen() {
        return screen;
    }

    public void setScreen(boolean screen) {
        this.screen = screen;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    @Override
    public String toString() {
        return "NotificationSettingDTO{" +
                "id=" + id +
                ", billingEvent=" + billingEvent +
                ", sms=" + sms +
                ", email=" + email +
                ", screen=" + screen +
                '}';
    }
}
