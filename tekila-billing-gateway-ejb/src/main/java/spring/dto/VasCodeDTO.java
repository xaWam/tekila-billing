package spring.dto;

import com.jaravir.tekila.module.service.ValueAddedServiceType;
import com.jaravir.tekila.module.service.entity.ServiceProvider;
import com.jaravir.tekila.module.service.entity.VASCodeSequence;

import java.io.Serializable;

public class VasCodeDTO extends BaseDTO implements Serializable {

    private VasCodeSequenceDTO generator;
    private ValueAddedServiceType type;
    private String name;
    private String dsc;


    public VasCodeSequenceDTO getGenerator() {
        return generator;
    }

    public void setGenerator(VasCodeSequenceDTO generator) {
        this.generator = generator;
    }

    public ValueAddedServiceType getType() {
        return type;
    }

    public void setType(ValueAddedServiceType type) {
        this.type = type;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDsc() {
        return dsc;
    }

    public void setDsc(String dsc) {
        this.dsc = dsc;
    }
}
