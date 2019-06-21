package spring.model.helper;

public class DoubleCharge {

    public String chargeid;
    public String agreement;
    public String status;

    public String getChargeid() {
        return chargeid;
    }

    public void setChargeid(String chargeid) {
        this.chargeid = chargeid;
    }

    public String getAgreement() {
        return agreement;
    }

    public void setAgreement(String agreement) {
        this.agreement = agreement;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }


    @Override
    public String toString() {
        return "DoubleCharge{" +
                "chargeid=" + chargeid +
                ", agreement='" + agreement + '\'' +
                ", status='" + status + '\'' +
                '}';
    }
}
