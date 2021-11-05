package kcs.edc.batch.jobs.opd.iac016l;

import kcs.edc.batch.cmmn.jobs.CmmnJob;
import kcs.edc.batch.jobs.opd.iac016l.vo.Iac016lVO;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;
import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpMethod;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;

/**
 * 금융감독원 OpenDart 기업공시정보 데이터수집 Tasklet
 */
@Slf4j
public class Iac016lTasklet extends CmmnJob implements Tasklet {

    @Value("#{jobExecutionContext[companyCodeList]}")
    private List<String> companyCodeList;

    private String[] pblntfDetailList;

    private String crtfcKey;

    private String dailyFilePath;

    private String rootPath;

    @Override
    public void beforeStep(StepExecution stepExecution) {
        super.beforeStep(stepExecution);
        this.crtfcKey = this.apiService.getJobPropHeader(getJobGroupId(), "crtfcKey");
        this.pblntfDetailList = getPblntfDetailTy();
//        this.dailyFilePath = makeDirectory(this.fileService.getRootPath());
        this.rootPath = this.fileService.getRootPath();

    }

    @Override
    public RepeatStatus execute(StepContribution stepContribution, ChunkContext chunkContext) throws Exception {

        this.writeCmmnLogStart();

        for (String companyCode : this.companyCodeList) {

            for (String pblnt : this.pblntfDetailList) {

                int pageNo = 1;

                String pblntf_ty = pblnt.substring(0, 1);

                UriComponentsBuilder builder = this.apiService.getUriComponetsBuilder();
                builder.replaceQueryParam("crtfc_key", this.crtfcKey);
                builder.replaceQueryParam("copr_code", companyCode);
                builder.replaceQueryParam("bgn_de", this.baseDt);
                builder.replaceQueryParam("end_de", this.baseDt);
                builder.replaceQueryParam("last_reprt_at", "Y");
                builder.replaceQueryParam("pblntf_ty", pblntf_ty);
                builder.replaceQueryParam("pblntf_detail_ty", pblnt);
                builder.replaceQueryParam("page_no", pageNo++);
                builder.replaceQueryParam("page_count", 100);
                URI uri = builder.build().toUri();

                Thread.sleep(50);

                Iac016lVO resultVO = this.apiService.sendApiForEntity(uri, Iac016lVO.class);
                if (!resultVO.getStatus().equals("000")) continue;
                log.info("companyCode: {}, pblntf: {}, list: {} ", companyCode, pblnt, resultVO.getList().size());

                int totalPage = Integer.parseInt(resultVO.getTotal_page());

                for (Iac016lVO.Item item : resultVO.getList()) {

                    if (Objects.isNull(item.getStock_code())) continue;

                    String saveFileNm = item.getRcept_no() + "_" + pblntf_ty + "_" + pblnt + ".zip";

                    //보고서 원문파일 download
//                    downloadReportFile(item.getRcept_no(), this.dailyFilePath, saveFileNm);

                    this.resultList.add(item);

                }
            }
            if(this.resultList.size() > 5) break; // test
        }

        // 파일생성
        this.fileService.makeFile(this.resultList);


        this.writeCmmnLogEnd();

        return RepeatStatus.FINISHED;
    }

    /**
     * 보고서 원문 파일 다운받기
     *
     * @param rceptNo
     * @param dailyFilePathNm
     * @param fileNm
     * @throws IOException
     */
    public void downloadReportFile(String rceptNo, String dailyFilePathNm, String fileNm) throws IOException {

        HttpComponentsClientHttpRequestFactory factory = new HttpComponentsClientHttpRequestFactory();
        factory.setConnectTimeout(5000); //타임아웃 설정 5초
        factory.setReadTimeout(5000);//타임아웃 설정 5초
        RestTemplate restTemplate = new RestTemplate(factory);

        String url = "https://opendart.fss.or.kr/api/document.xml";
        UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(url);
        builder.queryParam("crtfc_key", this.crtfcKey);
        builder.queryParam("rcept_no", rceptNo);
        URI uri = builder.build().toUri();

        Path file = restTemplate.execute(uri, HttpMethod.GET, null, response -> {
            Path zipFile = Files.createTempFile("opendart-", ".zip");
            InputStream ins = response.getBody();
            byte[] bytes = IOUtils.toByteArray(ins);
            Files.write(zipFile, bytes);
            return zipFile;
        });

        Path zipFile = file;
        byte[] buf = Files.readAllBytes(zipFile);

        //압축파일 저장하기
        saveZipFile(dailyFilePathNm, fileNm, buf);

    }

