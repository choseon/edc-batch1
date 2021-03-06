package kcs.edc.batch.jobs.saf.saf001l;

import com.fasterxml.jackson.core.JsonProcessingException;
import kcs.edc.batch.cmmn.jobs.CmmnJob;
import kcs.edc.batch.cmmn.property.CmmnProperties;
import kcs.edc.batch.cmmn.util.Base64;
import kcs.edc.batch.cmmn.util.DateUtil;
import kcs.edc.batch.jobs.saf.saf001l.vo.Saf001lVO;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FilenameUtils;
import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
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

    @Override
    public RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext) {

        this.writeCmmnLogStart();

        // header setting
        HttpHeaders headers = new HttpHeaders();
        headers.set("AuthKey", this.apiService.getJobPropHeader(getJobGroupId(), "AuthKey"));
        HttpEntity<String> entity = new HttpEntity<>("parameters", headers);

        List<Saf001lVO.Item> resultList = new ArrayList<>();

        try {
            for (String certNum : this.certNumList) {

                // parameter setting
                UriComponentsBuilder builder = this.apiService.getUriComponetsBuilder().replaceQueryParam("certNum", certNum);
                URI uri = builder.build().toUri();

                // send API
                Saf001lVO resultVO = this.apiService.sendApiExchange(uri, HttpMethod.GET, entity, Saf001lVO.class);

                if (Objects.isNull(resultVO)) continue;

                Saf001lVO.Item resultData = resultVO.getResultData();
                resultList.add(resultData);

                log.info("[{}/{}] certNum: {}, saf001l: {}, saf002l: {}, saf003l: {}, saf004l: {}",
                        this.itemCnt++, this.certNumList.size(), certNum, resultData.getDerivationModels().size(), resultData.getSimilarCertifications().size(),
                        resultData.getFactories().size(), resultData.getCertificationImageUrls().size());
            }

            // HT_SAF001L ?????????????????? ????????????
            try {
                this.writeCmmnLogStart(CmmnProperties.JOB_ID_SAF001L);
                List<Saf001lVO.DerivationModelItem> derivationModelList = getDerivationModelList(resultList);
                this.fileService.makeFile(CmmnProperties.JOB_ID_SAF001L, derivationModelList);
            } catch (FileNotFoundException e) {
                this.makeErrorLog(CmmnProperties.JOB_ID_SAF001L, e.toString());
            } catch (IllegalAccessException e) {
                this.makeErrorLog(CmmnProperties.JOB_ID_SAF001L, e.toString());
            }

            // HT_SAF002L ?????????????????? ?????? ????????????
            try {
                this.writeCmmnLogStart(CmmnProperties.JOB_ID_SAF002L);
                List<Saf001lVO.SimilarCertItem> similarCertificationList = getSimilarCertificationList(resultList);
                this.fileService.makeFile(CmmnProperties.JOB_ID_SAF002L, similarCertificationList);
            } catch (FileNotFoundException e) {
                this.makeErrorLog(CmmnProperties.JOB_ID_SAF002L, e.toString());
            } catch (IllegalAccessException e) {
                this.makeErrorLog(CmmnProperties.JOB_ID_SAF002L, e.toString());
            }

            // HT_SAF003L ?????????????????? ????????????
            try {
                this.writeCmmnLogStart(CmmnProperties.JOB_ID_SAF003L);
                List<Saf001lVO.FatoryItem> factoryList = getFactoryList(resultList);
                this.fileService.makeFile(CmmnProperties.JOB_ID_SAF003L, factoryList);
            } catch (FileNotFoundException e) {
                this.makeErrorLog(CmmnProperties.JOB_ID_SAF003L, e.toString());
            } catch (IllegalAccessException e) {
                this.makeErrorLog(CmmnProperties.JOB_ID_SAF003L, e.toString());
            }

            // HT_SAF004L ??????????????? ????????????
            try {
                this.writeCmmnLogStart(CmmnProperties.JOB_ID_SAF004L);
                List<Saf001lVO.CertificationImageUrlItem> certificationImageUrlList = getCertificationImageUrlList(resultList);
                this.fileService.makeFile(CmmnProperties.JOB_ID_SAF004L, certificationImageUrlList);
            } catch (FileNotFoundException e) {
                this.makeErrorLog(CmmnProperties.JOB_ID_SAF004L, e.toString());
            } catch (IllegalAccessException e) {
                this.makeErrorLog(CmmnProperties.JOB_ID_SAF004L, e.toString());
            }

        } catch (JsonProcessingException e) {
            this.makeErrorLog(CmmnProperties.JOB_ID_SAF001L, e.toString());
            this.makeErrorLog(CmmnProperties.JOB_ID_SAF002L, e.toString());
            this.makeErrorLog(CmmnProperties.JOB_ID_SAF003L, e.toString());
            this.makeErrorLog(CmmnProperties.JOB_ID_SAF004L, e.toString());
        } finally {
            this.writeCmmnLogEnd();
        }

        return RepeatStatus.FINISHED;
    }

    /**
     * ?????????????????? ??????????????????
     *
     * @param list
     * @return
     */
    private List<Saf001lVO.DerivationModelItem> getDerivationModelList(List<Saf001lVO.Item> list) {

        List<Saf001lVO.DerivationModelItem> resultList = new ArrayList<>();
        for (Saf001lVO.Item obj : list) {
            String crtfInfoId = obj.getCertUid();
            for (String model : obj.getDerivationModels()) {
                Saf001lVO.DerivationModelItem item = new Saf001lVO.DerivationModelItem();
                item.setCertInfoId(crtfInfoId);
                item.setModel(model);
                item.setRegrDt(this.baseDt);
                item.setFrstRgsrDtlDttm(DateUtil.getCurrentTime());
                item.setLastChngDtlDttm(DateUtil.getCurrentTime());
                resultList.add(item);

                log.info("saf001l >> certInfoId : {} model : {}", crtfInfoId, model);
            }
        }
        return resultList;
    }

    /**
     * ?????????????????? ?????????????????? ??????
     *
     * @param list
     * @return
     */
    private List<Saf001lVO.SimilarCertItem> getSimilarCertificationList(List<Saf001lVO.Item> list) {

        List<Saf001lVO.SimilarCertItem> resultList = new ArrayList<>();
        for (Saf001lVO.Item obj : list) {
            String crtfInfoId = obj.getCertUid();
            for (Saf001lVO.SimilarCertItem item : obj.getSimilarCertifications()) {
                item.setCertInfoId(crtfInfoId);
                item.setRegrDt(this.baseDt);
                item.setFrstRgsrDtlDttm(DateUtil.getCurrentTime());
                item.setLastChngDtlDttm(DateUtil.getCurrentTime());
                resultList.add(item);

                log.info("saf002l >> certInfoId : {}, certUid: {}, certNm: {}",
                        crtfInfoId, item.getCertUid(), item.getCertNum());
            }
        }
        return resultList;
    }

    /**
     * ?????????????????? ??????????????????
     *
     * @param list
     * @return
     */
    private List<Saf001lVO.FatoryItem> getFactoryList(List<Saf001lVO.Item> list) {

        List<Saf001lVO.FatoryItem> resultList = new ArrayList<>();
        for (Saf001lVO.Item obj : list) {
            String crtfInfoId = obj.getCertUid();
            for (Saf001lVO.FatoryItem item : obj.getFactories()) {
                item.setCertInfoId(crtfInfoId);
                item.setRegrDt(this.baseDt);
                item.setFrstRgsrDtlDttm(DateUtil.getCurrentTime());
                item.setLastChngDtlDttm(DateUtil.getCurrentTime());
                resultList.add(item);

                log.info("saf003l >> certInfoId : {}, markNm : {}", crtfInfoId, item.getMakerName());
            }
        }
        return resultList;
    }

    /**
     * ?????????????????? ???????????????
     *
     * @param list
     * @return
     */
    private List<Saf001lVO.CertificationImageUrlItem> getCertificationImageUrlList(List<Saf001lVO.Item> list) {

        List<Saf001lVO.CertificationImageUrlItem> resultList = new ArrayList<>();
        for (Saf001lVO.Item obj : list) {
            String crtfInfoId = obj.getCertUid();
            for (String imageUrl : obj.getCertificationImageUrls()) {
                Saf001lVO.CertificationImageUrlItem item = new Saf001lVO.CertificationImageUrlItem();
                item.setCertInfoId(crtfInfoId);

                if (Objects.isNull(imageUrl)) continue;

                String imageFileName = FilenameUtils.getName(imageUrl);
                item.setImageUrl(imageFileName);
                item.setCrtfImageUrl(imageUrl);
                item.setCrtfIamgeFileCn(Base64.encode(getByteArray(imageUrl)));
                item.setRegrDt(this.baseDt);
                item.setFrstRgsrDtlDttm(DateUtil.getCurrentTime());
                item.setLastChngDtlDttm(DateUtil.getCurrentTime());
                resultList.add(item);

                log.info("saf004l >> certInfoId : {}, imageUrl : {}", crtfInfoId, imageUrl);
            }
        }
        return resultList;
    }


    /**
     * ?????????????????? ???????????? ByteArrayOutputStream ByteArray
     *
     * @param imageUrl ???????????????
     * @return
     */
    private byte[] getByteArray(String imageUrl) {

        byte[] result = null;
        ByteArrayOutputStream baos = null;
        InputStream is = null;
        try {
            URL image = new URL(imageUrl);
            URLConnection ucon = image.openConnection();
            is = ucon.getInputStream();
            baos = new ByteArrayOutputStream();
            byte[] buffer = new byte[1024];
            int read;
            while (true) {
                try {
                    if (!((read = is.read(buffer, 0, buffer.length)) != -1)) break;
                    baos.write(buffer, 0, read);
                } catch (IOException e) {
                    log.info(e.toString());
                }
            }
            baos.flush();
            result = baos.toByteArray();

        } catch (MalformedURLException e) {
            log.info(e.toString());
        } catch (IOException e) {
            log.info(e.toString());
        } finally {
            if (baos != null) {
                try {
                    baos.close();
                } catch (IOException e) {
                    log.info(e.toString());
                }
            }
            if (is != null) {
                try {
                    is.close();
                } catch (IOException e) {
                    log.info(e.toString());
                }
            }
        }
        return result;
    }

}
