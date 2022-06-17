package com.tencent.devops.worker.common.task

import com.tencent.devops.common.pipeline.pojo.element.agent.GithubElement
import com.tencent.devops.plugin.worker.task.scm.GithubPullTask
import org.junit.Assert
import org.junit.Test

internal class TaskFactoryTest {

    @Test
    fun init() {
        TaskFactory.init()
        val task = TaskFactory.create(GithubElement.classType)
        Assert.assertTrue(task is GithubPullTask)
    }
}
