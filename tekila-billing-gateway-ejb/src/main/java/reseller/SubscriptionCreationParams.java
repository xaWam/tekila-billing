package reseller;

public class SubscriptionCreationParams {
    public String username;
    public String password;
    public Integer paymentId;
    public Integer regionId;
    public Integer serviceId;
    public String fullName;
    public String address;
    public String agreement;
    public String phoneMobile;
    public Integer zoneId;
    public Integer serviceTypeId;
    public Boolean modemCampaign;

    @Override
    public String toString() {
        return "SubscriptionCreationParams{" +
                "username='" + username + '\'' +
                ", password='" + password + '\'' +
                ", paymentId=" + paymentId +
                ", regionId=" + regionId +
                ", serviceId=" + serviceId +
                ", fullName='" + fullName + '\'' +
                ", address='" + address + '\'' +
                ", agreement='" + agreement + '\'' +
                ", phoneMobile='" + phoneMobile + '\'' +
                ", zoneId=" + zoneId +
                ", serviceTypeId=" + serviceTypeId +
                ", modemCampaign=" + modemCampaign +
                '}';
    }
}
