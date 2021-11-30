package kcs.edc.batch.jobs.kot.kot001m.vo;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.util.List;

@Getter
@Setter
@ToString
@JsonIgnoreProperties(ignoreUnknown = true)
public class Kot001mVO {

    private String pageNo;
    private String resultCode;
    private String totalCount;

    private List<Item> items;

    private String numOfRows;
    private String resultMsg;

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Item {

        /**
         * TRSN_PRCS_RSCD	전송처리결과코드
         */
        private String resultCode;

        /**
         * TRSN_PRCS_RSLT_MSG_CN	전송처리결과메시지내용
         **/
        private String resultMsg;

        /**
         * PRPG_LST_GCNT	페이지당목록개수
         **/
        private String numOfRows;

        /**
         * PGE_ORDR	페이지순서
         **/
        private String pageNo;

        /**
         * NTAR_LST_GCNT	게시물목록개수
         **/
        private String totalCount;

        /**
         * TWBS_SRNO	게시글일련번호
         **/
        @JsonProperty
        private String bbstxSn; // 게시글 일련번호

        /**
         * TWBS_TTLE	게시글제목
         **/
        @JsonProperty
        private String newsTitl; // 뉴스제목

        /**
         * FILE_PATH_NM	파일경로명
         **/
        @JsonProperty
        private String newsBdt; // 뉴스본문

        /**
         * MAKE_DTTM	작성일시
         **/
        @JsonProperty
        private String newsWrtDt; // 뉴스 작성일시

        /**
         * MDPN_NM	작성자명
         **/
        @JsonProperty
        private String WrterNm; // 뉴스작성자명

        /**
         * INFO_OFR_NM	정보제공명
         **/
        @JsonProperty
        private String infoCl; // 정보문류

        /**
         * NTAR_QRY_CNT	게시물조회건수
         **/
        @JsonProperty
        private String inqreCnt; // 조회수

        /**
         * SUMR_CN	요약내용
         **/
        @JsonProperty
        private String cntntSumar; // 내용요약

        /**
         * JOB_CLSF_NM	직업분류명
         **/
        @JsonProperty
        private String jobSeNm;// 일자리구분명

        /**
         * KYWD_CN	키워드내용
         **/
        @JsonProperty
        private String kwrd; // 키워드

        /**
         * CNTN_KND_NM	대륙종류명
         **/
        @JsonProperty
        private String regn; // 지역

        /**
         * CNTY_NM	국가명
         **/
        @JsonProperty
        private String natn; // 국가

        /**
         * OVRS_PROT_FCLT_NM	해외생산시설명
         **/
        @JsonProperty
        private String OvrofInfo; // 무역관정보

        /**
         * INTP_CLSF_NM	업종분류명
         **/
        @JsonProperty
        private String indstCl; // 산업분류

        /**
         * HS_SGN_VAL	HS부호값
         **/
        @JsonProperty
        private String hsCdNm; // HS코드명

        /**
         * PRLST_NM	품목명
         **/
        @JsonProperty
        private String cmdItNmKorn; // 품목한글명

        /**
         * ENGL_PRLST_NM	영문품목명
         **/
        @JsonProperty
        private String cmdItNmEng; // 품목영문명

        /**
         * LNK_TRGT_URL	링크대상URL
         **/
        @JsonProperty
        private String crrspOvrofLink; // 해당무역관링크

        /**
         * CNSL_ACAP_LNK_NM	상담접수링크명
         **/
        @JsonProperty
        private String inquryItmLink; // 문의사항링크

        /**
         * RGSR_DTTM	등록일시
         **/
        @JsonProperty
        private String regDt; // 등록일시

        /**
         * ALTT_DTTM	수정일시
         **/
        @JsonProperty
        private String updDt; // 수정일시

        /**
         * CLET_DT	수집일자
         **/
        private String CletFileCrtnDt; // 수집파일생성일자
    }

}
