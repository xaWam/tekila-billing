package reseller;

/**
 * Created by ShakirG on 07/09/2018.
 */
public class ServiceDTO {
    public long id;
    public String name;
    public long price;

    ServiceDTO() {
    }

    ServiceDTO(long id, String name, long price) {
        this.id = id;
        this.name = name;
        this.price = price;
    }
}