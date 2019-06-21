package com.jaravir.tekila.module.web.soap;

import com.jaravir.tekila.module.event.BillingEvent;
import com.jaravir.tekila.module.event.notification.channell.NotificationChannell;

import javax.xml.bind.annotation.XmlElement;
import java.io.Serializable;
import java.util.List;

/**
 * Created by kmaharov on 12.08.2016.
 */
public class BillingEventResponse extends BaseResponse implements Serializable {
    @XmlElement(nillable = true)
    public BillingEvent event;
    @XmlElement(nillable = true)
    public List<NotificationChannelResponse> channellList;

    public BillingEventResponse(BillingEvent event, List<NotificationChannelResponse> channellList) {
        this.event = event;
        this.channellList = channellList;
    }
}
