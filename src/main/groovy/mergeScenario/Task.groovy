package mergeScenario

import br.ufpe.cin.tas.search.task.merge.MergeTask

class Task {

    String project
    int id
    List testi
    List taski
    List<String> hashes
    String lastHash
    List<String> conflictingFiles
    MergeTask mergeTask

    boolean conflict //não é necessário se não usado o arquivo de merges
    String merge //não é necessário se não usado o arquivo de merges
    String base //não é necessário se não usado o arquivo de merges

}
