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

package com.tencent.devops.process.service

import com.tencent.devops.common.api.enums.RepositoryConfig
import com.tencent.devops.common.api.enums.RepositoryType
import com.tencent.devops.common.api.enums.ScmType
import com.tencent.devops.common.api.exception.OperationException
import com.tencent.devops.common.api.util.DHUtil
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.web.utils.I18nUtil
import com.tencent.devops.process.constant.ProcessMessageCode.FAIL_TO_GET_SVN_DIRECTORY
import com.tencent.devops.process.constant.ProcessMessageCode.GIT_NOT_FOUND
import com.tencent.devops.process.constant.ProcessMessageCode.NOT_SVN_CODE_BASE
import com.tencent.devops.process.constant.ProcessMessageCode.REPOSITORY_ID_AND_NAME_ARE_EMPTY
import com.tencent.devops.process.service.scm.ScmProxyService
import com.tencent.devops.repository.api.ServiceRepositoryResource
import com.tencent.devops.repository.api.scm.ServiceSvnResource
import com.tencent.devops.repository.pojo.CodeSvnRepository
import com.tencent.devops.repository.pojo.RepositoryInfoWithPermission
import com.tencent.devops.scm.pojo.enums.SvnFileType
import com.tencent.devops.ticket.api.ServiceCredentialResource
import com.tencent.devops.ticket.pojo.enums.CredentialType
import java.util.Base64
import javax.ws.rs.NotFoundException
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import org.tmatesoft.svn.core.wc.SVNRevision

