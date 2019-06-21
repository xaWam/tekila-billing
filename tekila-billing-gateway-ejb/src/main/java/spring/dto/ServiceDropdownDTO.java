package spring.dto;

import java.io.Serializable;

/**
 * @author ElmarMa on 3/30/2018
 */
public class ServiceDropdownDTO implements Serializable {

    private Long id;
    private String name;
    private String alternateName;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getAlternateName() {
        return alternateName;
    }

    public void setAlternateName(String alternateName) {
        this.alternateName = alternateName;
    }

    @Override
    public String toString() {
        return "ServiceDropdownDTO{" +
                "name='" + name + '\'' +
                ", alternateName='" + alternateName + '\'' +
                '}';
    }
}
