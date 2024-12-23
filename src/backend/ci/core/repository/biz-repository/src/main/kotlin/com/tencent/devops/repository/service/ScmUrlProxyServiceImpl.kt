package com.tencent.devops.repository.service

import org.springframework.stereotype.Service

@Service
class ScmUrlProxyServiceImpl : ScmUrlProxyService {

    override fun getProxyUrl(url: String): String = url
}
