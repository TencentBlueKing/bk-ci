package com.tencent.devops.worker.common.task

import com.tencent.devops.common.pipeline.pojo.element.agent.GithubElement
import com.tencent.devops.plugin.worker.task.scm.GithubPullTask
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test

internal class TaskFactoryTest {

    @Test
    fun init() {
        TaskFactory.init()
        val task = TaskFactory.create(GithubElement.classType)
        Assertions.assertTrue(task is GithubPullTask)
    }
}
