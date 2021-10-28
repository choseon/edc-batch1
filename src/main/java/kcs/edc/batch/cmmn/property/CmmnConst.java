package kcs.edc.batch.cmmn.property;

public class CmmnConst {

    /***********************************************************************************
     * JOB GROUP ID
     ***********************************************************************************/
    public static final String JOB_GRP_ID_BIZ = "biz";
    public static final String JOB_GRP_ID_BIG = "big";
    public static final String JOB_GRP_ID_SOM = "som";
    public static final String JOB_GRP_ID_SAF = "saf";
    public static final String JOB_GRP_ID_EBA = "eag";
    public static final String JOB_GRP_ID_NAV = "nav";
    public static final String JOB_GRP_ID_OPD = "opd";
    public static final String JOB_GRP_ID_KOT = "kot";
    public static final String JOB_GRP_ID_UCT = "uct";


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

    public static final String JOB_ID_OPD001M = "opd001m";
    public static final String JOB_ID_OPD002M = "opd002m";
    public static final String JOB_ID_OPD003M = "opd003m";

    public static final String JOB_ID_KOT001M = "kot001m";
    public static final String JOB_ID_KOT002M = "kot002m";

    public static final String JOB_ID_UCT001M = "uct001m";

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

    public static final int JOB_GRID_SIZE = 10;
    public static final int JOB_POOL_SIZE = 10;

    public static final int MAKE_FILE_SUCCESS = 0;
    public static final int MAKE_FILE_FAIL = 1;

    /***********************************************************************************
     * FILE RESOURCE INFO
     ***********************************************************************************/
    public static final String RESOURCE_FILE_NAME_SOM_KCS_KEWORD = "som_kcs_keyword.txt";
    public static final String RESOURCE_FILE_NAME_UCT_AREA = "uct_area.txt";
    public static final String RESOURCE_FILE_NAME_KCS_KEYWORD = "kcs_keyword.txt";

    /***********************************************************************************
     * FILE RESOURCE INFO
     ***********************************************************************************/
    public static final String CMMN_FILE_ACTION_TYPE_MERGE = "merge";
    public static final String CMMN_FILE_ACTION_TYPE_CLEAN = "clean";
}
