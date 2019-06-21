package spring.dto;

import com.jaravir.tekila.module.subscription.persistence.entity.Ats;
import com.jaravir.tekila.module.subscription.persistence.entity.AtsStatus;
import dto.jpa.Attachable;

import java.io.Serializable;

/**
 * @author MusaAl
 * @date 3/12/2018 : 3:28 PM
 */
public class AtsDTO implements Serializable, Attachable<Ats> {

    private long id;
    private String name;
    private String atsIndex;
    private String coor;
    private AtsStatus status;

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getAtsIndex() {
        return atsIndex;
    }

    public void setAtsIndex(String atsIndex) {
        this.atsIndex = atsIndex;
    }

    public String getCoor() {
        return coor;
    }

    public void setCoor(String coor) {
        this.coor = coor;
    }

    public AtsStatus getStatus() {
        return status;
    }

    public void setStatus(AtsStatus status) {
        this.status = status;
    }

    @Override
    public String toString() {
        return "AtsDTO{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", atsIndex='" + atsIndex + '\'' +
                ", coordinate='" + coor + '\'' +
                ", status=" + status +
                '}';
    }
}
