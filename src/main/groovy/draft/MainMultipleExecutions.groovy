package draft

import br.ufpe.cin.tan.util.Util

class MainMultipleExecutions {

    static void main(String[] args){
        for (n in 1..100){
            def taskFiles = Util.findFilesFromDirectory("tasks").reverse().findAll{
                !it.contains(File.separator+"taskfile"+File.separator)
            }

            int months = 1
            taskFiles.each{ taskFile ->
                SyntheticConflictAnalyzer analyzer = new SyntheticConflictAnalyzer(taskFile, true)
                analyzer.analyze(months)
            }
        }
    }
}
