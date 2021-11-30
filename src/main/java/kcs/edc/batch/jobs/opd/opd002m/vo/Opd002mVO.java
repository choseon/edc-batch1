package kcs.edc.batch.jobs.opd.opd002m.vo;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

/**
 * 기업공시내역 VO
 */
@Getter
@Setter
@JsonIgnoreProperties(ignoreUnknown = true)
public class Opd002mVO {

    private String status;
    private String message;
    private String page_no;
    private String page_count;
    private String total_count;
    private String total_page;
    private List<Item> list;

    @Getter
    @Setter
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Item {

        /**
         * PBLS_CO_JRPN_TPCD 발행회사법인구분코드
         */
        @JsonProperty
        private String corp_cls; // 법인구분

        /**
         * ENTS_KORE_NM	업체한글명
         */
        @JsonProperty
        private String corp_name; // 종목명(법인명)

        /**
         * STREX_CD 거래소코드
         */
        @JsonProperty
        private String stock_code; // 종목코드

        /**
         * RPDC_NM 보고서명
         */
        @JsonProperty
        private String report_nm; // 보고서명

        /**
         * RPDC_ACAP_NO 보고서 접수번호
         */
        @JsonProperty
        private String rcept_no; // 접수번호

        /**
         * PRER_NM 제출자명
         */
        @JsonProperty
        private String flr_nm; // 공시 제출인명

        /**
         * ACAP_DT 접수일자
         */
        @JsonProperty
        private String rcept_dt; // 접수일자(YYYYMMDD)

        /**
         * RMRK_CN 비고내용
         */
        @JsonProperty
        private String rm; // 비고

        /************************************************************************/

        /**
         * PBLNTF_TY 공시유형코드
         */
        private String pblntf_ty;

        /**
         * PBLNTF_DETAIL_TY 공시유형상세코드
         */
        private String pblntf_detail_ty;

        /**
         * FILE_PATH_NM 파일경로명
         */
        private String file_path_nm;

        /**
         * RCPN_FILE_PATH_NM 수신파일경로명
         */
        private String rcpn_file_path_nm;

        /**
         * SRBK_FILE_NM 원본파일명
         */
        private String srbk_file_nm;

        /**
         * SORG_FILE_NM 저장파일명
         */
        private String sorg_file_nm;

        /**
         * CLET_FILE_CRTN_DT 수집파일생성일자
         */
        private String cletFileCrtnDt;
    }

}

