package com.tencent.devops.store.service.common

import com.tencent.devops.common.api.constant.CommonMessageCode
import com.tencent.devops.common.api.pojo.Page
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.api.util.PageUtil
import com.tencent.devops.common.api.util.timestampmilli
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.service.utils.MessageCodeUtil
import com.tencent.devops.common.service.utils.SpringContextUtil
import com.tencent.devops.model.atom.tables.records.TStoreApproveRecord
import com.tencent.devops.store.dao.common.StoreApproveDao
import com.tencent.devops.store.dao.common.StoreMemberDao
import com.tencent.devops.store.pojo.common.StoreApproveDetail
import com.tencent.devops.store.pojo.common.StoreApproveInfo
import com.tencent.devops.store.pojo.common.StoreApproveRequest
import com.tencent.devops.store.pojo.common.enums.ApproveStatusEnum
import com.tencent.devops.store.pojo.common.enums.ApproveTypeEnum
import com.tencent.devops.store.pojo.common.enums.StoreTypeEnum
import com.tencent.devops.support.api.ServiceMessageApproveResource
import org.jooq.DSLContext
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

/**
 * store审批业务逻辑类
 * author: carlyin
 * since: 2019-08-05
 */
@Service
class StoreApproveService {

    @Autowired
    lateinit var dslContext: DSLContext

    @Autowired
    lateinit var storeApproveDao: StoreApproveDao

    @Autowired
    lateinit var storeMemberDao: StoreMemberDao

    @Autowired
    lateinit var client: Client

    private val logger = LoggerFactory.getLogger(StoreApproveService::class.java)

    /**
     * 审批store组件
     */
    fun approveStoreInfo(
        userId: String,
        approveId: String,
        storeApproveRequest: StoreApproveRequest
    ): Result<Boolean> {
        logger.info("approveStoreInfo userId is :$userId,approveId is :$approveId")
        logger.info("approveStoreInfo storeApproveRequest is :$storeApproveRequest")
        val storeApproveRecord = storeApproveDao.getStoreApproveInfo(dslContext, approveId)
        logger.info("approveStoreInfo storeApproveRecord is :$storeApproveRecord")
        if (null == storeApproveRecord) {
            return MessageCodeUtil.generateResponseDataObject(CommonMessageCode.PARAMETER_IS_INVALID, arrayOf(approveId), false)
        }
        val storeCode = storeApproveRecord.storeCode
        val storeType = storeApproveRecord.storeType
        // 判断是否是插件管理员在操作
        val flag = storeMemberDao.isStoreAdmin(dslContext, userId, storeCode, storeType)
        if (!flag) {
            return MessageCodeUtil.generateResponseDataObject(CommonMessageCode.PERMISSION_DENIED)
        }
        val busInfoService = getStoreApproveSpecifyBusInfoService(storeApproveRecord.type)
        val approveResult = busInfoService.approveStoreSpecifyBusInfo(userId, StoreTypeEnum.getStoreTypeObj(storeType.toInt())!!, storeCode, approveId, storeApproveRequest)
        logger.info("approveStoreInfo approveResult is :$approveResult")
        if (approveResult.isNotOk()) {
            return approveResult
        }
        return Result(true)
    }

    fun getStoreApproveInfos(
        userId: String,
        storeType: StoreTypeEnum,
        storeCode: String,
        applicant: String?,
        approveType: ApproveTypeEnum?,
        approveStatus: ApproveStatusEnum?,
        page: Int,
        pageSize: Int
    ): Result<Page<StoreApproveInfo>?> {
        logger.info("getStoreApproveInfos userId is :$userId, storeCode is :$storeCode, storeType is :$storeType, page is :$page, pageSize is :$pageSize")
        logger.info("getStoreApproveInfos applicant is :$applicant, approveType is :$approveType, approveStatus is :$approveStatus")
        // 判断查看用户是否是当前插件的成员
        val flag = storeMemberDao.isStoreMember(dslContext, userId, storeCode, storeType.type.toByte())
        if (!flag) {
            return MessageCodeUtil.generateResponseDataObject(CommonMessageCode.PERMISSION_DENIED)
        }
        val storeApproveInfoList = storeApproveDao.getStoreApproveInfos(
            dslContext = dslContext,
            storeCode = storeCode,
            storeType = storeType,
            applicant = applicant,
            approveType = approveType,
            approveStatus = approveStatus,
            page = page,
            pageSize = pageSize)
            ?.map {
                generateStoreApproveInfo(it)
        }
        val storeApproveInfoCount = storeApproveDao.getStoreApproveInfoCount(dslContext, storeCode, storeType, applicant, approveType, approveStatus)
        logger.info("the storeApproveInfoList is :$storeApproveInfoList, storeApproveInfoCount is :$storeApproveInfoCount")
        val totalPages = PageUtil.calTotalPage(pageSize, storeApproveInfoCount)
        return Result(Page(count = storeApproveInfoCount, page = page, pageSize = pageSize, totalPages = totalPages, records = storeApproveInfoList ?: listOf()))
    }

