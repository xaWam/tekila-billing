package com.jaravir.tekila.module.web.soap;

import com.jaravir.tekila.module.service.*;
import com.jaravir.tekila.module.service.entity.Resource;
import com.jaravir.tekila.module.service.entity.Service;
import com.jaravir.tekila.module.service.entity.ValueAddedService;
import com.jaravir.tekila.module.service.model.BillingModel;

import java.io.Serializable;
import java.util.*;
import javax.xml.bind.annotation.XmlElement;

public class ServiceResponse extends BaseResponse implements Serializable, Comparable<ServiceResponse> {

    @XmlElement(nillable = true)
    public long id;
    @XmlElement(nillable = true)
    public String name;
    @XmlElement(nillable = true)
    public long price;
    @XmlElement(nillable = true)
    public ServiceType serviceType;
    @XmlElement(nillable = true)
    public boolean isBillByLifeCycle;
    @XmlElement(nillable = true)
    public long installFee;
    @XmlElement(nillable = true)
    public String dsc;
    @XmlElement(nillable = true)
    public String provider;
    @XmlElement(nillable = true)
    public boolean isAllowEquipment;
    @XmlElement(nillable = true)
    public boolean isAllowStock;
    @XmlElement(nillable = true)
    public BillingModel billingModel;
    @XmlElement(nillable = true)
    public List<Resource> resourceList;
    @XmlElement(nillable = true)
    public List<ValueAddedServiceResponse> defaultVasList;
    @XmlElement(nillable = true)
    public List<ValueAddedServiceResponse> vasList;
    @XmlElement(nillable = true)
    public List<ValueAddedServiceResponse> vasSettings;
    @XmlElement(nillable = true)
    public List<NotificationSetting> notificationSettings;
    @XmlElement(nillable = true)
    public List<ValueAddedServiceResponse> allowedVASList;

    public ServiceResponse(Service entity) {

        this.id = entity.getId();
        if (entity.getAlternateName() != null && !entity.getAlternateName().isEmpty()) {
            this.name = entity.getAlternateName();
        } else {
            this.name = entity.getName();
        }
        this.resourceList = entity.getResourceList();


        this.serviceType = entity.getServiceType();

        this.isBillByLifeCycle = entity.getIsBillByLifeCycle();
        this.installFee = entity.getInstallationFee();

        this.dsc = entity.getDsc();
        this.provider = entity.getProvider().getName();

        this.isAllowEquipment = entity.isAllowEquipment();
        this.isAllowStock = entity.isAllowStock();

        this.billingModel = entity.getBillingModel();

        this.vasList = new ArrayList<>();
        for (ValueAddedService vas : entity.getVasList()) {
            this.vasList.add(new ValueAddedServiceResponse(vas));
        }

        this.defaultVasList = new ArrayList<>();
        for (ValueAddedService vas : entity.getDefaultVasList()) {
            this.defaultVasList.add(new ValueAddedServiceResponse(vas));
        }

        this.vasSettings = new ArrayList<>();
        for (ValueAddedService vas : entity.getVasSettings()) {
            this.vasSettings.add(new ValueAddedServiceResponse(vas));
        }
        this.allowedVASList = new ArrayList<>();
        for (ValueAddedService vas : entity.getAllowedVASList()) {
            this.allowedVASList.add(new ValueAddedServiceResponse(vas));
        }

        this.notificationSettings = entity.getNotificationSettings();

    }


    @Override
    public int compareTo(ServiceResponse other) {
        if (this.price < other.price) {
            return -1;
        } else if (this.price > other.price) {
            return 1;
        }
        return 0;
    }
}
