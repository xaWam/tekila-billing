package com.jaravir.tekila.module.web.soap;

import com.jaravir.tekila.module.subscription.persistence.entity.Balance;
import java.io.Serializable;
import javax.xml.bind.annotation.XmlElement;

/**
 *
 * @author khsadigov
 */
public class BalanceResponse extends BaseResponse implements Serializable {

    @XmlElement(nillable = true)
    public double realBalance;
    @XmlElement(nillable = true)
    public double promoBalance;
    @XmlElement(nillable = true)
    public double virtualBalance;

    public BalanceResponse(Balance entity) {
        this.realBalance = 1.0 * entity.getRealBalance() / 100000;
        this.promoBalance = 1.0 * entity.getPromoBalance() / 100000;
        this.virtualBalance = 1.0 * entity.getVirtualBalance() / 100000;
    }

}
