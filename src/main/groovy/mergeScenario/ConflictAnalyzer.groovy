package mergeScenario

import br.ufpe.cin.tan.util.CsvUtil
import br.ufpe.cin.tan.util.Util
import br.ufpe.cin.tas.search.task.GitRepository
import br.ufpe.cin.tas.search.task.merge.MergeTask

import groovy.util.logging.Slf4j

@Slf4j
class ConflictAnalyzer {

    //input
    String analyzedTasksCsv
    List<AnalyzedMergeScenario> analyzedMergeScenarios
    String projectName
    String mergeScenariosFile
    List<String[]> inputTasksWithMergeInfo
    List pairs

    //output
    String outputCsv
    String errorOutputCsv
    String taskPairsCsv
    List<String[]> analyzedTasks
    String url
    GitRepository gitRepository
    List<String[]> conflictResult
    String conflictResultFile

    List integrationErrors

    ConflictAnalyzer(String analyzedTasksCsv){
        this.pairs = []
        this.analyzedMergeScenarios = []
        this.integrationErrors = []
        this.analyzedTasksCsv = analyzedTasksCsv
        configureFilesAndTasks()
        this.analyzedTasks = CsvUtil.read(analyzedTasksCsv)
        extractInputTasksWithMergeInfo()
        extractGitRepository()
        extractProjectName()
    }

    def analyze(){
        importTaskPairs()
        if(pairs.empty){
            extractTaskPairs()
            exportTaskPairs()
        }
        def counter = extractMergeScenarios()
        log.info "scenarios: ${counter}"
    }

    private static createFolder(String folder) {
        File folderManager = new File(folder)
        if (!folderManager.exists()) folderManager.mkdir()
    }

    private extractGitRepository(){
        url = analyzedTasks.empty ? "" : analyzedTasks[0][1]
        gitRepository = GitRepository.getRepository(url)
    }

    private configureFilesAndTasks(){
        def name = analyzedTasksCsv - Constants.RELEVANT_SUFIX
        if(name==analyzedTasksCsv){ //it is controller file
            name = analyzedTasksCsv - Constants.RELEVANT_CONTROLLER_SUFIX
            configureMergeScenariosFile(name)
            log.info "mergeScenariosFile: $mergeScenariosFile"
            def init = name.lastIndexOf(File.separator)
            if(init>-1) name = name.substring(init+1)
            this.outputCsv = "output${File.separator}${name}-conflict-controller.csv"
            this.errorOutputCsv = "output${File.separator}${name}-conflict-controller-error.csv"
            conflictResultFile = "output${File.separator}${name}-conflict.csv"
            log.info "conflictResultFile: $conflictResultFile"
            importConflictInfo()
        } else {
            configureMergeScenariosFile(name)
            log.info "mergeScenariosFile: $mergeScenariosFile"
            def init = name.lastIndexOf(File.separator)
            if(init>-1) name = name.substring(init+1)
            this.outputCsv = "output${File.separator}${name}-conflict.csv"
            this.errorOutputCsv = "output${File.separator}${name}-conflict-error.csv"
            conflictResult = []
        }

        def taskpairsFolder = "tasks${File.separator}pairs"
        createFolder(taskpairsFolder)
        taskPairsCsv = "${taskpairsFolder}${File.separator}${name}-pairs.csv"
    }

    private importConflictInfo(){
        conflictResult = CsvUtil.read(conflictResultFile)
        if(conflictResult.size()>1) conflictResult.remove(0)
        conflictResult.each{ r ->
            def conflictingFiles = r[Constants.CONFLICTING_FILES_INDEX]
                    .substring(1, r[Constants.CONFLICTING_FILES_INDEX].size() - 1)
                    .tokenize(",")*.trim()
            def conflictingControllers = conflictingFiles.findAll{ Util.isControllerFile(it)}
            r[Constants.CONFLICTING_FILES_INDEX] = conflictingControllers.toString()
            r[Constants.CONFLICTING_FILES_INDEX-1] = conflictingControllers.empty? "0":"1"
        }
    }

    private configureMergeScenariosFile(String name){
        if(name.contains("${File.separator}taskfile${File.separator}")){
            mergeScenariosFile = name
        } else {
            def index = name.lastIndexOf(File.separator)
            mergeScenariosFile = name.substring(0, index) + File.separator + "taskfile" + File.separator +
                    name.substring(index+1) + ".csv"
        }
    }

