package kcs.edc.batch.jobs.opd.opd002m;

import kcs.edc.batch.cmmn.jobs.CmmnJob;
import kcs.edc.batch.cmmn.util.DateUtil;
import kcs.edc.batch.cmmn.util.ZipUtil;
import kcs.edc.batch.jobs.opd.opd002m.vo.Opd002mVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.util.ObjectUtils;
import org.springframework.web.client.RestClientException;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.*;
import java.net.*;
import java.util.ArrayList;
import java.util.List;

/**
 * 금융감독원 OpenDart 기업공시정보 데이터수집 Tasklet
 */
@Slf4j
public class Opd002mTasklet extends CmmnJob implements Tasklet {

    @Value("#{jobExecutionContext[companyCodeList]}")
    private List<String> companyCodeList;

    private String[] pblntfDetailList;

    private String crtfcKey;

    private String attachFilePath;

    @Value("${opd.callApiDelayTime}")
    private int callApiDelayTime;

    @Value("${opd.documentUrl}")
    private String documentUrl;

    @Value("${opd.attachDBPath}")
    private String attachDBPath; // DB에 저장될 첨부파일 root 경로


    @Override
    public void beforeStep(StepExecution stepExecution) {
        super.beforeStep(stepExecution);
        this.crtfcKey = this.apiService.getJobPropHeader(this.jobGroupId, "crtfcKey");
        this.pblntfDetailList = getPblntfDetailTy();
        this.attachFilePath = this.fileService.getAttachedFilePath() + this.baseDt + File.separator;
    }

    @Override
    public RepeatStatus execute(StepContribution stepContribution, ChunkContext chunkContext) {

        this.writeCmmnLogStart();

        try {
            int pageNo = 1;
            int totPageNo = 0;

            for (String pblnt : this.pblntfDetailList) { // 공시상세유형 목록

                do {
                    String pblntfType = pblnt.substring(0, 1); // 공시유형코드

                    UriComponentsBuilder builder = this.apiService.getUriComponetsBuilder();
                    builder.replaceQueryParam("crtfc_key", this.crtfcKey);
//                    builder.replaceQueryParam("corp_code", companyCode);
                    builder.replaceQueryParam("bgn_de", this.baseDt);
                    builder.replaceQueryParam("end_de", this.baseDt);
                    builder.replaceQueryParam("pblntf_ty", pblntfType);
                    builder.replaceQueryParam("pblntf_detail_ty", pblnt);
                    builder.replaceQueryParam("page_no", pageNo);
                    URI uri = builder.build().toUri();

                    Thread.sleep(this.callApiDelayTime);
                    Opd002mVO resultVO = this.apiService.sendApiForEntity(uri, Opd002mVO.class);

                    if (resultVO.getStatus().equals("000")) {

                        totPageNo = Integer.parseInt(resultVO.getTotal_page());

                        for (Opd002mVO.Item item : resultVO.getList()) {

                            item.setPblntf_ty(pblntfType);
                            item.setPblntf_detail_ty(pblnt);
                            item.setFile_path_nm(this.attachDBPath);
                            item.setRcpn_file_path_nm(this.baseDt + File.separator); // 년월일
                            item.setReport_nm(item.getReport_nm().replaceAll("\"", "")); // "(더블따옴표) 제거
                            item.setReport_nm(item.getReport_nm().replaceAll("\'", "")); // '(싱글따옴표) 제거
//                            item.setSrbk_file_nm("[" + item.getCorp_name() + "]" + item.getReport_nm() + ".zip");

                            String saveFileNm = item.getRcept_no() + "_" + pblntfType + "_" + pblnt + ".zip";
                            item.setSorg_file_nm(saveFileNm);

                            item.setCletFileCrtnDt(DateUtil.getCurrentDate());
                            this.resultList.add(item);

                            log.info("corpName: {}, reportNm: {}, pblnt: {}", item.getCorp_name(), item.getReport_nm(), pblnt);
                        }
                        pageNo++;

                    } else {
                        log.debug("resultVO.getStatus(): {}", resultVO.getStatus());
                        totPageNo = 0;
                    }

                } while (totPageNo >= pageNo);
            }

            // 데이터파일생성
            this.fileService.makeFile(this.resultList);


            // 첨부파일 다운로드
            int cnt = 1;
            for (Object o : this.resultList) {
                Opd002mVO.Item item = (Opd002mVO.Item) o;
                // 공시 뷰어에 접속하여 보고서 첨부파일 번호 목록 갖고 오기
                ArrayList<String> dcmNoList = getDcmNoList(item.getRcept_no());
                //압축파일명 = 첨부파일 다운로드 및 압축하기
                String saveFileNm = saveRptFile(dcmNoList, item.getRcept_no(), item.getPblntf_ty(), item.getPblntf_detail_ty(), item.getReport_nm());
                log.info("[{}/{}] downloadFile: corpName: {}, saveFileNm: {}",
                        cnt++, this.resultList.size(), item.getCorp_name(), saveFileNm);
            }

        } catch (FileNotFoundException e) {
            this.makeErrorLog(e.getMessage());
        } catch (IOException e) {
            this.makeErrorLog(e.getMessage());
        } catch (InterruptedException e) {
            this.makeErrorLog(e.getMessage());
        } catch (IllegalAccessException e) {
            this.makeErrorLog(e.getMessage());
        } catch (RestClientException e) {
            this.makeErrorLog(e.getMessage());
        } finally {
            this.writeCmmnLogEnd();
        }

        return RepeatStatus.FINISHED;
    }

