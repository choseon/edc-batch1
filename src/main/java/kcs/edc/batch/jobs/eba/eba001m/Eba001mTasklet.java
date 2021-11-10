package kcs.edc.batch.jobs.eba.eba001m;

import kcs.edc.batch.cmmn.jobs.CmmnJob;
import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.repeat.RepeatStatus;

import javax.xml.bind.JAXBContext;

public class Eba001mTasklet extends CmmnJob implements Tasklet {

    @Override
    public RepeatStatus execute(StepContribution stepContribution, ChunkContext chunkContext) throws Exception {

        JAXBContext.newInstance(new Class[]{});


        return null;
    }
}
