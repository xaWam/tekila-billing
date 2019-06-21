package spring.controller.vm;

/**
 * @author MusaAl
 * @date 4/3/2018 : 3:09 PM
 */
public class SubscriberExistVM {

    private String passportSeries;
    private String passportNumber;
    private String pinCode;

    public String getPassportSeries() {
        return passportSeries;
    }

    public void setPassportSeries(String passportSeries) {
        this.passportSeries = passportSeries;
    }

    public String getPassportNumber() {
        return passportNumber;
    }

    public void setPassportNumber(String passportNumber) {
        this.passportNumber = passportNumber;
    }

    public String getPinCode() {
        return pinCode;
    }

    public void setPinCode(String pinCode) {
        this.pinCode = pinCode;
    }

    @Override
    public String toString() {
        return "SubscriberExistVM{" +
                "passportSeries='" + passportSeries + '\'' +
                ", passportNumber='" + passportNumber + '\'' +
                ", pinCode='" + pinCode + '\'' +
                '}';
    }
}
