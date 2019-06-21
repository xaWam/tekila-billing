package com.jaravir.tekila.module.web.soap;

import com.jaravir.tekila.module.event.notification.channell.NotificationChannell;

import javax.xml.bind.annotation.XmlElement;
import java.io.Serializable;

/**
 * Created by kmaharov on 12.08.2016.
 */
public class NotificationChannelResponse extends BaseResponse implements Serializable{
    @XmlElement(nillable = true)
    public NotificationChannell channell;
    @XmlElement(nillable = true)
    public boolean isEnabled;

    public NotificationChannelResponse(NotificationChannell channell, boolean isEnabled) {
        this.channell = channell;
        this.isEnabled = isEnabled;
    }
}
