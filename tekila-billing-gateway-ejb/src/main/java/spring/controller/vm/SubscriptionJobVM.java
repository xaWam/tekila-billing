package spring.controller.vm;

import org.joda.time.DateTime;

/**
 * @author MusaAl
 * @date 1/24/2019 : 10:01 AM
 *
 * Use case -> when updating Subscription jobs runTime.
 */
public class SubscriptionJobVM {

    private Long id;

    private String runTime;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getRunTime() {
        return runTime;
    }

    public void setRunTime(String runTime) {
        this.runTime = runTime;
    }
}
