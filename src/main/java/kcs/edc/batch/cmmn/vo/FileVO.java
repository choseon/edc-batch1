package kcs.edc.batch.cmmn.vo;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.io.File;

@Getter
@Setter
@ToString
public class FileVO {

    /**
     * 파일 루트 경로
     */
    private String fileRootPath;

    /**
     * 파일 디렉토리명
     */
    private String fileDirName;

    /**
     * 파일경로 (파일루트경로 + 파일디렉토리명)
     */
    private String filePath;

    /**
     * 파일명
     */
    private String fileName;

    /**
     * 기본파일명
     */
    private String baseFileName;

    /**
     * 파일 확장자
     */
    private String fileExtension;

    public FileVO(String fileRootPath, String fileDirName, String fileName, String fileExtension) {
        this.fileRootPath = fileRootPath;
        this.fileDirName = fileDirName;
        this.baseFileName = fileName;
        this.fileName = fileName;
        this.fileExtension = fileExtension;
        this.filePath = fileRootPath + fileDirName + "/";
    }

    /**
     * 기본파일명에 추가되는 파일명 셋팅
     *
     * @param appendFileName
     */
    public void setAppendingFileName(String appendFileName) {
        this.fileName = this.baseFileName + "_" + appendFileName;
    }

    /**
     * 기본 파일경로에 추가되는 경로 셋팅
     *
     * @param appendFileFilePath
     */
    public void setAppendingFilePath(String appendFileFilePath) {
        this.filePath = this.filePath + appendFileFilePath + "/";
    }

    /**
     * 파일풀네임 (파일명 + 확장자)
     *
     * @return
     */
    public String getFileFullName() {
        return this.fileName + "." + this.fileExtension;
    }

    /**
     * 파일전체경로 (파일경로 + 파일명 + 확장자)
     *
     * @return
     */
    public String getFullFilePath() {
        return this.filePath + getFileFullName();
    }
}
