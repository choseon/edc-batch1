package kcs.edc.batch.jobs.opd.opd001m.vo;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
@JsonIgnoreProperties(ignoreUnknown = true)
public class Opd001mVO {

     @JsonProperty
     private String corp_code;

     @JsonProperty
     private String corp_name;

     @JsonProperty
     private String corp_name_eng;

     @JsonProperty
     private String stock_name;

     @JsonProperty
     private String stock_code;

     @JsonProperty
     private String ceo_nm;

     @JsonProperty
     private String corp_cls;

     @JsonProperty
     private String jurir_no;

     @JsonProperty
     private String bizr_no;

     @JsonProperty
     private String adres;

     @JsonProperty
     private String hm_url;

     @JsonProperty
     private String ir_url;

     @JsonProperty
     private String phn_no;

     @JsonProperty
     private String fax_no;

     @JsonProperty
     private String induty_code;

     @JsonProperty
     private String est_dt;

     @JsonProperty
     private String acc_mt;

}
