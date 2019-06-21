package spring.dto;

import java.io.Serializable;

/**
 * @author MusaAl
 * @date 1/31/2019 : 6:22 PM
 */
public class ServiceProviderDTO extends BaseDTO implements Serializable {

    private String name;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
