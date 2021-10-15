package kcs.edc.batch.jobs.som.som001m.vo;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@JsonIgnoreProperties(ignoreUnknown = true)
public class Som001mVO {

    /** CLET_DT */
    private String date;

    /** CLET_CHNL_NM */
    private String source;

    /** SBJT_WORD_NM */
    private String keyword;

    /** SBJT_WORD_FRCNT */
    private int frequency;

    /** KCS_RGSR_YN */
    private String registYn;

    private String delYn = "N";

    private String frstRegstId = "EX_BDP";

    private String frstRgsrDtlDttm;

    private String lastChprId = "EX_BDP";

    private String lastChngDtlDttm;
}
