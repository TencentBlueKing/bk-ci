package com.tencent.devops.stream.trigger.parsers.triggerMatch

import com.tencent.devops.common.ci.v2.TriggerOn
import com.tencent.devops.stream.pojo.GitProjectPipeline
import com.tencent.devops.stream.pojo.GitRequestEvent
import com.tencent.devops.stream.pojo.git.GitEvent
import com.tencent.devops.stream.pojo.git.GitMergeRequestEvent
import com.tencent.devops.stream.pojo.git.GitPushEvent
import com.tencent.devops.stream.pojo.git.GitTagPushEvent
import org.springframework.stereotype.Component

class TriggerMatcher(
    val event: GitEvent,
    val gitRequestEvent: GitRequestEvent,
    val pipeline: GitProjectPipeline,
    val triggerOn: TriggerOn,
    val changeList: List<String>
) {

    fun match(): TriggerResult{
        when(event){
            is GitPushEvent ->{

            }
            is GitMergeRequestEvent ->{

            }
            is GitTagPushEvent ->{

            }
        }
    }

    private fun getBranch(): String {
        return when (event) {
            is GitPushEvent -> event.ref.removePrefix("refs/heads/")
            is GitTagPushEvent -> event.ref.removePrefix("refs/heads/")
            is GitMergeRequestEvent -> event.object_attributes.target_branch
            else -> ""
        }
    }

    private fun getTag(): String {
        return when (event) {
            is GitPushEvent -> event.ref.removePrefix("refs/tags/")
            is GitTagPushEvent -> event.ref.removePrefix("refs/tags/")
            is GitMergeRequestEvent -> event.object_attributes.target_branch
            else -> ""
        }
    }
}

