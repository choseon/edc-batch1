package kcs.edc.batch.jobs.big.news.vo;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@JsonIgnoreProperties(ignoreUnknown = true)
public class Big001mVO {

    private int result;
    private ReturnObject return_object;

    @Getter
    @Setter
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class ReturnObject {

        private int total_hits;
        private List<DocumentItem> documents = new ArrayList<>();
    }

    @Getter
    @Setter
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class DocumentItem {

        /**
         * srch_ques_word_nm	검색질의단어명
         **/
        private String srchQuesWordNm;

        /**
         * artc_id	기사ID
         **/
        @JsonProperty("news_id")
        private String artcId;

        /**
         * artc_ttle	기사제목
         **/
        @JsonProperty("title")
        private String artc_ttle;

        /**
         * artc_cn	기사내용
         **/
        @JsonProperty("content")
        private String artcCn;

        /**
         * sumr_cn	요약내용
         **/
        @JsonProperty("hilight")
        private String sumrCn;

        /**
         * artc_pbls_hr	기사발행시간
         **/
        @JsonProperty("published_at")
        private String artcPblsHr;

        /**
         * oxpr_clsf_nm	언론사명
         **/
        private String oxprClsfNm;

        /**
         * oxpr_nm	기자성명
         **/
        @JsonProperty("provider")
        private String oxprNm;

        /**
         * jonl_fnm	언론사분류명
         **/
        @JsonProperty("byline")
        private String jonlFnm;

        /**
         * issue_srwr_yn	이슈검색어여부
         **/
        private String issueSrwrYn;

        /**
         * kcs_rgsr_yn	관세청등록여부
         **/
        private String KcsRgrsYn;

        /**
         * del_yn	삭제여부
         **/
        private String delYn = "N";

        /**
         * frst_regst_id	최초등록자ID
         **/
        private String frstRegstId = "EX_BDP";

        /**
         * frst_rgsr_dtl_dttm	최초등록상세일시
         **/
        private String frstRgsrDtlDttm;

        /**
         * last_chpr_id	최종변경자ID
         **/
        private String lastChprId = "EX_BDP";

        /**
         * last_chng_dtl_dttm	최종변경상세일시
         **/
        private String lastChngDtlDttm;
    }
}
