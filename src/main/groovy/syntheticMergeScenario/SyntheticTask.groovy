package syntheticMergeScenario

import br.ufpe.cin.tas.search.task.merge.MergeTask

class SyntheticTask {

    String project
    int id
    List testi
    List taski
    List<String> hashes
    String lastHash
    List<String> conflictingFiles
    MergeTask mergeTask

}
