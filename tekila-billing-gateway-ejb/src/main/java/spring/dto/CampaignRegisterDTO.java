package spring.dto;

import com.fasterxml.jackson.annotation.JsonView;
import com.jaravir.tekila.jsonview.JsonViews;
import com.jaravir.tekila.module.campaign.Campaign;
import com.jaravir.tekila.module.campaign.CampaignStatus;
import com.jaravir.tekila.module.service.entity.ValueAddedService;
import com.jaravir.tekila.module.subscription.persistence.entity.Subscription;
import org.joda.time.DateTime;

import java.io.Serializable;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.util.List;

/**
 * @author ElmarMa on 3/27/2018
 */
public class CampaignRegisterDTO implements Serializable {

    private Long id;
    private CampaignDTO campaign;
    private DateTime joinDate;
    private DateTime processedDate;
    private CampaignStatus status;
    private Long bonusAmount;
    private String campaignNotes;
    private DateTime bonusDate;
    private int lifeCycleCount;
    private Double serviceRateBonusAmount;


    public static class CampaignDTO implements Serializable {
        private Long id;
        private String name;

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
    }


    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public CampaignDTO getCampaign() {
        return campaign;
    }

    public void setCampaign(CampaignDTO campaign) {
        this.campaign = campaign;
    }

    public DateTime getJoinDate() {
        return joinDate;
    }

    public void setJoinDate(DateTime joinDate) {
        this.joinDate = joinDate;
    }

    public DateTime getProcessedDate() {
        return processedDate;
    }

    public void setProcessedDate(DateTime processedDate) {
        this.processedDate = processedDate;
    }

    public CampaignStatus getStatus() {
        return status;
    }

    public void setStatus(CampaignStatus status) {
        this.status = status;
    }

    public Long getBonusAmount() {
        return bonusAmount;
    }

    public void setBonusAmount(Long bonusAmount) {
        this.bonusAmount = bonusAmount;
    }

    public String getCampaignNotes() {
        return campaignNotes;
    }

    public void setCampaignNotes(String campaignNotes) {
        this.campaignNotes = campaignNotes;
    }

    public DateTime getBonusDate() {
        return bonusDate;
    }

    public void setBonusDate(DateTime bonusDate) {
        this.bonusDate = bonusDate;
    }

    public int getLifeCycleCount() {
        return lifeCycleCount;
    }

    public void setLifeCycleCount(int lifeCycleCount) {
        this.lifeCycleCount = lifeCycleCount;
    }

    public Double getServiceRateBonusAmount() {
        return serviceRateBonusAmount;
    }

    public void setServiceRateBonusAmount(Double serviceRateBonusAmount) {
        this.serviceRateBonusAmount = serviceRateBonusAmount;
    }



    public String getBonusAmountForView() {
        if (bonusAmount != null) {
            if (String.valueOf(bonusAmount).length() < 6) {
                return String.format("%d", bonusAmount);
            }

            double interim = bonusAmount / 100000d;
            DecimalFormat df = new DecimalFormat();
            df.setRoundingMode(RoundingMode.UNNECESSARY);
            return String.format("%.2f AZN", interim);
        } else if (serviceRateBonusAmount != null) {
            return String.format("%.1f %%", serviceRateBonusAmount * 100.0);
        }
        return "N/A";
    }

}
