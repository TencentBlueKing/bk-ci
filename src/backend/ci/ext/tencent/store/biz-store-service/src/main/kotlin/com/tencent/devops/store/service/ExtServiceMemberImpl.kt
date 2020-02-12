package com.tencent.devops.store.service

import com.tencent.devops.common.api.constant.CommonMessageCode
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.service.utils.MessageCodeUtil
import com.tencent.devops.store.dao.ExtServiceDao
import com.tencent.devops.store.dao.ExtServiceFeatureDao
import com.tencent.devops.store.pojo.common.StoreMemberReq
import com.tencent.devops.store.pojo.common.enums.StoreTypeEnum
import com.tencent.devops.store.service.common.impl.StoreMemberServiceImpl
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired

abstract class ExtServiceMemberImpl : StoreMemberServiceImpl() {

    @Autowired
    lateinit var extServiceDao: ExtServiceDao

    @Autowired
    lateinit var extServiceFeatureDao: ExtServiceFeatureDao

    override fun add(
        userId: String,
        storeMemberReq: StoreMemberReq,
        storeType: StoreTypeEnum,
        collaborationFlag: Boolean?,
        sendNotify: Boolean
    ): Result<Boolean> {
        logger.info("addExtensionMember userId is:$userId,storeMemberReq is:$storeMemberReq,storeType is:$storeType")
        val serviceCode = storeMemberReq.storeCode
        val serviceRecord = extServiceFeatureDao.getLatestServiceByCode(dslContext, serviceCode)
        logger.info("addExtensionMember serviceRecord is:$serviceRecord")
        if (null == serviceRecord) {
            return MessageCodeUtil.generateResponseDataObject(CommonMessageCode.PARAMETER_IS_INVALID, arrayOf(serviceCode))
        }
        if (!storeMemberDao.isStoreAdmin(dslContext, userId, serviceCode, storeType.type.toByte())) {
            return MessageCodeUtil.generateResponseDataObject(CommonMessageCode.PERMISSION_DENIED)
        }
        val repositoryHashId = serviceRecord.repositoryHashId
        val addRepoMemberResult = addRepoMember(storeMemberReq, userId, repositoryHashId)
        logger.info("addExtensionMember is:$addRepoMemberResult")
        if (addRepoMemberResult.isNotOk()) {
            return Result(status = addRepoMemberResult.status, message = addRepoMemberResult.message, data = false)
        }
        return super.add(userId, storeMemberReq, storeType, collaborationFlag, sendNotify)
    }

    abstract fun addRepoMember(storeMemberReq: StoreMemberReq, userId: String, repositoryHashId: String): Result<Boolean>

    override fun delete(userId: String, id: String, storeCode: String, storeType: StoreTypeEnum): Result<Boolean> {
        logger.info("deleteExtServiceMember userId is:$userId,id is:$id,storeCode is:$storeCode,storeType is:$storeType")
        val serviceRecord = extServiceFeatureDao.getServiceByCode(dslContext, storeCode)
        logger.info("deleteExtServiceMember serviceRecord is:$serviceRecord")
        if (null == serviceRecord) {
            return MessageCodeUtil.generateResponseDataObject(CommonMessageCode.PARAMETER_IS_INVALID, arrayOf(storeCode))
        }
        val memberRecord = storeMemberDao.getById(dslContext, id)
            ?: return MessageCodeUtil.generateResponseDataObject(CommonMessageCode.PARAMETER_IS_INVALID, arrayOf(id))
        // 如果删除的是管理员，只剩一个管理员则不允许删除
        if ((memberRecord.type).toInt() == 0) {
            val validateAdminResult = isStoreHasAdmins(storeCode, storeType)
            if (validateAdminResult.isNotOk()) {
                return Result(status = validateAdminResult.status, message = validateAdminResult.message, data = false)
            }
        }
        val username = memberRecord.username
        val repositoryHashId = serviceRecord.repositoryHashId
        val deleteRepoMemberResult = deleteRepoMember(userId, username, repositoryHashId)
        logger.info("deleteExtServiceMember is:$deleteRepoMemberResult")
        if (deleteRepoMemberResult.isNotOk()) {
            return Result(status = deleteRepoMemberResult.status, message = deleteRepoMemberResult.message, data = false)
        }
        return super.delete(userId, id, storeCode, storeType)
    }

    abstract fun deleteRepoMember(userId: String, username: String, repositoryHashId: String): Result<Boolean>

    override fun getStoreName(storeCode: String): String {
        return extServiceDao.getServiceLatestByCode(dslContext, storeCode)?.serviceName ?: ""
    }

    companion object {
        val logger = LoggerFactory.getLogger(this::class.java)
    }
}