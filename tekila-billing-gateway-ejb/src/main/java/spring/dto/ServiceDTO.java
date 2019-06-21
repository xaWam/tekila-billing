package spring.dto;

import com.jaravir.tekila.module.service.ServiceType;
import com.jaravir.tekila.module.service.entity.ServiceProvider;

import java.io.Serializable;

/**
 * @author MusaAl
 * @date 4/8/2018 : 4:20 PM
 */
public class ServiceDTO implements Serializable {

    private Long id;
    private String name;
    private long servicePrice;
    private ServiceType serviceType;
    private long installFee;
    private String dsc;
    private ServiceProvider provider;

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

    public long getServicePrice() {
        return servicePrice;
    }

    public void setServicePrice(long servicePrice) {
        this.servicePrice = servicePrice;
    }

    public ServiceType getServiceType() {
        return serviceType;
    }

    public void setServiceType(ServiceType serviceType) {
        this.serviceType = serviceType;
    }

    public long getInstallFee() {
        return installFee;
    }

    public void setInstallFee(long installFee) {
        this.installFee = installFee;
    }

    public String getDsc() {
        return dsc;
    }

    public void setDsc(String dsc) {
        this.dsc = dsc;
    }

    public ServiceProvider getProvider() {
        return provider;
    }

    public void setProvider(ServiceProvider provider) {
        this.provider = provider;
    }
}

