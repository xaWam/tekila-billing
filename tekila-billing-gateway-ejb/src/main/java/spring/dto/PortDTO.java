package spring.dto;

import java.io.Serializable;

/**
 * @author ElmarMa on 3/30/2018
 */
public class PortDTO implements Serializable {
    private long portId;
    private String title;
    private boolean reserved;

    public PortDTO(int portId, String title, boolean reserved) {
        this.portId = portId;
        this.title = title;
        this.reserved = reserved;
    }

    public Long getPortId() {
        return portId;
    }

    public void setPortId(Long portId) {
        this.portId = portId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public boolean isReserved() {
        return reserved;
    }

    public void setReserved(boolean reserved) {
        this.reserved = reserved;
    }

    @Override
    public String toString() {
        return "PortDTO{" +
                "portId=" + portId +
                ", title='" + title + '\'' +
                ", reserved=" + reserved +
                '}';
    }
}
