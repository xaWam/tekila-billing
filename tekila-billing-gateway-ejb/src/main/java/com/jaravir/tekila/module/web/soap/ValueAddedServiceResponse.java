package com.jaravir.tekila.module.web.soap;

import com.jaravir.tekila.module.service.VASSetting;
import com.jaravir.tekila.module.service.ValueAddedServiceType;
import com.jaravir.tekila.module.service.entity.ValueAddedService;
import java.util.Date;
import java.util.List;
import javax.xml.bind.annotation.XmlElement;

/**
 *
 * @author khsadigov
 */
public class ValueAddedServiceResponse extends BaseResponse {

    @XmlElement(nillable = true)
    public long id;
    @XmlElement(nillable = true)
    public String name;
    @XmlElement(nillable = true)
    public String provider;
    @XmlElement(nillable = true)
    public long price;
    @XmlElement(nillable = true)
    public Date creationDate;
    @XmlElement(nillable = true)
    public List<VASSetting> settings;
    @XmlElement(nillable = true)
    public String expression;
    @XmlElement(nillable = true)
    public String chargeableItem;
    @XmlElement(nillable = true)
    public boolean provisioned;
    @XmlElement(nillable = true)
    public ValueAddedServiceType serviceType;

    public ValueAddedServiceResponse() {
    }

    public ValueAddedServiceResponse(ValueAddedService entity) {
        this.id = entity.getId();
        if (entity.getAlternateName() != null && !entity.getAlternateName().isEmpty()) {
            this.name = entity.getAlternateName();
        } else {
            this.name = entity.getName();
        }
        this.provider = entity.getProvider().getName();
        this.price = entity.getPrice();
        this.creationDate = entity.getCreationDate();
        this.settings = entity.getSettings();
        this.expression = entity.getExpression();
        this.chargeableItem = entity.getChargeableItem();
        this.provisioned = entity.isProvisioned();
        this.serviceType = entity.getCode().getType();
    }
}
