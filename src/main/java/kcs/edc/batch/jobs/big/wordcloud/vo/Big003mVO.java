package kcs.edc.batch.jobs.big.wordcloud.vo;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@JsonIgnoreProperties(ignoreUnknown = true)
public class Big003mVO {

    private String from;
    private String until;
    private String keyword;
    private int result;
    private ReturnObject return_object;

    @Getter
    @Setter
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class ReturnObject {
        List<NodeItem> nodes = new ArrayList<>();
    }

    @Getter
    @Setter
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class NodeItem {

        private String artcPblsDt;

        private String srchQuesWordNm;

        @JsonProperty("name")
        private String rltnWordNm;

        @JsonProperty("weight")
        private double rltnWordScr;

        private String KcsRgrsYn;

        private String delYn = "N";

        private String frstRegstId = "EX_BDP";

        private String frstRgsrDtlDttm;

        private String lastChprId = "EX_BDP";

        private String lastChngDtlDttm;
    }
}
