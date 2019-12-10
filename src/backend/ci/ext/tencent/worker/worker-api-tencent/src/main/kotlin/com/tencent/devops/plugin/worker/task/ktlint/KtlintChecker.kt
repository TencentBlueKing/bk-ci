package com.tencent.devops.plugin.worker.task.ktlint

import com.github.shyiko.klob.Glob
import com.github.shyiko.ktlint.core.KtLint
import com.github.shyiko.ktlint.core.LintError
import com.github.shyiko.ktlint.core.ParseException
import com.github.shyiko.ktlint.core.Reporter
import com.github.shyiko.ktlint.core.ReporterProvider
import com.github.shyiko.ktlint.core.RuleExecutionException
import com.github.shyiko.ktlint.core.RuleSet
import com.github.shyiko.ktlint.core.RuleSetProvider
import com.github.shyiko.ktlint.internal.EditorConfig
import com.github.shyiko.ktlint.internal.MavenDependencyResolver
import com.github.shyiko.ktlint.test.DumpAST
import com.tencent.devops.worker.common.logger.LoggerService
import org.apache.commons.lang.exception.ExceptionUtils
import org.eclipse.aether.RepositoryException
import org.eclipse.aether.artifact.DefaultArtifact
import org.eclipse.aether.repository.RemoteRepository
import org.eclipse.aether.repository.RepositoryPolicy
import org.eclipse.aether.repository.RepositoryPolicy.CHECKSUM_POLICY_IGNORE
import org.eclipse.aether.repository.RepositoryPolicy.UPDATE_POLICY_NEVER
import org.jetbrains.kotlin.backend.common.onlyIf
import org.slf4j.LoggerFactory
import picocli.CommandLine
import picocli.CommandLine.Command
import picocli.CommandLine.Option
import picocli.CommandLine.Parameters
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.IOException
import java.io.PrintStream
import java.net.URLDecoder
import java.nio.file.Path
import java.nio.file.Paths
import java.util.ArrayList
import java.util.LinkedHashMap
import java.util.ServiceLoader
import java.util.concurrent.ArrayBlockingQueue
import java.util.concurrent.Callable
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Executors
import java.util.concurrent.Future
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import kotlin.concurrent.thread

@Command(
        headerHeading = """An anti-bikeshedding Kotlin linter with built-in formatter
(https://github.com/shyiko/ktlint).

Usage:
  ktlint <flags> [patterns]
  java -jar ktlint <flags> [patterns]

Examples:
  # check the style of all Kotlin files inside the current dir (recursively)
  # (hidden folders will be skipped)
  ktlint

  # check only certain locations (prepend ! to negate the pattern)
  ktlint "src/**/*.kt" "!src/**/*Test.kt"

  # auto-correct style violations
  ktlint -F "src/**/*.kt"

  # custom reporter
  ktlint --reporter=plain?group_by_file
  # multiple reporters can be specified like this
  ktlint --reporter=plain \
    --reporter=checkstyle,output=ktlint-checkstyle-report.xml
  # 3rd-party reporter
  ktlint --reporter=html,artifact=com.gihub.user:repo:master-SNAPSHOT

Flags:""",
        synopsisHeading = "",
        customSynopsis = [""],
        sortOptions = false
)
class KtlintChecker constructor(path: File) {

    companion object {
        private val logger = LoggerFactory.getLogger(KtlintChecker::class.java)
    }

    private val DEPRECATED_FLAGS = mapOf(
            "--ruleset-repository" to
                    "--repository",
            "--reporter-repository" to
                    "--repository",
            "--ruleset-update" to
                    "--repository-update",
            "--reporter-update" to
                    "--repository-update"
    )
    @Option(names = ["--android", "-a"], description = ["Turn on Android Kotlin Style Guide compatibility"])
    private var android: Boolean = false

    @Option(names = ["--color"], description = ["Make output colorful"])
    private var color: Boolean = false

    @Option(names = ["--debug"], description = ["Turn on debug output"])
    private var debug: Boolean = false

    // todo: this should have been a command, not a flag (consider changing in 1.0.0)
    @Option(names = ["--format", "-F"], description = ["Fix any deviations from the code style"])
    private var format: Boolean = false

    @Option(names = ["--limit"], description = [
            "Maximum number of errors to show (default: show all)"
    ])
    private var limit: Int = -1
        get() = if (field < 0) Int.MAX_VALUE else field

    @Option(names = ["--print-ast"], description = [
            "Print AST (useful when writing/debugging rules)"
    ])
    private var printAST: Boolean = false

