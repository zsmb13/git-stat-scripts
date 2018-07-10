import com.xenomachina.argparser.ArgParser
import java.io.File


private fun getGitOutput(name: String, repoPath: String): Sequence<String> {
    val command = """git log --author="$name" --pretty=tformat: --numstat"""

    val builder = ProcessBuilder(command.split(" "))
    builder.directory(File(repoPath).absoluteFile)
    builder.redirectErrorStream(true)
    val process = builder.start()

    return process.inputStream.bufferedReader().lineSequence()
}

private class GitStats(val filesChanged: Int, val linesAdded: Int, val linesRemoved: Int)

private fun createGitStats(lines: Sequence<String>): GitStats {
    var addedTotal = 0
    var removedTotal = 0
    var count = 0

    lines.forEach { line ->
        val (added, removed) = line.split("\t")
        added.toIntOrNull()?.let { addedTotal += it }
        removed.toIntOrNull()?.let { removedTotal += it }
        count++
    }

    return GitStats(count, addedTotal, removedTotal)
}

fun main(args: Array<String>) {

    class Arguments(parser: ArgParser) {
        val name by parser.storing("-n", "--name",
                help = "The name of the user to fetch stats for")
        val repo by parser.storing("-r", "--repo", "--repository", "--path",
                help = "The path to the git repository to use")
    }

    val arguments = ArgParser(args).parseInto(::Arguments)

    val stats = createGitStats(getGitOutput(arguments.name, arguments.repo))

    println("Fetching stats for contributor ${arguments.name} in repository ${arguments.repo.substringAfterLast("/").substringAfterLast("\\")}...")
    println("${stats.filesChanged} file changes")
    println("${stats.linesAdded} lines added")
    println("${stats.linesRemoved} lines removed")
    val average = (stats.linesAdded + stats.linesRemoved).toDouble() / stats.filesChanged
    println("${String.format("%.2f", average)} lines changed per file on average")

}
