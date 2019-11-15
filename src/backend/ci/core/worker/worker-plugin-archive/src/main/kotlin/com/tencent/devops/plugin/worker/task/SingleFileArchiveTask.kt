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

package com.tencent.devops.plugin.worker.task

import com.tencent.devops.common.api.exception.ParamBlankException
import com.tencent.devops.common.archive.element.SingleArchiveElement
import com.tencent.devops.process.pojo.BuildTask
import com.tencent.devops.process.pojo.BuildVariables
import com.tencent.devops.worker.common.task.ITask
import com.tencent.devops.worker.common.task.TaskClassType
import com.tencent.devops.worker.common.utils.ArchiveUtils.archiveCustomFiles
import com.tencent.devops.worker.common.utils.ArchiveUtils.archivePipelineFiles
import java.io.File

@TaskClassType(classTypes = [SingleArchiveElement.classType])
class SingleFileArchiveTask : ITask() {

    override fun execute(buildTask: BuildTask, buildVariables: BuildVariables, workspace: File) {
        val taskParams = buildTask.params ?: mapOf()
        val filePath = taskParams["filePath"] ?: throw ParamBlankException("param [filePath] is empty")
        val isCustomize = taskParams["customize"] ?: throw ParamBlankException("param [isCustomize] is empty")

        val count = if (isCustomize.toBoolean()) {
            val destPath = taskParams["destPath"] ?: throw ParamBlankException("param [destPath] is empty")
            archiveCustomFiles(filePath, destPath, workspace, buildVariables)
        } else {
            archivePipelineFiles(filePath, workspace, buildVariables)
        }
        if (count == 0) {
            throw RuntimeException("没有匹配到任何待归档文件，请检查工作空间下面的文件")
        }
    }
}