package com.tencent.devops.remotedev.pojo.job

/**
 * @param keyMap 作为执行时schema参数映射，keyMap中的key为执行放需要的key，value为schema中对应的可以取值的
 */
@Suppress("UnnecessaryAbstractClass")
abstract class JobActionExtraParam(
    open val keyMap: Map<String, KeyMapData>
)

/**
 *
 */
data class KeyMapData(
    val name: String,
    val type: KeyMapDataType = KeyMapDataType.STRING,
    val required: Boolean = true
)

enum class KeyMapDataType {
    STRING,
    ARRAY
}

data class JobBackendActionExtraParam(
    override val keyMap: Map<String, KeyMapData>
) : JobActionExtraParam(keyMap)

data class JobPipelineActionExtraParam(
    override val keyMap: Map<String, KeyMapData>,
    val userId: String,
    val projectId: String,
    val pipelineId: String
) : JobActionExtraParam(keyMap)
