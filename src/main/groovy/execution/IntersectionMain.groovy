package execution

import br.ufpe.cin.tan.util.Util
import br.ufpe.cin.tas.util.CsvUtil
import conflictAnalysis.ConflictManagerBasedOnIntersection
import mergeScenario.JoinCsv
import groovy.util.logging.Slf4j
import syntheticMergeScenario.SCAnalyzerBasedOnChangedFiles

@Slf4j
class IntersectionMain {

    /* passo-a-passo:
    * 1 - gerar os resultados de análise por projeto
    * 2 - rodar script no projeto utility (src\main\groovy\input\JoinCsv.groovy),
    *     colocando o resultado do passo 1 na pasta temp
    * 3 - o script precisa de uma intervenção manual para agrupar os resultados de projetos com múltiplos csv
    * 4 - o resultado final são 2 csv por projeto (-relevant e relevant-controller), que deverão
    * ser copiados para a pasta tasks desse projeto aqui
    * 5 - em seguida, rodar o main e pegar o resultado na pasta output */

    static void main(String[] args){
        def taskFiles = Util.findFilesFromDirectory("tasks").reverse().findAll{
            !it.contains(File.separator+"taskfile"+File.separator) &&
                    !it.contains(File.separator+"pairs"+File.separator)
        }

        int months = 1
        taskFiles.each{ taskFile ->
            SCAnalyzerBasedOnChangedFiles analyzer = new SCAnalyzerBasedOnChangedFiles(taskFile)
            analyzer.analyze(months)
        }

        def obj = new JoinCsv("output")
        obj.joinAllInDefaultFolders()

        ConflictManagerBasedOnIntersection conflictManager = new ConflictManagerBasedOnIntersection()
        conflictManager.collectInfo()

        showResult("output${File.separator}conflict.csv")
        //showResult("output${File.separator}conflict-controller.csv")
        //showErrorResult("output${File.separator}conflict-error.csv")
        //showErrorResult("output${File.separator}conflict-controller-error.csv")
    }

    static showResult(String csv){
        if(!new File(csv).exists()) return
        println "File: ${csv}"
        def lines = CsvUtil.read(csv)
        if(lines.empty) return
        lines.remove(0)
        println "All integrations: " + lines.size()
        def noconflicts = lines.findAll{ it[3]=="0" }
        println "Clean integrations: " + noconflicts?.size()
        def conflicts = lines.findAll{ it[3]=="1" }
        println "Conflicting integrations: " + conflicts?.size()
        def conflictsOfInterest = lines.findAll{ it[5]=="1" }
        println "Conflicts of interest: " + conflictsOfInterest?.size()

        def groups1 = lines.groupBy { it[0] }
        println "Integrations per project: "
        groups1.each{ println "${it.key}: ${it.value.size()}"}

        def groups2 = conflicts.groupBy { it[0] }
        println "Conflicts per project: "
        groups2.each{ println "${it.key}: ${it.value.size()}"}

        println "Tasks per project:"
        def tasksPerProject = groups1.collect{
            def tasks = (it.value*.getAt(1) + it.value*.getAt(2)).unique()
            [project: it.key, tasks:tasks, n:tasks.size()]
        }

        tasksPerProject.each{
            println "${it.project}: ${it.n}"
            //println "${it.project}: ${it.n} (${it.tasks})"
        }
    }

    static showErrorResult(String csv) {
        println "File: ${csv}"
        def lines = CsvUtil.read(csv)
        lines.remove(0)
        println "All errors while integrating tasks: " + lines.size()
        def groups = lines.groupBy { it[0] }
        println "Errors per project: "
        groups.each{ println "${it.key}: ${it.value.size()}"}
    }

}
