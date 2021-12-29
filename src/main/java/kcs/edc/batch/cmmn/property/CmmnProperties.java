package kcs.edc.batch.cmmn.property;

public class CmmnProperties {

    /***********************************************************************************
     * JOB GROUP ID
     ***********************************************************************************/
    public static final String JOB_GRP_ID_BIZ = "biz";
    public static final String JOB_GRP_ID_BIG = "big";
    public static final String JOB_GRP_ID_SOM = "som";
    public static final String JOB_GRP_ID_SAF = "saf";
    public static final String JOB_GRP_ID_EBA = "eba";
    public static final String JOB_GRP_ID_NAV = "nav";
    public static final String JOB_GRP_ID_OPD = "opd";
    public static final String JOB_GRP_ID_KOT = "kot";
    public static final String JOB_GRP_ID_UCT = "uct";
    public static final String JOB_GRP_ID_COM = "com";


    /***********************************************************************************
     * JOB ID
     ***********************************************************************************/
    public static final String JOB_ID_BIZ001M = "biz001m";

    public static final String JOB_ID_BIG001M = "big001m";
    public static final String JOB_ID_BIG002M = "big002m";
    public static final String JOB_ID_BIG003M = "big003m";
    public static final String JOB_ID_BIG004M = "big004m";

    public static final String JOB_ID_SOM001M = "som001m";
    public static final String JOB_ID_SOM002M = "som002m";
    public static final String JOB_ID_SOM003M = "som003m";
    public static final String JOB_ID_SOM004M = "som004m";
    public static final String JOB_ID_SOM005M = "som005m";

    public static final String JOB_ID_SAF001M = "saf001m";
    public static final String JOB_ID_SAF001L = "saf001l";
    public static final String JOB_ID_SAF002L = "saf002l";
    public static final String JOB_ID_SAF003L = "saf003l";
    public static final String JOB_ID_SAF004L = "saf004l";

    public static final String JOB_ID_EBA001M = "eba001m";

    public static final String JOB_ID_NAV001M = "nav001m";
    public static final String JOB_ID_NAV021M = "nav002m";
    public static final String JOB_ID_NAV003M = "nav003m";
    public static final String JOB_ID_NAV004M = "nav004m";

    public static final String JOB_ID_UCT001M = "uct001m";

    public static final String JOB_ID_OPD001M = "opd001m";
    public static final String JOB_ID_OPD002M = "opd002m";

    public static final String JOB_ID_KOT001M = "kot001m";
    public static final String JOB_ID_KOT002M = "kot002m";


    /***********************************************************************************
     * JOB CONFIGRATION POST FIX
     ***********************************************************************************/
    public static final String POST_FIX_JOB = "Job";
    public static final String POST_FIX_STEP = "Step";
    public static final String POST_FIX_PARTITION_STEP = "PartitionStep";
    public static final String POST_FIX_FLOW = "Flow";
    public static final String POST_FIX_FILE_STEP = "FileStep";
    public static final String POST_FIX_FILE_MERGE_STEP = "FileMergeStep";
    public static final String POST_FIX_FILE_CLEAN_STEP = "FileCleanStep";

    /***********************************************************************************
     * FILE RESOURCE INFO
     ***********************************************************************************/
    public static final String RESOURCE_FILE_NAME_SOM_KCS_KEWORD = "som_kcs_keyword.txt";
    public static final String RESOURCE_FILE_NAME_UCT_AREA = "uct_area.txt";
    public static final String RESOURCE_FILE_NAME_KCS_KEYWORD = "big_kcs_keyword.txt";
    public static final String RESOURCE_FILE_NAME_UCT_SCRIPT = "uct_file_merge_script.sh";
    public static final String RESOURCE_FILE_NAME_KOT_SCRIPT = "kot_img_download.sh";

    /***********************************************************************************
     * FILE ACTION TYPE
     ***********************************************************************************/
    public static final String CMMN_FILE_ACTION_TYPE_MERGE = "FILE_MERGE";
    public static final String CMMN_FILE_ACTION_TYPE_CLEAN = "FILE_CLEAN";
    public static final String CMMN_FILE_ACTION_TYPE_BACKUP_CLEAN = "FILE_BACKUP_CLEAN";

    /***********************************************************************************
     * LOG_FILE INFO
     ***********************************************************************************/
    public static final String LOG_FILE_STEP = "EXT_FILE_CREATE";
    public static final String LOG_JOB_STAT_SUCCEEDED = "Succeeded";
    public static final String LOG_JOB_STAT_FAIL = "Fail";

    /***********************************************************************************
     * SCHEDULER CYCLE
     ***********************************************************************************/
    public static final String SCHEDULER_CYCLE_MONTHLY = "monthly";
    public static final String SCHEDULER_CYCLE_WEEKLY = "weekly";
    public static final String SCHEDULER_CYCLE_DAILY = "daily";

}