    /**
     * 공시 뷰어에 접속하여 보고서 첨부파일 번호 목록 갖고 오기
     *
     * @param rcept_no
     * @return
     */
    private ArrayList<String> getDcmNoList(String rcept_no) throws IOException {
        // 공시 보고서 뷰어에 접속하여 html 파일 읽어오기 시작
        String viewerUrl = "https://dart.fss.or.kr/dsaf001/main.do?rcpNo=" + rcept_no; // 뷰어 url
        String viewerFileNm = this.attachFilePath + "viewerFileNm_" + rcept_no + ".txt"; //내려받을 파일명 지정

        ArrayList<String> returnArr = new ArrayList<String>();

        OutputStream out = null;
        InputStream in = null;
        BufferedReader reader = null;

        String rcpNo = "";
        String rcpNo2 = "";
        String rcpNo3 = "";

        try {

            URL url = new URL(viewerUrl);

            File dir = new File(this.attachFilePath);
            if (!dir.exists()) {
                if (!dir.mkdirs()) {
                    log.info("폴더 생성 실패", dir.getPath());
                    return null;
                }
            }
            out = new BufferedOutputStream(new FileOutputStream(viewerFileNm));
            in = url.openConnection().getInputStream();

            byte[] buffer = new byte[1024 * 100];

            int numRead;
            while ((numRead = in.read(buffer)) != -1) {
                out.write(buffer, 0, numRead);
            }

            //파일에서 dcmNo 추출하기 시작
            reader = new BufferedReader(new FileReader(viewerFileNm));

            String strHtmlLine = "";
            String dcmNo = "";
            boolean dcmNoExistFlag = false; //dcmNo 중복 여부

            while ((strHtmlLine = reader.readLine()) != null) {

                if (rcept_no.equals(rcpNo)) {

                    if (strHtmlLine.indexOf("node1['dcmNo'] = \"") > 0) {
                        dcmNo = strHtmlLine.substring(strHtmlLine.indexOf("node1['dcmNo'] = \"") + 18, strHtmlLine.length() - 2); //추출하기
                        dcmNoExistFlag = false;
                        for (int check = 0; check < returnArr.size(); check++) {//중복 저장 방지를 위한 확인
                            if (dcmNo.equals(returnArr.get(check))) {
                                dcmNoExistFlag = true;
                            }
                        }
                        if (!dcmNoExistFlag) {
                            returnArr.add(dcmNo); //dcmNo 담기
                        }
                        rcpNo = "";
                    }
                }

                if (strHtmlLine.indexOf("node1['rcpNo'] = \"") > 0) {
                    rcpNo = strHtmlLine.substring(strHtmlLine.indexOf("node1['rcpNo'] = \"") + 18, strHtmlLine.length() - 2); //추출하기
                    if (!rcept_no.equals(rcpNo)) {
                        rcpNo = "";
                    }
                } else {
                    rcpNo = "";
                }

                if (rcept_no.equals(rcpNo2)) {
                    if (strHtmlLine.indexOf("node2['dcmNo'] = \"") > 0) {
                        dcmNo = strHtmlLine.substring(strHtmlLine.indexOf("node2['dcmNo'] = \"") + 18, strHtmlLine.length() - 2); //추출하기
                        dcmNoExistFlag = false;
                        for (int check = 0; check < returnArr.size(); check++) {//중복 저장 방지를 위한 확인
                            if (dcmNo.equals(returnArr.get(check))) {
                                dcmNoExistFlag = true;
                            }
                        }
                        if (!dcmNoExistFlag) {
                            returnArr.add(dcmNo); //dcmNo 담기
                        }
                        rcpNo2 = "";

                    }
                }

                if (strHtmlLine.indexOf("node2['rcpNo'] = \"") > 0) {
                    rcpNo2 = strHtmlLine.substring(strHtmlLine.indexOf("node2['rcpNo'] = \"") + 18, strHtmlLine.length() - 2); //추출하기
                    if (!rcept_no.equals(rcpNo2)) {
                        rcpNo2 = "";
                    }
                } else {
                    rcpNo2 = "";
                }
                if (strHtmlLine.indexOf("openPdfDownload('") > 0) {
                    if (strHtmlLine.indexOf("openPdfDownload('") + 17 < strHtmlLine.indexOf("',")) {
                        rcpNo3 = strHtmlLine.substring(strHtmlLine.indexOf("openPdfDownload('") + 17, strHtmlLine.indexOf("',")); //추출하기

                        if (rcept_no.equals(rcpNo3)) {
                            if (strHtmlLine.indexOf("', '") + 4 < strHtmlLine.indexOf("');\">다운로드")) {
                                dcmNo = strHtmlLine.substring(strHtmlLine.indexOf("', '") + 4, strHtmlLine.indexOf("');\">다운로드")); //추출하기
                                dcmNoExistFlag = false;
                                for (int check = 0; check < returnArr.size(); check++) {//중복 저장 방지를 위한 확인
                                    if (dcmNo.equals(returnArr.get(check))) {
                                        dcmNoExistFlag = true;
                                    }
                                }
                                if (!dcmNoExistFlag) {
                                    returnArr.add(dcmNo); //dcmNo 담기
                                }
                            }
                        }
                    }
                }
            }

        } finally {
            if (in != null) {
                try {
                    in.close();
                } catch (IOException e) {
                    log.info(e.getMessage());
                }
            }
            if (out != null) {
                try {
                    out.close();
                } catch (IOException e) {
                    log.info(e.getMessage());
                }
            }
            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException e) {
                    log.info(e.getMessage());
                }
            }

