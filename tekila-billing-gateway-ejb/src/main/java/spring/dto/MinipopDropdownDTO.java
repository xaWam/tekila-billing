package spring.dto;

import java.io.Serializable;

/**
 * @author ElmarMa on 3/30/2018
 */
public class MinipopDropdownDTO implements Serializable {
    private Long id;
    private String switch_id;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getSwitch_id() {
        return switch_id;
    }

    public void setSwitch_id(String switch_id) {
        this.switch_id = switch_id;
    }

    @Override
    public String toString() {
        return "MinipopDropdownDTO{" +
                "id=" + id +
                ", switch_id='" + switch_id + '\'' +
                '}';
    }
}
