/*
 * Tencent is pleased to support the open source community by making BK-CI 蓝鲸持续集成平台 available.
 *
 * Copyright (C) 2022 THL A29 Limited, a Tencent company.  All rights reserved.
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

package com.tencent.bkrepo.scanner.model

import com.tencent.bkrepo.common.scanner.pojo.scanner.SubScanTaskStatus
import com.tencent.bkrepo.scanner.utils.Converter
import org.springframework.data.mongodb.core.index.CompoundIndex
import org.springframework.data.mongodb.core.index.CompoundIndexes
import org.springframework.data.mongodb.core.mapping.Document
import java.time.LocalDateTime

/**
 * 制品使用指定扫描方案的最新一次扫描任务
 */
@Document("plan_artifact_latest_sub_scan_task")
@CompoundIndexes(
    CompoundIndex(
        name = "projectId_repoName_fullPath_planId_scanner_idx",
        def = "{'projectId': 1, 'repoName': 1, 'fullPath': 1, 'planId': 1, 'scanner': 1}",
        background = true,
        unique = true
    ),
    CompoundIndex(
        name = "planId_idx",
        def = "{'planId': 1}",
        background = true
    ),
    CompoundIndex(
        name = "latestSubScanTaskId_idx",
        def = "{'latestSubScanTaskId': 1}",
        background = true
    )
)
class TPlanArtifactLatestSubScanTask(
    id: String? = null,
    createdBy: String,
    createdDate: LocalDateTime,
    lastModifiedBy: String,
    lastModifiedDate: LocalDateTime,
    startDateTime: LocalDateTime?,
    finishedDateTime: LocalDateTime?,

    parentScanTaskId: String,
    /**
     * 制品最新一次扫描子任务的id，复用扫描结果时为null
     */
    val latestSubScanTaskId: String? = null,
    planId: String? = null,

    projectId: String,
    repoName: String,
    repoType: String,
    packageKey: String? = null,
    version: String? = null,
    fullPath: String,
    artifactName: String,

    status: String,
    scanner: String,
    scannerType: String,
    sha256: String,
    size: Long,
    credentialsKey: String?,

    scanResultOverview: Map<String, Number>?
) : SubScanTaskDefinition(
    id = id,
    createdBy = createdBy,
    createdDate = createdDate,
    lastModifiedBy = lastModifiedBy,
    lastModifiedDate = lastModifiedDate,
    startDateTime = startDateTime,
    finishedDateTime = finishedDateTime,
    parentScanTaskId = parentScanTaskId,
    planId = planId,
    projectId = projectId,
    repoName = repoName,
    repoType = repoType,
    packageKey = packageKey,
    version = version,
    fullPath = fullPath,
    artifactName = artifactName,
    status = status,
    scanner = scanner,
    scannerType = scannerType,
    sha256 = sha256,
    size = size,
    credentialsKey = credentialsKey,
    scanResultOverview = scanResultOverview
) {
    companion object {
        fun convert(
            task: TSubScanTask,
            resultStatus: String,
            overview: Map<String, Any?>? = null,
            modifiedBy: String? = null
        ) = with(task) {
            val now = LocalDateTime.now()
            val numberOverview = overview?.let { Converter.convert(it) }
            val finishedDateTime = if (SubScanTaskStatus.finishedStatus(resultStatus)) {
                now
            } else {
                null
            }
            TPlanArtifactLatestSubScanTask(
                createdBy = createdBy,
                createdDate = createdDate,
                lastModifiedBy = modifiedBy ?: lastModifiedBy,
                lastModifiedDate = now,
                startDateTime = startDateTime,
                finishedDateTime = finishedDateTime,
                parentScanTaskId = parentScanTaskId,
                latestSubScanTaskId = id,
                planId = planId,
                projectId = projectId,
                repoName = repoName,
                repoType = repoType,
                packageKey = packageKey,
                version = version,
                fullPath = fullPath,
                artifactName = artifactName,
                status = resultStatus,
                scanner = scanner,
                scannerType = scannerType,
                sha256 = sha256,
                size = size,
                credentialsKey = credentialsKey,
                scanResultOverview = numberOverview
            )
        }
    }
}
