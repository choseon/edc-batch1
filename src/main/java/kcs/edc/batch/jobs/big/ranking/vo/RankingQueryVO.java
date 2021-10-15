package kcs.edc.batch.jobs.big.ranking.vo;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class RankingQueryVO {

    private String access_key;
    private Argument argument = new Argument();

    @Getter
    @Setter
    public class Argument {
        private String from;
        private String until;
        private int offset = 100;
        private String target_access_key;
    }
}
