package kcs.edc.batch.jobs.saf.saf001m.vo;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.util.List;

@Getter
@Setter
@JsonIgnoreProperties(ignoreUnknown = true)
public class Saf001mVO {

    private String resultCode;
    private String resultMsg;

    private List<Item> resultData;

    @Getter
    @Setter
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Item{
        private String certUid;

        private String certOrganName;

        private String certNum;

        private String certState;

        private String certDate;

        private String certDiv;

        private String productName;

        private String brandName;

        private String modelName;

        private String categoryName;

        private String importDiv;

        private String makerName;

        private String makerCntryName;

        private String importerName;

        private String firstCertNum;

        private String remark;

        private String signDate;

        private String delYn = "N";

        private String frstRegstId = "EX_BDP";

        private String frstRgsrDtlDttm;

        private String lastChprId = "EX_BDP";

        private String lastChngDtlDttm;


//        private String certChgDate;
//        private String certChgReason;
//        private String derivationModels;
//        private String certificationImageUrls;
//        private String factories;
//        private String similarCertifications;
    }


}
