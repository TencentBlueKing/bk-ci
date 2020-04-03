package com.tencent.devops.store.service

import com.tencent.devops.common.api.constant.CommonMessageCode
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.service.utils.MessageCodeUtil
import com.tencent.devops.repository.api.ServiceGitRepositoryResource
import com.tencent.devops.store.constant.StoreMessageCode
import com.tencent.devops.store.dao.ExtServiceDao
import com.tencent.devops.store.dao.ExtServiceFeatureDao
import com.tencent.devops.store.dao.common.StoreMemberDao
import com.tencent.devops.store.pojo.common.README
import com.tencent.devops.store.pojo.common.enums.StoreTypeEnum
import org.jooq.DSLContext
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
class ServiceRepositoryService {
    @Autowired
    lateinit var client: Client

    @Autowired
    lateinit var extServiceDao: ExtServiceDao

    @Autowired
    lateinit var extServiceFeatureDao: ExtServiceFeatureDao

    @Autowired
    lateinit var storeMemberDao: StoreMemberDao

    @Autowired
    lateinit var dslContext: DSLContext

    private val logger = LoggerFactory.getLogger(ServiceRepositoryService::class.java)

    /**
     * 更改扩展代码库的用户信息
     * @param userId 移交的用户ID
     * @param projectCode 项目代码
     * @param serviceCode 扩展代码
     */
    fun updateServiceRepositoryUserInfo(userId: String, projectCode: String, serviceCode: String): Result<Boolean> {
        logger.info("updateServiceRepositoryUserInfo userId is:$userId,projectCode is:$projectCode,serviceCode is:$serviceCode")
        // 判断用户是否是插件管理员，移交代码库只能针对插件管理员
        if (! storeMemberDao.isStoreAdmin(dslContext, userId, serviceCode, StoreTypeEnum.ATOM.type.toByte())) {
            return MessageCodeUtil.generateResponseDataObject(CommonMessageCode.PERMISSION_DENIED)
        }
        val serviceRecord = extServiceFeatureDao.getLatestServiceByCode(dslContext, serviceCode)
        logger.info("updateServiceRepositoryUserInfo serviceRecord is:$serviceRecord")
        if (null == serviceRecord) {
            return MessageCodeUtil.generateResponseDataObject(CommonMessageCode.PARAMETER_IS_INVALID, arrayOf(serviceCode))
        }
        val updateServiceRepositoryUserInfoResult = client.get(ServiceGitRepositoryResource::class)
            .updateRepositoryUserInfo(userId, projectCode, serviceRecord.repositoryHashId)
        logger.info("updateServiceRepositoryUserInfo is:$updateServiceRepositoryUserInfoResult")
        return updateServiceRepositoryUserInfoResult
    }

    fun getReadMeFile(userId: String, serviceCode: String): Result<String?> {
        val featureRecord = extServiceFeatureDao.getLatestServiceByCode(dslContext, serviceCode)
            ?: throw RuntimeException(MessageCodeUtil.getCodeMessage(StoreMessageCode.USER_SERVICE_NOT_EXIST, arrayOf(serviceCode)))
        val fileStr = client.get(ServiceGitRepositoryResource::class).getFileContent(
            featureRecord.repositoryHashId,
            README, null, null, null
        ).data
        return Result(fileStr)
    }
}