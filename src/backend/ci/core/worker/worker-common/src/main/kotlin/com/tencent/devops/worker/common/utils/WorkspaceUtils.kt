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
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated
 * documentation files (the "Software"), to deal in the Software without restriction, including without limitation the
 * rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to
 * permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of
 * the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT
 * LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN
 * NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE
 * SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package com.tencent.devops.worker.common.utils

import com.tencent.devops.common.log.pojo.TaskBuildLogProperty
import com.tencent.devops.common.log.pojo.enums.LogStorageMode
import java.io.File
import java.io.IOException

@Suppress("TooManyFunctions")
object WorkspaceUtils {

    fun getLandun() = File(".")

    fun getWorkspace() = File(getLandun(), "workspace")

    fun getPipelineWorkspace(pipelineId: String, workspace: String): File {
        return if (workspace.isNotBlank()) {
            File(workspace) // .normalize() 会导致在windows机器下填写 ./ 时，File.exists() 会返回false，表示文件夹不存在
        } else {
            File(getWorkspace(), "$pipelineId/src").normalize()
        }
    }

    fun getPipelineLogDir(pipelineId: String): File {
        val prefix = "DEVOPS_BUILD_LOGS_${pipelineId}_"
        var tmpDir = System.getProperty("java.io.tmpdir")
        val errorMsg = try {
            val dir = File.createTempFile(prefix, null, null)
            dir.delete()
            if (dir.mkdir()) {
                return dir
            }
            if (!dir.startsWith(tmpDir)) { // #5046 做一次修正
                tmpDir = dir.parent
            }
            "temporary directory create failed"
        } catch (ioe: IOException) {
            ioe.message
        }
        throw IOException("$tmpDir: $errorMsg")
    }

    @Suppress("LongParameterList")
    fun getBuildLogProperty(
        pipelineLogDir: File,
        pipelineId: String,
        buildId: String,
        elementId: String,
        executeCount: Int,
        logStorageMode: LogStorageMode
    ): TaskBuildLogProperty {
        val childPath = getBuildLogChildPath(pipelineId, buildId, elementId, executeCount)
        val logFile = File(pipelineLogDir, childPath)
        logFile.parentFile.mkdirs()
        logFile.createNewFile()
        return TaskBuildLogProperty(elementId, childPath, logFile, logStorageMode)
    }

    private fun getBuildLogChildPath(
        pipelineId: String,
        buildId: String,
        elementId: String,
        executeCount: Int
    ) = "/$pipelineId/$buildId/$elementId/$executeCount.log"
}
