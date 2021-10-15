package kcs.edc.batch.cmmn.jobs;

import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.repeat.RepeatStatus;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class CmmnMergeFile extends CmmnTask implements Tasklet {

    private final List<String> mergeJobList;

    public CmmnMergeFile(List<String> mergeJobList) {
        this.mergeJobList = mergeJobList;
    }

    @Override
    public RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext) throws Exception {

        writeCmmnLogStart();

        // partitioner의 thread를 적용한 경우 분할하여 생성된 파일을 병합한다.
        // job의 temp 경로에서 tsv파일을 리스트로 읽고 최종파일생성한다.
        // 최종파일생성후 temp파일은 삭제한다.
        for (String jobId : mergeJobList) {

            List<Object[]> list = getMergeListFromTsvFile(jobId);
            makeFile(jobId, list);
        }

//        for (String jobId : mergeJobList) {
//            mergeFile(jobId);
//        }

        writeCmmnLogEnd();

        return RepeatStatus.FINISHED;
    }
}
