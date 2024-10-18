package com.tencent.devops.gpt.service.processor

import com.tencent.devops.common.pipeline.pojo.element.Element
import com.tencent.devops.common.pipeline.pojo.element.agent.LinuxScriptElement
import com.tencent.devops.common.pipeline.pojo.element.agent.WindowsScriptElement
import com.tencent.devops.common.pipeline.pojo.element.market.MarketBuildAtomElement
import com.tencent.devops.common.web.utils.I18nUtil
import com.tencent.devops.gpt.constant.GptMessageCode.SCRIPT_ERROR_ANALYSIS_CHAT_TASK_NOT_SUPPORT
import com.tencent.devops.gpt.constant.GptMessageCode.SCRIPT_ERROR_ANALYSIS_CHAT_TASK_STRUCTURAL_DAMAGE
import dev.langchain4j.memory.chat.MessageWindowChatMemory
import dev.langchain4j.service.MemoryId
import dev.langchain4j.service.SystemMessage
import dev.langchain4j.service.TokenStream
import dev.langchain4j.service.UserMessage
import dev.langchain4j.service.V
import java.net.URLDecoder
import org.glassfish.jersey.server.ChunkedOutput
import dev.langchain4j.data.message.SystemMessage as SM
import dev.langchain4j.data.message.UserMessage as UM

class ScriptErrorAnalysisProcessor(private val output: ChunkedOutput<String>) {
    private val lineOne = StringBuilder()
    val aiMsg = StringBuilder()
    private var valid: Boolean? = null
    val pushMsg: MessageWindowChatMemory = MessageWindowChatMemory.withMaxMessages(10)
    fun init() {
        lineOne.clear()
        aiMsg.clear()
        pushMsg.clear()
        valid = null
    }

    fun getPushSystemMsg(): String {
        return pushMsg.messages().find { it is SM }?.text() ?: ""
    }

    fun getPushUserMsg(): String {
        val msg = pushMsg.messages().filterIsInstance<UM>().firstOrNull() ?: return ""
        return msg.contents().joinToString { it.toString() }
    }

    fun checkSucceed() = valid == true

    fun next(input: String): Boolean {
        if (valid == null) {
            lineOne.append(input)
            if (input.contains("\n") && lineOne.contains("yes")) {
                valid = true
            }
            if (input.contains("\n") && lineOne.contains("no")) {
                valid = false
            }
        }
        when (valid) {
            true -> {
                aiMsg.append(input)
                output.write(input)
                return true
            }

            false -> return false
            null -> return true
        }
    }

    fun getTaskScript(ele: Element): List<String>? {
        if (ele is LinuxScriptElement) {
            val script = URLDecoder.decode(ele.script, "UTF-8")
            return script.lines().filterNot { it.startsWith("# ") || it.isBlank() }
        }
        if (ele is WindowsScriptElement) {
            val script = URLDecoder.decode(ele.script, "UTF-8")
            return script.lines().filterNot { it.startsWith("REM ") || it.isBlank() }
        }
        if (ele is MarketBuildAtomElement && ele.getAtomCode() == "run") {
            val input = ele.data["input"] as Map<String, Any>? ?: run {
                output.write(
                    I18nUtil.getCodeLanMessage(
                        SCRIPT_ERROR_ANALYSIS_CHAT_TASK_STRUCTURAL_DAMAGE,
                        params = arrayOf("input")
                    )
                )
                return null
            }
            val script = input["script"] as String? ?: run {
                output.write(
                    I18nUtil.getCodeLanMessage(
                        SCRIPT_ERROR_ANALYSIS_CHAT_TASK_STRUCTURAL_DAMAGE,
                        params = arrayOf("input.script")
                    )
                )
                return null
            }
            return script.lines().filterNot { it.isBlank() }
        }
        output.write(I18nUtil.getCodeLanMessage(SCRIPT_ERROR_ANALYSIS_CHAT_TASK_NOT_SUPPORT))
        return null
    }

    interface Prompt {
        @SystemMessage("You are a professional script engineer.")
        @UserMessage(
            "Please help me find out the reason for the script execution error. " +
                "Please answer me in standard markdown format. " +
                "Please use the format below to provide a detailed answer to the specific cause of the error: " +
                "是否找到错误原因: [yes or no]. 如果为yes, 继续输出。\n" +
                "错误原因: ...\n" +
                "解决办法: ...\n" +
                "The script content is: '{{script}}' and the error log is: '{{errorLog}}'"
        )
        fun ask(
            @MemoryId memoryId: String,
            @V("script") script: String,
            @V("errorLog") errorLog: String
        ): TokenStream
    }
}
