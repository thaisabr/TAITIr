package mergeScenario

import br.ufpe.cin.tan.util.CsvUtil
import br.ufpe.cin.tan.util.Util

import java.nio.file.FileSystems
import java.nio.file.Files
import java.nio.file.Path

/* Gera um único CSV de tarefas a partir de um conjunto de CSVs. */

class JoinCsv {

    List<String[]> buffer
    String outputFile
    List<String> files

    String mainFolder
    String conflictFolder
    String conflictControllerFolder
    String conflictErrorFolder
    String conflictControllerErrorFolder

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

    def joinAllInDefaultFolders(){
        organizeFilesInFolders()

        this.mainFolder = "output${File.separator}conflict"
        this.outputFile = "output${File.separator}conflict.csv"
        files = Util.findFilesFromDirectory(this.mainFolder)
        joinAllInFolder()

        this.mainFolder = "output${File.separator}conflict-controller"
        this.outputFile = "output${File.separator}conflict-controller.csv"
        files = Util.findFilesFromDirectory(this.mainFolder)
        joinAllInFolder()

        this.mainFolder = "output${File.separator}conflict-error"
        this.outputFile = "output${File.separator}conflict-error.csv"
        files = Util.findFilesFromDirectory(this.mainFolder)
        joinAllInFolder()

        this.mainFolder = "output${File.separator}conflict-controller-error"
        this.outputFile = "output${File.separator}conflict-controller-error.csv"
        files = Util.findFilesFromDirectory(this.mainFolder)
        joinAllInFolder()
    }

    def joinAllInFolder(){
        updateBuffer()
        generateCsv()
    }

    def organizeFilesInFolders(){
        moveFiles()

        def conflictFiles = Util.findFilesFromDirectory(conflictFolder)
        println "conflictFiles: ${conflictFiles.size()}"
        conflictFiles.each{ println it }

        def conflictControllerFiles = Util.findFilesFromDirectory(conflictControllerFolder)
        println "conflictControllerFiles: ${conflictControllerFiles.size()}"
        conflictControllerFiles.each{ println it }

        def conflictErrorFiles = Util.findFilesFromDirectory(conflictErrorFolder)
        println "conflictErrorFiles: ${conflictErrorFiles.size()}"
        conflictErrorFiles.each{ println it }

        def conflictControllerErrorFiles = Util.findFilesFromDirectory(conflictControllerErrorFolder)
        println "conflictControllerErrorFiles: ${conflictControllerErrorFiles.size()}"
        conflictControllerErrorFiles.each{ println it }

    }

    private moveFiles(){
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

        def relevantsError = files.findAll{ it.endsWith("-conflict-error.csv") }
        relevantsError.each{ file ->
            Path source = FileSystems.getDefault().getPath(file)
            Path target = FileSystems.getDefault().getPath(conflictErrorFolder)
            Files.move(source, target.resolve(source.getFileName()))
        }

        def controllerRelevantsError = files.findAll{ it.endsWith("-conflict-controller-error.csv") }
        controllerRelevantsError.each{ file ->
            Path source = FileSystems.getDefault().getPath(file)
            Path target = FileSystems.getDefault().getPath(conflictControllerErrorFolder)
            Files.move(source, target.resolve(source.getFileName()))
        }
    }

    private configureFolders(String folder){
        conflictFolder = "$folder${File.separator}conflict"
        conflictControllerFolder = "$folder${File.separator}conflict-controller"
        conflictErrorFolder = "$folder${File.separator}conflict-error"
        conflictControllerErrorFolder = "$folder${File.separator}conflict-controller-error"
        createFolder(conflictFolder)
        createFolder(conflictControllerFolder)
        createFolder(conflictErrorFolder)
        createFolder(conflictControllerErrorFolder)
    }

    private static createFolder(String folder){
        File folderManager = new File(folder)
        if(folderManager.exists()) Util.deleteFolder(folder)
        else folderManager.mkdir()
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
        if(entries.empty) return
        if(header==null) header = entries.get(0)
        entries.remove(0)
        buffer += entries
    }

    private generateCsv(){
        List<String[]> content = []
        content += header
        content += buffer
        println "buffer size: ${buffer.size()}"
        if(!buffer.empty) CsvUtil.write(outputFile, content)
    }

}
