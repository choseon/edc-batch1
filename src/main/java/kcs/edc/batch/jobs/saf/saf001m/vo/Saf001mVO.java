package kcs.edc.batch.jobs.saf.saf001m.vo;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.util.List;

@Getter
@Setter
@ToString
public class Saf001mVO {

    private String resultCode;
    private String resultMsg;

    private List<Item> resultData;

    @Getter
    @Setter
    public static class Item{
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
        private String derivationModels;
        private String certificationImageUrls;
        private String factories;
        private String similarCertifications;
    }


}
