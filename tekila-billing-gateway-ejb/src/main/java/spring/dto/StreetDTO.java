package spring.dto;


import java.io.Serializable;

/**
 * @author MusaAl
 * @date 3/12/2018 : 3:28 PM
 */
public class StreetDTO extends BaseDTO implements Serializable {

    private String  atsIndex;
    private long streetIndex;
    private String name;

    public String getAtsIndex() {
        return atsIndex;
    }

    public void setAtsIndex(String atsIndex) {
        this.atsIndex = atsIndex;
    }

    public long getStreetIndex() {
        return streetIndex;
    }

    public void setStreetIndex(long streetIndex) {
        this.streetIndex = streetIndex;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