    private extractInputTasksWithMergeInfo(){
        inputTasksWithMergeInfo = CsvUtil.read(mergeScenariosFile)
        inputTasksWithMergeInfo.remove(0)
    }

    private extractProjectName(){
        def result = ""
        def i = analyzedTasksCsv.lastIndexOf("-")
        def j = analyzedTasksCsv.lastIndexOf(File.separator)
        def aux = analyzedTasksCsv.substring(j+1, i).toLowerCase()
        projectName = aux
    }

    private extractTaskPairs(){
        log.info "Extracting task pairs"
        pairs = []
        def analyzedTasksList = analyzedTasks.subList(13, analyzedTasks.size()).sort{ it[Constants.TASK_INDEX] }
        List<String[]> selectedInputTasks = []
        analyzedTasksList.each{ analyzedTask ->
            def correspondingInputTask = inputTasksWithMergeInfo.find{ inputTask ->
                url == inputTask[Constants.URL_MERGE_SCENARIO_INDEX] &&
                analyzedTask[Constants.TASK_INDEX] == inputTask[Constants.TASK_MERGE_SCENARIO_INDEX]
            }
            if(correspondingInputTask) {
                selectedInputTasks += correspondingInputTask
            }
        }
        selectedInputTasks = selectedInputTasks.sort{ it[Constants.TASK_MERGE_SCENARIO_INDEX] }

        log.info "selectedInputTasks: ${selectedInputTasks.size()}"
        selectedInputTasks.each{
            log.info "id: ${it[1]}; merge: ${it[7]}; base: ${it[8]}"
        }

        def inputPairs = selectedInputTasks.groupBy { u ->
            [u[Constants.MERGE_SCENARIO_INDEX], u[Constants.BASE_SCENARIO_INDEX]] }
            .findAll{ it.value.size()==2 }
        log.info "Pairs: ${inputPairs.size()}"
        inputPairs.each{ pair ->
            log.info "[merge, base]: " + pair.key.toString()
            pair.value.each{
                log.info "task: ${it.toString()}"
            }
            def idLeft = pair.value.get(0)[Constants.TASK_MERGE_SCENARIO_INDEX]
            def idRight = pair.value.get(1)[Constants.TASK_MERGE_SCENARIO_INDEX]
            pairs.add([idLeft, idRight])
        }
    }

    private exportTaskPairs(){
        List<String[]> content = []
        content += ["Project", "left", "right"] as String[]
        pairs.each{ pair ->
            content += [url, pair[0], pair[1]] as String[]
        }
        CsvUtil.write(taskPairsCsv, content)
    }

    private importTaskPairs(){
        if(new File(taskPairsCsv).exists()){
            List<String[]> taskPairs = CsvUtil.read(taskPairsCsv)
            if(taskPairs.size()>0) taskPairs.remove(0)
            pairs = []
            taskPairs.each{ tp ->
                pairs.add([tp[1], tp[2]])
            }
        }

    }

    private configureMergeTask(String[] taskData, def taskId){
        def hashes = taskData[Constants.HASHES_INDEX]
                .substring(1, taskData[Constants.HASHES_INDEX].size()-1).tokenize(",")*.trim()
        def testi = taskData[Constants.TESTI_INDEX]
                .substring(1, taskData[Constants.TESTI_INDEX].size()-1).tokenize(",")*.trim()
        def taski = taskData[Constants.TASKI_INDEX]
                .substring(1, taskData[Constants.TASKI_INDEX].size()-1).tokenize(",")*.trim()

        def taskWithMergeInfo = inputTasksWithMergeInfo.find{ it[1] == taskId }
        def merge = taskWithMergeInfo[Constants.MERGE_SCENARIO_INDEX]
        def base = taskWithMergeInfo[Constants.BASE_SCENARIO_INDEX]
        def commitsSet = gitRepository.searchCommits(hashes)
        def mergeTask = new MergeTask(url, taskId, commitsSet, merge, base, hashes?.first())
        def finalTask = new Task(project: projectName, id:taskId as int, testi:testi,
                taski:taski, hashes:hashes, lastHash:hashes?.first(), //o commit mais atual é o primeiro
                mergeTask: mergeTask)
        finalTask
    }

