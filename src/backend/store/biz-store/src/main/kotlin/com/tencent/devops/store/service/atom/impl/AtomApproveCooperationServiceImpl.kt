package com.tencent.devops.store.service.atom.impl

import com.tencent.devops.common.api.constant.CommonMessageCode
import com.tencent.devops.common.api.constant.DEVOPS
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.service.utils.MessageCodeUtil
import com.tencent.devops.store.dao.atom.AtomApproveRelDao
import com.tencent.devops.store.dao.atom.MarketAtomDao
import com.tencent.devops.store.dao.common.StoreApproveDao
import com.tencent.devops.store.dao.common.StoreProjectRelDao
import com.tencent.devops.store.pojo.common.ATOM_COLLABORATOR_APPLY_REFUSE_TEMPLATE
import com.tencent.devops.store.pojo.common.StoreApproveRequest
import com.tencent.devops.store.pojo.common.StoreMemberReq
import com.tencent.devops.store.pojo.common.enums.ApproveStatusEnum
import com.tencent.devops.store.pojo.common.enums.StoreMemberTypeEnum
import com.tencent.devops.store.pojo.common.enums.StoreProjectTypeEnum
import com.tencent.devops.store.pojo.common.enums.StoreTypeEnum
import com.tencent.devops.store.service.common.AbstractStoreApproveSpecifyBusInfoService
import com.tencent.devops.store.service.common.StoreNotifyService
import org.jooq.DSLContext
import org.jooq.impl.DSL
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import java.util.concurrent.Executors

/**
 * 插件协作逻辑处理
 * since: 2019-08-05
 */
@Service("ATOM_COLLABORATOR_APPLY_APPROVE_SERVICE")
class AtomApproveCooperationServiceImpl @Autowired constructor(
    private val dslContext: DSLContext,
    private val marketAtomDao: MarketAtomDao,
    private val atomApproveRelDao: AtomApproveRelDao,
    private val storeApproveDao: StoreApproveDao,
    private val storeProjectRelDao: StoreProjectRelDao,
    private val atomMemberService: AtomMemberServiceImpl,
    private val storeNotifyService: StoreNotifyService
) : AbstractStoreApproveSpecifyBusInfoService() {

    private val executorService = Executors.newFixedThreadPool(2)

    private val logger = LoggerFactory.getLogger(AtomApproveCooperationServiceImpl::class.java)

    override fun approveStoreSpecifyBusInfo(
        userId: String,
        storeType: StoreTypeEnum,
        storeCode: String,
        approveId: String,
        storeApproveRequest: StoreApproveRequest
    ): Result<Boolean> {
        logger.info("approveStoreSpecifyBusInfo userId is :$userId,storeType is :$storeType,storeCode is :$storeCode")
        logger.info("approveStoreSpecifyBusInfo storeApproveRequest is :$storeApproveRequest")
        val atomApproveRelRecord = atomApproveRelDao.getByApproveId(dslContext, approveId)
        logger.info("approveStoreInfo atomApproveRelRecord is :$atomApproveRelRecord")
        if (null == atomApproveRelRecord) {
            return MessageCodeUtil.generateResponseDataObject(CommonMessageCode.PARAMETER_IS_INVALID, arrayOf(approveId), false)
        }
        val atomApproveRecord = storeApproveDao.getStoreApproveInfo(dslContext, approveId)
        logger.info("approveStoreSpecifyBusInfo atomApproveRecord is :$atomApproveRecord")
        if (storeApproveRequest.approveStatus == ApproveStatusEnum.PASS) {
            // 为用户添加插件代码库的权限和插件开发人员的权限
            val storeMemberReq = StoreMemberReq(listOf(atomApproveRecord!!.applicant), StoreMemberTypeEnum.DEVELOPER, storeCode)
            val addAtomMemberResult = atomMemberService.add(userId, storeMemberReq, storeType, true)
            logger.info("approveStoreSpecifyBusInfo addAtomMemberResult is :$addAtomMemberResult")
            if (addAtomMemberResult.isNotOk()) {
                return Result(status = addAtomMemberResult.status, message = addAtomMemberResult.message, data = false)
            }
        }
        dslContext.transaction { t ->
            val context = DSL.using(t)
            storeApproveDao.updateStoreApproveInfo(
                dslContext = context,
                userId = userId,
                approveId = approveId,
                approveMsg = storeApproveRequest.approveMsg,
                approveStatus = storeApproveRequest.approveStatus
            )
            // 如果审批通过，需要为用户添加插件代码库的开发权限和保存调试项目
            if (storeApproveRequest.approveStatus == ApproveStatusEnum.PASS) {
                storeProjectRelDao.addStoreProjectRel(
                    dslContext = context,
                    userId = atomApproveRecord!!.applicant,
                    storeCode = storeCode,
                    storeType = storeType.type.toByte(),
                    projectCode = atomApproveRelRecord.testProjectCode,
                    type = StoreProjectTypeEnum.TEST.type.toByte()
                )
            }
        }
        if (storeApproveRequest.approveStatus == ApproveStatusEnum.REFUSE) {
            // 给用户发送驳回通知
            executorService.submit<Unit> {
                val receivers = mutableSetOf(atomApproveRecord!!.applicant)
                val atomName = marketAtomDao.getLatestAtomByCode(dslContext, storeCode)?.name ?: ""
                val bodyParams = mapOf("atomAdmin" to userId, "atomName" to atomName, "approveMsg" to storeApproveRequest.approveMsg)
                storeNotifyService.sendNotifyMessage(
                    templateCode = ATOM_COLLABORATOR_APPLY_REFUSE_TEMPLATE,
                    sender = DEVOPS,
                    receivers = receivers,
                    bodyParams = bodyParams
                )
            }
        }
        return Result(true)
    }

    override fun getBusAdditionalParams(
        userId: String,
        storeType: StoreTypeEnum,
        storeCode: String,
        approveId: String
    ): Map<String, String>? {
        logger.info("getBusAdditionalParams userId is :$userId,storeType is :$storeType,storeCode is :$storeCode,approveId is :$approveId")
        val testProjectCode = storeProjectRelDao.getUserStoreTestProjectCode(dslContext, userId, storeCode, storeType)
        return if (null != testProjectCode) {
            val additionalParams = mapOf("testProjectCode" to testProjectCode)
            logger.info("getBusAdditionalParams additionalParams is :$additionalParams")
            additionalParams
        } else {
            null
        }
    }
}
