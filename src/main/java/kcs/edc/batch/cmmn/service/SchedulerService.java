package kcs.edc.batch.cmmn.service;

import kcs.edc.batch.cmmn.property.SchedulerProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class SchedulerService {

    @Autowired
    protected SchedulerProperties schedulerProperties;

    private SchedulerProperties.SchedulerProp schedulerProp;

    public void init(String jobGroupId) {
        this.schedulerProp = this.schedulerProperties.getScedulerProp(jobGroupId);
        log.info("SchedulerService init() >> jobGroupId: {}", jobGroupId);
    }

    public String getBaseFormat() {
        String cycle = getBaseLine().substring(0, 1);
        if(cycle.equals("D")) {
            return "yyyMMdd";
        } else if(cycle.equals("W")) {
            return "yyyyMM";
        } else if(cycle.equals("Y")) {
            return "yyyy";
        }
        return null;
    }

    public String getBaseLine() {
        return this.schedulerProp.getBaseline();
    }

    public int getPeriod() {
        return this.schedulerProp.getPeriod();
    }

    public String getCycle() {
        return this.schedulerProp.getCycle();
    }

    public Boolean getIsActive() {
        return this.schedulerProp.getIsActive();
    }


}
