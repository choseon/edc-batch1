package kcs.edc.batch.jobs.saf.saf001l.vo;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

@Getter @Setter
@JsonIgnoreProperties(ignoreUnknown = true)
public class Saf001lVO {

    private String resultCode;
    private String resultMsg;

    private Item resultData;

    @Getter @Setter
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Item {

        private String certUid;
        private String certOrganName;
        private String certNum;
        private String certState;
        private String certDiv;
        private String certDate;
        private String certChgDate;
        private String certChgReason;
        private String firstCertNum;
        private String productName;
        private String brandName;
        private String modelName;
        private String categoryName;
        private String importDiv;
        private String makerName;
        private String makerCntryName;
        private String importerName;
        private String remark;
        private String signDate;
        private List<String> derivationModels = new ArrayList<>();
        private List<String> certificationImageUrls = new ArrayList<>();
        private List<FatoryItem> factories = new ArrayList<>();
        private List<SimilarCertItem> similarCertifications = new ArrayList<>();

    }

    @Getter @Setter
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class DerivationModelItem {

        private String certInfoId;

        @JsonProperty
        private String model;

        private String regrDt;
        private String delYn = "N";
        private String frstRegstId = "EX_BDP";
        private String frstRgsrDtlDttm;
        private String lastChprId = "EX_BDP";
        private String lastChngDtlDttm;
    }

    @Getter @Setter
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class SimilarCertItem {

        private String certInfoId;

        @JsonProperty
        private String certUid;

        @JsonProperty
        private String certNum;

        @JsonProperty
        private String certState;
//        private String certDiv;
//        private String certDate;
//        private String certChgDate;
//        private String certChgReason;
//        private String firstCertNum;
//        private String productName;
//        private String brandName;
//        private String modelName;
//        private String categoryName;
//        private String importDiv;
//        private String makerName;
//        private String makerCntryName;
//        private String importerName;
//        private String remark;
//        private String signDate;
//        private String derivationModels;
//        private String certificationImageUrls;
//        private String factories;
//        private String similarCertifications;
        private String regrDt;
        private String delYn = "N";
        private String frstRegstId = "EX_BDP";
        private String frstRgsrDtlDttm;
        private String lastChprId = "EX_BDP";
        private String lastChngDtlDttm;
    }

    @Getter @Setter
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class FatoryItem {

        private String certInfoId;

        @JsonProperty
        private String makerName;

        @JsonProperty
        private String makerCntryName;

        private String regrDt;
        private String delYn = "N";
        private String frstRegstId = "EX_BDP";
        private String frstRgsrDtlDttm;
        private String lastChprId = "EX_BDP";
        private String lastChngDtlDttm;
    }

    @Getter @Setter
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class CertificationImageUrlItem {

        private String certInfoId;

        @JsonProperty
        private String imageUrl;

        private String crtfImageUrl;
        private String crtfIamgeFileCn;
        private String regrDt;
        private String delYn = "N";
        private String frstRegstId = "EX_BDP";
        private String frstRgsrDtlDttm;
        private String lastChprId = "EX_BDP";
        private String lastChngDtlDttm;
    }





}
