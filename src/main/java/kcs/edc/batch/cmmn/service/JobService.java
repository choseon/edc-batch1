package kcs.edc.batch.cmmn.service;

import kcs.edc.batch.cmmn.property.CmmnProperties;
import kcs.edc.batch.cmmn.property.JobProperties;
import kcs.edc.batch.cmmn.util.DateUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.ObjectUtils;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Slf4j
@Service
public class JobService {

    @Autowired
    protected JobProperties jobProperties;
    private JobProperties.JobProp jobProp;

    /**
     * 기준일
     */
    private String baseDt;

    /**
     * 시작일
     */
    private String startDt;

    /**
     * 종료일
     */
    private String endDt;

    /**
     * 수집기간
     */
    private int period;

    /**
     * JobService 초기화
     *
     * @param jobGroupId
     * @param baseDt
     * @throws ParseException
     */
    public void init(String jobGroupId, String baseDt) throws ParseException {

        log.info("JobService init() >> jobGroupId: {}", jobGroupId);

        this.jobProp = getJobProp(jobGroupId);
        this.period = this.jobProp.getPeriod();

        // 일배치일 경우만 초기화하고, 월배치는 패턴이 달라 제외(해당 Tasklet에서 초기화함)
        if (!getCycle().equals(CmmnProperties.SCHEDULER_CYCLE_DAILY)) return;

        if (ObjectUtils.isEmpty(baseDt)) {
            this.baseDt = DateUtil.getBaseLineDate(getBaseLine());
        } else {
            this.baseDt = baseDt;
        }

        this.startDt = DateUtil.getOffsetDate(this.baseDt, (this.period - 1) * -1);

        if (jobGroupId.equals(CmmnProperties.JOB_GRP_ID_BIG)) {
            // BigKinds의 경우 endDt가 baseDt + 1로 파라미터 넘겨줘야함.
            this.endDt = DateUtil.getOffsetDate(this.baseDt, 1);
        } else {
            this.endDt = this.baseDt;
        }
    }

    /**
     * 배치잡 전체 리스트 조회
     *
     * @return
     */
    public List<String> getJobList() {
        List<String> result = new ArrayList<>();
        Map<String, JobProperties.JobProp> jobs = this.jobProperties.getInfo();
        Set<String> keySet = jobs.keySet();
        for (String key : keySet) {

            if (key.equals(CmmnProperties.JOB_GRP_ID_COM)) continue; // com은 제외
            if (!jobs.get(key).getIsActive()) continue; // 비활성화 제외

            List<String> nodes = jobs.get(key).getNodes();
            if (ObjectUtils.isEmpty(nodes)) continue;

            result.addAll(nodes);
        }
        return result;
    }

    public JobProperties.JobProp getJobProp(String jobGroupId) {
        return this.jobProperties.getJobProp(jobGroupId);
    }

    public String getBaseLine() {
        return this.jobProp.getBaseline();
    }

    public String getCycle() {
        return this.jobProp.getCycle();
    }

    public Boolean getIsActive() {
        return this.jobProp.getIsActive();
    }

    public int getPeriod() {
        return period;
    }

    public String getBaseDt() {
        return baseDt;
    }

    public String getStartDt() {
        return startDt;
    }

    public String getEndDt() {
        return endDt;
    }
}
