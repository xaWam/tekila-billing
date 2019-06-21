package spring.model;

import javax.xml.bind.annotation.*;
import java.util.List;

/**
 * @author gurbanaz
 * @date 08.04.2019 / 13:52
 */

@XmlRootElement
@XmlAccessorType(XmlAccessType.FIELD)
public class Report {

    @XmlElement(name = "reportingentity")
    private String reportingEntity;
    @XmlElement(name = "startdate")
    private String startDate;
    @XmlElement(name = "enddate")
    private String endDate;
    @XmlElementWrapper(name = "records")
    @XmlElement(name = "record")
    private List<Record> records;

    public Report() {
    }

    public Report(String reportingEntity, String startDate, String endDate, List<Record> records) {
        this.reportingEntity = reportingEntity;
        this.startDate = startDate;
        this.endDate = endDate;
        this.records = records;
    }

    public String getReportingEntity() {
        return reportingEntity;
    }

    public void setReportingEntity(String reportingEntity) {
        this.reportingEntity = reportingEntity;
    }

    public String getStartDate() {
        return startDate;
    }

    public void setStartDate(String startDate) {
        this.startDate = startDate;
    }

    public String getEndDate() {
        return endDate;
    }

    public void setEndDate(String endDate) {
        this.endDate = endDate;
    }

    public List<Record> getRecords() {
        return records;
    }

    public void setRecords(List<Record> records) {
        this.records = records;
    }
}
