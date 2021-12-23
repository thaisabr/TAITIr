package draft


import br.ufpe.cin.tas.search.task.GitRepository
import groovy.util.logging.Slf4j
import org.eclipse.jgit.annotations.NonNull
import org.eclipse.jgit.api.Git
import org.eclipse.jgit.diff.DiffConfig
import org.eclipse.jgit.diff.DiffEntry
import org.eclipse.jgit.diff.DiffFormatter
import org.eclipse.jgit.lib.Config
import org.eclipse.jgit.lib.ObjectReader
import org.eclipse.jgit.lib.Repository
import org.eclipse.jgit.revwalk.FollowFilter
import org.eclipse.jgit.revwalk.RevCommit
import org.eclipse.jgit.revwalk.RevTree
import org.eclipse.jgit.revwalk.RevWalk
import org.eclipse.jgit.treewalk.AbstractTreeIterator
import org.eclipse.jgit.treewalk.CanonicalTreeParser

/* Código para testar uso do diff com o propósito de verificar se duas tarefas alteram a mesma região do arquivo, indo
* um nível além de checar se as tarefas simplesmente alteram o mesmo arquivo, visando maior precisão no cálculo do risco
* de conflito. */

@Slf4j
class GitRepositoryManager {

    String url
    String name
    String localPath
    String lastCommit
    Repository repository

    GitRepositoryManager(GitRepository gitRepository){
        this.url = gitRepository.url
        this.name = gitRepository.name
        this.localPath = gitRepository.localPath
        this.lastCommit = gitRepository.lastCommit
        Git git = Git.open(new File(localPath))
        this.repository = git.repository
    }

    def diff(String sha){
        def shaParent = sha+"^"
        //-M to deal with renaming and -U for unified format
        //A saída seria pegar no resultado do diff, todas as linhas que começam com @@, pois temos informação da área alterada
        ProcessBuilder builder = new ProcessBuilder("git", "diff", "-M", "-U", shaParent, sha)
        builder.directory(new File(localPath))
        Process process = builder.start()
        def lines = process.inputStream.readLines()
        process.inputStream.close()

        def result = lines.findAll{
            it.startsWith("diff --git ") ||
            it.startsWith("@@ ")
        }

        println "result: ${result.size()}"
        result.each{ println it }

        formatDiffOutput(result)
    }

    static formatDiffOutput(List<String> result){
        def changes = []

        def file
        def lines
        result.each{ line ->
            if(line.startsWith("diff --git ")){
                def index = line.lastIndexOf(" ")
                file = (line.substring(index+1)).substring(2)
                lines = null
            }
            else if(line.startsWith("@@ ")){
                //consideramos a nova versão do arquivo, mas para lidar com linhas removidas, teríamos que considerar a versão original
                /* Exemplo: @@ -11,6 +9,4 @@
                * Na versão original, temos 6 linhas, a contar da linha 11.
                * Na versão nova, a região equivalente possui 4 linhas, a contar da linha 9. Ou seja, 2 linhas foram removidas.
                * */
                def index1 = line.lastIndexOf("+") //aqui pegamos a hunk da nova versão; para pegar a da original, usar "-"
                def interval = line.substring(index1+1)
                def index2 = interval.indexOf(" ")
                interval = interval.substring(0, index2)
                def index3 = interval.indexOf(",")
                def n
                if(index3<0) {
                    index3 = interval.indexOf(" ")
                    n = 0
                }
                def indexInit = (interval.substring(0, index3) as int) - 1
                if(!n) n = interval.substring(index3+1) as int
                def indexEnd = indexInit - 1 + n
                lines = indexInit..indexEnd
            }
            if(file && lines) changes.add([file:file, lines:lines])
        }
        def grouped = changes.groupBy{ it.file }
        def finalResult = []
        grouped.each{
            def changedLines = (it.value*.lines).flatten()
            finalResult += [file: it.key, lines:changedLines]
        }
        finalResult
    }

    static void main(String[] args) {
        def url = "https://github.com/rapidftr/RapidFTR.git"
        GitRepository gitRepository = GitRepository.getRepository(url)
        def output = gitRepository.diff("95c7df3af92a36b0adecec340a7b98ac6ffc2711")
        println "output: ${output.size()}"
        output.each{ println it }

        /* task 1057: [77260aab257a54aabc4bdc394475a88fb0ab2e9a, c3dd3e19950bbfe848359c97db89146bacb3e552,
         8526921156583eaeca3738351ff17f0cb20e52af, aebbab1c36da5d604af836a9c4786e6ebc699b78,
         bd963c20932786d7e226811d973a55ed5a694391, 95c7df3af92a36b0adecec340a7b98ac6ffc2711,
         4a929e57656dd9236fa3bca8bd0d4dffceefda01, b5a1b7914018bce4620fcddf14c3a924f0011e8f] */
        //O commit 95c7df3af92a36b0adecec340a7b98ac6ffc2711 renomeia {pdf_generator.rb → export_generator.rb}

        /* task 1058: [c5f51d64f166983fa4a8bc1dd582a8751fa0ed37, 9866dcdcb3430aa123d8a649a22fa4b04fba0adc,
        42a873fa20ac8a235be2829e61daa04e27eb149e, 4db36a76e300d6a470c8ac28056e40396e95df1c,
        563d6138e6f36ad3a3757a259b48d529c019d529] */
        //O commit 563d6138e6f36ad3a3757a259b48d529c019d529 altera lib/pdf_generator.rb

        GitRepositoryManager manager = new GitRepositoryManager(gitRepository)
        manager.runDiff("95c7df3af92a36b0adecec340a7b98ac6ffc2711",
               "563d6138e6f36ad3a3757a259b48d529c019d529", "lib/pdf_generator.rb")
    }

    //Note: path should be the original name of the renamed file
    private void runDiff(String oldCommit, String newCommit, String path) {
        // Diff README.md between two commits. The file is named README.md in
        // the new commit (5a10bd6e), but was named "jgit-cookbook README.md" in
        // the old commit (2e1d65e4).
        DiffEntry diff = diffFile(oldCommit, newCommit, path)

        // Display the diff
        System.out.println("Showing diff of " + path)
        DiffFormatter formatter = new DiffFormatter(System.out)
        Git git = Git.open(new File(localPath))
        formatter.setRepository(git.repository)
        //noinspection ConstantConditions
        formatter.format(diff)
        git.close()
    }

    private AbstractTreeIterator prepareTreeParser(String objectId) {
        Git git = Git.open(new File(localPath))
        RevWalk walk = new RevWalk(git.repository)
        RevCommit commit = walk.parseCommit(repository.resolve(objectId))
        RevTree tree = walk.parseTree(commit.getTree().getId())
        CanonicalTreeParser treeParser = new CanonicalTreeParser()
        ObjectReader reader = git.repository.newObjectReader()
        treeParser.reset(reader, tree.getId())
        walk.dispose()
        git.close()
        return treeParser
    }

    private @NonNull DiffEntry diffFile(String oldCommit, String newCommit, String path){
        Config config = new Config()
        config.setBoolean("diff", null, "renames", true)
        DiffConfig diffConfig = config.get(DiffConfig.KEY)
        Git git = Git.open(new File(localPath))
        List<DiffEntry> diffList = git.diff().
                setOldTree(prepareTreeParser(oldCommit)).
                setNewTree(prepareTreeParser(newCommit)).
                setPathFilter(FollowFilter.create(path, diffConfig)).
                call()
        git.close()
        if (diffList.size() == 0)
            return null
        if (diffList.size() > 1)
            throw new RuntimeException("invalid diff")
        return diffList.get(0)
    }

}
