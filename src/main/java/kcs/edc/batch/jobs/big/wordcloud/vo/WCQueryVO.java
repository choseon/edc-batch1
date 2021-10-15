package kcs.edc.batch.jobs.big.wordcloud.vo;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class WCQueryVO {

    private String access_key;
    private Argument argument = new Argument();

    @Getter
    @Setter
    public class Argument {
        private String query;
        private PublishedAt published_at = new PublishedAt();
        private List<String> provider;
        private List<String> category;
        private List<String> category_incident;
        private String byline;
        private List<String> provider_subject;

        @Getter
        @Setter
        public class PublishedAt {
            private String from;
            private String until;
        }
    }
}
