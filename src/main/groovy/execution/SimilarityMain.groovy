package execution

import br.ufpe.cin.tan.util.CsvUtil
import br.ufpe.cin.tan.util.Util
import conflictAnalysis.ConflictManagerBasedOnSimilarity
import mergeScenario.ConflictAnalyzer
import mergeScenario.JoinCsv

class SimilarityMain {

    /* passo-a-passo:
    * 1 - gerar os resultados de análise por projeto
    * 2 - rodar script no projeto utility (src\main\groovy\input\JoinCsv.groovy),
    *     colocando o resultado do passo 1 na pasta temp
    * 3 - o script precisa de uma intervenção manual para agrupar os resultados de projetos com múltiplos csv
    * 4 - o resultado final são 3 csv por projeto (_candidates, -relevant e relevant-controller), que deverão
    * ser copiados para a pasta tasks desse projeto aqui
    * 5 - em seguida, rodar o main e pegar o resultado na pasta output */

    static void main(String[] args){
        def taskFiles = Util.findFilesFromDirectory("tasks").reverse().findAll{
            !it.contains(File.separator+"taskfile"+File.separator) &&
                    !it.contains(File.separator+"pairs"+File.separator)
        }

        taskFiles.each{ taskFile ->
            ConflictAnalyzer analyzer = new ConflictAnalyzer(taskFile)
            analyzer.analyze()
        }

        def obj = new JoinCsv("output")
        obj.joinAllInDefaultFolders()

        ConflictManagerBasedOnSimilarity conflictManager = new ConflictManagerBasedOnSimilarity()
        conflictManager.collectInfo()

        showResult("output${File.separator}conflict.csv")
        showResult("output${File.separator}conflict-controller.csv")
        showErrorResult("output${File.separator}conflict-error.csv")
        showErrorResult("output${File.separator}conflict-controller-error.csv")

    }

    static showResult(String csv){
        println "File: ${csv}"
        def lines = CsvUtil.read(csv)
        lines.remove(0)
        println "All integrations: " + lines.size()
        def noconflicts = lines.findAll{ it[3]=="0" }
        println "Clean integrations: " + noconflicts?.size()
        def conflicts = lines.findAll{ it[3]=="1" }
        println "Conflicting integrations: " + conflicts?.size()
        def conflictsOfInterest = lines.findAll{ it[5]=="1" }
        println "Conflicts of interest: " + conflictsOfInterest?.size()

        def resultPerProject = lines.groupBy { it[0] }
        resultPerProject.each{ rpp ->
            def conflictingIntegrations = rpp.value.findAll{ it[3] == "1"}.size()
            def noconflictingIntegrations = rpp.value.findAll{ it[3] == "0"}.size()
            println "${rpp.key}: ${rpp.value.size()} integrations, ${conflictingIntegrations} conflicts and " +
                    "${noconflictingIntegrations} clean."
        }

        conflicts.eachWithIndex{ conflict, index ->
            def conflictingFiles = conflict[4].substring(1, conflict[4].size()-1).tokenize(",")*.trim()
            def commonChangedFiles = conflict[22].substring(1, conflict[22].size()-1).tokenize(",")*.trim()
            def intersection = conflictingFiles.intersect(commonChangedFiles)
            println "Conflict ${index+1} - conflictingFiles: ${conflictingFiles.size()}, " +
                    "commonChangedFiles: ${commonChangedFiles.size()}, " +
                    "intersection: ${intersection.size()}"
        }

    }

    static showErrorResult(String csv) {
        println "File: ${csv}"
        def lines = CsvUtil.read(csv)
        lines.remove(0)
        println "All errors while integrating tasks: " + lines.size()
        def groups = lines.groupBy { it[0]}
        println "Errors per project: "
        groups.each{ println "${it.key}: ${it.value.size()}"}
    }
}
