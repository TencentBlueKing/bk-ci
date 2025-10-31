/*
 * Tencent is pleased to support the open source community by making BK-CI 蓝鲸持续集成平台 available.
 *
 * Copyright (C) 2019 Tencent.  All rights reserved.
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

package com.tencent.devops.process.trigger.scm.converter

import com.tencent.devops.process.pojo.trigger.WebhookChangeFiles
import com.tencent.devops.process.yaml.actions.GitActionCommon
import com.tencent.devops.process.yaml.pojo.YamlFileActionType
import com.tencent.devops.scm.api.pojo.Change

object WebhookConverterUtils {

    @SuppressWarnings("NestedBlockDepth")
    fun getChangeFiles(changes: List<Change>): WebhookChangeFiles {
        val allFiles = mutableSetOf<String>()
        val addedFiles = mutableSetOf<String>()
        val updatedFiles = mutableSetOf<String>()
        val deletedFiles = mutableSetOf<String>()
        val renamedFiles = mutableMapOf<String, String>()
        val renamedOldFiles = mutableMapOf<String, String>()
        changes.forEach {
            with(it) {
                when {
                    added -> {
                        allFiles.add(path)
                        addedFiles.add(path)
                    }
                    deleted -> {
                        oldPath?.let { filePath ->
                            allFiles.add(filePath)
                            deletedFiles.add(filePath)
                        }
                    }
                    renamed -> {
                        oldPath?.let { filePath ->
                            allFiles.add(filePath)
                            renamedFiles[path] = filePath
                            renamedOldFiles[filePath] = path
                        }
                        allFiles.add(path)
                    }
                    else ->
                        updatedFiles.add(path)
                }
            }
        }
        // 文件移出.ci目录也算删除
        deletedFiles.addAll(getRemovedCiFiles(renamedOldFiles))
        return WebhookChangeFiles(
            allFiles = allFiles,
            addedFiles = addedFiles,
            updatedFiles = updatedFiles,
            deletedFiles = deletedFiles,
            renamedFiles = renamedFiles,
            renamedOldFiles = renamedOldFiles
        )
    }

    fun getYamlActionType(filePath: String, changeFiles: WebhookChangeFiles): YamlFileActionType {
        return with(changeFiles) {
            when {
                addedFiles.contains(filePath) -> YamlFileActionType.CREATE
                updatedFiles.contains(filePath) -> YamlFileActionType.UPDATE
                deletedFiles.contains(filePath) -> YamlFileActionType.DELETE
                renamedFiles.contains(filePath) -> YamlFileActionType.RENAME
                else -> YamlFileActionType.TRIGGER
            }
        }
    }

    /**
     * 获取[重命名]场景下移出.ci目录的yaml文件
     */
    private fun getRemovedCiFiles(renamedOldFiles: Map<String, String>): List<String> {
        return renamedOldFiles.filter { (oldFile, newFile) ->
            GitActionCommon.isCiFile(oldFile) &&
                    !GitActionCommon.isCiFile(newFile)
        }.map { it.key }
    }
}
