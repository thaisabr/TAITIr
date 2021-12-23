package draft

import br.ufpe.cin.tan.util.CsvUtil
import br.ufpe.cin.tan.util.Util
import br.ufpe.cin.tas.search.task.GitRepository
import br.ufpe.cin.tas.search.task.merge.MergeTask
import groovy.time.TimeCategory
import groovy.util.logging.Slf4j
import syntheticMergeScenario.SyntheticAnalyzedMergeScenario
import syntheticMergeScenario.SyntheticTask

@Slf4j
class SCAnalyzerBasedOnDiff {

    static int counter
    boolean multipleExecutions

    //input
    String analyzedTasksCsv
    List<SyntheticAnalyzedMergeScenario> analyzedMergeScenarios
    String projectName
    String mergeScenariosFile
    List<String[]> inputTasksWithMergeInfo
    List pairs
    List<String[]> tasksWithDate

    //output
    String outputFolder
    String outputCsv
    String errorOutputCsv
    String taskPairsCsv
    List<String[]> analyzedTasks
    String url
    GitRepository gitRepository
    List<String[]> conflictResult
    String conflictResultFile

    List integrationErrors

    GitRepositoryManager gitRepositoryManager

    SCAnalyzerBasedOnDiff(String analyzedTasksCsv){
        pairs = []
        this.analyzedMergeScenarios = []
        this.integrationErrors = []
        this.analyzedTasksCsv = analyzedTasksCsv
        configureFilesAndTasks()
        extractInputTasksWithMergeInfo()
        extractGitRepository()
        extractProjectName()
        gitRepositoryManager = new GitRepositoryManager(gitRepository)
    }

    SCAnalyzerBasedOnDiff(String analyzedTasksCsv, boolean multipleExecutions){
        pairs = []
        this.multipleExecutions = multipleExecutions
        this.analyzedMergeScenarios = []
        this.analyzedTasksCsv = analyzedTasksCsv
        configureFilesAndTasks()
        extractInputTasksWithMergeInfo()
        extractGitRepository()
        extractProjectName()
    }

    def analyze(int months){
        importTaskPairs()
        if(pairs.empty){
            computeTaskPairs(months)
            exportTaskPairs()
        }
        def counter = extractMergeScenarios()
        log.info "scenarios: ${counter}"
    }

    private static createFolder(String folder) {
        File folderManager = new File(folder)
        if (!folderManager.exists()) folderManager.mkdir()
    }

    private configureMergeScenariosFile(String name){
        if(name.contains("${File.separator}taskfile${File.separator}")){
            mergeScenariosFile = name
        } else{
            def index = name.lastIndexOf(File.separator)
            mergeScenariosFile = name.substring(0, index) + File.separator + "taskfile" + File.separator +
                    name.substring(index+1) + ".csv"
        }
    }

    private extractInputTasksWithMergeInfo(){
        inputTasksWithMergeInfo = CsvUtil.read(mergeScenariosFile)
        inputTasksWithMergeInfo.remove(0)
    }

    private extractGitRepository(){
        url = analyzedTasks.empty ? "" : analyzedTasks[0][1]
        gitRepository = GitRepository.getRepository(url)
    }

