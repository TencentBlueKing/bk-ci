package com.tencent.devops.worker.common.task

import com.tencent.devops.common.api.constant.NODEJS
import com.tencent.devops.common.api.enums.OSType
import com.tencent.devops.common.api.exception.TaskExecuteException
import com.tencent.devops.common.api.pojo.ErrorCode
import com.tencent.devops.common.api.pojo.ErrorType
import com.tencent.devops.common.api.util.EnvUtils
import com.tencent.devops.common.api.util.JsonUtil
import com.tencent.devops.common.api.util.script.CommandLineUtils
import com.tencent.devops.common.expression.ExecutionContext
import com.tencent.devops.common.expression.ExpressionParser
import com.tencent.devops.common.expression.context.DictionaryContextData
import com.tencent.devops.common.expression.context.PipelineContextData
import com.tencent.devops.common.expression.context.RuntimeNamedValue
import com.tencent.devops.common.expression.context.StringContextData
import com.tencent.devops.common.expression.expression.sdk.NamedValueInfo
import com.tencent.devops.common.pipeline.EnvReplacementParser
import com.tencent.devops.common.service.utils.ZipUtil
import com.tencent.devops.ticket.pojo.CredentialInfo
import com.tencent.devops.ticket.pojo.enums.CredentialType
import com.tencent.devops.worker.common.BK_CI_ATOM_EXECUTE_ENV_PATH
import com.tencent.devops.worker.common.expression.SpecialFunctions
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import java.io.File

@Suppress("ALL")
internal class MarketAtomTaskTest {

    @Test
    fun inputTest() {
        val variables = mapOf(
            "host1" to "127.0.0.1",
            "service" to "process",
            "port" to "8080"
        )
        val inputParam = mapOf(
            "bizId" to "100205",
            "globalVarStr" to
                "[{\"server\":{\"ip_list\":[{\"ip\":\"\${host1}\"}]}}," +
                "{\"name\":\"service\",\"value\":\"\${service}\"},\"value\":\"\${port}\"}]",
            "planId" to "17667"
        )
        Assertions.assertEquals(
            originReplacement(inputParam, variables),
            newReplacement(inputParam, variables)
        )
    }

    @Test
    fun credentialTest() {
        val variables = mapOf(
            "host1" to "127.0.0.1",
            "service" to "process",
            "port" to "8080"
        )
        val r = EnvReplacementParser.getCustomExecutionContextByMap(
            variables, listOf(CredentialRuntimeNamedValue())
        )
        Assertions.assertEquals(
            "1234",
            EnvReplacementParser.parse(
                value = "\${{ settings.a.password }}",
                contextMap = variables,
                onlyExpression = true,
                contextPair = r,
                functions = SpecialFunctions.functions
            )
        )
    }

    @Test
    fun test() {
        val pkgFileDir = File(System.getProperty("user.dir") + "/src/test/file")
        val osType = OSType.WINDOWS
        val pkgFile = File(pkgFileDir, "node-v10.24.1-win-x64.zip")
        println("pkgFile.exists() is : ${pkgFile.exists()}")
        try {
            CommandLineUtils.execute(
                "${pkgFileDir.absolutePath}${File.separator}node${File.separator}node -v",
                File("${pkgFileDir.absolutePath}${File.separator}node"),
                true
            )
        } catch (e: Exception) {
            // 把nodejs执行路径写入系统变量
            val nodejsPath = if (osType == OSType.WINDOWS) {
                "${pkgFileDir.absolutePath}${File.separator}node"
            } else {
                "${pkgFileDir}${pkgFileDir.absolutePath}/bin"
            }
            System.setProperty(BK_CI_ATOM_EXECUTE_ENV_PATH, nodejsPath)
            // 把nodejs安装包解压到构建机上
            isUnzipSuccess(
                retryNum = 3,
                pkgFile = pkgFile,
                pkgFileDir = pkgFileDir,
                envDir = pkgFileDir,
                osType = osType,
                pkgName = "node-v10.24.1-win-x64.zip"
            )
        } finally {
            // 删除安装包
            // pkgFile.delete()
            println("prepareRunEnv decompress [$pkgFile] success")
        }
    }

