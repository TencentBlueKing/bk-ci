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

package com.tencent.devops.stream.trigger.parsers

import com.tencent.devops.common.api.exception.ErrorCodeException
import com.tencent.devops.scm.pojo.GitCodeFileInfo
import com.tencent.devops.common.webhook.pojo.code.git.GitMergeRequestEvent
import com.tencent.devops.stream.v2.service.StreamScmService
import java.util.Base64
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component

@Component
class YamlVersion @Autowired constructor(
    private val scmService: StreamScmService
) {

    /**
     * MR触发时，yml以谁为准：
     * - 当前MR变更中不存在yml文件，取目标分支（默认为未改动时目标分支永远是最新的）
     * - 当前MR变更中存在yml文件，通过对比两个文件的blobId：
     *   - blobId一样/目标分支文件不存，取源分支文件
     *   - blobId不一样，判断当前文件的根提交的blobID是否相同
     *      - 如果相同取源分支的(更新过了)
     *      - 如果不同，报错提示用户yml文件版本落后需要更新
     * 注：注意存在fork库不同projectID的提交
     */
    @Throws(ErrorCodeException::class)
    fun checkYmlVersion(
        mrEvent: GitMergeRequestEvent,
        targetGitToken: String,
        sourceGitToken: String?,
        filePath: String,
        changeSet: Set<String>
    ): Pair<Boolean, String> {
        val targetFile = getFileInfo(
            token = targetGitToken,
            ref = mrEvent.object_attributes.target_branch,
            filePath = filePath,
            gitProjectId = mrEvent.object_attributes.target_project_id.toString()
        )
        if (!changeSet.contains(filePath)) {
            return if (targetFile?.content.isNullOrBlank()) {
                Pair(true, "")
            } else {
                Pair(true, String(Base64.getDecoder().decode(targetFile!!.content)))
            }
        }

        val sourceFile = getFileInfo(
            token = sourceGitToken ?: targetGitToken,
            ref = mrEvent.object_attributes.last_commit.id,
            filePath = filePath,
            gitProjectId = mrEvent.object_attributes.source_project_id.toString()
        )
        val sourceContent = if (sourceFile?.content.isNullOrBlank()) {
            ""
        } else {
            String(Base64.getDecoder().decode(sourceFile!!.content))
        }

        if (targetFile?.blobId.isNullOrBlank()) {
            return Pair(true, sourceContent)
        }

        if (targetFile?.blobId == sourceFile?.blobId) {
            return Pair(true, sourceContent)
        }

        val mergeRequest = scmService.getMergeInfo(
            gitProjectId = mrEvent.object_attributes.target_project_id,
            mergeRequestId = mrEvent.object_attributes.id,
            token = targetGitToken
        )
        val baseTargetFile = getFileInfo(
            token = targetGitToken,
            ref = mergeRequest.baseCommit,
            filePath = filePath,
            gitProjectId = mrEvent.object_attributes.target_project_id.toString()
        )
        if (targetFile?.blobId == baseTargetFile?.blobId) {
            return Pair(true, sourceContent)
        }
        return Pair(false, "")
    }

    private fun getFileInfo(
        token: String,
        gitProjectId: String,
        filePath: String?,
        ref: String?
    ): GitCodeFileInfo? {
        return try {
            scmService.getFileInfo(
                token = token,
                ref = ref,
                filePath = filePath,
                gitProjectId = gitProjectId,
                useAccessToken = true
            )
        } catch (e: ErrorCodeException) {
            if (e.statusCode == 404) {
                return null
            }
            throw e
        }
    }
}
