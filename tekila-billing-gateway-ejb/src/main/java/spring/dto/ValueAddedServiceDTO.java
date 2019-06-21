package spring.dto;

import java.io.Serializable;
import java.text.DecimalFormat;
import java.util.Date;
import java.util.List;

public class ValueAddedServiceDTO extends BaseDTO implements Serializable {

    private String name;
    private VasCodeDTO code;
//    private Resource resource;
//    private TaxationCategory category;
    private boolean isManagedByPrincipal;
    private ServiceProviderDTO provider;
    private long price;
    private Date creationDate;
//    private List<VASSetting> settings;
    private String expression;
    private String chargeableItem;
    private boolean provisioned;
    private long maxNumber;
    private double count;
    private boolean isActive;
    private boolean isStaticIp;
    private boolean doPartialCharge;
//    private StaticIPType staticIPType;
    private boolean credit;
    private boolean suspension;
    private String alternateName;
    private boolean isIptv;
    private boolean isSip;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public VasCodeDTO getCode() {
        return code;
    }

    public void setCode(VasCodeDTO code) {
        this.code = code;
    }

    public boolean isManagedByPrincipal() {
        return isManagedByPrincipal;
    }

    public void setManagedByPrincipal(boolean managedByPrincipal) {
        isManagedByPrincipal = managedByPrincipal;
    }

    public ServiceProviderDTO getProvider() {
        return provider;
    }

    public void setProvider(ServiceProviderDTO provider) {
        this.provider = provider;
    }

    public long getPrice() {
        return price;
    }

    public void setPrice(long price) {
        this.price = price;
    }

    public Date getCreationDate() {
        return creationDate;
    }

    public void setCreationDate(Date creationDate) {
        this.creationDate = creationDate;
    }

    public String getExpression() {
        return expression;
    }

    public void setExpression(String expression) {
        this.expression = expression;
    }

    public String getChargeableItem() {
        return chargeableItem;
    }

    public void setChargeableItem(String chargeableItem) {
        this.chargeableItem = chargeableItem;
    }

    public boolean isProvisioned() {
        return provisioned;
    }

    public void setProvisioned(boolean provisioned) {
        this.provisioned = provisioned;
    }

    public long getMaxNumber() {
        return maxNumber;
    }

    public void setMaxNumber(long maxNumber) {
        this.maxNumber = maxNumber;
    }

    public double getCount() {
        return count;
    }

    public void setCount(double count) {
        this.count = count;
    }

    public boolean isActive() {
        return isActive;
    }

    public void setActive(boolean active) {
        isActive = active;
    }

    public boolean isStaticIp() {
        return isStaticIp;
    }

    public void setStaticIp(boolean staticIp) {
        isStaticIp = staticIp;
    }

    public boolean isDoPartialCharge() {
        return doPartialCharge;
    }

    public void setDoPartialCharge(boolean doPartialCharge) {
        this.doPartialCharge = doPartialCharge;
    }

    public boolean isCredit() {
        return credit;
    }

    public void setCredit(boolean credit) {
        this.credit = credit;
    }

    public boolean isSuspension() {
        return suspension;
    }

    public void setSuspension(boolean suspension) {
        this.suspension = suspension;
    }

    public String getAlternateName() {
        return alternateName;
    }

    public void setAlternateName(String alternateName) {
        this.alternateName = alternateName;
    }

    public boolean isIptv() {
        return isIptv;
    }

    public void setIptv(boolean iptv) {
        isIptv = iptv;
    }

    public boolean isSip() {
        return isSip;
    }

    public void setSip(boolean sip) {
        isSip = sip;
    }
}
