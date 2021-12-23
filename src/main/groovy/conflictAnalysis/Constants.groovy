package conflictAnalysis

class Constants {

    static final String RELEVANT_SUFIX = "-relevant.csv"
    static final String RELEVANT_CONTROLLER_SUFIX = "-relevant-controller.csv"
    static final int LIMIT = 100000

    //tasks input file with merge information
    static final URL_MERGE_SCENARIO_INDEX = 0
    static final TASK_MERGE_SCENARIO_INDEX = 1
    static final LAST_MERGE_SCENARIO_INDEX = 6
    static final MERGE_SCENARIO_INDEX = 7
    static final BASE_SCENARIO_INDEX = 8

    //analyzed tasks file
    static final TASK_INDEX = 0
    static final HASHES_INDEX = 4
    static final TESTI_INDEX = 9
    static final TASKI_INDEX = 10

    //conflict result file
    static final PROJECT_INDEX = 0
    static final LEFT_ID_INDEX = 1
    static final RIGHT_ID_INDEX = 2
    static final CONFLICT_INDEX = 3
    static final CONFLICTING_FILES_INDEX = 4
    static final CONFLICT_OF_INTEREST_INDEX = 5
    static final CONFLICTING_FILES_OF_INTEREST_INDEX = 6
    static final LEFT_TESTI_INDEX = 7
    static final RIGHT_TESTI_INDEX = 8
    static final CONFLICT_FILE_HEADER = ["PROJECT", "LEFT_TASK", "RIGHT_TASK", "CONFLICT", "CONFLICTING_FILES",
                                         "CONFLICT_OF_INTEREST", "CONFLICTING_FILES_OF_INTEREST",
                                         "TESTIL", "TESTIR",
                                         "#COMMON_CHANGED_FILES", "COMMON_CHANGED_FILES",
                                         "#COMMON_CHANGED_PROD_FILES", "COMMON_CHANGED_PROD_FILES"]
    static final CONFLICT_FILE_HEADER_EXTRA = ["#CONFLICTS",
                                               "#DIFF_CONFLICTS_TESTIL", "DIFF_CONFLICTS_TESTIL",
                                               "#DIFF_CONFLICTS_TESTIR", "DIFF_CONFLICTS_TESTIR",
                                               "#INTERSECTION_CONFLICTS_TESTIL", "INTERSECTION_CONFLICTS_TESTIL",
                                               "#INTERSECTION_CONFLICTS_TESTIR", "INTERSECTION_CONFLICTS_TESTIR",
                                               "SIMJAC_TESTIL_TESTIR", "SIMCOS_TESTIL_TESTIR",
                                               "SIMJAC_TESTIL_CONFLICTS", "SIMCOS_TESTIL_CONFLICTS",
                                               "SIMJAC_TESTIR_CONFLICTS", "SIMCOS_TESTIR_CONFLICTS"]
    static final CONFLICT_INTERSECTION_FILE_HEADER_EXTRA = ["#CONFLICTS",
                                               "#DIFF_CONFLICTS_TESTIL", "DIFF_CONFLICTS_TESTIL",
                                               "#DIFF_CONFLICTS_TESTIR", "DIFF_CONFLICTS_TESTIR",
                                               "#INTERSECTION_CONFLICTS_TESTIL", "INTERSECTION_CONFLICTS_TESTIL",
                                               "#INTERSECTION_CONFLICTS_TESTIR", "INTERSECTION_CONFLICTS_TESTIR",
                                               "INTERSECTION_SIZE", "INTERSECTION_TESTIL_TESTIR"]

}
