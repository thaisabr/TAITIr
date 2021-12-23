package mergeScenario

class AnalyzedMergeScenario {

    String project
    String merge
    String base
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
        return "project: $project, merge: $merge, base:$base, leftTask:$leftTask, rightTask:$rightTask, " +
                "conflict:$conflict, conflictOfInterest: $conflictOfInterest, leftTestI:$leftTesti, rightTestI: $rightTesti"
    }

}
