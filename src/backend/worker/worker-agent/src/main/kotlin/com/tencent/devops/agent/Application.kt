/*
 * Tencent is pleased to support the open source community by making BK-CI 蓝鲸持续集成平台 available.
 *
 * Copyright (C) 2019 THL A29 Limited, a Tencent company.  All rights reserved.
 *
 * BK-CI 蓝鲸持续集成平台 is licensed under the MIT license.
 *
 * A copy of the MIT License is included in this file.
 *
 *
 * Terms of the MIT License:
 * ---------------------------------------------------
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation
 * files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy,
 * modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the
 * Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT
 * LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN
 * NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE
 * SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package com.tencent.devops.agent

import com.tencent.devops.agent.runner.WorkRunner
import com.tencent.devops.common.pipeline.ElementSubTypeRegisterLoader
import com.tencent.devops.worker.common.BUILD_TYPE
import com.tencent.devops.worker.common.Runner
import com.tencent.devops.worker.common.WorkspaceInterface
import com.tencent.devops.worker.common.api.ApiFactory
import com.tencent.devops.worker.common.env.BuildType
import com.tencent.devops.worker.common.task.TaskFactory
import java.io.File

fun main(args: Array<String>) {
    ElementSubTypeRegisterLoader.registerElementForJsonUtil()
    ApiFactory.init()
    TaskFactory.init()
    if(System.getProperty(BUILD_TYPE) == BuildType.DOCKER.name){
        System.setProperty(BUILD_TYPE, BuildType.DOCKER.name)
        Runner.run(object : WorkspaceInterface {
            override fun getWorkspace(variables: Map<String, String>, pipelineId: String): File {
                val workspace = System.getProperty("devops_workspace")

                val dir = if (workspace.isNullOrBlank()) {
                    File("/data/devops/workspace")
                } else {
                    File(workspace)
                }
                dir.mkdirs()
                return dir
            }
        })
    } else {
        System.setProperty(BUILD_TYPE, BuildType.AGENT.name)
        WorkRunner.execute(args)
    }
}