    @Option(names = ["--relative"], description = [
            "Print files relative to the working directory " +
                    "(e.g. dir/file.kt instead of /home/user/project/dir/file.kt)"
    ])
    private var relative: Boolean = false

    @Option(names = ["--reporter"], description = [
            "A reporter to use (built-in: plain (default), plain?group_by_file, json, checkstyle). " +
                    "To use a third-party reporter specify either a path to a JAR file on the filesystem or a" +
                    "<groupId>:<artifactId>:<version> triple pointing to a remote artifact (in which case ktlint will first " +
                    "check local cache (~/.m2/repository) and then, if not found, attempt downloading it from " +
                    "Maven Central/JCenter/JitPack/user-provided repository)\n" +
                    "e.g. \"html,artifact=com.github.username:ktlint-reporter-html:master-SNAPSHOT\""
    ])
    private var reporters = ArrayList<String>()

    @Option(names = ["--repository"], description = [
            "An additional Maven repository (Maven Central/JCenter/JitPack are active by default) " +
                    "(value format: <id>=<url>)"
    ])
    private var repositories = ArrayList<String>()
    @Option(names = ["--ruleset-repository", "--reporter-repository"], hidden = true)
    private var repositoriesDeprecated = ArrayList<String>()

    @Option(names = ["--repository-update", "-U"], description = [
            "Check remote repositories for updated snapshots"
    ])
    private var forceUpdate: Boolean? = null
    @Option(names = ["--ruleset-update", "--reporter-update"], hidden = true)
    private var forceUpdateDeprecated: Boolean? = null

    @Option(names = ["--ruleset", "-R"], description = [
            "A path to a JAR file containing additional ruleset(s) or a " +
                    "<groupId>:<artifactId>:<version> triple pointing to a remote artifact (in which case ktlint will first " +
                    "check local cache (~/.m2/repository) and then, if not found, attempt downloading it from " +
                    "Maven Central/JCenter/JitPack/user-provided repository)"
    ])
    private var rulesets = ArrayList<String>()

    @Option(names = ["--skip-classpath-check"], description = ["Do not check classpath for potential conflicts"])
    private var skipClasspathCheck: Boolean = false

    @Option(names = ["--verbose", "-v"], description = ["Show error codes"])
    private var verbose: Boolean = false

    @Option(names = ["--editorconfig"], description = ["Path to .editorconfig"])
    private var editorConfigPath: String? = null

    @Parameters(hidden = true)
    private var patterns = ArrayList<String>()

    private val workDir = path.canonicalPath
    private fun File.location() = if (relative) this.toRelativeString(File(workDir)) else this.path

    private fun usage() =
            ByteArrayOutputStream()
                    .also { CommandLine.usage(this, PrintStream(it), CommandLine.Help.Ansi.OFF) }
                    .toString()
                    .replace(" ".repeat(32), " ".repeat(30))

    private fun parseCmdLine(args: Array<String>) {
        try {
            CommandLine.populateCommand(this, *args)
            repositories.addAll(repositoriesDeprecated)
            if (forceUpdateDeprecated != null && forceUpdate == null) {
                forceUpdate = forceUpdateDeprecated
            }
        } catch (e: Exception) {
            logger.warn("Fail to parse the ktlint command $args", e)
            LoggerService.addRedLine("Fail to parse the ktlin command $args")
            exitProcess(1)
        }

        args.forEach { arg ->
            if (arg.startsWith("--") && arg.contains("=")) {
                val flag = arg.substringBefore("=")
                val alt = DEPRECATED_FLAGS[flag]
                if (alt != null) {
                    LoggerService.addYellowLine("$flag flag is deprecated and will be removed in 1.0.0 (use $alt instead)")
                }
            }
        }
    }