    private configureFilesAndTasks(){
        log.info "analyzedTasksCsv: ${analyzedTasksCsv}"
        def name = analyzedTasksCsv - Constants.RELEVANT_SUFIX
        if(name == analyzedTasksCsv){ //it is controller file
            name = analyzedTasksCsv - Constants.RELEVANT_CONTROLLER_SUFIX
            configureMergeScenariosFile(name)
            configureTasks()
            def init = name.lastIndexOf(File.separator)
            if(init>-1) name = name.substring(init+1)
            if(multipleExecutions){
                outputFolder = "exec${counter}"
                File folderManager = new File("output${File.separator}$outputFolder")
                if(!folderManager.exists()) outputFolder = "exec${counter-1}"
                this.outputCsv = "output${File.separator}${outputFolder}${File.separator}${name}-conflict-controller.csv"
                this.errorOutputCsv = "output${File.separator}${outputFolder}${File.separator}${name}-conflict-controller-error.csv"
                conflictResultFile = "output${File.separator}${outputFolder}${File.separator}${name}-conflict.csv"
            } else {
                this.outputCsv = "output${File.separator}${name}-conflict-controller.csv"
                this.errorOutputCsv = "output${File.separator}${name}-conflict-controller-error.csv"
                conflictResultFile = "output${File.separator}${name}-conflict.csv"
            }
            log.info "conflictResultFile: $conflictResultFile"
            importConflictInfo()
        } else {
            configureMergeScenariosFile(name)
            configureTasks()
            def init = name.lastIndexOf(File.separator)
            if(init>-1) name = name.substring(init+1)
            if(multipleExecutions){
                outputFolder = "exec${++counter}"
                File folderManager = new File("output${File.separator}$outputFolder")
                if(!folderManager.exists()) folderManager.mkdir()
                this.outputCsv = "output${File.separator}${outputFolder}${File.separator}${name}-conflict.csv"
                this.errorOutputCsv = "output${File.separator}${outputFolder}${File.separator}${name}-conflict-error.csv"
            } else {
                this.outputCsv = "output${File.separator}${name}-conflict.csv"
                this.errorOutputCsv = "output${File.separator}${name}-conflict-error.csv"
            }
            conflictResult = []
        }

        def taskpairsFolder = "tasks${File.separator}pairs"
        createFolder(taskpairsFolder)
        taskPairsCsv = "${taskpairsFolder}${File.separator}${name}-pairs.csv"
    }

    private configureTasks(){
        this.analyzedTasks = CsvUtil.read(analyzedTasksCsv)
        configureTasksWithDate()
    }

    private configureTasksWithDate(){
        tasksWithDate = analyzedTasks.subList(13, analyzedTasks.size()).collect{
            def dates = (it[1]-"["-"]").tokenize(', ')
            def lastDate = dates.last()
            def date = Date.parse("dd-MM-yyyy", lastDate)
            [task:it[0], date:date]
        }
        tasksWithDate = tasksWithDate.findAll{ it.date != null }
        log.info "tasksWithDate: ${tasksWithDate.size()}"
        tasksWithDate.each{ log.info it.toString() }
    }

    private importConflictInfo(){
        conflictResult = CsvUtil.read(conflictResultFile)
        if(conflictResult.size()>1) conflictResult.remove(0)
        conflictResult.each{ r ->
            def conflictingFiles = r[Constants.CONFLICTING_FILES_INDEX]
                    .substring(1, r[Constants.CONFLICTING_FILES_INDEX].size() - 1)
                    .tokenize(",")*.trim()
            def conflictingControllers = conflictingFiles.findAll{ Util.isControllerFile(it) }
            r[Constants.CONFLICTING_FILES_INDEX] = conflictingControllers.toString()
            r[Constants.CONFLICTING_FILES_INDEX-1] = conflictingControllers.empty? "0":"1"
        }
    }

    private extractProjectName(){
        def i = analyzedTasksCsv.indexOf("-")
        def j = analyzedTasksCsv.lastIndexOf(File.separator)
        def aux = analyzedTasksCsv.substring(j+1, i).toLowerCase()
        projectName = aux
    }

    private exportTaskPairs(){
        List<String[]> content = []
        content += ["Project", "task", "pairs"] as String[]
        pairs.each{ pair ->
            content += [url, pair.task, pair.pairs] as String[]
        }
        CsvUtil.write(taskPairsCsv, content)
    }

    private importTaskPairs(){
        if(new File(taskPairsCsv).exists()){
            List<String[]> taskPairs = CsvUtil.read(taskPairsCsv)
            if(taskPairs.size()>0) taskPairs.remove(0)
            pairs = []
            taskPairs.each{ tp ->
                def value = tp[2].substring(1, tp[2].size()-1).tokenize(', ') as String[]
                pairs.add([task: tp[1], pairs: value])
            }
        }
    }

