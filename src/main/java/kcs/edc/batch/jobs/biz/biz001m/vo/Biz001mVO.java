package kcs.edc.batch.jobs.biz.biz001m.vo;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.util.List;

@Getter
@Setter
@JsonIgnoreProperties(ignoreUnknown = true)
public class Biz001mVO {

    private List<Item> jsonArray;

    @Getter
    @Setter
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Item {
        private String pblancId;

        private String pblancNm;

        private String bsnsSumryCn;

        private String jrsdInsttNm;

        private String pldirSportRealmMlsfcCodeNm;

        private String pldirSportRealmLclasCodeNm;

        private String areaNm;

        private String entrprsStle;

        private String industNm;

        private String trgetNm;

        private String reqstBeginEndDe;

        private int inqireCo;

        private String pblancUrl;

        private String rceptEngnNm;

        private String rceptInsttChargerDeptNm;

        private String rceptInsttTelno;

        private String rceptInsttEmailAdres;

        private String creatPnttm;

        private String delYn = "N";

        private String frstRegtId = "EX_BPD";

        private String frstRgsrDtlDttm;

        private String lastChprId = "EX_BDP";

        private String lastChngDtlDttm;
    }


}