    fun check(args: Array<String>) {
        parseCmdLine(args)

        if (printAST) {
            printAST()
            return
        }
        val start = System.currentTimeMillis()
        // load 3rd party ruleset(s) (if any)
        val dependencyResolver = lazy(LazyThreadSafetyMode.NONE) { buildDependencyResolver() }
        if (!rulesets.isEmpty()) {
            loadJARs(dependencyResolver, rulesets)
        }
        // standard should go first
        val ruleSetProviders = ServiceLoader.load(RuleSetProvider::class.java)
                .map { it.get().id to it }
                .sortedBy { if (it.first == "standard") "\u0000${it.first}" else it.first }
        if (debug) {
            ruleSetProviders.forEach { LoggerService.addNormalLine("[DEBUG] Discovered ruleset \"${it.first}\"") }
        }
        val tripped = AtomicBoolean()
        val reporter = loadReporter(dependencyResolver) { tripped.get() }
        val resolveUserData = userDataResolver()

        data class LintErrorWithCorrectionInfo(val err: LintError, val corrected: Boolean)

        fun process(fileName: String, fileContent: String): List<LintErrorWithCorrectionInfo> {
            if (debug) {
                LoggerService.addNormalLine("[DEBUG] Checking ${if (fileName != "<text>") File(fileName).location() else fileName}")
            }
            val result = ArrayList<LintErrorWithCorrectionInfo>()
            val userData = resolveUserData(fileName)
            if (format) {
                val formattedFileContent = try {
                    format(fileName, fileContent, ruleSetProviders.map { it.second.get() }, userData) { err, corrected ->
                        if (!corrected) {
                            result.add(LintErrorWithCorrectionInfo(err, corrected))
                            tripped.set(true)
                        }
                    }
                } catch (e: Exception) {
                    result.add(LintErrorWithCorrectionInfo(e.toLintError(), false))
                    tripped.set(true)
                    fileContent // making sure `cat file | ktlint --stdint > file` is (relatively) safe
                }
                if (fileContent !== formattedFileContent) {
                    File(fileName).writeText(formattedFileContent, charset("UTF-8"))
                }
            } else {
                try {
                    lint(fileName, fileContent, ruleSetProviders.map { it.second.get() }, userData) { err ->
                        result.add(LintErrorWithCorrectionInfo(err, false))
                        tripped.set(true)
                    }
                } catch (e: Exception) {
                    result.add(LintErrorWithCorrectionInfo(e.toLintError(), false))
                    tripped.set(true)
                }
            }
            return result
        }
        val (fileNumber, errorNumber) = Pair(AtomicInteger(), AtomicInteger())
        fun report(fileName: String, errList: List<LintErrorWithCorrectionInfo>) {
            fileNumber.incrementAndGet()
            val errListLimit = minOf(errList.size, maxOf(limit - errorNumber.get(), 0))
            errorNumber.addAndGet(errListLimit)
            reporter.before(fileName)
            errList.head(errListLimit).forEach { (err, corrected) ->
                reporter.onLintError(
                        fileName,
                        if (!err.canBeAutoCorrected) err.copy(detail = err.detail + " (cannot be auto-corrected)") else err,
                        corrected
                )
            }
            reporter.after(fileName)
        }
        reporter.beforeAll()
        fileSequence()
                .takeWhile { errorNumber.get() < limit }
                .map { file -> Callable { file to process(file.path, file.readText()) } }
                .parallel({ (file, errList) -> report(file.location(), errList) })

        reporter.afterAll()
        if (debug) {
            LoggerService.addNormalLine("[DEBUG] ${
            System.currentTimeMillis() - start
            }ms / $fileNumber file(s) / $errorNumber error(s)")
        }
        if (tripped.get()) {
            exitProcess(1)
        }
    }

    private fun userDataResolver(): (String) -> Map<String, String> {
        val cliUserData = mapOf("android" to android.toString())
        if (editorConfigPath != null) {
            val userData = (
                    EditorConfig.of(File(editorConfigPath).canonicalPath)
                            ?.onlyIf({ debug }) { printEditorConfigChain(it) }
                            ?: emptyMap<String, String>()
                    ) + cliUserData
            return fun(fileName: String) = userData + ("file_path" to fileName)
        }
        val workdirUserData = lazy {
            (
                    EditorConfig.of(workDir)
                            ?.onlyIf({ debug }) { printEditorConfigChain(it) }
                            ?: emptyMap<String, String>()
                    ) + cliUserData
        }
        val editorConfig = EditorConfig.cached()
        val editorConfigSet = ConcurrentHashMap<Path, Boolean>()
        return fun(fileName: String): Map<String, String> {
            if (fileName == "<text>") {
                return workdirUserData.value
            }
            return (
                    editorConfig.of(Paths.get(fileName).parent)
                            ?.onlyIf({ debug }) {
                                printEditorConfigChain(it) {
                                    editorConfigSet.put(it.path, true) != true
                                }
                            }
                            ?: emptyMap<String, String>()
                    ) + cliUserData + ("file_path" to fileName)
        }
    }

