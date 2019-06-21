package com.jaravir.tekila.module.web.soap;

import com.jaravir.tekila.module.campaign.Campaign;
import com.jaravir.tekila.module.campaign.CampaignStatus;
import com.jaravir.tekila.module.campaign.CampaignTarget;
import com.jaravir.tekila.module.service.entity.Service;
import com.jaravir.tekila.module.service.entity.ServiceProvider;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import javax.xml.bind.annotation.XmlElement;

/**
 *
 * @author khsadigov
 */
public class CampaignResponse {

    @XmlElement(nillable = true)
    public List<ServiceResponse> serviceList;
    @XmlElement(nillable = true)
    public CampaignTarget target;
    @XmlElement(nillable = true)
    public int count;
    @XmlElement(nillable = true)
    public Double bonusCount;
    @XmlElement(nillable = true)
    public int lifeCycleCount;
    @XmlElement(nillable = true)
    public ServiceProvider provider;
    @XmlElement(nillable = true)
    public CampaignStatus status;
    @XmlElement(nillable = true)
    public String name;
    @XmlElement(nillable = true)
    public Date expirationDate;
    @XmlElement(nillable = true)
    public boolean isCompound;
    @XmlElement(nillable = true)
    public boolean isAutomatic;
    @XmlElement(nillable = true)
    public Map<ServiceResponse, Long> bonusLimits;
    @XmlElement(nillable = true)
    public boolean isCancelInvoice;
    @XmlElement(nillable = true)
    public boolean isActivateOnDemand;
    @XmlElement(nillable = true)
    public Double equipmentDiscount;
    @XmlElement(nillable = true)
    public String desc;

    public CampaignResponse() {
    }

    public CampaignResponse(Campaign entity) {

        this.serviceList = new ArrayList<>();
        for (Service item : entity.getServiceList()) {
            this.serviceList.add(new ServiceResponse(item));
        }

        this.count = entity.getCount();
        this.bonusCount = entity.getBonusCount();
        this.lifeCycleCount = entity.getLifeCycleCount();
        this.provider = entity.getProvider();
        this.status = entity.getStatus();
        this.name = entity.getName();
        this.expirationDate = entity.getExpirationDate() != null ? entity.getExpirationDate().toDate() : null;
        this.isCompound = entity.isCompound();
        this.isAutomatic = entity.isAutomatic();

        Iterator<Map.Entry<Service, Long>> entries = entity.getBonusLimits().entrySet().iterator();
        this.bonusLimits = new HashMap<>();
        while (entries.hasNext()) {
            Map.Entry<Service, Long> entry = entries.next();
            this.bonusLimits.put(new ServiceResponse(entry.getKey()), entry.getValue());
        }

        this.isCancelInvoice = entity.isCancelInvoice();
        this.isActivateOnDemand = entity.isActivateOnDemand();
        this.equipmentDiscount = entity.getEquipmentDiscount();
        this.desc = entity.getDesc();

    }
}
