package spring.controller.vm;


/**
 * @author GurbanAz
 * @date 05.03.2018 / 10:29 AM
 */
public class SubscriptionMinipopVM {

    private Long subscriptionId;
    private Long minipopId;
    private Integer portId;


    public Long getSubscriptionId() {
        return subscriptionId;
    }

    public void setSubscriptionId(Long subscriptionId) {
        this.subscriptionId = subscriptionId;
    }

    public Long getMinipopId() {
        return minipopId;
    }

    public void setMinipopId(Long minipopId) {
        this.minipopId = minipopId;
    }

    public Integer getPortId() {
        return portId;
    }

    public void setPortId(Integer portId) {
        this.portId = portId;
    }


    @Override
    public String toString() {
        return "SubscriptionMinipopVM{" +
                "subscriptionId=" + subscriptionId +
                ", minipopId=" + minipopId +
                ", portId=" + portId +
                '}';
    }
}