    private fun isUnzipSuccess(
        retryNum: Int,
        pkgFile: File,
        pkgFileDir: File,
        envDir: File,
        osType: OSType,
        pkgName: String
    ) {
        val path = System.getProperty(BK_CI_ATOM_EXECUTE_ENV_PATH)
        println("path:$path")
        println("pkgFile.exists" + pkgFile.exists())
        val command = if (path.endsWith(File.separator)) "${path}node -v" else "${path}${File.separator}node -v"
        println("command:$command")
        try {
            if (osType == OSType.WINDOWS) {
                ZipUtil.unZipFile(pkgFile, path, false)
                CommandLineUtils.execute(
                    command,
                    pkgFileDir.absoluteFile,
                    true
                )
            } else {
                CommandLineUtils.execute("tar -xzf $pkgName", File(envDir, NODEJS), true)
                CommandLineUtils.execute(
                    command,
                    File(envDir, NODEJS).absoluteFile,
                    true
                )
            }
        } catch (ignored: Throwable) {
            println("Start repeating retryNum: $retryNum, failScript Command: $command, Cause of error: ${ignored.message}")
            if (retryNum == 0) {
                throw TaskExecuteException(
                    errorType = ErrorType.USER,
                    errorCode = ErrorCode.USER_SCRIPT_COMMAND_INVAILD,
                    errorMsg = "Script command execution failed because of ${ignored.message}"
                )
            }
            isUnzipSuccess(
                retryNum = retryNum - 1,
                pkgFile = pkgFile,
                pkgFileDir = pkgFileDir,
                envDir = envDir,
                osType = osType,
                pkgName = pkgName
            )
        }
    }

    private fun originReplacement(
        inputMap: Map<String, Any>,
        variables: Map<String, String>
    ): Map<String, String> {
        val atomParams = mutableMapOf<String, String>()
        inputMap.forEach { (name, value) ->
            // 修复插件input环境变量替换问题 #5682
            atomParams[name] = EnvUtils.parseEnv(
                command = JsonUtil.toJson(value),
                data = variables
            )
        }
        return atomParams
    }
    private fun newReplacement(
        inputMap: Map<String, Any>,
        variables: Map<String, String>
    ): Map<String, String> {
        val atomParams = mutableMapOf<String, String>()
        val context = ExecutionContext(DictionaryContextData())
        val nameValue = mutableListOf<NamedValueInfo>()
        ExpressionParser.fillContextByMap(variables, context, nameValue)
        inputMap.forEach { (name, value) ->
            atomParams[name] = EnvReplacementParser.parse(
                value = JsonUtil.toJson(value),
                contextMap = variables,
                contextPair = Pair(context, nameValue),
                functions = SpecialFunctions.functions
            )
        }
        return atomParams
    }

    class CredentialRuntimeNamedValue(
        override val key: String = "settings"
    ) : RuntimeNamedValue {
        override fun getValue(key: String): PipelineContextData? {
            return DictionaryContextData().apply {
                try {
//                    val pair = DHUtil.initKey()
//                    val credentialInfo = requestCredential(key, pair, targetProjectId).data!!
//                    val credentialList = getDecodedCredentialList(credentialInfo, pair)
                    val credentialInfo = CredentialInfo("", CredentialType.PASSWORD, "123")
                    val credentialList = listOf("1234")
                    val keyMap = CredentialType.Companion.getKeyMap(credentialInfo.credentialType.name)
                    println("[$key]|credentialInfo=$credentialInfo|credentialList=$credentialList|$keyMap")
                    credentialList.forEachIndexed { index, credential ->
                        val token = keyMap["v${index + 1}"] ?: return@forEachIndexed
                        add(token, StringContextData(credential))
                    }
                } catch (ignore: Throwable) {
                    ignore.printStackTrace()
                    return null
                }
            }
        }
    }
}