    private fun printEditorConfigChain(ec: EditorConfig, predicate: (EditorConfig) -> Boolean = { true }) {
        for (lec in generateSequence(ec) { it.parent }.takeWhile(predicate)) {
            LoggerService.addNormalLine("[DEBUG] Discovered .editorconfig (${lec.path.parent.toFile().location()})" +
                    " {${lec.entries.joinToString(", ")}}")
        }
    }

    private fun loadReporter(dependencyResolver: Lazy<MavenDependencyResolver>, tripped: () -> Boolean): Reporter {
        data class ReporterTemplate(val id: String, val artifact: String?, val config: Map<String, String>, var output: String?)

        val tpls = (if (reporters.isEmpty()) listOf("plain") else reporters)
                .map { reporter ->
                    val split = reporter.split(",")
                    val (reporterId, rawReporterConfig) = split[0].split("?", limit = 2) + listOf("")
                    ReporterTemplate(
                            reporterId,
                            split.lastOrNull { it.startsWith("artifact=") }?.let { it.split("=")[1] },
                            mapOf("verbose" to verbose.toString(), "color" to color.toString()) + parseQuery(rawReporterConfig),
                            split.lastOrNull { it.startsWith("output=") }?.let { it.split("=")[1] }
                    )
                }
                .distinct()
        val reporterLoader = ServiceLoader.load(ReporterProvider::class.java)
        val reporterProviderById = reporterLoader.associate { it.id to it }.let { map ->
            val missingReporters = tpls.filter { !map.containsKey(it.id) }.mapNotNull { it.artifact }.distinct()
            if (!missingReporters.isEmpty()) {
                loadJARs(dependencyResolver, missingReporters)
                reporterLoader.reload()
                reporterLoader.associate { it.id to it }
            } else map
        }
        if (debug) {
            reporterProviderById.forEach { (id) -> LoggerService.addNormalLine("[DEBUG] Discovered reporter \"$id\"") }
        }
        fun ReporterTemplate.toReporter(): Reporter {
            val reporterProvider = reporterProviderById[id]
            if (reporterProvider == null) {
                LoggerService.addRedLine("Error: reporter \"$id\" wasn't found (available: ${
                reporterProviderById.keys.sorted().joinToString(",")
                })")
                exitProcess(1)
            }
            if (debug) {
                LoggerService.addNormalLine("[DEBUG] Initializing \"$id\" reporter with $config" +
                        (output?.let { ", output=$it" } ?: ""))
            }
            val stream = if (output != null) {
                File(output).parentFile?.mkdirsOrFail(); PrintStream(output, "UTF-8")
            } else System.out
            return reporterProvider!!.get(stream, config)
                    .let { reporter ->
                        if (output != null) {
                            object : Reporter by reporter {
                                override fun afterAll() {
                                    reporter.afterAll()
                                    stream.close()
                                    if (tripped()) {
                                        LoggerService.addRedLine("\"$id\" report written to ${File(output).absoluteFile.location()}")
                                    }
                                }
                            }
                        } else {
                            reporter
                        }
                    }
        }
        return Reporter.from(*tpls.map { it.toReporter() }.toTypedArray())
    }

    private fun Exception.toLintError(): LintError = this.let { e ->
        when (e) {
            is ParseException ->
                LintError(e.line, e.col, "",
                        "Not a valid Kotlin file (${e.message?.toLowerCase()})")
            is RuleExecutionException -> {
                if (debug) {
                    LoggerService.addNormalLine("[DEBUG] Internal Error (${e.ruleId}) - ${ExceptionUtils.getStackTrace(e)}")
                }
                LintError(e.line, e.col, "", "Internal Error (${e.ruleId}). " +
                        "Please create a ticket at https://github.com/shyiko/ktlint/issue " +
                        "(if possible, provide the source code that triggered an error)")
            }
            else -> throw e
        }
    }

    private fun printAST() {
        fun process(fileName: String, fileContent: String) {
            if (debug) {
                LoggerService.addNormalLine("[DEBUG] Analyzing ${if (fileName != "<text>") File(fileName).location() else fileName}")
            }
            try {
                lint(fileName, fileContent, listOf(RuleSet("debug", DumpAST(System.out, color))), emptyMap()) {}
            } catch (e: Exception) {
                if (e is ParseException) {
                    throw ParseException(e.line, e.col, "Not a valid Kotlin file (${e.message?.toLowerCase()})")
                }
                throw e
            }
        }
        for (file in fileSequence()) {
            process(file.path, file.readText())
        }
    }

    private fun fileSequence() =
            when {
                patterns.isEmpty() ->
                    Glob.from("**/*.kt", "**/*.kts")
                            .iterate(Paths.get(workDir), Glob.IterationOption.SKIP_HIDDEN)
                else ->
                    Glob.from(*patterns.map { expandTilde(it) }.toTypedArray())
                            .iterate(Paths.get(workDir))
            }
                    .asSequence()
                    .map(Path::toFile)

    // a complete solution would be to implement https://www.gnu.org/software/bash/manual/html_node/Tilde-Expansion.html
    // this implementation takes care only of the most commonly used case (~/)
    private fun expandTilde(path: String) = path.replaceFirst(Regex("^~"), System.getProperty("user.home"))

    private fun <T> List<T>.head(limit: Int) = if (limit == size) this else this.subList(0, limit)

    private fun buildDependencyResolver(): MavenDependencyResolver {
        val mavenLocal = File(File(System.getProperty("user.home"), ".m2"), "repository")
        mavenLocal.mkdirsOrFail()
        val dependencyResolver = MavenDependencyResolver(
                mavenLocal,
                listOf(
                        RemoteRepository.Builder(
                                "central", "default", "http://repo1.maven.org/maven2/"
                        ).setSnapshotPolicy(RepositoryPolicy(false, UPDATE_POLICY_NEVER,
                                CHECKSUM_POLICY_IGNORE)).build(),
                        RemoteRepository.Builder(
                                "bintray", "default", "http://jcenter.bintray.com"
                        ).setSnapshotPolicy(RepositoryPolicy(false, UPDATE_POLICY_NEVER,
                                CHECKSUM_POLICY_IGNORE)).build(),
                        RemoteRepository.Builder(
                                "jitpack", "default", "http://jitpack.io").build()
                ) + repositories.map { repository ->
                    val colon = repository.indexOf("=").apply {
                        if (this == -1) {
                            throw RuntimeException("$repository is not a valid repository entry " +
                                    "(make sure it's provided as <id>=<url>")
                        }
                    }
                    val id = repository.substring(0, colon)
                    val url = repository.substring(colon + 1)
                    RemoteRepository.Builder(id, "default", url).build()
                },
                forceUpdate == true
        )
        if (debug) {
            dependencyResolver.setTransferEventListener { e ->
                LoggerService.addNormalLine("[DEBUG] Transfer ${e.type.toString().toLowerCase()} ${e.resource.repositoryUrl}" +
                        e.resource.resourceName + (e.exception?.let { " (${it.message})" } ?: ""))
            }
        }
        return dependencyResolver
    }

    // fixme: isn't going to work on JDK 9
    private fun loadJARs(dependencyResolver: Lazy<MavenDependencyResolver>, artifacts: List<String>) {
        (ClassLoader.getSystemClassLoader() as java.net.URLClassLoader)
                .addURLs(artifacts.flatMap { artifact ->
                    if (debug) {
                        LoggerService.addNormalLine("[DEBUG] Resolving $artifact")
                    }
                    val result = try {
                        dependencyResolver.value.resolve(DefaultArtifact(artifact)).map { it.toURI().toURL() }
                    } catch (e: IllegalArgumentException) {
                        val file = File(expandTilde(artifact))
                        if (!file.exists()) {
                            LoggerService.addRedLine("Error: $artifact does not exist")
                            // exitProcess(1)
                            // return
                            throw e
                        }
                        listOf(file.toURI().toURL())
                    } catch (e: RepositoryException) {
                        if (debug) {
                            LoggerService.addNormalLine(ExceptionUtils.getStackTrace(e))
                        }
                        LoggerService.addRedLine("Error: $artifact wasn't found")
                        // exitProcess(1)
                        throw e
                    }
                    if (debug) {
                        result.forEach { url -> LoggerService.addNormalLine("[DEBUG] Loading $url") }
                    }
                    if (!skipClasspathCheck) {
                        if (result.any { it.toString().substringAfterLast("/").startsWith("ktlint-core-") }) {
                            LoggerService.addRedLine("\"$artifact\" appears to have a runtime/compile dependency on \"ktlint-core\".\n" +
                                    "Please inform the author that \"com.github.shyiko:ktlint*\" should be marked " +
                                    "compileOnly (Gradle) / provided (Maven).\n" +
                                    "(to suppress this warning use --skip-classpath-check)")
                        }
                        if (result.any { it.toString().substringAfterLast("/").startsWith("kotlin-stdlib-") }) {
                            LoggerService.addRedLine("\"$artifact\" appears to have a runtime/compile dependency on \"kotlin-stdlib\".\n" +
                                    "Please inform the author that \"org.jetbrains.kotlin:kotlin-stdlib*\" should be marked " +
                                    "compileOnly (Gradle) / provided (Maven).\n" +
                                    "(to suppress this warning use --skip-classpath-check)")
                        }
                    }
                    result
                })
    }

    private fun parseQuery(query: String) = query.split("&")
            .fold(LinkedHashMap<String, String>()) { map, s ->
                if (!s.isEmpty()) {
                    s.split("=", limit = 2).let { e ->
                        map.put(e[0],
                                URLDecoder.decode(e.getOrElse(1) { "true" }, "UTF-8"))
                    }
                }
                map
            }

    private fun lint(
        fileName: String,
        text: String,
        ruleSets: Iterable<RuleSet>,
        userData: Map<String, String>,
        cb: (e: LintError) -> Unit
    ) =
            if (fileName.endsWith(".kt", ignoreCase = true)) {
                KtLint.lint(text, ruleSets, userData, cb)
            } else {
                KtLint.lintScript(text, ruleSets, userData, cb)
            }

    private fun format(
        fileName: String,
        text: String,
        ruleSets: Iterable<RuleSet>,
        userData: Map<String, String>,
        cb: (e: LintError, corrected: Boolean) -> Unit
    ): String =
            if (fileName.endsWith(".kt", ignoreCase = true)) {
                KtLint.format(text, ruleSets, userData, cb)
            } else {
                KtLint.formatScript(text, ruleSets, userData, cb)
            }

    private fun java.net.URLClassLoader.addURLs(url: Iterable<java.net.URL>) {
        val method = java.net.URLClassLoader::class.java.getDeclaredMethod("addURL", java.net.URL::class.java)
        method.isAccessible = true
        url.forEach { method.invoke(this, it) }
    }

    private fun File.mkdirsOrFail() {
        if (!mkdirs() && !isDirectory) {
            throw IOException("Unable to create \"${this}\" directory")
        }
    }

    /**
     * Executes "Callable"s in parallel (lazily).
     * The results are gathered one-by-one (by `cb(<callable result>)`) in the order of corresponding "Callable"s
     * in the "Sequence" (think `seq.toList().map { executorService.submit(it) }.forEach { cb(it.get()) }` but without
     * buffering an entire sequence).
     *
     * Once kotlinx-coroutines are out of "experimental" stage everything below can be replaced with
     * ```
     * suspend fun <T> Sequence<Callable<T>>.parallel(...) {
     *     val ctx = newFixedThreadPoolContext(numberOfThreads, "Sequence<Callable<T>>.parallel")
     *     ctx.use {
     *         val channel = produce(ctx, numberOfThreads) {
     *             for (task in this@parallel) {
     *                 send(async(ctx) { task.call() })
     *             }
     *         }
     *         for (res in channel) {
     *             cb(res.await())
     *         }
     *     }
     * }
     * ```
     */
    private fun <T> Sequence<Callable<T>>.parallel(
        cb: (T) -> Unit,
        numberOfThreads: Int = Runtime.getRuntime().availableProcessors()
    ) {
        val pill = object : Future<T> {

            override fun isDone(): Boolean {
                throw UnsupportedOperationException()
            }

            override fun get(timeout: Long, unit: TimeUnit): T {
                throw UnsupportedOperationException()
            }

            override fun get(): T {
                throw UnsupportedOperationException()
            }

            override fun cancel(mayInterruptIfRunning: Boolean): Boolean {
                throw UnsupportedOperationException()
            }

            override fun isCancelled(): Boolean {
                throw UnsupportedOperationException()
            }
        }
        val q = ArrayBlockingQueue<Future<T>>(numberOfThreads)
        val producer = thread(start = true) {
            val executorService = Executors.newCachedThreadPool()
            try {
                for (task in this) {
                    q.put(executorService.submit(task))
                }
                q.put(pill)
            } catch (e: InterruptedException) {
                // we've been asked to stop consuming sequence
            } finally {
                executorService.shutdown()
            }
        }
        try {
            while (true) {
                val result = q.take()
                if (result != pill) cb(result.get()) else break
            }
        } finally {
            producer.interrupt() // in case q.take()/result.get() throws
            producer.join()
        }
    }

    private fun exitProcess(code: Int) {
        throw ExitProcessException(code)
    }
}

class ExitProcessException(code: Int) : RuntimeException()
