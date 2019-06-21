package spring.dto;

import com.jaravir.tekila.module.periodic.JobCategory;
import com.jaravir.tekila.module.periodic.JobStatus;
import org.joda.time.DateTime;

/**
 * @author MusaAl
 * @date 1/17/2019 : 11:21 PM
 */
public class SubscriptionJobsDTO {

    private long id;
    private DateTime startTime;
    private DateTime deadline;
    private JobCategory category;
    private UserDTOSecure user;
    private JobStatus status;
    protected DateTime lastUpdateDate;
    private boolean counter;

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public DateTime getStartTime() {
        return startTime;
    }

    public void setStartTime(DateTime startTime) {
        this.startTime = startTime;
    }

    public DateTime getDeadline() {
        return deadline;
    }

    public void setDeadline(DateTime deadline) {
        this.deadline = deadline;
    }

    public JobCategory getCategory() {
        return category;
    }

    public void setCategory(JobCategory category) {
        this.category = category;
    }

    public UserDTOSecure getUser() {
        return user;
    }

    public void setUser(UserDTOSecure user) {
        this.user = user;
    }

    public JobStatus getStatus() {
        return status;
    }

    public void setStatus(JobStatus status) {
        this.status = status;
    }

    public boolean isCounter() {
        return counter;
    }

    public void setCounter(boolean counter) {
        this.counter = counter;
    }

    public DateTime getLastUpdateDate() {
        return lastUpdateDate;
    }

    public void setLastUpdateDate(DateTime lastUpdateDate) {
        this.lastUpdateDate = lastUpdateDate;
    }
}
