package kcs.edc.batch.jobs.som.som005m.vo;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@JsonIgnoreProperties(ignoreUnknown = true)
public class Som005mVO {

    private String label;
    private int score;
    private int frequency;
    private String categorySetName;
    private List<String> categoryList;
    private List<Item> childList;

    @Getter
    @Setter
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Item {

        /** CLET_DT */
        private String date;

        /** CLET_CHNL_NM */
        private String source;

        /** KCS_REGR_YN */
        private String registYn;

        /** SBJT_WORD_NM */
        private String keyword;

        /** ISSUE_KYWD_NM */
        @JsonProperty
        private String label;

        /** ISSUE_KYWD_NM_FRCNT */
        @JsonProperty
        private int frequency;

        private String delYn = "N";

        private String frstRegstId = "EX_BDP";

        private String frstRgsrDtlDttm;

        private String lastChprId = "EX_BDP";

        private String lastChngDtlDttm;


//        private String subjectKeyword;
//        private String polarity;
//        private String search_keyword;
//        private float score;
//        private List<String> categoryList;
    }
}
