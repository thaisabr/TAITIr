package syntheticMergeScenario

import br.ufpe.cin.tan.util.CsvUtil
import br.ufpe.cin.tan.util.Util
import groovy.util.logging.Slf4j

import java.nio.file.FileSystems
import java.nio.file.Files
import java.nio.file.Path

/* Gera um único CSV de tarefas a partir de um conjunto de CSVs. */

@Slf4j
class JoinCsv {

    List<String[]> buffer
    String outputFile
    List<String> files

    String mainFolder
    String conflictFolder
    String conflictControllerFolder

    String[] header

    //Para organizar os csv em pastas de acordo com o tipo (controller ou não controller)
    JoinCsv(String folder){
        this.mainFolder = folder
        buffer = []
        files = Util.findFilesFromDirectory(folder)
        configureFolders(folder)
    }

    //Para gerar um único csv com os csv de pasta
    JoinCsv(String folder, String outputFile){
        this.mainFolder = folder
        buffer = []
        files = Util.findFilesFromDirectory(folder)
        println "files: ${files.size()}"
        this.outputFile = outputFile
    }

    def joinAllInFolder(){
        updateBuffer()
        generateCsv()
    }

    def organizeByType(){
        organizeFilesByType()

        def conflictFiles = Util.findFilesFromDirectory(conflictFolder)
        println "conflictFiles: ${conflictFiles.size()}"
        conflictFiles.each{ println it }

        def conflictControllerFiles = Util.findFilesFromDirectory(conflictControllerFolder)
        println "conflictControllerFiles: ${conflictControllerFiles.size()}"
        conflictControllerFiles.each{ println it }

    }

    private organizeFilesByType(){
        def relevants = files.findAll{ it.endsWith("-conflict.csv") }
        relevants.each{ file ->
            Path source = FileSystems.getDefault().getPath(file)
            Path target = FileSystems.getDefault().getPath(conflictFolder)
            Files.move(source, target.resolve(source.getFileName()))
        }

        def controllerRelevants = files.findAll{ it.endsWith("-conflict-controller.csv") }
        controllerRelevants.each{ file ->
            Path source = FileSystems.getDefault().getPath(file)
            Path target = FileSystems.getDefault().getPath(conflictControllerFolder)
            Files.move(source, target.resolve(source.getFileName()))
        }
    }

    private configureFolders(String folder){
        conflictFolder = "$folder${File.separator}conflict"
        conflictControllerFolder = "$folder${File.separator}conflict-controller"
        createFolder(conflictFolder)
        createFolder(conflictControllerFolder)
    }

    private static createFolder(String folder){
        File folderManager = new File(folder)
        if(!folderManager.exists()){
            folderManager.mkdir()
        }
    }

    private updateBuffer(){
        buffer = []
        header = null
        files.each{ file ->
            println "Reading file: $file"
            updateBuffer(file)
        }
    }

    private updateBuffer(String filename){
        List<String[]> entries = CsvUtil.read(filename)
        if(header==null) {
            header = entries.get(0)*.replaceAll("-","")
        }
        entries.remove(0)
        println "lines: ${entries.size()}"
        buffer += entries

        println "all merges: " + entries.size()
        def noconflicts = entries.findAll{ it[3]=="0" }
        println "clean merges: " + noconflicts?.size()
        def conflicts = entries.findAll{ it[3]=="1" }
        println "conflicting merges: " + conflicts?.size()
    }

    private generateCsv(){
        List<String[]> content = []
        content += header
        content += buffer
        println "buffer size: ${buffer.size()}"
        CsvUtil.write(outputFile, content)
    }

}
