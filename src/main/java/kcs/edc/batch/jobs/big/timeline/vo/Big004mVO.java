package kcs.edc.batch.jobs.big.timeline.vo;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@JsonIgnoreProperties(ignoreUnknown = true)
public class Big004mVO {

    private int result;
    private ReturnObjectVO return_object;

    @Getter
    @Setter
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class ReturnObjectVO {
        private int total_hists;
        private List<TimeLineItem> time_line = new ArrayList<>();
    }

    @Getter
    @Setter
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class TimeLineItem {

        private String srchQuesWordNm;

        @JsonProperty("label")
        private String artcPblsDt;

        @JsonProperty("hits")
        private String srchDocGcnt;

        private String KcsRgrsYn;

        private String delYn = "N";

        private String frstRegstId = "EX_BDP";

        private String frstRgsrDtlDttm;

        private String lastChprId = "EX_BDP";

        private String lastChngDtlDttm;

    }

}
