package reseller.vm;

import java.io.Serializable;

/**
 * Created by ShakirG on 07/09/2018.
 */


public  class SubscriptionNewService implements Serializable {
    public String agreement;
    public long serviceID;
    public SubscriptionNewService() {

    }
    public SubscriptionNewService(String agreement, long serviceID) {
        this.agreement = agreement;
        this.serviceID = serviceID;
    }

    @Override
    public String toString() {
        return "SubscriptionNewService{" +
                "agreement='" + agreement + '\'' +
                ", serviceID=" + serviceID +
                '}';
    }
}