    private extractMergeScenarios(){
        def counter = 0
        def tasks = analyzedTasks.subList(13, analyzedTasks.size())
        analyzedMergeScenarios = []
        integrationErrors = []
        pairs?.each { pair ->
            def leftTask = tasks.find{ it[0] == pair[0] }
            def rightTask = tasks.find{ it[0] == pair[1] }
            def leftTaskId = pair[0]
            def rightTaskId = pair[1]
            log.info "pair: $leftTaskId, $rightTaskId"

            def finalLeftTask = configureMergeTask(leftTask, leftTaskId)
            def finalRightTask = configureMergeTask(rightTask, rightTaskId)

            def conflictingFiles = extractConflictInfo(finalLeftTask.mergeTask, finalRightTask.mergeTask)
            if(conflictingFiles != null){
                def conflict = !conflictingFiles.empty

                def conflictingWithProductionFiles = Util.findAllProductionFiles(conflictingFiles)
                def conflictAffectFilesOfInterest = !conflictingWithProductionFiles.empty

                def commonFiles = finalLeftTask.mergeTask.changedFiles.intersect(finalRightTask.mergeTask.changedFiles)
                def commonProductionFiles =  finalLeftTask.mergeTask.productionFiles.intersect(finalRightTask.mergeTask.productionFiles)

                analyzedMergeScenarios += new AnalyzedMergeScenario(leftTesti: finalLeftTask.testi,
                        rightTesti: finalRightTask.testi, conflict: conflict?1:0,
                        conflictOfInterest: conflictAffectFilesOfInterest?1:0,
                        leftTask: finalLeftTask.id, rightTask: finalRightTask.id,
                        conflictingFiles: conflictingFiles, conflictingFilesOfInterest: conflictingWithProductionFiles,
                        project: projectName, commonFiles: commonFiles, commonProductionFiles:commonProductionFiles
                )

                if(analyzedMergeScenarios.size()>=Constants.LIMIT){
                    counter += analyzedMergeScenarios.size()
                    exportResult()
                    analyzedMergeScenarios = []
                }
            }
        }

        if(analyzedMergeScenarios.size()<Constants.LIMIT) {
            counter += analyzedMergeScenarios.size()
            exportResult()
        }

        exportErrors()

        counter
    }

    private List<String> extractConflictInfo(MergeTask leftTask, MergeTask rightTask){
        List<String> conflictingFiles
        if(conflictResult.empty){
            conflictingFiles = gitRepository.reproduceMergeScenarioByJgit(leftTask.base, leftTask.newestCommit,
                    rightTask.newestCommit)
            if(conflictingFiles == null){ //if it is null, there was an error while reproducing merge scenario
                log.info "Error while integrating tasks: ${leftTask.repositoryUrl}, ${leftTask.id}, ${rightTask.id}"
                this.integrationErrors += [url: leftTask.repositoryUrl, left:leftTask.id, right:rightTask.id]
            } else {
                leftTask.conflictingFiles = conflictingFiles
                rightTask.conflictingFiles = conflictingFiles
            }
        } else {
            def r = conflictResult.find{ (leftTask.id == it[Constants.LEFT_ID_INDEX]) &&
                    (rightTask.id == it[Constants.RIGHT_ID_INDEX])
            }
            if(r) {
                conflictingFiles = r[Constants.CONFLICTING_FILES_INDEX]
                        .substring(1, r[Constants.CONFLICTING_FILES_INDEX].size() - 1)
                        .tokenize(",")*.trim()
            }
        }
        conflictingFiles
    }

    private containsHeader(){
        def result = false
        File file = new File(outputCsv)
        if(file.exists()){
            def originalLines = CsvUtil.read(outputCsv)
            if(originalLines.size()>0) result = true
        }
        result
    }

    private exportResult(){
        List<String[]> content = []
        if(!containsHeader()){
            content +=  Constants.CONFLICT_FILE_HEADER as String[]
        }
        analyzedMergeScenarios?.each {
            content += [it.project, it.leftTask, it.rightTask, it.conflict, it.conflictingFiles, it.conflictOfInterest,
                        it.conflictingFilesOfInterest, it.leftTesti, it.rightTesti, it.commonFiles.size(), it.commonFiles,
                        it.commonProductionFiles.size(), it.commonProductionFiles
            ] as String[]
        }
        CsvUtil.append(outputCsv, content)
    }

    private exportErrors(){
        List<String[]> content = []
        content += ["PROJECT", "LEFT_TASK", "RIGHT_TASK"] as String[]
        integrationErrors?.each { content += [it.url, it.left, it.right] as String[] }
        CsvUtil.write(errorOutputCsv, content)
    }

}
