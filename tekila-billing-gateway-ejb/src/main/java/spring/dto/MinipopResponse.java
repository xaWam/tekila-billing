package spring.dto;

import com.jaravir.tekila.common.device.DeviceStatus;
import com.jaravir.tekila.provision.broadband.devices.MiniPop;

public class MinipopResponse {
    public final Long id;
    public final DeviceStatus deviceStatus;
    public final String mac;
    public final String switchId;
    public final String address;
    public final int numberOfPorts;
    public final String availablePortsAsText;
    public final String reservedPortsAsText;

    public MinipopResponse(MiniPop miniPop) {
        this.id = miniPop.getId();
        this.deviceStatus = miniPop.getDeviceStatus();
        this.mac = miniPop.getMac();
        this.switchId = miniPop.getSwitch_id();
        this.address = miniPop.getAddress();
        this.numberOfPorts = miniPop.getNumberOfPorts();
        this.availablePortsAsText = miniPop.getAvailablePortsAsText();
        this.reservedPortsAsText = miniPop.getReservedPortsAsText();
    }

    public static MinipopResponse from(MiniPop minipop) {
        return new MinipopResponse(minipop);
    }
}
