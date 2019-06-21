package spring.dto;

import com.jaravir.tekila.module.service.ResourceBucketType;

import java.io.Serializable;

public class SubscriptionResourceBucketDTO extends BaseDTO implements Serializable {

    private String capacity;
    private ResourceBucketType type;
    private String dsc;
    private String unit;

    public String getCapacity() {
        return capacity;
    }

    public void setCapacity(String capacity) {
        this.capacity = capacity;
    }

    public ResourceBucketType getType() {
        return type;
    }

    public void setType(ResourceBucketType type) {
        this.type = type;
    }

    public String getDsc() {
        return dsc;
    }

    public void setDsc(String dsc) {
        this.dsc = dsc;
    }

    public String getUnit() {
        return unit;
    }

    public void setUnit(String unit) {
        this.unit = unit;
    }
}
