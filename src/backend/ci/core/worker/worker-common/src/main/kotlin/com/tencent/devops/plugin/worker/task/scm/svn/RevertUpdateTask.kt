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

package com.tencent.devops.plugin.worker.task.scm.svn

import com.tencent.devops.common.api.enums.RepositoryConfig
import com.tencent.devops.common.log.Ansi
import com.tencent.devops.common.pipeline.enums.CodePullStrategy
import com.tencent.devops.common.pipeline.enums.SVNVersion
import com.tencent.devops.plugin.worker.task.scm.util.SvnUtil
import com.tencent.devops.scm.utils.code.svn.SvnUtils
import com.tencent.devops.worker.common.logger.LoggerService
import org.tmatesoft.svn.core.SVNDepth
import org.tmatesoft.svn.core.SVNURL
import org.tmatesoft.svn.core.wc.SVNRevision
import java.io.File

class RevertUpdateTask constructor(
    override val svnUrl: SVNURL,
    override val projectName: String,
    override val username: String,
    override val privateKey: String,
    override val passPhrase: String?,
    override val revision: SVNRevision,
    override val workspace: File,
    override val strategy: CodePullStrategy,
    override val update: Boolean,
    override val enableSubmodule: Boolean,
    override val svnDepth: SVNDepth,
    override val pipelineId: String,
    override val buildId: String,
    override val repositoryConfig: RepositoryConfig,
    override val svnVersion: SVNVersion?
) : SvnUpdateTask(
    svnUrl,
    projectName,
    username,
    privateKey,
    passPhrase,
    revision,
    workspace,
    strategy,
    update,
    enableSubmodule,
    svnDepth,
    pipelineId,
    buildId,
    repositoryConfig,
    svnVersion
) {

    override fun preUpdate() {
        if (workspace.exists()) {
            if (!File(workspace, ".svn").exists()) {
                LoggerService.addNormalLine(Ansi().fgYellow().a(".svn file is not exist").reset().toString())
                return
            }
            val client = SvnUtils.getClientManager(svnUrl, username, privateKey, passPhrase).wcClient
            try {
                LoggerService.addNormalLine("Clean up the workspace(${workspace.path})")
                SvnUtil.deleteWcLockAndCleanup(client, workspace)
            } catch (t: Throwable) {
                LoggerService.addYellowLine("Fail to cleanup the workspace because of ${t.message}")
            }
            try {
                LoggerService.addNormalLine("Revert the workspace(${workspace.path})")
                client.doRevert(arrayOf<File>(workspace.canonicalFile), SVNDepth.INFINITY, null)
            } catch (t: Throwable) {
                LoggerService.addYellowLine("Fail to revert the workspace because of ${t.message}")
            }
        }
    }
}