    private groupTasksPerTime(List datedTasks, int months){
        datedTasks.each{ datedTask ->
            def others = datedTasks - datedTask
            def pairs = []
            use(TimeCategory) {
                others.findAll { otherTask ->
                    def duration
                    if(datedTask.date.after(otherTask.date)){
                        duration = datedTask.date - otherTask.date
                    } else {
                        duration = otherTask.date - datedTask.date
                    }
                    if (duration.days <= months*30) { //considering a month has 30 days
                        pairs += otherTask.task
                    }
                }
            }
            if(!pairs.empty) {
                def task = datedTask.task[Constants.TASK_INDEX]
                def pairsId = pairs*.getAt(Constants.TASK_INDEX)
                this.pairs.add([task:task, pairs:pairsId])
            }
        }
    }

    private computeTaskPairs(int months) {
        List<String[]> set = analyzedTasks.subList(13, analyzedTasks.size())
        pairs = []
        if (!set || set.empty || set.size() == 1) return

        if(conflictResult.empty) {
            def datedTasks = findDateforTasks(set)
            groupTasksPerTime(datedTasks, months)
            pairs = pairs.unique()
            log.info "Task pairs: ${pairs.size()}"
            pairs.each{
                log.info it.toString()
            }
        } else {
            conflictResult.each{ r ->
                def leftTask = analyzedTasks.find{ it[Constants.TASK_INDEX] == r[Constants.LEFT_ID_INDEX] }
                def rightTask = analyzedTasks.find{ it[Constants.TASK_INDEX] == r[Constants.RIGHT_ID_INDEX] }
                if(leftTask && rightTask) {
                    pairs.add([task: leftTask[Constants.TASK_INDEX], pairs: [rightTask[Constants.TASK_INDEX]]])
                }
            }
        }
    }

