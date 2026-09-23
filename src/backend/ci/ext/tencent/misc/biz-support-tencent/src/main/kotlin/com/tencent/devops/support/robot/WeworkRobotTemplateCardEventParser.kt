package com.tencent.devops.support.robot

import com.tencent.devops.notify.pojo.wework.WeworkReviewCardConst
import org.w3c.dom.Document
import org.xml.sax.InputSource
import java.io.StringReader
import javax.xml.parsers.DocumentBuilderFactory

/**
 * 解析消息推送（原群机器人）template_card 点击回调。
 * 文档 path/94874 为小写节点（event_key / task_id / from.alias）；
 * 应用消息回调仍可能是 EventKey / UserId。
 */
object WeworkRobotTemplateCardEventParser {

    data class ReviewCardClick(
        val userId: String,
        val action: String,
        val taskId: String,
        val selectedItems: Map<String, String>
    )

    fun parse(xmlString: String): ReviewCardClick? {
        if (xmlString.isBlank()) return null
        val document = parseXml(xmlString)
        val eventKey = firstTag(document, "event_key", "EventKey")
        val parsed = WeworkReviewCardConst.parseButtonKey(eventKey) ?: return null
        val userId = firstTag(document, "alias", "Alias", "userid", "UserId", "Id")
        return ReviewCardClick(
            userId = WeworkReviewCardConst.weworkUserId(userId),
            action = parsed.first,
            taskId = parsed.second,
            selectedItems = parseSelectedItems(document)
        )
    }

    private fun parseSelectedItems(document: Document): Map<String, String> {
        val result = linkedMapOf<String, String>()
        val items = document.getElementsByTagName("selected_item").takeIf { it.length > 0 }
            ?: document.getElementsByTagName("SelectedItem")
        for (i in 0 until items.length) {
            val item = items.item(i)
            val children = item.childNodes
            var key = ""
            var optionId = ""
            for (j in 0 until children.length) {
                val node = children.item(j)
                when (node.nodeName) {
                    "question_key", "QuestionKey" -> key = node.textContent.orEmpty()
                    "option_ids", "OptionIds" -> {
                        val optionNodes = node.childNodes
                        for (k in 0 until optionNodes.length) {
                            val optionName = optionNodes.item(k).nodeName
                            if (optionName == "option_id" || optionName == "OptionId") {
                                optionId = optionNodes.item(k).textContent.orEmpty()
                                break
                            }
                        }
                    }
                }
            }
            if (key.isNotBlank() && optionId.isNotBlank()) {
                result[key] = optionId
            }
        }
        return result
    }

    private fun firstTag(document: Document, vararg names: String): String {
        names.forEach { name ->
            val nodes = document.getElementsByTagName(name)
            if (nodes.length > 0) {
                val text = nodes.item(0).textContent.orEmpty()
                if (text.isNotBlank()) return text
            }
        }
        return ""
    }

    private fun parseXml(xmlString: String): Document {
        val dbf = DocumentBuilderFactory.newInstance()
        val db = dbf.newDocumentBuilder()
        return db.parse(InputSource(StringReader(xmlString)))
    }
}
