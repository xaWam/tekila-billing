package spring.controller.vm;


/**
 * @author gurbanaz
 * @date 15.03.2019 17:30
 */
public class SubscriptionVASSettingsVM {

    private Integer id;
    private String value;
    private String length;
    private long total;
    private String dsc;


    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public String getLength() {
        return length;
    }

    public void setLength(String length) {
        this.length = length;
    }

    public long getTotal() {
        return total;
    }

    public void setTotal(long total) {
        this.total = total;
    }

    public String getDsc() {
        return dsc;
    }

    public void setDsc(String dsc) {
        this.dsc = dsc;
    }


    @Override
    public String toString() {
        return "SubscriptionVASSettingsVM{" +
                "id=" + id +
                ", value='" + value + '\'' +
                ", length='" + length + '\'' +
                ", total=" + total +
                ", dsc='" + dsc + '\'' +
                '}';
    }
}
