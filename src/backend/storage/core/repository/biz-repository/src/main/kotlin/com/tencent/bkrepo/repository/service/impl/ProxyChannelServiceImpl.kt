/*
 * Tencent is pleased to support the open source community by making BK-CI 蓝鲸持续集成平台 available.
 *
 * Copyright (C) 2020 THL A29 Limited, a Tencent company.  All rights reserved.
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

package com.tencent.bkrepo.repository.service.impl

import com.tencent.bkrepo.common.api.exception.ErrorCodeException
import com.tencent.bkrepo.common.api.message.CommonMessageCode
import com.tencent.bkrepo.common.api.util.Preconditions
import com.tencent.bkrepo.common.artifact.pojo.RepositoryType
import com.tencent.bkrepo.common.artifact.util.http.UrlFormatter
import com.tencent.bkrepo.repository.dao.repository.ProxyChannelRepository
import com.tencent.bkrepo.repository.model.TProxyChannel
import com.tencent.bkrepo.repository.pojo.proxy.ProxyChannelCreateRequest
import com.tencent.bkrepo.repository.pojo.proxy.ProxyChannelInfo
import com.tencent.bkrepo.repository.service.ProxyChannelService
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service

/**
 * 代理源服务实现类
 */
@Service
class ProxyChannelServiceImpl(
    private val proxyChannelRepository: ProxyChannelRepository
) : ProxyChannelService {

    override fun findById(id: String): ProxyChannelInfo? {
        val tProxyChannel = proxyChannelRepository.findByIdOrNull(id)
        return convert(tProxyChannel)
    }

    override fun createProxy(userId: String, request: ProxyChannelCreateRequest) {
        with(request) {
            Preconditions.checkArgument(public, this::public.name)
            Preconditions.checkNotBlank(name, this::name.name)
            Preconditions.checkArgument(!checkExistByName(name, repoType), this::name.name)
            Preconditions.checkArgument(!checkExistByUrl(url, repoType), this::url.name)
            val tProxyChannel = TProxyChannel(
                public = public,
                name = name.trim(),
                url = formatUrl(url),
                repoType = repoType,
                credentialKey = credentialKey,
                username = username,
                password = password
            )
            proxyChannelRepository.insert(tProxyChannel)
        }
    }

    override fun listPublicChannel(repoType: RepositoryType): List<ProxyChannelInfo> {
        return proxyChannelRepository.findByPublicAndRepoType(true, repoType).map { convert(it)!! }
    }

    override fun checkExistById(id: String, repoType: RepositoryType): Boolean {
        if (id.isBlank()) return false
        return proxyChannelRepository.findByIdAndRepoType(id, repoType) != null
    }

    override fun checkExistByName(name: String, repoType: RepositoryType): Boolean {
        if (name.isBlank()) return false
        return proxyChannelRepository.findByNameAndRepoType(name, repoType) != null
    }

    override fun checkExistByUrl(url: String, repoType: RepositoryType): Boolean {
        if (url.isBlank()) return false
        return proxyChannelRepository.findByUrlAndRepoType(formatUrl(url), repoType) != null
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
                ProxyChannelInfo(
                    id = it.id!!,
                    public = it.public,
                    name = it.name,
                    url = it.url,
                    repoType = it.repoType,
                    credentialKey = it.credentialKey,
                    username = it.username,
                    password = it.password
                )
            }
        }
    }
}
