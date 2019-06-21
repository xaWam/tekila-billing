package spring.dto;

import org.joda.time.DateTime;

/**
 * @author MusaAl
 * @date 4/2/2018 : 10:37 AM
 */
public class BaseDTO {

    protected long id;
    protected DateTime lastUpdateDate;

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public DateTime getLastUpdateDate() {
        return lastUpdateDate;
    }

    public void setLastUpdateDate(DateTime lastUpdateDate) {
        this.lastUpdateDate = lastUpdateDate;
    }
}
