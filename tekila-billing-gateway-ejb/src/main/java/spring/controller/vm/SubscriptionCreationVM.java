package spring.controller.vm;

import com.jaravir.tekila.module.service.NotificationSettingRow;
import com.jaravir.tekila.module.service.model.BillingPrinciple;
import com.jaravir.tekila.module.store.equip.Equipment;
import com.jaravir.tekila.module.subscription.persistence.entity.Subscriber;
import com.jaravir.tekila.provision.broadband.devices.MiniPop;
import spring.dto.SubscriberDTO;
import spring.dto.SubscriptionCreationDTO;

import java.util.List;

/**
 * @author MusaAl
 * @date 4/6/2018 : 11:50 AM
 */
public class SubscriptionCreationVM {

    private SubscriberDTO selectedSubscriber;

    private SubscriptionCreationDTO subscriptionDto;


    private Long equipmentID;

//    private List<NotificationSettingRow> notificationSettings;

    private String zoneId;

    private Long serviceId;

    private String serviceTypeId;

    private SubscriptionVASSettingsVM subscriptionVASSettingsVM;


    private String selectedReseller;

    private Long minipopId;

    private Integer portId;

    private Long atsId;

//    private Integer billingModel;
    private BillingPrinciple billingPrinciple;



    public SubscriberDTO getSelectedSubscriber() {
        return selectedSubscriber;
    }

    public void setSelectedSubscriber(SubscriberDTO selectedSubscriber) { this.selectedSubscriber = selectedSubscriber; }

    public SubscriptionCreationDTO getSubscriptionDto() { return subscriptionDto; }

    public void setSubscriptionDto(SubscriptionCreationDTO subscriptionDto) {
        this.subscriptionDto = subscriptionDto;
    }

    public String getZoneId() {
        return zoneId;
    }

    public void setZoneId(String zoneId) {
        this.zoneId = zoneId;
    }

    public Long getServiceId() {
        return serviceId;
    }

    public void setServiceId(Long serviceId) {
        this.serviceId = serviceId;
    }

    public String getServiceTypeId() {
        return serviceTypeId;
    }

    public void setServiceTypeId(String serviceTypeId) {
        this.serviceTypeId = serviceTypeId;
    }

    public String getSelectedReseller() {
        return selectedReseller;
    }

    public void setSelectedReseller(String selectedReseller) {
        this.selectedReseller = selectedReseller;
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

    public Long getEquipmentID() {
        return equipmentID;
    }

    public void setEquipmentID(Long equipmentID) {
        this.equipmentID = equipmentID;
    }

    public BillingPrinciple getBillingPrinciple() { return billingPrinciple; }

    public void setBillingPrinciple(BillingPrinciple billingPrinciple) { this.billingPrinciple = billingPrinciple; }

    public Long getAtsId() {
        return atsId;
    }

    public void setAtsId(Long atsId) {
        this.atsId = atsId;
    }

    public SubscriptionVASSettingsVM getSubscriptionVASSettingsVM() {
        return subscriptionVASSettingsVM;
    }

    public void setSubscriptionVASSettingsVM(SubscriptionVASSettingsVM subscriptionVASSettingsVM) {
        this.subscriptionVASSettingsVM = subscriptionVASSettingsVM;
    }

    @Override
    public String toString() {
        return "SubscriptionCreationVM{" +
                "selectedSubscriber=" + selectedSubscriber +
                ", subscriptionDto=" + subscriptionDto +
                ", equipmentID=" + equipmentID +
                ", zoneId='" + zoneId + '\'' +
                ", serviceId=" + serviceId +
                ", serviceTypeId='" + serviceTypeId + '\'' +
                ", subscriptionVASSettingsVM=" + subscriptionVASSettingsVM +
                ", selectedReseller='" + selectedReseller + '\'' +
                ", minipopId=" + minipopId +
                ", portId=" + portId +
                ", atsId=" + atsId +
                ", billingPrinciple=" + billingPrinciple +
                '}';
    }
}