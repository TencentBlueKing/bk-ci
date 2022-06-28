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

package com.tencent.bkrepo.repository.service.repo.impl

import com.tencent.bkrepo.common.api.exception.ErrorCodeException
import com.tencent.bkrepo.common.api.message.CommonMessageCode
import com.tencent.bkrepo.common.artifact.pojo.RepositoryType
import com.tencent.bkrepo.common.artifact.util.http.UrlFormatter
import com.tencent.bkrepo.common.security.util.RsaUtils
import com.tencent.bkrepo.repository.dao.ProxyChannelDao
import com.tencent.bkrepo.repository.model.TProxyChannel
import com.tencent.bkrepo.repository.pojo.proxy.ProxyChannelCreateRequest
import com.tencent.bkrepo.repository.pojo.proxy.ProxyChannelDeleteRequest
import com.tencent.bkrepo.repository.pojo.proxy.ProxyChannelInfo
import com.tencent.bkrepo.repository.pojo.proxy.ProxyChannelUpdateRequest
import com.tencent.bkrepo.repository.service.repo.ProxyChannelService
import java.time.LocalDateTime
import org.springframework.stereotype.Service

/**
 * 代理源服务实现类
 */
@Service
class ProxyChannelServiceImpl(
    private val proxyChannelDao: ProxyChannelDao
) : ProxyChannelService {

    override fun createProxy(userId: String, request: ProxyChannelCreateRequest) {
        with(request) {
            val pw = if (password.isNullOrEmpty()) {
                password
            } else {
                RsaUtils.encrypt(password!!)
            }
            val tProxyChannel = TProxyChannel(
                public = public,
                name = name.trim(),
                url = formatUrl(url),
                repoType = repoType,
                credentialKey = credentialKey,
                username = username,
                password = pw,
                projectId = projectId,
                repoName = repoName,
                createdBy = userId,
                createdDate = LocalDateTime.now(),
                lastModifiedBy = userId,
                lastModifiedDate = LocalDateTime.now()
            )
            proxyChannelDao.insert(tProxyChannel)
        }
    }

    override fun updateProxy(userId: String, request: ProxyChannelUpdateRequest) {
        with(request) {
            val pw = if (password.isNullOrEmpty()) {
                password
            } else {
                RsaUtils.encrypt(password!!)
            }
            val tProxyChannel = proxyChannelDao.findByUniqueParams(
                projectId = projectId,
                repoName = repoName,
                repoType = repoType,
                name = name
            )
            tProxyChannel?.let {
                tProxyChannel.public = public
                tProxyChannel.lastModifiedDate = LocalDateTime.now()
                tProxyChannel.lastModifiedBy = userId
                tProxyChannel.url = url
                tProxyChannel.username = username
                tProxyChannel.password = pw
                proxyChannelDao.save(tProxyChannel)
            }
        }
    }

    override fun queryProxyChannel(
        projectId: String,
        repoName: String,
        repoType: RepositoryType,
        name: String
    ): ProxyChannelInfo? {
        val proxy = proxyChannelDao.findByUniqueParams(
            projectId = projectId,
            repoName = repoName,
            repoType = repoType,
            name = name
        )
        return convert(proxy)
    }

    override fun deleteProxy(request: ProxyChannelDeleteRequest) {
        with(request) {
            proxyChannelDao.deleteByUnique(
                projectId = projectId,
                repoName = repoName,
                repoType = repoType,
                name = name
            )
        }
    }

    override fun listPublicChannel(repoType: RepositoryType): List<ProxyChannelInfo> {
        return proxyChannelDao.findByRepoType(repoType).map { convert(it)!! }
    }

    private fun formatUrl(url: String): String {
        return try {
            UrlFormatter.formatUrl(url)
        } catch (exception: IllegalArgumentException) {
            throw ErrorCodeException(CommonMessageCode.PARAMETER_INVALID, "url")
        }
    }

    companion object {

        private fun convert(tProxyChannel: TProxyChannel?): ProxyChannelInfo? {
            return tProxyChannel?.let {
                val pw = if (it.password.isNullOrEmpty()) {
                    it.password
                } else {
                    try {
                        RsaUtils.decrypt(it.password!!)
                    } catch (e: Exception) {
                        it.password
                    }
                }
                ProxyChannelInfo(
                    id = it.id!!,
                    public = it.public,
                    name = it.name,
                    url = it.url,
                    repoType = it.repoType,
                    credentialKey = it.credentialKey,
                    username = it.username,
                    password = pw,
                    projectId = it.projectId,
                    repoName = it.repoName
                )
            }
        }
    }
}
