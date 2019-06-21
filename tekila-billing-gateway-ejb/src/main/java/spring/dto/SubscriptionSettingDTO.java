package spring.dto;

import com.jaravir.tekila.module.service.entity.ServiceSetting;

import java.io.Serializable;

/**
 * @author ElmarMa on 3/29/2018
 */
public class SubscriptionSettingDTO implements Serializable {

    private ServiceSettingDTO properties;
    private String value;
    private String dsc;
    private String name;


    public static class ServiceSettingDTO implements Serializable {

        private String title;
        private String dsc;

        public String getTitle() {
            return title;
        }

        public void setTitle(String title) {
            this.title = title;
        }

        public String getDsc() {
            return dsc;
        }

        public void setDsc(String dsc) {
            this.dsc = dsc;
        }

        @Override
        public String toString() {
            return "ServiceSettingDTO{" +
                    "title='" + title + '\'' +
                    ", dsc='" + dsc + '\'' +
                    '}';
        }
    }

    public ServiceSettingDTO getProperties() {
        return properties;
    }

    public void setProperties(ServiceSettingDTO properties) {
        this.properties = properties;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public String getDsc() {
        return dsc;
    }

    public void setDsc(String dsc) {
        this.dsc = dsc;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @Override
    public String toString() {
        return "SubscriptionSettingDTO{" +
                "properties=" + properties +
                ", value='" + value + '\'' +
                ", dsc='" + dsc + '\'' +
                ", name='" + name + '\'' +
                '}';
    }
}
