package com.tencent.devops.stream.common.exception

import com.tencent.devops.stream.pojo.GitProjectPipeline
import com.tencent.devops.stream.pojo.enums.GitCICommitCheckState
import com.tencent.devops.common.webhook.pojo.code.git.GitEvent
import com.tencent.devops.stream.pojo.GitRequestEventForHandle
import com.tencent.devops.stream.pojo.v2.GitCIBasicSetting

open class TriggerBaseException(
    val requestEvent: GitRequestEventForHandle,
    val reasonParams: List<String>? = null,
    val gitEvent: GitEvent? = null,
    val commitCheck: CommitCheck? = null,
    val basicSetting: GitCIBasicSetting? = null,
    val pipeline: GitProjectPipeline? = null,
    val yamls: Yamls? = null,
    val version: String? = null,
    val filePath: String? = null
) : Exception()

data class CommitCheck(
    val isNoPipelineCheck: Boolean = false,
//    val push: Boolean,
    val block: Boolean,
    val state: GitCICommitCheckState
)

data class Yamls(
    val originYaml: String?,
    val parsedYaml: String?,
    val normalYaml: String?
)
