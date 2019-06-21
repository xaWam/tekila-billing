package spring.dto;

public class SubscriptionEquipmentUpdateDTO {

    private String agreementId;
    private Long equipmentId;


    public String getAgreementId() {
        return agreementId;
    }

    public void setAgreementId(String agreementId) {
        this.agreementId = agreementId;
    }

    public Long getEquipmentId() {
        return equipmentId;
    }

    public void setEquipmentId(Long equipmentId) {
        this.equipmentId = equipmentId;
    }

    @Override
    public String toString() {
        return "SubscriptionEquipmentUpdateDTO{" +
                "agreementId='" + agreementId + '\'' +
                ", equipmentId='" + equipmentId + '\'' +
                '}';
    }
}
