package com.tencent.devops.environment.pojo

import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.core.JsonToken
import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.databind.JsonDeserializer
import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import com.tencent.devops.common.api.exception.InvalidParamException

data class ResourceRemaining(
    @field:JsonDeserialize(using = StrictPercentageDeserializer::class)
    val cpu: Int? = null,
    @field:JsonDeserialize(using = StrictPercentageDeserializer::class)
    val memory: Int? = null,
    @field:JsonDeserialize(using = StrictPercentageDeserializer::class)
    val disk: Int? = null
) {
    fun validate() {
        if (listOfNotNull(cpu, memory, disk).isEmpty()) {
            throw InvalidParamException("资源剩余至少填写一项")
        }
        if (listOfNotNull(cpu, memory, disk).any { it !in 1..99 }) {
            throw InvalidParamException("请输入 1 到 99 的整数")
        }
    }
}

class StrictPercentageDeserializer : JsonDeserializer<Int>() {
    override fun deserialize(parser: JsonParser, context: DeserializationContext): Int {
        if (parser.currentToken != JsonToken.VALUE_NUMBER_INT || parser.bigIntegerValue !in
            java.math.BigInteger.ONE..java.math.BigInteger.valueOf(99)) {
            throw InvalidParamException("请输入 1 到 99 的整数")
        }
        return parser.intValue
    }
}

data class AgentResourceSnapshot(
    val cpu: Double? = null,
    val cpuTime: Long? = null,
    val memory: Double? = null,
    val memoryTime: Long? = null,
    val disk: Double? = null,
    val diskTime: Long? = null
) {
    companion object {
        const val MAX_AGE_MILLIS = 180_000L
        fun fresh(value: Double?, time: Long?, now: Long): Double? = value?.takeIf {
            it.isFinite() && it in 0.0..100.0 && time != null && time <= now && now - time <= MAX_AGE_MILLIS
        }
    }
}
