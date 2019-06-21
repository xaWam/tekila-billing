package reseller;

import com.jaravir.tekila.module.stats.persistence.entity.OnlineBroadbandStats;
import com.jaravir.tekila.module.subscription.persistence.entity.Subscription;
import org.apache.log4j.Logger;
import org.joda.time.DateTime;

import java.util.Date;

/**
 * Created by ShakirG on 15/03/2018.
 */
public class OnlineSessionObject {

    private final static Logger log = Logger.getLogger(OnlineSessionObject.class);
    public String name;
    public String surname;
    public String user_id;
    public Date date_time;
    public String MB_in;
    public String MB_out;
    public String ip_address;
    public String port;
    public String vendor;

    public OnlineSessionObject(){}


    public OnlineSessionObject(Subscription subscription, OnlineBroadbandStats onlineBroadbandStats){
log.debug(onlineBroadbandStats);
this.name = subscription.getDetails().getName();
this.surname = subscription.getDetails().getSurname();
        this.user_id = subscription.getAgreement();
        if (onlineBroadbandStats.getStartTime() != null){
            this.date_time = onlineBroadbandStats.getStartTime();
        }
        this.MB_in = onlineBroadbandStats.getUp();
        this.MB_out = onlineBroadbandStats.getDown();
        this.ip_address = onlineBroadbandStats.getFramedAddress();
        this.port = onlineBroadbandStats.getCallingStationID();
        this.vendor = "";

    }




    public String getUser_id() {
        return user_id;
    }

    public void setUser_id(String user_id) {
        this.user_id = user_id;
    }

    public Date getDate_time() {
        return date_time;
    }

    public void setDate_time(Date date_time) {
        this.date_time = date_time;
    }

    public String getMB_in() {
        return MB_in;
    }

    public void setMB_in(String MB_in) {
        this.MB_in = MB_in;
    }

    public String getMB_out() {
        return MB_out;
    }

    public void setMB_out(String MB_out) {
        this.MB_out = MB_out;
    }

    public String getIp_address() {
        return ip_address;
    }

    public void setIp_address(String ip_address) {
        this.ip_address = ip_address;
    }

    public String getPort() {
        return port;
    }

    public void setPort(String port) {
        this.port = port;
    }

    public String getVendor() {
        return vendor;
    }

    public void setVendor(String vendor) {
        this.vendor = vendor;
    }

}







