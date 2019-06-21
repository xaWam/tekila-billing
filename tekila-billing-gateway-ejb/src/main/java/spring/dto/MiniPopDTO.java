package spring.dto;

import com.jaravir.tekila.common.device.DeviceStatus;
import com.jaravir.tekila.module.subscription.persistence.entity.House;
import com.jaravir.tekila.provision.broadband.devices.MinipopCategory;
import com.jaravir.tekila.provision.broadband.devices.Port;

import java.io.Serializable;
import java.util.List;

/**
 * @author GurbanAz
 */
public class MiniPopDTO extends BaseDTO implements Serializable {

    private String switch_id;
    private String mac;
    private String ip;
    private String serial;
    private String model;
    private String address;
    private int numberOfPorts;
    private List<Port> reservedPortList;
    private DeviceStatus deviceStatus;
    private Integer preferredPort;
    private Integer masterVlan;
    private Integer subVlan;
    private Integer midipopSlot;
    private Integer midipopPort;
    private String block;
    private List<House> houses;
    private MinipopCategory category;
    /**
     * TODO : Getter from the entity class throws NoFreePortLeftException,
     * TODO : that is why i did add not add below field to DTO class.
     * TODO : Exception should be handled only then MapStruct will be able to generate it.
     */
//  private Port nextAvailablePort;

    private Integer nextAvailablePortHintAsNumber;

    private String housesAsText;

    public String getSwitch_id() {
        return switch_id;
    }

    public void setSwitch_id(String switch_id) {
        this.switch_id = switch_id;
    }

    public String getMac() {
        return mac;
    }

    public void setMac(String mac) {
        this.mac = mac;
    }

    public String getIp() {
        return ip;
    }

    public void setIp(String ip) {
        this.ip = ip;
    }

    public String getSerial() {
        return serial;
    }

    public void setSerial(String serial) {
        this.serial = serial;
    }

    public String getModel() {
        return model;
    }

    public void setModel(String model) {
        this.model = model;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public int getNumberOfPorts() {
        return numberOfPorts;
    }

    public void setNumberOfPorts(int numberOfPorts) {
        this.numberOfPorts = numberOfPorts;
    }

    public List<Port> getReservedPortList() {
        return reservedPortList;
    }

    public void setReservedPortList(List<Port> reservedPortList) {
        this.reservedPortList = reservedPortList;
    }

    public DeviceStatus getDeviceStatus() {
        return deviceStatus;
    }

    public void setDeviceStatus(DeviceStatus deviceStatus) {
        this.deviceStatus = deviceStatus;
    }

    public Integer getPreferredPort() {
        return preferredPort;
    }

    public void setPreferredPort(Integer preferredPort) {
        this.preferredPort = preferredPort;
    }

    public Integer getMasterVlan() {
        return masterVlan;
    }

    public void setMasterVlan(Integer masterVlan) {
        this.masterVlan = masterVlan;
    }

    public Integer getSubVlan() {
        return subVlan;
    }

    public void setSubVlan(Integer subVlan) {
        this.subVlan = subVlan;
    }

    public Integer getMidipopSlot() {
        return midipopSlot;
    }

    public void setMidipopSlot(Integer midipopSlot) {
        this.midipopSlot = midipopSlot;
    }

    public Integer getMidipopPort() {
        return midipopPort;
    }

    public void setMidipopPort(Integer midipopPort) {
        this.midipopPort = midipopPort;
    }

    public String getBlock() {
        return block;
    }

    public void setBlock(String block) {
        this.block = block;
    }

    public List<House> getHouses() {
        return houses;
    }

    public void setHouses(List<House> houses) {
        this.houses = houses;
    }

    public MinipopCategory getCategory() {
        return category;
    }

    public void setCategory(MinipopCategory category) {
        this.category = category;
    }

//    public Port getNextAvailablePort() {
//        return nextAvailablePort;
//    }
//
//    public void setNextAvailablePort(Port nextAvailablePort) {
//        this.nextAvailablePort = nextAvailablePort;
//    }


    public Integer getNextAvailablePortHintAsNumber() {
        return nextAvailablePortHintAsNumber;
    }

    public void setNextAvailablePortHintAsNumber(Integer nextAvailablePortHintAsNumber) {
        this.nextAvailablePortHintAsNumber = nextAvailablePortHintAsNumber;
    }

    public String getHousesAsText() {
        return housesAsText;
    }

    public void setHousesAsText(String housesAsText) {
        this.housesAsText = housesAsText;
    }
}
