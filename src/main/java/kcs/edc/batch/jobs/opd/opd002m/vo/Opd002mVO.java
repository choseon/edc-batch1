package kcs.edc.batch.jobs.opd.opd002m.vo;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@JsonIgnoreProperties(ignoreUnknown = true)
public class Opd002mVO {

    private String status;
    private String message;
    private String page_no;
    private String page_count;
    private String total_count;
    private String total_page;
    private List<Item> list;

    @Getter
    @Setter
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Item {

        private String corp_cls;
        private String corp_name;
//        private String corp_code;
        private String stock_code;
        private String report_nm;
        private String rcept_no;
        private String flr_nm;
        private String rcept_dt;
        private String rm;

        private String pblntf_ty;
        private String pblntf_detail_ty;
        private String file_path_nm;
        private String rcpn_file_path_nm;
        private String srbk_file_nm;
        private String sorg_file_nm;
    }

}

