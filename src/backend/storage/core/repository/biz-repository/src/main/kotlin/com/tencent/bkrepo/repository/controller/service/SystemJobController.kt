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
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package com.tencent.bkrepo.repository.controller.service

import com.tencent.bkrepo.common.api.pojo.Response
import com.tencent.bkrepo.common.security.permission.Principal
import com.tencent.bkrepo.common.security.permission.PrincipalType
import com.tencent.bkrepo.common.service.util.ResponseBuilder
import com.tencent.bkrepo.repository.job.FileReferenceCleanupJob
import com.tencent.bkrepo.repository.job.FileSynchronizeJob
import com.tencent.bkrepo.repository.job.NodeDeletedCorrectionJob
import com.tencent.bkrepo.repository.job.PackageDownloadsMigrationJob
import com.tencent.bkrepo.repository.job.PackageVersionCorrectionJob
import com.tencent.bkrepo.repository.job.RootNodeCleanupJob
import com.tencent.bkrepo.repository.job.StorageInstanceMigrationJob
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@Principal(type = PrincipalType.ADMIN)
@RestController
@RequestMapping("/api/job")
class SystemJobController(
    private val fileSynchronizeJob: FileSynchronizeJob,
    private val storageInstanceMigrationJob: StorageInstanceMigrationJob,
    private val fileReferenceCleanupJob: FileReferenceCleanupJob,
    private val rootNodeCleanupJob: RootNodeCleanupJob,
    private val nodeDeletedCorrectionJob: NodeDeletedCorrectionJob,
    private val packageDownloadsMigrationJob: PackageDownloadsMigrationJob,
    private val packageVersionCorrectionJob: PackageVersionCorrectionJob
) {

    @PostMapping("/sync/file")
    fun synchronizeFile(): Response<Void> {
        fileSynchronizeJob.run()
        return ResponseBuilder.success()
    }

    @PostMapping("/migrate/storage/{projectId}/{repoName}/{newKey}")
    fun migrate(
        @PathVariable projectId: String,
        @PathVariable repoName: String,
        @PathVariable newKey: String
    ): Response<Void> {
        storageInstanceMigrationJob.migrate(projectId, repoName, newKey)
        return ResponseBuilder.success()
    }

    @PostMapping("/cleanup/reference")
    fun cleanupFileReference(): Response<Void> {
        fileReferenceCleanupJob.cleanup()
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
}
