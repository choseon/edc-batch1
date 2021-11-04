package kcs.edc.batch.cmmn.service;

import kcs.edc.batch.cmmn.property.JobProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Iterator;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
public class JobService {

    @Autowired
    private JobProperties jobProperties;


    public List<String> getJobId(String jobGroupId) {
        return jobProperties.getGroup().get(jobGroupId);
    }

    public String getJobGroupId(String jobId) {

        Map<String, List<String>> group = jobProperties.getGroup();
        Iterator<String> iterator = group.keySet().iterator();

        while (iterator.hasNext()) {
            String next = iterator.next();
            if (next.contains(jobId)) {
                return next;
            }
        }
        return null;
    }
}
