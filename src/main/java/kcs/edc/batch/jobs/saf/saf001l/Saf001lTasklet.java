package kcs.edc.batch.jobs.saf.saf001l;

import com.sun.org.apache.xerces.internal.impl.dv.util.Base64;
import kcs.edc.batch.cmmn.jobs.CmmnJob;
import kcs.edc.batch.cmmn.property.CmmnConst;
import kcs.edc.batch.cmmn.util.DateUtil;
import kcs.edc.batch.jobs.saf.saf001l.vo.Saf001lVO;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FilenameUtils;
import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Slf4j
public class Saf001lTasklet extends CmmnJob implements Tasklet {

    @Value("#{jobExecutionContext[certNumList]}")
    private List<String> certNumList;
    private String authKey;

    private List<Saf001lVO.DerivationModelItem> derivationModels = new ArrayList<>();
    private List<Saf001lVO.CertificationImageUrlItem> certificationImageUrls  = new ArrayList<>();;
    private List<Saf001lVO.FatoryItem> factories = new ArrayList<>();;
    private List<Saf001lVO.SimilarCertItem> similarCertifications  = new ArrayList<>();;

    @Override
    public void beforeStep(StepExecution stepExecution) {

        super.beforeStep(stepExecution);
        this.authKey = this.apiService.getJobPropHeader(getJobGrpName(), "AuthKey");
    }

    @Override
    public RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext) throws Exception {

        this.writeCmmnLogStart();

        // header setting
        HttpHeaders headers = new HttpHeaders();
        headers.set("AuthKey", this.authKey);
        HttpEntity<String> entity = new HttpEntity<>("parameters", headers);

        for (String certNum : this.certNumList) {

            // parameter setting
            UriComponentsBuilder builder = this.apiService.getUriComponetsBuilder().replaceQueryParam("certNum", certNum);
            URI uri = builder.build().toUri();

            // send API
            Saf001lVO resultVO = this.apiService.sendApiExchange(uri, HttpMethod.GET, entity, Saf001lVO.class);
            if(Objects.isNull(resultVO)) return RepeatStatus.FINISHED;

            Saf001lVO.Item resultData = resultVO.getResultData();

            String crtfInfoId = resultData.getCertUid();

            // ht_saf001l
            for (String model : resultData.getDerivationModels()) {
                Saf001lVO.DerivationModelItem item = new Saf001lVO.DerivationModelItem();
                item.setCertInfoId(crtfInfoId);
                item.setModel(model);
                item.setRegrDt(this.baseDt);
                item.setFrstRgsrDtlDttm(DateUtil.getCurrentTime());
                item.setLastChngDtlDttm(DateUtil.getCurrentTime());
                this.derivationModels.add(item);

                log.info("saf001l >> DerivationModelItem certInfoId : {} model : {}", crtfInfoId, model);
            }


            // ht_saf002l
            for (Saf001lVO.SimilarCertItem item : resultData.getSimilarCertifications()) {
                item.setCertInfoId(crtfInfoId);
                item.setRegrDt(this.baseDt);
                item.setFrstRgsrDtlDttm(DateUtil.getCurrentTime());
                item.setLastChngDtlDttm(DateUtil.getCurrentTime());
                this.similarCertifications.add(item);

                log.info("saf002l >> DerivationModelItem certInfoId : {}, certState : {}", crtfInfoId, item.getCertState());
            }


            // ht_saf003l
            for (Saf001lVO.FatoryItem item : resultData.getFactories()) {
                item.setCertInfoId(crtfInfoId);
                item.setRegrDt(this.baseDt);
                item.setFrstRgsrDtlDttm(DateUtil.getCurrentTime());
                item.setLastChngDtlDttm(DateUtil.getCurrentTime());
                this.factories.add(item);

                log.info("saf003l >> FatoryItem certInfoId : {}, markNm : {}", crtfInfoId, item.getMakerName());
            }


            // ht_saf004l
            for (String imageUrl : resultData.getCertificationImageUrls()) {
                Saf001lVO.CertificationImageUrlItem item = new Saf001lVO.CertificationImageUrlItem();
                item.setCertInfoId(crtfInfoId);

                if(Objects.isNull(imageUrl)) continue;

                String imageFileName = FilenameUtils.getName(imageUrl);
                item.setImageUrl(imageFileName);
                item.setCrtfImageUrl(imageUrl);
                item.setCrtfIamgeFileCn(Base64.encode(getByteArray(imageUrl)));
                item.setRegrDt(this.baseDt);
                item.setFrstRgsrDtlDttm(DateUtil.getCurrentTime());
                item.setLastChngDtlDttm(DateUtil.getCurrentTime());
                this.certificationImageUrls.add(item);

                log.info("saf004l >> FatoryItem certInfoId : {}, imageUrl : {}", crtfInfoId, imageUrl);
            }
        }

        // 파일생성
        this.fileService.makeFile(CmmnConst.JOB_ID_SAF001L, this.derivationModels);
        this.fileService.makeFile(CmmnConst.JOB_ID_SAF004L, this.certificationImageUrls);
        this.fileService.makeFile(CmmnConst.JOB_ID_SAF002L, this.similarCertifications);
        this.fileService.makeFile(CmmnConst.JOB_ID_SAF003L, this.factories);

        this.writeCmmnLogEnd();

        return RepeatStatus.FINISHED;
    }



    public byte[] getByteArray(String imageUrl) {
        URL image = null;
        try {
            image = new URL(imageUrl);
        } catch (MalformedURLException e) {
            log.info(e.getMessage());
        }
        URLConnection ucon = null;
        try {
            ucon = image.openConnection();
        } catch (IOException e) {
            log.info(e.getMessage());
        }
        InputStream is = null;
        try {
            is = ucon.getInputStream();
        } catch (IOException e) {
            log.info(e.getMessage());
        }
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        byte[] buffer = new byte[1024];
        int read;
        while(true) {
            try {
                if (!((read = is.read(buffer, 0, buffer.length)) != -1)) break;
                baos.write(buffer, 0, read);
            } catch (IOException e) {
                log.info(e.getMessage());
            }
        }
        try {
            baos.flush();
        } catch (IOException e) {
            log.info(e.getMessage());
        }
        return baos.toByteArray();
    }
}
