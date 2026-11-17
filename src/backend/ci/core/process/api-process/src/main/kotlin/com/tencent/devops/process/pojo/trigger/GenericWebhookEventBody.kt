package com.tencent.devops.process.pojo.trigger

import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.databind.JsonDeserializer
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import com.fasterxml.jackson.module.kotlin.readValue
import com.tencent.devops.common.api.util.JsonUtil
import io.swagger.v3.oas.annotations.media.Schema
import org.slf4j.LoggerFactory

/**
 * 通用 webhook 事件请求体
 *
 * [body] 统一存原始请求体 JSON 字符串，[bodyClazz] 存该字符串反序列化的目标类全限定名：
 * - 表单式/字段映射平台（如 TAPD、market 远程开发）：body 为扁平 map 的 JSON，读取用 [bodyAsMap]；
 * - 嵌套 JSON、需还原为强类型对象的平台（如制品到达事件）：body 为原始报文，读取用 [parseBody]。
 *
 * 兼容旧数据：历史记录里 body 曾是 JSON 对象（`Map<String,String>`），[FlexibleBodyDeserializer]
 * 会把 JSON 对象/数组自动转成其 JSON 字符串，字符串则原样读取，因此新老数据都能读。
 */
@Schema(title = "通用webhook事件请求体")
data class GenericWebhookEventBody(
    @get:Schema(description = "请求头")
    val headers: Map<String, String>? = null,
    @get:Schema(description = "请求参数")
    val queryParams: Map<String, String>? = null,
    @get:Schema(description = "请求体（原始JSON字符串）")
    @param:JsonDeserialize(using = FlexibleBodyDeserializer::class)
    val body: String? = null,
    @get:Schema(description = "body反序列化目标类全限定名")
    val bodyClazz: String? = null
) : TriggerEventBody {

    /**
     * 依据 [bodyClazz] 将 [body] 还原为具体事件对象；缺少 body/bodyClazz 或反序列化失败时返回 null
     */
    fun <T> parseBody(): T? {
        if (body.isNullOrBlank() || bodyClazz.isNullOrBlank()) return null
        return try {
            @Suppress("UNCHECKED_CAST")
            val clazz = Class.forName(bodyClazz) as Class<T>
            JsonUtil.to(body, clazz)
        } catch (ignored: Throwable) {
            logger.warn("fail to parse webhook body|bodyClazz=$bodyClazz", ignored)
            null
        }
    }

    /**
     * 将 [body] 反序列化为扁平 `Map<String, String>`（表单式平台使用）；body 为空或解析失败返回 null
     */
    fun bodyAsMap(): Map<String, String>? {
        if (body.isNullOrBlank()) return null
        return try {
            JsonUtil.getObjectMapper().readValue<Map<String, String>>(body)
        } catch (ignored: Throwable) {
            logger.warn("fail to parse webhook body as map", ignored)
            null
        }
    }

    companion object {
        private val logger = LoggerFactory.getLogger(GenericWebhookEventBody::class.java)
        const val classType = "generic"

        /**
         * 用扁平 map 构造 body（表单式平台）：序列化为 JSON 字符串存入 [body]
         */
        fun ofMap(map: Map<String, String>): String = JsonUtil.toJson(map, false)
    }
}

/**
 * body 字段兼容反序列化器
 *
 * - 字符串（新数据）：原样返回；
 * - JSON 对象/数组（旧数据，body 曾是 Map）：转成其 JSON 字符串；
 * - null：返回 null。
 */
class FlexibleBodyDeserializer : JsonDeserializer<String?>() {
    override fun deserialize(p: JsonParser, ctxt: DeserializationContext): String? {
        val node = p.readValueAsTree<JsonNode>() ?: return null
        return when {
            node.isNull -> null
            node.isTextual -> node.asText()
            else -> node.toString()
        }
    }
}