    List<String> extractConflictInfo(MergeTask leftTask, MergeTask rightTask){
        List<String> conflictingFiles = []
        List<String> commonChangedFiles = []
        if(conflictResult.empty){
            commonChangedFiles = leftTask.changedFiles.intersect(rightTask.changedFiles)
            commonChangedFiles.each{ commonChangedFile ->
                def leftChanges = leftTask.changedFilesWithLines.find{ it.file == commonChangedFile }
                def rightChanges = rightTask.changedFilesWithLines.find{ it.file == commonChangedFile }
                def commonChangedLines = []
                if(leftChanges && rightChanges) commonChangedLines = leftChanges.lines.intersect(rightChanges.lines)
                if(!commonChangedLines.empty){
                    conflictingFiles += commonChangedFile
                }
            }
            leftTask.conflictingFiles = conflictingFiles
            rightTask.conflictingFiles = conflictingFiles
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

    private extractMergeScenarios(){
        def counter = 0
        def tasks = analyzedTasks.subList(13, analyzedTasks.size())
        analyzedMergeScenarios = []
        integrationErrors = []
        pairs?.each { item ->
            def leftTask = tasks.find{ it[Constants.TASK_INDEX] == item.task }
            item.pairs?.each{ pair ->
                def rightTask = tasks.find{ it[Constants.TASK_INDEX] == pair }
                def leftTaskId = leftTask[Constants.TASK_INDEX] as int
                def rightTaskId = rightTask[Constants.TASK_INDEX] as int
                def previous = analyzedMergeScenarios.find{
                    (it.leftTask==leftTaskId && it.rightTask==rightTaskId) ||
                            (it.leftTask==rightTaskId && it.rightTask==leftTaskId)
                }

                if(!previous) {
                    log.info "Analyzing conflict from pair: $leftTaskId, $rightTaskId"

                    def hashesLeft = leftTask[Constants.HASHES_INDEX]
                            .substring(1, leftTask[Constants.HASHES_INDEX].size() - 1).tokenize(",")*.trim()
                    def testiLeft = leftTask[Constants.TESTI_INDEX]
                            .substring(1, leftTask[Constants.TESTI_INDEX].size() - 1).tokenize(",")*.trim()
                    def taskiLeft = leftTask[Constants.TASKI_INDEX]
                            .substring(1, leftTask[Constants.TASKI_INDEX].size() - 1).tokenize(",")*.trim()

                    def leftTaskWithMergeInfo = inputTasksWithMergeInfo.find{ it[1] == (leftTaskId as String) }
                    def leftMerge = leftTaskWithMergeInfo[Constants.MERGE_SCENARIO_INDEX]
                    def leftBase = leftTaskWithMergeInfo[Constants.BASE_SCENARIO_INDEX]
                    def leftCommitsSet = gitRepository.searchCommits(hashesLeft)
                    def leftMergeTask = new MergeTask(url, leftTaskId as String, leftCommitsSet, leftMerge, leftBase,
                            hashesLeft?.first())
                    def leftSyntheticTask = new SyntheticTask(project: projectName, id: leftTaskId, testi: testiLeft,
                            taski: taskiLeft, hashes: hashesLeft, lastHash: hashesLeft?.first(), //o commit mais atual é o primeiro
                            mergeTask: leftMergeTask)

                    def hashesRight = rightTask[Constants.HASHES_INDEX]
                            .substring(1, rightTask[Constants.HASHES_INDEX].size() - 1).tokenize(",")*.trim()
                    def testiRight = rightTask[Constants.TESTI_INDEX]
                            .substring(1, rightTask[Constants.TESTI_INDEX].size() - 1).tokenize(",")*.trim()
                    def taskiRight = rightTask[Constants.TASKI_INDEX]
                            .substring(1, rightTask[Constants.TASKI_INDEX].size() - 1).tokenize(",")*.trim()

                    def rightTaskWithMergeInfo = inputTasksWithMergeInfo.find{ it[1] == (rightTaskId as String) }
                    def rightMerge = rightTaskWithMergeInfo[Constants.MERGE_SCENARIO_INDEX]
                    def rightBase = rightTaskWithMergeInfo[Constants.BASE_SCENARIO_INDEX]
                    def rightCommitsSet = gitRepository.searchCommits(hashesRight)
                    def rightMergeTask = new MergeTask(url, rightTaskId as String, rightCommitsSet, rightMerge, rightBase,
                            hashesRight?.first())
                    def rightSyntheticTask = new SyntheticTask(project: projectName, id: rightTaskId, testi: testiRight,
                            taski: taskiRight, hashes: hashesRight, lastHash: hashesRight?.first(), //o commit mais atual é o primeiro
                            mergeTask: rightMergeTask)

                    def conflictingFiles = extractConflictInfo(leftSyntheticTask.mergeTask, rightSyntheticTask.mergeTask)
                    if (conflictingFiles != null) {
                        def conflict = !conflictingFiles.empty

                        def conflictingWithProductionFiles = Util.findAllProductionFiles(conflictingFiles)
                        def conflictAffectFilesOfInterest = !conflictingWithProductionFiles.empty

                        def commonFiles = leftMergeTask.changedFiles.intersect(rightMergeTask.changedFiles)
                        def commonProductionFiles = leftMergeTask.productionFiles.intersect(rightMergeTask.productionFiles)

                        analyzedMergeScenarios += new SyntheticAnalyzedMergeScenario(leftTesti: leftSyntheticTask.testi,
                                rightTesti: rightSyntheticTask.testi, conflict: conflict ? 1 : 0,
                                conflictOfInterest: conflictAffectFilesOfInterest?1:0,
                                leftTask: leftSyntheticTask.id, rightTask: rightSyntheticTask.id,
                                conflictingFiles: conflictingFiles,  conflictingFilesOfInterest: conflictingWithProductionFiles,
                                project: projectName, commonFiles: commonFiles, commonProductionFiles: commonProductionFiles
                        )
                        if (analyzedMergeScenarios.size() >= Constants.LIMIT) {
                            counter += analyzedMergeScenarios.size()
                            exportResult()
                            analyzedMergeScenarios = []
                        }
                    }
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
            content += Constants.CONFLICT_FILE_HEADER as String[]
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

    private findDateforTasks(List<String> set){
        def result = []
        set.each{ t ->
            def td = tasksWithDate.find{ it.task == t[Constants.TASK_INDEX] }
            if(td) result.add([task:t, date:td.date])
        }
        result.sort { it.task[Constants.TASK_INDEX] as double } //ordenadas por índice
    }
}
