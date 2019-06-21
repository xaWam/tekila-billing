package spring.dto;

import com.jaravir.tekila.module.service.entity.ResourceBucket;
import org.joda.time.DateTime;
import org.joda.time.LocalTime;

import java.util.List;

/**
 * @author MusaAl
 * @date 2/1/2019 : 9:57 AM
 */
public class ResourceDTO {

    private static final long serialVersionUID = 8849529587751959671L;
    private String name;
//    private List<ResourceBucket> bucketList;
    private DateTime expirationDate;
    private LocalTime activeFrom;
    private LocalTime activeTill;
    private List<Integer> activeDaysOfWeekList;
    private boolean isActiveOnSpecialDays;
    private String dsc;

    public static long getSerialVersionUID() {
        return serialVersionUID;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public DateTime getExpirationDate() {
        return expirationDate;
    }

    public void setExpirationDate(DateTime expirationDate) {
        this.expirationDate = expirationDate;
    }

    public LocalTime getActiveFrom() {
        return activeFrom;
    }

    public void setActiveFrom(LocalTime activeFrom) {
        this.activeFrom = activeFrom;
    }

    public LocalTime getActiveTill() {
        return activeTill;
    }

    public void setActiveTill(LocalTime activeTill) {
        this.activeTill = activeTill;
    }

    public List<Integer> getActiveDaysOfWeekList() {
        return activeDaysOfWeekList;
    }

    public void setActiveDaysOfWeekList(List<Integer> activeDaysOfWeekList) {
        this.activeDaysOfWeekList = activeDaysOfWeekList;
    }

    public boolean isActiveOnSpecialDays() {
        return isActiveOnSpecialDays;
    }

    public void setActiveOnSpecialDays(boolean activeOnSpecialDays) {
        isActiveOnSpecialDays = activeOnSpecialDays;
    }

    public String getDsc() {
        return dsc;
    }

    public void setDsc(String dsc) {
        this.dsc = dsc;
    }
}
