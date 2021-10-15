package kcs.edc.batch.jobs.big.ranking.vo;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@JsonIgnoreProperties(ignoreUnknown = true)
public class Big005mVO {

    private int result;
    private ReturnObjectVO return_object;

    @Getter
    @Setter
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class ReturnObjectVO {
        private List<QueryItem> queries = new ArrayList<>();
    }

    @Getter
    @Setter
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class QueryItem {

        @JsonProperty("query")
        private String srchQuesWordNm;

        @JsonProperty("date")
        private String artcPblsDt;

        @JsonProperty("count")
        private String srchDocGcnt;

        private String KcsRgrsYn;

        private String delYn = "N";

        private String frstRegstId = "EX_BDP";

        private String frstRgsrDtlDttm;

        private String lastChprId = "EX_BDP";

        private String lastChngDtlDttm;
    }
}
