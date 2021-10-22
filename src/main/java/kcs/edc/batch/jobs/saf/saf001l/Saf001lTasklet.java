package kcs.edc.batch.jobs.saf.saf001l;

import kcs.edc.batch.cmmn.jobs.CmmnTask;
import kcs.edc.batch.cmmn.property.JobConstant;
import kcs.edc.batch.jobs.saf.saf001l.vo.Saf001lVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.StepExecutionListener;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Slf4j
public class Saf001lTasklet extends CmmnTask implements Tasklet, StepExecutionListener {

    @Value("#{jobExecutionContext[certNumList]}")
    private List<String> certNumList;
    private String authKey;

    private List<Saf001lVO.DerivationModelItem> derivationModels = new ArrayList<>();
    private List<Saf001lVO.CertificationImageUrlItem> certificationImageUrls  = new ArrayList<>();;
    private List<Saf001lVO.FatoryItem> factories = new ArrayList<>();;
    private List<Saf001lVO.SimilarCertItem> similarCertifications  = new ArrayList<>();;

    @Override
    public void beforeStep(StepExecution stepExecution) {

        jobProp = apiProperties.getJobProp(getJobGrpName());
        authKey = jobProp.getHeader().get("AuthKey");
    }

    @Override
    public RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext) throws Exception {

        writeCmmnLogStart();

        // header setting
        HttpHeaders headers = new HttpHeaders();
        headers.set("AuthKey", authKey);
        HttpEntity<String> entity = new HttpEntity<>("parameters", headers);

        for (String certNum : certNumList) {

            // parameter setting
            UriComponentsBuilder builder = getUriComponetsBuilder().replaceQueryParam("certNum", certNum);
            URI uri = builder.build().toUri();

            // send API
            ResponseEntity<String> exchange = restTemplate.exchange(uri, HttpMethod.GET, entity, String.class);
            String resultJson = exchange.getBody();
            log.info("uri {}", uri);
            log.info("resultJson {}", resultJson);
            if(Objects.isNull(resultJson)) return RepeatStatus.FINISHED;

            Saf001lVO resultVO = objectMapper.readValue(resultJson, Saf001lVO.class);
            if(Objects.isNull(resultVO)) return RepeatStatus.FINISHED;

            Saf001lVO.Item resultData = resultVO.getResultData();

            String certUid = resultData.getCertUid();

            for (String s : resultData.getDerivationModels()) {
                Saf001lVO.DerivationModelItem item = new Saf001lVO.DerivationModelItem();
                item.setCertInfoId(certUid);
                item.setModel(s);
                derivationModels.add(item);
            }

            for (Saf001lVO.SimilarCertItem item : resultData.getSimilarCertifications()) {
                item.setCertInfoId(certUid);
                similarCertifications.add(item);
            }

            for (Saf001lVO.FatoryItem item : resultData.getFactories()) {
                item.setCertInfoId(certUid);
                factories.add(item);
            }


            for (String s : resultData.getCertificationImageUrls()) {
                Saf001lVO.CertificationImageUrlItem item = new Saf001lVO.CertificationImageUrlItem();
                item.setCertInfoId(certUid);
                item.setImageUrl(s);
                certificationImageUrls.add(item);
            }
        }

        // 파일 생성
        makeFile(JobConstant.JOB_ID_SAF001L, derivationModels);
        makeFile(JobConstant.JOB_ID_SAF002L, similarCertifications);
        makeFile(JobConstant.JOB_ID_SAF003L, factories);
        makeFile(JobConstant.JOB_ID_SAF004L, certificationImageUrls);

        writeCmmnLogEnd();

        return RepeatStatus.FINISHED;
    }


    @Override
    public ExitStatus afterStep(StepExecution stepExecution) {
        return ExitStatus.COMPLETED;
    }

}
