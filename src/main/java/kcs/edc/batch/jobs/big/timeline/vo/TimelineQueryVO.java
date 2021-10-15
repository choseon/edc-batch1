package kcs.edc.batch.jobs.big.timeline.vo;

import lombok.Data;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
public class TimelineQueryVO {

    private String access_key;
    private Argument argument = new Argument();

    @Getter
    @Setter
    public static class Argument {
        private String query;
        private PublishedAtVO published_at = new PublishedAtVO();
        private List<String> provider = new ArrayList<>();
        private List<String> category;
        private List<String> category_incident;
        private String byline;
        private List<String> provider_subject;
        private String interval = "day";

        @Getter
        @Setter
        public static class PublishedAtVO {
            private String from;
            private String until;
        }
    }
}
