package com.tencent.devops.plugin.worker.task.codecc.util

import com.tencent.devops.common.api.util.JsonUtil
import com.tencent.devops.plugin.worker.pojo.CodeccExecuteConfig
import com.tencent.devops.plugin.worker.task.codecc.LinuxCodeccConstants.COV_TOOLS
import com.tencent.devops.worker.common.logger.LoggerService
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger
import kotlin.reflect.KFunction1

object CodeccExecuteHelper {

    fun executeCodecc(
        codeccExecuteConfig: CodeccExecuteConfig,
        covFun: KFunction1<CodeccExecuteConfig, String>,
        toolFun: KFunction1<CodeccExecuteConfig, String>
    ): String {
        val runTypes = getRunTypes(codeccExecuteConfig)
        val runCoverity = runTypes.first
        val runTools = runTypes.second

        val result = StringBuilder()

        var expectCount = 0
        if (runCoverity) expectCount++
        if (runTools) expectCount++

        val lock = CountDownLatch(expectCount)
        val successCount = AtomicInteger(0)
        val errorMsg = StringBuilder()
        // 按照旧的逻辑执行COVERITY
        val executor = Executors.newFixedThreadPool(expectCount)
        try {
            if (runCoverity) {
                // 保证coverity跑失败不影响多工具
                executor.execute {
                    try {
                        result.append(covFun(codeccExecuteConfig))
                        successCount.getAndIncrement()
                        LoggerService.addNormalLine("run coverity or klocwork successful")
                    } catch (e: Exception) {
                        errorMsg.append("run coverity or klocwork fail: ${e.message}\n")
                    } finally {
                        lock.countDown()
                    }
                }
            }
            // 其他类型扫描走新的逻辑
            if (runTools) {
                executor.execute {
                    try {
                        result.append(toolFun(codeccExecuteConfig))
                        successCount.getAndIncrement()
                        LoggerService.addNormalLine("run codecc tools successful")
                    } catch (e: Exception) {
                        errorMsg.append("run codecc tools fail: ${e.message}\n")
                    } finally {
                        lock.countDown()
                    }
                }
            }
            // 判断最后结果
            // 4个小时当做超时
            lock.await(codeccExecuteConfig.timeOut, TimeUnit.MINUTES)
            if (successCount.get() != expectCount) throw RuntimeException("运行codecc任务失败: $errorMsg")

            return result.toString()
        } finally {
            executor.shutdownNow()
        }
    }

    private fun getRunTypes(codeccExecuteConfig: CodeccExecuteConfig): Pair<Boolean, Boolean> {
        with(codeccExecuteConfig) {
            val tools = JsonUtil.to<List<String>>(codeccExecuteConfig.buildTask.params?.get("tools")!!)
            val runCoverity = (filterTools.isEmpty() && COV_TOOLS.minus(tools).size != COV_TOOLS.size) ||
                (filterTools.isNotEmpty() && COV_TOOLS.minus(filterTools).size != COV_TOOLS.size)
            val runTools = (codeccExecuteConfig.filterTools.isEmpty() && tools.minus(COV_TOOLS).isNotEmpty()) ||
                (filterTools.isNotEmpty() && filterTools.minus(COV_TOOLS).isNotEmpty())
            return Pair(runCoverity, runTools)
        }
    }
}