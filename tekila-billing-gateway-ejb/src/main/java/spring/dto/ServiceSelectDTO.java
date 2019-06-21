package spring.dto;

/**
 * @author MusaAl
 * @date 4/5/2018 : 2:16 PM
 */
public class ServiceSelectDTO extends BaseDTO{

    private String name;
    private boolean isAvailableOnEcare;
    private boolean isAllowEquipment;
    private boolean isAllowStock;

    public String getName() {
        return name;
    }
    public void setName(String name) {
        this.name = name;
    }

    public boolean isAvailableOnEcare() {
        return isAvailableOnEcare;
    }
    public void setAvailableOnEcare(boolean availableOnEcare) {
        isAvailableOnEcare = availableOnEcare;
    }

    public boolean isAllowEquipment() {
        return isAllowEquipment;
    }
    public void setAllowEquipment(boolean allowEquipment) {
        isAllowEquipment = allowEquipment;
    }

    public boolean isAllowStock() {
        return isAllowStock;
    }
    public void setAllowStock(boolean allowStock) {
        isAllowStock = allowStock;
    }
}
