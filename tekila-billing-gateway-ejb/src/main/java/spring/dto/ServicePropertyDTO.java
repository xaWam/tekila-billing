package spring.dto;

import com.jaravir.tekila.module.service.entity.Profile;
import com.jaravir.tekila.module.service.entity.Zone;

public class ServicePropertyDTO {
    private Long id;
    private Zone zone;
    private Profile profile;
    private long price;
    private String profileValue;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Zone getZone() {
        return zone;
    }

    public void setZone(Zone zone) {
        this.zone = zone;
    }

    public Profile getProfile() {
        return profile;
    }

    public void setProfile(Profile profile) {
        this.profile = profile;
    }

    public long getPrice() {
        return price;
    }

    public void setPrice(long price) {
        this.price = price;
    }

    public String getProfileValue() {
        return profileValue;
    }

    public void setProfileValue(String profileValue) {
        this.profileValue = profileValue;
    }
}
