package kcs.edc.batch.cmmn.property;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.List;
import java.util.Map;
import java.util.Set;

@Getter
@Setter
@Configuration
@ConfigurationProperties(prefix = "file")
public class FileProperties {

    private String resourcePath;

    private String dataPath;

    private String logPath;

    private String tempDirName;

    private String prefixTableName;

    private String logTableName;

    private String fileExtension;

    private Map<String, String> attachPath;

    private String getJobAttachPath(String jobId) {
        return (attachPath.containsKey(jobId)) ? attachPath.get(jobId) : null;
    }

//    private String resourcePath;
//
//    private List<String> portalJobGroupIdList;
//
//    private Map<String, FileProp> destination;
////    private List<FileProp> destination;
//
//    @Getter
//    @Setter
//    public static class FileProp {
//
////        private String name;
////        private String rootPath;
////        private Map<String, Map<String, String>> rootPath;
//        private Map<String, String> rootPath;
//
//
//        private String attachedFileDir;
//
//        private String dataFileDir;
//
//        private String logPath;
//
//        private String tempPath;
//
//        private String prefixTableName;
//
//        private String logTableName;
//
//        private String fileExtension;
//    }
//
//    public FileProp getFileProp(String jobGroupId) {
//
//        if(isPortalJobGroup(jobGroupId)) { // portal
//            return destination.get("portal");
//        } else { // hive
//            return this.destination.get("hive");
//        }
//    }
//
//    public Boolean isPortalJobGroup(String jobGroupId) {
//
//        for (String grpNm : this.portalJobGroupIdList) {
//            if(grpNm.equals(jobGroupId)) {
//                return true;
//            }
//        }
//        return false;
//    }


}