@Suppress("ALL")
@Service
class CodeService @Autowired constructor(
    private val scmProxyService: ScmProxyService,
    private val client: Client
) {
    fun getSvnDirectories(projectId: String, repoHashId: String?, relativePath: String?): List<String> {
        val repositoryConfig = getRepositoryConfig(repoHashId, null)

        val repository = (client.get(ServiceRepositoryResource::class).get(
            projectId = projectId,
            repositoryId = repositoryConfig.getURLEncodeRepositoryId(),
            repositoryType = repositoryConfig.repositoryType
        ).data
            ?: throw NotFoundException(
                I18nUtil.getCodeLanMessage(
                    messageCode = GIT_NOT_FOUND,
                    params = arrayOf("$repoHashId")
                )
            )) as? CodeSvnRepository
            ?: throw IllegalArgumentException(
                I18nUtil.getCodeLanMessage(
                    messageCode = NOT_SVN_CODE_BASE,
                    params = arrayOf("$repoHashId")
                )
            )

        try {
            val credential = getSvnCredential(projectId, repository)
            val username = repository.userName
            val svnType = if (repository.svnType.isNullOrEmpty()) {
                "SSH"
            } else {
                repository.svnType!!
            }
            logger.info("Code get svn svnType| $svnType")
            val svnFileInfoList = client.get(ServiceSvnResource::class).getDirectories(
                url = repository.url,
                userId = username,
                svnType = svnType,
                svnPath = relativePath,
                revision = SVNRevision.HEAD.number,
                credential1 = credential.first,
                credential2 = credential.second,
                credential3 = credential.third
            ).data!!
            logger.info("Code get svn directories($svnFileInfoList)")

            val directories = mutableListOf<String>()
            svnFileInfoList.forEach {
                if (it.type == SvnFileType.DIR) {
                    directories.add(it.name)
                }
            }
            return directories
        } catch (t: Throwable) {
            logger.warn("[$projectId|$repoHashId|$relativePath] Fail to get SVN directory", t)
            throw OperationException(
                I18nUtil.getCodeLanMessage(
                    messageCode = FAIL_TO_GET_SVN_DIRECTORY,
                    params = arrayOf("${t.message}")
                )
            )
        }
    }

    fun getGitRefs(projectId: String, repoHashId: String?, search: String? = null): List<String> {
        val repositoryConfig = getRepositoryConfig(repoHashId, null)
        val result = mutableListOf<String>()
        val branches = scmProxyService.listBranches(projectId, repositoryConfig, search).data ?: listOf()
        val tags = scmProxyService.listTags(projectId, repositoryConfig, search).data ?: listOf()
        // 取前100
        result.addAll(branches)
        result.addAll(tags)
        return result
    }

    fun listRepository(projectId: String, scmType: ScmType): List<RepositoryInfoWithPermission> {
        try {
            val result = client.get(ServiceRepositoryResource::class).list(projectId, scmType)
            if (result.isNotOk() || result.data == null) {
                logger.warn("[$projectId|$scmType] Fail to get the repository with message $result")
            }
            return result.data!!
        } catch (t: Throwable) {
            logger.warn("[$projectId|$scmType] Fail to get the repository", t)
            throw t
        }
    }

    private fun getSvnCredential(
        projectId: String,
        repository: CodeSvnRepository
    ): Triple<String /*username*/, String /*password*/, String? /*passPhrase*/> {
        val pair = getCredential(projectId, repository.credentialId)
        val credentials = pair.first
        val credentialType = pair.second

        return if (repository.svnType == CodeSvnRepository.SVN_TYPE_HTTP) {
            return if (credentialType == CredentialType.USERNAME_PASSWORD) {
                if (credentials.size <= 1) {
                    Triple(repository.userName, credentials[0], null)
                } else {
                    Triple(credentials[0], credentials[1], null)
                }
            } else {
                Triple(repository.userName, credentials[0], null)
            }
        } else {
            val privateKey = credentials[0]
            val passPhrase = if (credentials.size > 1) {
                val p = credentials[1]
                if (p.isEmpty()) {
                    null
                } else {
                    p
                }
            } else {
                null
            }
            Triple(repository.userName, privateKey, passPhrase)
        }
    }

    private fun getCredential(projectId: String, credentialId: String): Pair<List<String>, CredentialType> {
        val pair = DHUtil.initKey()
        val encoder = Base64.getEncoder()
        val result = client.get(ServiceCredentialResource::class)
            .get(
                projectId = projectId,
                credentialId = credentialId,
                publicKey = encoder.encodeToString(pair.publicKey)
            )

        if (result.isNotOk() || result.data == null) {
            logger.warn("Fail to get the credential($credentialId) because of ${result.message}")
            throw NotFoundException(result.message!!)
        }

        val credential = result.data!!

        val credentialList = mutableListOf<String>()
        credentialList.add(decode(credential.v1, credential.publicKey, pair.privateKey))
        if (!credential.v2.isNullOrEmpty()) credentialList.add(
            decode(
                encode = credential.v2!!,
                publicKey = credential.publicKey,
                privateKey = pair.privateKey
            )
        )
        if (!credential.v3.isNullOrEmpty()) credentialList.add(
            decode(
                encode = credential.v3!!,
                publicKey = credential.publicKey,
                privateKey = pair.privateKey
            )
        )
        if (!credential.v4.isNullOrEmpty()) credentialList.add(
            decode(
                encode = credential.v4!!,
                publicKey = credential.publicKey,
                privateKey = pair.privateKey
            )
        )

        return Pair(credentialList, credential.credentialType)
    }

    private fun decode(encode: String, publicKey: String, privateKey: ByteArray): String {
        val decoder = Base64.getDecoder()
        return String(
            DHUtil.decrypt(
                data = decoder.decode(encode),
                partBPublicKey = decoder.decode(publicKey),
                partAPrivateKey = privateKey
            )
        )
    }

    private fun getRepositoryConfig(repoHashId: String?, repoName: String?): RepositoryConfig {
        if (!repoHashId.isNullOrBlank()) {
            return RepositoryConfig(repoHashId, null, RepositoryType.ID)
        }
        if (!repoName.isNullOrBlank()) {
            return RepositoryConfig(null, repoName, RepositoryType.NAME)
        }
        throw OperationException(
            I18nUtil.getCodeLanMessage(REPOSITORY_ID_AND_NAME_ARE_EMPTY)
        )
    }

    companion object {
        private val logger = LoggerFactory.getLogger(CodeService::class.java)
    }
}
