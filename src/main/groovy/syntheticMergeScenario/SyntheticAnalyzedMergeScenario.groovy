package syntheticMergeScenario

class SyntheticAnalyzedMergeScenario {

    String project
    int leftTask
    int rightTask
    List<String> leftTesti
    List<String> rightTesti
    List<String> commonFiles
    List<String> commonProductionFiles
    int conflict
    int conflictOfInterest

    List<String> conflictingFiles
    List<String> conflictingFilesOfInterest

    @Override
    String toString() {
        return "project: $project, leftTask:$leftTask, rightTask:$rightTask, conflict:$conflict, " +
                "conflictOfInterest: $conflictOfInterest, leftITest:$leftTesti, rightITest: $rightTesti"
    }

}
