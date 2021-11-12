package kcs.edc.batch.cmmn.vo;

public class FileVO {

    private String fileRootPath;

    private String fileDirName;

    private String filePath;

    private String fileName;

    public FileVO(String fileRootPath, String fileDirName, String fileName) {
        this.fileRootPath = fileRootPath;
        this.fileDirName = fileDirName;
        this.fileName = fileName;
    }
}
