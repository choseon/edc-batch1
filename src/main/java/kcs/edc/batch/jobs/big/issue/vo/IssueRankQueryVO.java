package kcs.edc.batch.jobs.big.issue.vo;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class IssueRankQueryVO {

    private String access_key;
    private Argument argument = new Argument();

    @Getter
    @Setter
    public class Argument {
        private String date;
        private List<String> provider;

    }
}
