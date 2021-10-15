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

        private String srchQuesWordNm;

        @JsonProperty("news_id")
        private String artcId;

        @JsonProperty("title")
        private String artc_ttle;

        @JsonProperty("content")
        private String artcCn;

        @JsonProperty("hilight")
        private String sumrCn;

        @JsonProperty("published_at")
        private String artcPblsHr;

        private String oxprClsfNm;

        @JsonProperty("provider")
        private String oxprNm;

        @JsonProperty("byline")
        private String jonlFnm;

        private String issueSrwrYn;

        private String KcsRgrsYn;

        private String delYn = "N";

        private String frstRegstId = "EX_BDP";

        private String frstRgsrDtlDttm;

        private String lastChprId = "EX_BDP";

        private String lastChngDtlDttm;
    }
}
