/*
 * Tencent is pleased to support the open source community by making BK-CI 蓝鲸持续集成平台 available.
 *
 * Copyright (C) 2021 THL A29 Limited, a Tencent company.  All rights reserved.
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

package com.tencent.bkrepo.repository.controller.service

import com.tencent.bkrepo.common.api.pojo.Response
import com.tencent.bkrepo.common.security.permission.Principal
import com.tencent.bkrepo.common.security.permission.PrincipalType
import com.tencent.bkrepo.common.service.util.ResponseBuilder
import com.tencent.bkrepo.repository.job.DeletedNodeCleanupJob
import com.tencent.bkrepo.repository.job.NodeDeletedCorrectionJob
import com.tencent.bkrepo.repository.job.PackageDownloadsMigrationJob
import com.tencent.bkrepo.repository.job.PackageVersionCorrectionJob
import com.tencent.bkrepo.repository.job.RepoUsedVolumeSynJob
import com.tencent.bkrepo.repository.job.RootNodeCleanupJob
import com.tencent.bkrepo.repository.job.StorageInstanceMigrationJob
import org.springframework.format.annotation.DateTimeFormat
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.time.LocalDateTime

@Principal(type = PrincipalType.ADMIN)
@RestController
@RequestMapping("/api/job")
class SystemJobController(
    private val storageInstanceMigrationJob: StorageInstanceMigrationJob,
    private val rootNodeCleanupJob: RootNodeCleanupJob,
    private val deletedNodeCleanupJob: DeletedNodeCleanupJob,
    private val nodeDeletedCorrectionJob: NodeDeletedCorrectionJob,
    private val packageDownloadsMigrationJob: PackageDownloadsMigrationJob,
    private val packageVersionCorrectionJob: PackageVersionCorrectionJob,
    private val repoUsedVolumeSynJob: RepoUsedVolumeSynJob
) {

    /**
     *
     * @param projectId 项目id
     * @param repoName 仓库名称
     * @param newKey 需要迁移的新实例的key
     * @param failedPointId 上次失败节点的id
     * @param skipPage 为了找到失败节p点，需要跳过多少页
     * @param preStartTime 上次的失败的开始时间
     * */
    @PostMapping("/migrate/storage/{projectId}/{repoName}/{newKey}")
    fun migrate(
        @PathVariable projectId: String,
        @PathVariable repoName: String,
        @PathVariable newKey: String,
        @RequestParam failedPointId: String? = null,
        @RequestParam skipPage: Int? = null,
        @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
        @RequestParam preStartTime: LocalDateTime? = null
    ): Response<Void> {
        storageInstanceMigrationJob.migrate(
            projectId,
            repoName, newKey, failedPointId, skipPage, preStartTime
        )
        return ResponseBuilder.success()
    }

    @PostMapping("/correct/node")
    fun correctNode(): Response<Void> {
        nodeDeletedCorrectionJob.correct()
        return ResponseBuilder.success()
    }

    @PostMapping("/correct/version")
    fun correctVersion(): Response<List<Any>> {
        return ResponseBuilder.success(packageVersionCorrectionJob.correct())
    }

    @PostMapping("/cleanup/rootNode")
    fun cleanupRootNode(): Response<Void> {
        rootNodeCleanupJob.cleanup()
        return ResponseBuilder.success()
    }

    @PostMapping("/migrate/downloads")
    fun migrateDownloads(): Response<Void> {
        packageDownloadsMigrationJob.migrate()
        return ResponseBuilder.success()
    }

    @PostMapping("/cleanup/deletedNode")
    fun cleanupDeletedNode(): Response<Void> {
        deletedNodeCleanupJob.start()
        return ResponseBuilder.success()
    }

    @PostMapping("/volume/{projectId}/{repoName}")
    fun synRepoUsedVolume(
        @PathVariable projectId: String,
        @PathVariable repoName: String
    ): Response<Long> {
        return ResponseBuilder.success(repoUsedVolumeSynJob.syn(projectId, repoName))
    }
}