    /**
     * 압축파일 저장하기
     *
     * @param dailyFilePathNm
     * @param fileNm
     * @param buf
     */
    public void saveZipFile(String dailyFilePathNm, String fileNm, byte[] buf) {

        if (buf != null) {

            try {
                File file = new File(this.rootPath + dailyFilePathNm + fileNm);
                FileOutputStream fos = new FileOutputStream(file);
                fos.write(buf);
                fos.close();
            } catch (Throwable e) {
                log.info(e.getMessage());
            }
        }

    }

    //공시상세유형
    private String[] getPblntfDetailTy() {
        String[] cd = new String[10];

        cd[0] = "A001"; //	사업보고서
        cd[1] = "A002"; //	반기보고서
         cd[2] = "A003"; //	분기보고서
        cd[3] = "A004"; //	등록법인결산서류(자본시장법이전)
        cd[4] = "A005"; //	소액공모법인결산서류
        cd[5] = "B001"; //	주요사항보고서
        cd[6] = "B002"; //	주요경영사항신고(자본시장법 이전)
        cd[7] = "B003"; //	최대주주등과의거래신고(자본시장법 이전)
        cd[8] = "C001"; //	증권신고(지분증권)
        cd[9] = "C002"; //	증권신고(채무증권)
 /*      cd[10] = "C003"; //	증권신고(파생결합증권)
        cd[11] = "C004"; //	증권신고(합병등)
        cd[12] = "C005"; //	증권신고(기타)
        cd[13] = "C006"; //	소액공모(지분증권)
        cd[14] = "C007"; //	소액공모(채무증권)
        cd[15] = "C008"; //	소액공모(파생결합증권)
        cd[16] = "C009"; //	소액공모(합병등)
        cd[17] = "C010"; //	소액공모(기타)
        cd[18] = "C011"; //	호가중개시스템을통한소액매출
        cd[19] = "D001"; //	주식등의대량보유상황보고서
        cd[20] = "D002"; //	임원ㆍ주요주주특정증권등소유상황보고서
        cd[21] = "D003"; //	의결권대리행사권유
        cd[22] = "D004"; //	공개매수
        cd[23] = "E001"; //	자기주식취득/처분
        cd[24] = "E002"; //	신탁계약체결/해지
        cd[25] = "E003"; //	합병등종료보고서
        cd[26] = "E004"; //	주식매수선택권부여에관한신고
        cd[27] = "E005"; //	사외이사에관한신고
        cd[28] = "E006"; //	주주총회소집공고
        cd[29] = "E007"; //	시장조성/안정조작
        cd[30] = "E008"; //	합병등신고서(자본시장법 이전)
        cd[31] = "E009"; //	금융위등록/취소(자본시장법 이전)
        cd[32] = "F001"; //	감사보고서
        cd[33] = "F002"; //	연결감사보고서
        cd[34] = "F003"; //	결합감사보고서
        cd[35] = "F004"; //	회계법인사업보고서
        cd[36] = "F005"; //	감사전재무제표미제출신고서
        cd[37] = "G001"; //	증권신고(집합투자증권-신탁형)
        cd[38] = "G002"; //	증권신고(집합투자증권-회사형)
        cd[39] = "G003"; //	증권신고(집합투자증권-합병)
        cd[40] = "H001"; //	자산유동화계획/양도등록
        cd[41] = "H002"; //	사업/반기/분기보고서
        cd[42] = "H003"; //	증권신고(유동화증권등)
        cd[43] = "H004"; //	채권유동화계획/양도등록
        cd[44] = "H005"; //	수시보고
        cd[45] = "H006"; //	주요사항보고서
        cd[46] = "I001"; //	수시공시
        cd[47] = "I002"; //	공정공시
        cd[48] = "I003"; //	시장조치/안내
        cd[49] = "I004"; //	지분공시
        cd[50] = "I005"; //	증권투자회사
        cd[51] = "I006"; //	채권공시
        cd[52] = "J001"; //	대규모내부거래관련
        cd[53] = "J002"; //	대규모내부거래관련(구)
        cd[54] = "J004"; //	기업집단현황공시
        cd[55] = "J005"; //	비상장회사중요사항공시
        cd[56] = "J006"; //	기타공정위공시*/

        //A ", ); 정기공시
        //B ", ); 주요사항보고
        //C ", ); 발행공시
        //D ", ); 지분공시
        //E ", ); 기타공시
        //F ", ); 외부감사관련
        //G ", ); 펀드공시
        //H ", ); 자산유동화
        //I ", ); 거래소공시
        //j ", ); 공정위공시

        return cd;

    }
}
