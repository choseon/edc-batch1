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

    private String fullFilePath;

    private String baseFileName;

    private String fileExtension;

    public FileVO(String fileRootPath, String fileDirName, String fileName, String fileExtension) {
        this.fileRootPath = fileRootPath;
        this.fileDirName = fileDirName;
        this.baseFileName = fileName;
        this.fileName = fileName;
        this.fileExtension = fileExtension;
        this.filePath = fileRootPath + fileDirName + "/";
    }

    public void setAppendingFileName(String suffixFileName) {
        this.fileName = this.baseFileName + "_" + suffixFileName;
    }

    public String getFileName() {
        return this.fileName + "." + this.fileExtension;
    }

    public String getFullFilePath() {
        return this.filePath + getFileName();
    }
}
