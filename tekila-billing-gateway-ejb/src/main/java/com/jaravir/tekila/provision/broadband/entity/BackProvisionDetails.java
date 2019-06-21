package com.jaravir.tekila.provision.broadband.entity;

import org.apache.commons.lang3.builder.ToStringBuilder;

public class BackProvisionDetails {
    public final String switchName;
    public final String slot;
    public final String port;
    public final String mac;
    public final String password;

//    public BackProvisionDetails(
//            final String switchName,
//            final Long slot,
//            final Long port,
//            final String mac
//    ) {
//        this.switchName = switchName;
//        this.slot = (slot != null) ? String.valueOf(slot) : null;
//        this.port = (port != null) ? String.valueOf(port) : null;
//        this.mac = mac;
//    }

    public BackProvisionDetails(String switchName, Long slot, Long port, String mac, String password) {
        this.switchName = switchName;
        this.slot = String.valueOf(slot);
        this.port = String.valueOf(port);
        this.mac = mac;
        this.password = password;
    }


    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("BackProvisionDetails{");
        sb.append("switchName='").append(switchName).append('\'');
        sb.append(", slot='").append(slot).append('\'');
        sb.append(", port='").append(port).append('\'');
        sb.append(", mac='").append(mac).append('\'');
        sb.append(", password='").append(password).append('\'');
        sb.append('}');
        return sb.toString();
    }
}
