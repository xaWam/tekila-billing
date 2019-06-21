package spring.dto;

import com.jaravir.tekila.module.store.ip.IpAddressStatus;

/**
 * @author MusaAl
 * @date 2/11/2019 : 5:26 PM
 */
public class IpAddressDTO {

    private int[] address;
    private IpAddressStatus status;
    private String addressAsString;

    public int[] getAddress() {
        return address;
    }

    public void setAddress(int[] address) {
        this.address = address;
    }

    public IpAddressStatus getStatus() {
        return status;
    }

    public void setStatus(IpAddressStatus status) {
        this.status = status;
    }

    public String getAddressAsString() {
        return addressAsString;
    }

    public void setAddressAsString(String addressAsString) {
        this.addressAsString = addressAsString;
    }
}
