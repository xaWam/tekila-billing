package spring.dto;

import com.jaravir.tekila.module.campaign.CampaignTarget;

import java.io.Serializable;

/**
 * @author ElmarMa on 3/28/2018
 */
public class CampaignMinifiedDTO implements Serializable {

    private Long id;
    private String name;
    private CampaignTarget target;
    private String desc;
    private String notes;

    public CampaignMinifiedDTO() {
    }

    public CampaignMinifiedDTO(Long id, String name, CampaignTarget target, String description, String notes) {
        this.id = id;
        this.name = name;
        this.target = target;
        this.desc = description;
        this.notes = notes;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public CampaignTarget getTarget() {
        return target;
    }

    public void setTarget(CampaignTarget target) {
        this.target = target;
    }

    public String getDesc() {
        return desc;
    }

    public void setDesc(String desc) {
        this.desc = desc;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    @Override
    public String toString() {
        return "CampaignMinifiedDTO{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", target=" + target +
                ", description='" + desc + '\'' +
                ", notes='" + notes + '\'' +
                '}';
    }
}
