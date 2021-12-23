package conflictAnalysis

import br.ufpe.cin.tan.util.CsvUtil

class ConflictManagerBasedOnIntersection {

    List<String[]> conflicts
    //List<String[]> controllerConflicts

    ConflictManagerBasedOnIntersection(){
        conflicts = CsvUtil.read("output${File.separator}conflict.csv")
        //controllerConflicts = CsvUtil.read("output${File.separator}conflict-controller.csv")
    }

    def collectInfo(){
        collectInfoFromConflicts(conflicts, "output${File.separator}conflict.csv")
        //collectInfoFromConflicts(controllerConflicts, "output${File.separator}conflict-controller.csv")
    }

    private static collectInfoFromConflicts(List<String[]> conflicts, String outputFile){
        if(conflicts.empty) return
        List<String[]> conflictsUpdated = []
        def newHeader = Constants.CONFLICT_INTERSECTION_FILE_HEADER_EXTRA as String[]
        conflictsUpdated.add((conflicts.get(0)+newHeader) as String[])
        conflicts.subList(1, conflicts.size()).each{ conflict ->
            def leftTesti = conflict[Constants.LEFT_TESTI_INDEX]
                    .substring(1, conflict[Constants.LEFT_TESTI_INDEX].size()-1).tokenize(",")*.trim()
            def rightTesti = conflict[Constants.RIGHT_TESTI_INDEX]
                    .substring(1, conflict[Constants.RIGHT_TESTI_INDEX].size()-1).tokenize(",")*.trim()
            def intersectionTesti = leftTesti.intersect(rightTesti)

            if(conflict[Constants.CONFLICT_OF_INTEREST_INDEX]=="1"){ //houve conflito
                def conflictFiles = conflict[Constants.CONFLICTING_FILES_OF_INTEREST_INDEX]
                        .substring(1, conflict[Constants.CONFLICTING_FILES_OF_INTEREST_INDEX].size()-1).tokenize(",")*.trim()

                def diffConflictsLeftTesti = conflictFiles - leftTesti
                def diffConflictsLeftTestiSize = diffConflictsLeftTesti.size()
                def diffConflictsRightTesti = conflictFiles - rightTesti
                def diffConflictsRightTestiSize = diffConflictsRightTesti.size()
                def intersectionConflictsLeftTesti = conflictFiles.intersect(leftTesti)
                def intersectionConflictsLeftTestiSize = intersectionConflictsLeftTesti.size()
                def intersectionConflictsRightTesti = conflictFiles.intersect(rightTesti)
                def intersectionConflictsRightTestiSize = intersectionConflictsRightTesti.size()

                def result = [conflictFiles.size(),
                              diffConflictsLeftTestiSize, diffConflictsLeftTesti,
                              diffConflictsRightTestiSize, diffConflictsRightTesti,
                              intersectionConflictsLeftTestiSize, intersectionConflictsLeftTesti,
                              intersectionConflictsRightTestiSize, intersectionConflictsRightTesti,
                              intersectionTesti.size(), intersectionTesti
                ] as String[]
                conflictsUpdated.add((conflict + result) as String[])
            } else {
                def result = [0, 0, [], 0, [], 0, [], 0, [], intersectionTesti.size(), intersectionTesti] as String[]
                conflictsUpdated.add((conflict + result) as String[])
            }
        }
        CsvUtil.write(outputFile, conflictsUpdated)
    }

}
