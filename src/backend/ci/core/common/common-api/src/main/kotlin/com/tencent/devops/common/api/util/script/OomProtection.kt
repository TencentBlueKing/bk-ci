package com.tencent.devops.common.api.util.script

import org.apache.commons.exec.CommandLine
import org.apache.commons.exec.DefaultExecutor
import org.slf4j.LoggerFactory
import java.io.File

/**
 * agent 和 worker 独立读取同名环境变量，不检查对方版本，也不要求配套发布。
 *
 * 新 worker 开启后保护自身，并在用户命令 exec 前解除子进程继承的 -1000。
 * 旧 worker 不识别此开关，允许它及其用户进程继续继承 agent 的保护。
 * 开关在 JVM 启动时冻结，不读取任务变量，避免并发任务改变进程启动策略。
 */
object OomProtection {
    // 只处理宿主机普通构建；平台将同名环境变量传入容器时，不启用 Docker 保护。
    val enabled: Boolean = System.getProperty("build.type") == "AGENT" &&
        System.getProperty("os.name").startsWith("Linux") &&
        System.getenv("DEVOPS_AGENT_OOM_PROTECT")?.trim().equals("true", ignoreCase = true)

    /**
     * 不要求新版 agent 已经保护 JVM：旧 agent 启动的新 worker 也可自行设置分数。
     * 已继承 -1000 时无需重复写入，兼容 agent 启动后切换到普通构建用户的情况。
     * 缺少 root/CAP_SYS_RESOURCE 等权限时记录警告并沿用原分数，不阻断正常构建。
     * 无论自身保护是否成功，开启的命令包装仍会将用户子进程恢复为 0。
     */
    fun protectWorker() {
        if (!enabled) return
        try {
            val score = File("/proc/self/oom_score_adj")
            if (score.readText().trim() != "-1000") {
                score.writeText("-1000")
                check(score.readText().trim() == "-1000") { "worker OOM score verification failed" }
            }
        } catch (e: Exception) {
            LoggerFactory.getLogger(OomProtection::class.java).warn(
                "Cannot protect worker from OOM; continuing with current score. " +
                    "Lowering oom_score_adj requires root/CAP_SYS_RESOURCE.", e
            )
        }
    }

    // 只使用 shell 内建命令，在子进程内重置并读回确认，再执行用户命令。
    // 不临时修改父 JVM 的分数，避免暴露 JVM 或影响并发命令。
    // "$@" 保留参数边界，exec 保留 PID/退出码；失败返回 125，不运行该用户命令。
    internal const val RESET_AND_EXEC =
        "printf '0' > /proc/self/oom_score_adj || exit 125; " +
        "IFS= read -r score < /proc/self/oom_score_adj || exit 125; " +
        "[ \"\$score\" = 0 ] || exit 125; exec \"\$@\""

    fun wrap(command: CommandLine, protect: Boolean = enabled): CommandLine {
        if (!protect) return command
        return CommandLine("/bin/sh")
            .addArgument("-c", false)
            .addArgument(RESET_AND_EXEC, false)
            .addArgument("bk-ci-user-command", false)
            .addArguments(command.toStrings(), false)
    }
}

/** 覆盖 commons-exec 的公共 launch 边界，同步、异步及不同 execute 重载均经过这里。 */
open class OomAwareExecutor : DefaultExecutor() {
    override fun launch(
        command: CommandLine,
        environment: Map<String, String>?,
        dir: File?
    ): Process = super.launch(OomProtection.wrap(command), environment, dir)
}
