package kcs.edc.batch.cmmn.jobs;

import kcs.edc.batch.cmmn.service.FileService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;

import java.util.List;

@Slf4j
public class CmmnMergeFile implements Tasklet {

    @Autowired
    protected FileService fileService;

    @Value("#{jobParameters[baseDt]}")
    protected String baseDt; // 수집일

    private List<String> mergeJobList;

    private String jobId;

//    public CmmnMergeFile(List<String> mergeJobList) {
//        this.mergeJobList = mergeJobList;
//    }

    public CmmnMergeFile(String jobId) {
        this.jobId = jobId;

    }

    @Override
    public RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext) throws Exception {

        this.fileService.init(this.jobId, this.baseDt);
        this.fileService.mergeTempFile();

        log.info(">>>>>> {}", this.fileService.getTableName());
/*
        writeCmmnLogStart();

        // partitioner의 thread를 적용한 경우 분할하여 생성된 파일을 병합한다.
        // job의 temp 경로에서 tsv파일을 리스트로 읽고 최종파일생성한다.
        // 최종파일생성후 temp파일은 삭제한다.
        if(mergeJobList != null && mergeJobList.size() > 0) {
            for (String jobId : mergeJobList) {

                List<Object[]> list = getMergeListFromTsvFile(jobId);
                makeFile(jobId, list);
            }

//        for (String jobId : mergeJobList) {
//            mergeFile(jobId);
//        }
        } else if(jobId != null) {
            List<Object[]> list = getMergeListFromTsvFile(jobId);
            makeFile(jobId, list);
        }

        writeCmmnLogEnd();
*/

        return RepeatStatus.FINISHED;
    }
}
