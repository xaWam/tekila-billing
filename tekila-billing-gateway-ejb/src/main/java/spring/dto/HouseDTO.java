package spring.dto;


import java.io.Serializable;
/**
 * @author GurbanAz
 */
public class HouseDTO implements Serializable {

    private String houseNo;

    public String getHouseNo() {
        return houseNo;
    }

    public void setHouseNo(String houseNo) {
        this.houseNo = houseNo;
    }
}
