package kcs.edc.batch.jobs.uct.uct001m.vo;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;

import java.util.List;
import java.util.Map;

@Getter
@Setter
@JsonIgnoreProperties(ignoreUnknown = true)
public class Uct001mVO {

    private Validation validation;

    private List<Item> dataset;

    @Getter
    @Setter
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Validation {
        private Map<String, Object> status;
        private String message;
        private Map<String, Object> count;
        private Map<String, Object> datasetTimer;
    }

    @Getter
    @Setter
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Item {

        //        @JsonProperty("IMEX_DCLR_YY")
        private String yr; // 년도


        private String rtCode; // 보고국가 숫자코드


        private String rt3ISO; // 보고국가 영문코드


        private String rtTitle; // 보고국가 영문명


        private String ptCode; // 상대국가 숫자코드


        private String pt3ISO; // 상대국가 영문코드


        private String ptTitle; // 상대국가 영문명


        private String cmdCode; // HS단위부호


        private String cmdDescE; // HS단위부호 영문명


        private String netWeight; // 순중량


        private String tradeValue; // 미화가격


        private String CletFileCrtnDt; // 수집파일생성일자
    }

}
