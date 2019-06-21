package com.jaravir.tekila.extern.sip.domain;

/**
 * @author MusaAl
 * @date 8/29/2018 : 4:13 PM
 */
public class Extension {

    private int extensionNo;

    private int incomeStatus;

    private int outgStatus;

    private int activeDeactiveServ;

    private int blackListServ;

    private int clirServ;

    private int forwServ;

    private int callBarrServ;

    private int closedGroup;

    public int getExtensionNo() {
        return extensionNo;
    }

    public void setExtensionNo(int extensionNo) {
        this.extensionNo = extensionNo;
    }

    public int getIncomeStatus() {
        return incomeStatus;
    }

    public void setIncomeStatus(int incomeStatus) {
        this.incomeStatus = incomeStatus;
    }

    public int getOutgStatus() {
        return outgStatus;
    }

    public void setOutgStatus(int outgStatus) {
        this.outgStatus = outgStatus;
    }

    public int getActiveDeactiveServ() {
        return activeDeactiveServ;
    }

    public void setActiveDeactiveServ(int activeDeactiveServ) {
        this.activeDeactiveServ = activeDeactiveServ;
    }

    public int getBlackListServ() {
        return blackListServ;
    }

    public void setBlackListServ(int blackListServ) {
        this.blackListServ = blackListServ;
    }

    public int getClirServ() {
        return clirServ;
    }

    public void setClirServ(int clirServ) {
        this.clirServ = clirServ;
    }

    public int getForwServ() {
        return forwServ;
    }

    public void setForwServ(int forwServ) {
        this.forwServ = forwServ;
    }

    public int getCallBarrServ() {
        return callBarrServ;
    }

    public void setCallBarrServ(int callBarrServ) {
        this.callBarrServ = callBarrServ;
    }

    public int getClosedGroup() {
        return closedGroup;
    }

    public void setClosedGroup(int closedGroup) {
        this.closedGroup = closedGroup;
    }
}