    fun getUserStoreApproveInfo(userId: String, storeType: StoreTypeEnum, storeCode: String, approveType: ApproveTypeEnum): Result<StoreApproveInfo?> {
        logger.info("getUserStoreApproveInfo userId is :$userId, storeType is :$storeType, storeCode is :$storeCode")
        val storeApproveInfoRecord = storeApproveDao.getUserStoreApproveInfo(dslContext, userId, storeType, storeCode, approveType)
        logger.info("getUserStoreApproveInfo storeApproveInfoRecord is :$storeApproveInfoRecord")
        return if (null != storeApproveInfoRecord) {
            Result(generateStoreApproveInfo(storeApproveInfoRecord))
        } else {
            Result(data = null)
        }
    }

    fun getStoreApproveDetail(userId: String, approveId: String): Result<StoreApproveDetail?> {
        logger.info("getUserStoreApproveInfo userId is :$userId, approveId is :$approveId")
        val storeApproveRecord = storeApproveDao.getStoreApproveInfo(dslContext, approveId)
        logger.info("getUserStoreApproveInfo storeApproveRecord is :$storeApproveRecord")
        if (null != storeApproveRecord) {
            // 判断查看用户是否是当前插件的成员
            val flag = storeMemberDao.isStoreMember(dslContext, userId, storeApproveRecord.storeCode, storeApproveRecord.storeType)
            if (!flag) {
                return MessageCodeUtil.generateResponseDataObject(CommonMessageCode.PERMISSION_DENIED)
            }
            val approveType = storeApproveRecord.type
            val busInfoService = getStoreApproveSpecifyBusInfoService(approveType)
            val additionalParams = busInfoService.getBusAdditionalParams(
                userId,
                StoreTypeEnum.getStoreTypeObj(storeApproveRecord.storeType.toInt())!!,
                storeApproveRecord.storeCode,
                approveId
            )
            val storeApproveDetail = StoreApproveDetail(
                approveId = storeApproveRecord.id,
                content = storeApproveRecord.content,
                applicant = storeApproveRecord.applicant,
                approveType = storeApproveRecord.type,
                approveStatus = storeApproveRecord.status,
                approveMsg = storeApproveRecord.approveMsg,
                storeCode = storeApproveRecord.storeCode,
                storeType = StoreTypeEnum.getStoreType(storeApproveRecord.storeType.toInt()),
                additionalParams = additionalParams,
                creator = storeApproveRecord.creator,
                modifier = storeApproveRecord.modifier,
                createTime = storeApproveRecord.createTime.timestampmilli(),
                updateTime = storeApproveRecord.updateTime.timestampmilli()
            )
            logger.info("getUserStoreApproveInfo storeApproveDetail is :$storeApproveDetail")
            return Result(data = storeApproveDetail)
        } else {
            return Result(data = null)
        }
    }

    fun moaApproveCallBack(
        verifier: String,
        result: Int,
        taskId: String,
        message: String
    ): Result<Boolean> {
        logger.info("moaApproveCallBack verifier is :$verifier, result is :$result, taskId is :$taskId, message is :$message")
        val approveStatus = if (result == 0) ApproveStatusEnum.REFUSE else ApproveStatusEnum.PASS
        val approveStoreInfoResult = approveStoreInfo(verifier, taskId, StoreApproveRequest(message, approveStatus))
        logger.info("approveStoreInfoResult is :$approveStoreInfoResult")
        if (approveStoreInfoResult.isNotOk()) {
            return approveStoreInfoResult
        }
        // 结单
        val moaCompleteResult = client.get(ServiceMessageApproveResource::class).moaComplete(taskId)
        logger.info("moaCompleteResult is :$moaCompleteResult")
        return Result(true)
    }

    private fun getStoreApproveSpecifyBusInfoService(approveType: String): StoreApproveSpecifyBusInfoService {
        return SpringContextUtil.getBean(StoreApproveSpecifyBusInfoService::class.java, "${approveType}_APPROVE_SERVICE")
    }

    private fun generateStoreApproveInfo(it: TStoreApproveRecord): StoreApproveInfo {
        return StoreApproveInfo(
            approveId = it.id,
            content = it.content,
            applicant = it.applicant,
            approveType = it.type,
            approveStatus = it.status,
            approveMsg = it.approveMsg,
            storeCode = it.storeCode,
            storeType = StoreTypeEnum.getStoreType(it.storeType.toInt()),
            creator = it.creator,
            modifier = it.modifier,
            createTime = it.createTime.timestampmilli(),
            updateTime = it.updateTime.timestampmilli()
        )
    }
}
