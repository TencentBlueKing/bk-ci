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

package com.tencent.devops.process.yaml

import com.tencent.devops.common.client.Client
import com.tencent.devops.model.process.tables.records.TPipelineYamlBranchFileRecord
import com.tencent.devops.process.engine.dao.PipelineYamlBranchFileDao
import com.tencent.devops.process.yaml.actions.GitActionCommon
import com.tencent.devops.process.yaml.common.Constansts
import com.tencent.devops.repository.api.scm.ServiceScmFileApiResource
import com.tencent.devops.repository.pojo.credential.AuthRepository
import com.tencent.devops.scm.api.enums.ContentKind
import com.tencent.devops.scm.api.pojo.Content
import com.tencent.devops.scm.api.pojo.Tree
import org.jooq.DSLContext
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
class PipelineYamlFileService @Autowired constructor(
    private val client: Client,
    private val dslContext: DSLContext,
    private val pipelineYamlBranchFileDao: PipelineYamlBranchFileDao
) {
    fun listFileTree(
        projectId: String,
        ref: String,
        authRepository: AuthRepository
    ): List<Tree> {
        return client.get(ServiceScmFileApiResource::class).listFileTree(
            projectId = projectId,
            path = Constansts.ciFileDirectoryName,
            ref = ref,
            recursive = true,
            authRepository = authRepository
        ).data?.filter {
            it.kind == ContentKind.FILE && GitActionCommon.checkYamlPipelineFile(it.path)
        } ?: emptyList()
    }

    fun getFileContent(
        projectId: String,
        path: String,
        ref: String,
        authRepository: AuthRepository
    ): Content {
        return client.get(ServiceScmFileApiResource::class).getFileContent(
            projectId = projectId,
            path = path,
            ref = ref,
            authRepository = authRepository
        ).data!!
    }

    fun getBranchFilePath(
        projectId: String,
        repoHashId: String,
        branch: String,
        filePath: String
    ): TPipelineYamlBranchFileRecord? {
        return pipelineYamlBranchFileDao.get(
            dslContext = dslContext,
            projectId = projectId,
            repoHashId = repoHashId,
            branch = branch,
            filePath = filePath
        )
    }

    fun getAllBranchFilePath(
        projectId: String,
        repoHashId: String,
        branch: String
    ): List<String> {
        return pipelineYamlBranchFileDao.getAllFilePath(
            dslContext = dslContext,
            projectId = projectId,
            repoHashId = repoHashId,
            branch = branch
        )
    }

    fun deleteBranchFile(
        projectId: String,
        repoHashId: String,
        branch: String,
        filePath: String,
        softDelete: Boolean = false
    ) {
        if (softDelete) {
            pipelineYamlBranchFileDao.softDelete(
                dslContext = dslContext,
                projectId = projectId,
                repoHashId = repoHashId,
                branch = branch,
                filePath = filePath
            )
        } else {
            pipelineYamlBranchFileDao.deleteFile(
                dslContext = dslContext,
                projectId = projectId,
                repoHashId = repoHashId,
                branch = branch,
                filePath = filePath
            )
        }
    }
}
