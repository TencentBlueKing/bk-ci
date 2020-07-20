package com.tencent.devops.sign.service.impl

import com.tencent.devops.sign.api.pojo.IpaSignInfo
import com.tencent.devops.sign.dao.SignIpaInfoDao
import com.tencent.devops.sign.service.SignInfoService
import org.jooq.DSLContext
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
class SignInfoServiceImpl(
    private val dslContext: DSLContext,
    private val signIpaInfoDao: SignIpaInfoDao
) : SignInfoService {

    companion object {
        private val logger = LoggerFactory.getLogger(SignInfoServiceImpl::class.java)
    }

    override fun save(resignId: String, ipaSignInfoHeader: String, info: IpaSignInfo) {
        signIpaInfoDao.saveSignInfo(dslContext, resignId, ipaSignInfoHeader, info)
    }
}