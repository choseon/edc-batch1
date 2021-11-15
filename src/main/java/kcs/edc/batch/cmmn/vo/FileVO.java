package kcs.edc.batch.cmmn.vo;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
public class FileVO {

    private String fileRootPath;

    private String fileDirName;

    private String filePath;

    private String fileName;

    private String prefixFileName;

    private String suffixFileName;

    public FileVO(String fileRootPath, String fileDirName, String fileName) {
        this.fileRootPath = fileRootPath;
        this.fileDirName = fileDirName;
        this.fileName = fileName;
    }
}
