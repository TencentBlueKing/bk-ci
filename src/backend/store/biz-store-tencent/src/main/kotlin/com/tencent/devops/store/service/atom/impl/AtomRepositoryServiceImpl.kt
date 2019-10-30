package com.tencent.devops.store.service.atom.impl

import com.tencent.devops.common.api.constant.CommonMessageCode
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.service.utils.MessageCodeUtil
import com.tencent.devops.store.dao.atom.MarketAtomDao
import com.tencent.devops.store.dao.common.StoreMemberDao
import com.tencent.devops.store.pojo.common.enums.StoreTypeEnum
import com.tencent.devops.store.service.atom.AtomRepositoryService
import org.jooq.DSLContext
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
class AtomRepositoryServiceImpl : AtomRepositoryService {

    @Autowired
    lateinit var client: Client

    @Autowired
    lateinit var marketAtomDao: MarketAtomDao

    @Autowired
    lateinit var storeMemberDao: StoreMemberDao

    @Autowired
    lateinit var dslContext: DSLContext

    private val logger = LoggerFactory.getLogger(AtomRepositoryServiceImpl::class.java)

    /**
     * 更改插件代码库的用户信息
     * @param userId 移交的用户ID
     * @param projectCode 项目代码
     * @param atomCode 插件代码
     */
    override fun updateAtomRepositoryUserInfo(userId: String, projectCode: String, atomCode: String): Result<Boolean> {
        logger.info("updateAtomRepositoryUserInfo userId is:$userId,projectCode is:$projectCode,atomCode is:$atomCode")
        // 判断用户是否是插件管理员，移交代码库只能针对插件管理员
        if (! storeMemberDao.isStoreAdmin(dslContext, userId, atomCode, StoreTypeEnum.ATOM.type.toByte())) {
            return MessageCodeUtil.generateResponseDataObject(CommonMessageCode.PERMISSION_DENIED)
        }
        val atomRecord = marketAtomDao.getLatestAtomByCode(dslContext, atomCode)
        logger.info("updateAtomRepositoryUserInfo atomRecord is:$atomRecord")
        if (null == atomRecord) {
            return MessageCodeUtil.generateResponseDataObject(CommonMessageCode.PARAMETER_IS_INVALID, arrayOf(atomCode))
        }
        val updateAtomRepositoryUserInfoResult = client.get(ServcieGitRepositoryResource::class)
            .updateRepositoryUserInfo(userId, projectCode, atomRecord.repositoryHashId)
        logger.info("updateAtomRepositoryUserInfoResult is:$updateAtomRepositoryUserInfoResult")
        return updateAtomRepositoryUserInfoResult
    }
}