            File delFile = new File(viewerFileNm);
            //이제 필요 없어진 뷰어 html 삭제
            if (delFile.exists()) {
                if (!delFile.delete()) {
                    log.info("파일 삭제 실패: {}", delFile.getPath());
                }
            }
        }
        return returnArr;
    }

    public String saveRptFile(ArrayList<String> dcmNoList, String rcept_no, String pblntf_ty, String pblntf_dtl_ty, String repNm) throws InterruptedException, IOException {

        String zipFileNm = ""; //생성할 압축파일 명

        String[] fileType = new String[3];
        ArrayList<String> arrFileNm = new ArrayList<String>();

        boolean saveFlag = true; //이미 다운로드 받은 파일이 있을 경우 저장하지 않기 위해 사용할 변수

        fileType[0] = "pdf"; //
        fileType[1] = "excel"; //
        fileType[2] = "zip"; //

        //다운로드 받은 파일을 압축하기 전 저장
        File folder = new File(this.attachFilePath + rcept_no + pblntf_ty + pblntf_dtl_ty);

        if (!folder.exists()) {
            if (!folder.mkdir()) {
                log.info("폴더 생성 실패 : {}", folder.getPath());
                return null;
            }
        }

        // pdf, excel 등 파일 다운로드 받기
        for (int i = 0; dcmNoList.size() > i; i++) {
            for (int j = 0; j < fileType.length; j++) {

                String fileURL = "https://dart.fss.or.kr/pdf/download/" + fileType[j] + ".do?rcp_no=" + rcept_no + "&dcm_no=" + dcmNoList.get(i) + "&lang=ko";

                // 끊김방지를 위하여 난수 발생하여 delay
                int min = 1;
                int max = 4;
                int callApiViewerDownDelayRandomTime = (int) ((Math.random() * (max - min) + min)) * 1000;
                Thread.sleep(callApiViewerDownDelayRandomTime);

                URL url = new URL(fileURL);
                HttpURLConnection httpConn = (HttpURLConnection) url.openConnection();
                int responseCode = httpConn.getResponseCode();

                if (responseCode == HttpURLConnection.HTTP_OK) {
                    String fileName = "";
                    String disposition = httpConn.getHeaderField("Content-Disposition");

                    if (disposition != null && disposition.indexOf(".dsd") < 0) { //화면에선 제공하지 않는 .dsd파일이 다운로드 되기도 해서 걸러냄

                        int index = disposition.indexOf("filename=");
                        if (index > 0) {
                            fileName = disposition.substring(index + 9, disposition.length());
                        }

                        if (fileType[j].equals("zip")) {
                            fileName = repNm + ".zip";
                            fileName = fileName.replaceAll(" ", "");
                            fileName = fileName.replaceAll("\"", "");
                            fileName = fileName.replaceAll(";", "");
                            fileName = fileName.replaceAll("/", "");
                            fileName = fileName.replaceAll("\\?", "");
                            fileName = fileName.replaceAll("//", "");
                            fileName = fileName.replaceAll(":", "");
                            fileName = fileName.replaceAll("\\*", "");
                            fileName = fileName.replaceAll("<", "");
                            fileName = fileName.replaceAll(">", "");
                            fileName = fileName.replaceAll("|", "");
                        } else {
                            fileName = fileName.replaceAll("\"", "");
                            fileName = fileName.replaceAll(";", "");
                            fileName = new String(fileName.getBytes("ISO-8859-1"), "EUC-KR");
                        }

                        InputStream inputStream = httpConn.getInputStream();

                        //압축 전 다운로드 받은 파일 저장해두기. 파일명 지정
                        String saveFilePathNm = folder.getPath() + File.separator + fileName;

                        // 이미 저장한 파일이면 생략하기
                        for (int k = 0; k < arrFileNm.size(); k++) {
                            if (saveFilePathNm.equals(arrFileNm.get(k))) {
                                saveFlag = false;
                            }
                        }
                        if (saveFlag) {
                            FileOutputStream outputStream = new FileOutputStream(saveFilePathNm);

                            int bytesRead = -1;
                            byte[] buffer = new byte[1024 * 100];
                            while ((bytesRead = inputStream.read(buffer)) != -1) {
                                outputStream.write(buffer, 0, bytesRead);
                            }

                            outputStream.close();

                            arrFileNm.add(fileName);

                        }
                        inputStream.close();

                    } else {
                        fileName = "";
                    }

                } else {
                    log.info("No file to download. Server replied HTTP code: " + responseCode + "--" + url);
                }
                httpConn.disconnect();
            }
        }
        zipFileNm = rcept_no + "_" + pblntf_ty + "_" + pblntf_dtl_ty + ".zip";

        //파일 압축하기
        //압축할 파일이 있는 디렉토리, 압축한 zip파일을 저장할 디렉토리, 압축zip파일명
        ZipUtil.createZipFile(folder.getPath(), this.attachFilePath, zipFileNm);


        return zipFileNm;
    }


    //공시상세유형
    private String[] getPblntfDetailTy() {
        String[] cd = new String[4];

        cd[0] = "A001"; //	정기공시
        cd[1] = "E001"; //	기타공시
        cd[2] = "F001"; //	외부감사관련
        cd[3] = "I001"; //	거래소공시

        return cd;
    }


}
