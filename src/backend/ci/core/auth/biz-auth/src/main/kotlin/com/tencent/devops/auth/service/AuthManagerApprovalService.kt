package com.tencent.devops.auth.service

import com.tencent.devops.auth.constant.AuthMessageCode
import com.tencent.devops.auth.dao.AuthManagerApprovalDao
import com.tencent.devops.auth.dao.ManagerUserDao
import com.tencent.devops.auth.pojo.enum.ApprovalType
import com.tencent.devops.common.api.exception.ErrorCodeException
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.notify.enums.WeworkReceiverType
import com.tencent.devops.common.notify.enums.WeworkTextType
import com.tencent.devops.model.auth.tables.records.TAuthManagerApprovalRecord
import com.tencent.devops.model.auth.tables.records.TAuthManagerUserRecord
import com.tencent.devops.notify.api.service.ServiceNotifyResource
import com.tencent.devops.notify.pojo.WeworkMarkdownAction
import com.tencent.devops.notify.pojo.WeworkMarkdownAttachment
import com.tencent.devops.notify.pojo.WeworkRobotNotifyMessage
import org.jooq.DSLContext
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import java.time.LocalDateTime

@Service
@SuppressWarnings("ALL")
class AuthManagerApprovalService @Autowired constructor(
    val dslContext: DSLContext,
    val authManagerApprovalDao: AuthManagerApprovalDao,
    val managerUserDao: ManagerUserDao,
    val managerOrganizationService: ManagerOrganizationService,
    private val client: Client
) {
    fun userRenewalAuth(
        approvalId: Int,
        approvalType: ApprovalType
    ): Boolean {
        val approvalRecord = authManagerApprovalDao.getApprovalById(dslContext, approvalId)
        val checkResult = checkBeforeExecute(dslContext, approvalId, approvalRecord)
        if (checkResult) {
            return true
        }
        val managerId = approvalRecord!!.managerId
        val userId = approvalRecord.userId
        val userManagerRecord = managerUserDao.get(dslContext, managerId, userId)
        val managerOrganization = managerOrganizationService.getManagerOrganization(managerId)
        val authName = managerOrganization!!.name
        val authDetail = "${managerOrganization.organizationName}->${managerOrganization.strategyName}"
        val expiredTime = userManagerRecord!!.endTime
        when (approvalType) {
            ApprovalType.AGREE -> {
                val weworkRobotNotifyMessage = buildManagerApprovalMessage(
                    userId = userId,
                    approvalId = approvalId,
                    manager = userManagerRecord.createUser,
                    authName = authName,
                    authDetail = authDetail,
                    expiredTime = expiredTime.toString()
                )
                client.get(ServiceNotifyResource::class).sendWeworkRobotNotify(weworkRobotNotifyMessage)
                authManagerApprovalDao.updateApprovalStatus(
                    dslContext = dslContext,
                    approvalId = approvalId,
                    status = USER_AGREE_TO_RENEWAL
                )
            }
            ApprovalType.REFUSE -> {
                authManagerApprovalDao.updateApprovalStatus(
                    dslContext = dslContext,
                    approvalId = approvalId,
                    status = USER_REFUSE_TO_RENEWAL
                )
            }
        }
        return true
    }

    private fun buildManagerApprovalMessage(
        userId: String,
        approvalId: Int,
        manager: String,
        authName: String,
        authDetail: String,
        expiredTime: String
    ): WeworkRobotNotifyMessage {
        val actions: MutableList<WeworkMarkdownAction> = ArrayList()
        val agreeButton = WeworkMarkdownAction(
            name = "agree",
            text = "同意续期",
            type = "button",
            value = approvalId.toString(),
            replaceText = "你已选择同意用户续期",
            borderColor = "2EAB49",
            textColor = "2EAB49"
        )
        val refuseButton = WeworkMarkdownAction(
            name = "refuse",
            text = "拒绝续期",
            type = "button",
            value = approvalId.toString(),
            replaceText = "你已选择拒绝用户续期",
            borderColor = "2EAB49",
            textColor = "2EAB49"
        )
        actions.add(agreeButton)
        actions.add(refuseButton)
        return WeworkRobotNotifyMessage(
            receivers = manager,
            receiverType = WeworkReceiverType.single,
            textType = WeworkTextType.markdown,
            message = "**蓝盾超级管理员权限续期申请审批**\\n申请人：$userId\\n授权名称：$authName" +
                "\\n授权详情：$authDetail\\n用户权限过期时间：$expiredTime\\n请选择是否同意用户续期权限\\n",
            attachments = WeworkMarkdownAttachment(
                callbackId = "approval",
                actions = actions
            )
        )
    }

    fun managerApproval(
        approvalId: Int,
        approvalType: ApprovalType
    ): Boolean {
        val approvalRecord = authManagerApprovalDao.getApprovalById(dslContext, approvalId)
        val checkResult = checkBeforeExecute(dslContext, approvalId, approvalRecord)
        if (checkResult) {
            return true
        }
        val managerId = approvalRecord!!.managerId
        val userId = approvalRecord.userId
        when (approvalType) {
            ApprovalType.AGREE -> {
                val weworkRobotNotifyMessage = WeworkRobotNotifyMessage(
                    receivers = userId,
                    receiverType = WeworkReceiverType.single,
                    textType = WeworkTextType.markdown,
                    message = "审批人同意了您的权限续期"
                )
                client.get(ServiceNotifyResource::class).sendWeworkRobotNotify(weworkRobotNotifyMessage)
                authManagerApprovalDao.updateApprovalStatus(
                    dslContext = dslContext,
                    approvalId = approvalId,
                    status = MANAGER_AGREE_TO_APPROVAL
                )
                managerUserDao.updateRecordsExpireTime(dslContext, managerId, userId)
            }
            ApprovalType.REFUSE -> {
                val weworkRobotNotifyMessage = WeworkRobotNotifyMessage(
                    receivers = userId,
                    receiverType = WeworkReceiverType.single,
                    textType = WeworkTextType.markdown,
                    message = "审批人拒绝了您的权限续期"
                )
                client.get(ServiceNotifyResource::class).sendWeworkRobotNotify(weworkRobotNotifyMessage)
                authManagerApprovalDao.updateApprovalStatus(
                    dslContext = dslContext,
                    approvalId = approvalId,
                    status = MANAGER_REFUSE_TO_APPROVAL
                )
            }
        }
        return true
    }

    fun checkExpiringManager() {
        val expiringRecords = managerUserDao.listExpiringRecords(dslContext) ?: return
        expiringRecords.forEach {
            val approvalRecord = authManagerApprovalDao.get(dslContext, it.managerId, it.userId)
            val managerOrganization = managerOrganizationService.getManagerOrganization(it.managerId)
            val authName = managerOrganization!!.name
            val authDetail = "${managerOrganization.organizationName}->${managerOrganization.strategyName}"
            logger.info("approvalRecord : $approvalRecord")
            // 若该过期记录为空，则从未发起审批，则直接发送续期消息给用户
            if (approvalRecord == null) {
                startRenewalProcess(
                    authManagerUserRecord = it,
                    dslContext = dslContext,
                    authName = authName,
                    authDetail = authDetail
                )
            } else {
                val now = LocalDateTime.now()
                val isApprovalExpired = now > approvalRecord.endTime
                if (isApprovalExpired) {
                    val isRefuseLastTime = approvalRecord.status == MANAGER_REFUSE_TO_APPROVAL ||
                        approvalRecord.status == USER_REFUSE_TO_RENEWAL
                    // 若是本轮审批，并且上一次用户拒绝续期或者审批拒绝续期，则不再重发
                    if (approvalRecord.expiredTime == it.endTime && isRefuseLastTime
                    ) {
                        return@forEach
                    } else {
                        startRenewalProcess(
                            authManagerUserRecord = it,
                            dslContext = dslContext,
                            authName = authName,
                            authDetail = authDetail
                        )
                    }
                } else {
                    // 审核单还未失效，不用重复发起审批
                    return@forEach
                }
            }
        }
    }

    private fun startRenewalProcess(
        authManagerUserRecord: TAuthManagerUserRecord,
        dslContext: DSLContext,
        authName: String,
        authDetail: String
    ) {
        val managerId = authManagerUserRecord.managerId
        val userId = authManagerUserRecord.userId
        val approvalId = authManagerApprovalDao.createApproval(
            dslContext = dslContext,
            userId = userId,
            managerId = managerId,
            expireTime = authManagerUserRecord.endTime,
            status = START_APPROVAL
        )
        val weworkRobotNotifyMessage = buildUserRenewalMessage(
            userId = userId,
            manager = authManagerUserRecord.createUser,
            approvalId = approvalId,
            authName = authName,
            authDetail = authDetail,
            expiredTime = authManagerUserRecord.endTime.toString()
        )

        val sendResult = client.get(ServiceNotifyResource::class).sendWeworkRobotNotify(weworkRobotNotifyMessage).data
        if (sendResult != true) {
            authManagerApprovalDao.deleteByapprovalId(dslContext, approvalId)
            logger.warn(
                "startRenewalProcess :send wework message failed .userId = $userId | " +
                    "managerId = $managerId | authDetail = $authDetail"
            )
        }
    }

    private fun buildUserRenewalMessage(
        userId: String,
        manager: String,
        approvalId: Int,
        authName: String,
        authDetail: String,
        expiredTime: String
    ): WeworkRobotNotifyMessage {
        val agreeButton = WeworkMarkdownAction(
            name = "agree",
            text = "同意续期",
            type = "button",
            value = approvalId.toString(),
            replaceText = "你已选择同意续期",
            borderColor = "2EAB49",
            textColor = "2EAB49"
        )
        val refuseButton = WeworkMarkdownAction(
            name = "refuse",
            text = "拒绝续期",
            type = "button",
            value = approvalId.toString(),
            replaceText = "你已选择拒绝续期",
            borderColor = "2EAB49",
            textColor = "2EAB49"
        )
        val actions: MutableList<WeworkMarkdownAction> = ArrayList()
        actions.add(agreeButton)
        actions.add(refuseButton)
        return WeworkRobotNotifyMessage(
            receivers = userId,
            receiverType = WeworkReceiverType.single,
            textType = WeworkTextType.markdown,
            message = "**蓝盾超级管理员权限续期**\\n授权名称：$authName\\n授权详情：$authDetail" +
                "\\n审批人：$manager\\n用户权限过期时间：$expiredTime\\n请选择是否需要续期权限\\n",
            attachments = WeworkMarkdownAttachment(
                callbackId = "renewal",
                actions = actions
            )
        )
    }

    private fun checkBeforeExecute(
        dslContext: DSLContext,
        approvalId: Int,
        approvalRecord: TAuthManagerApprovalRecord?
    ): Boolean {
        if (approvalRecord == null) {
            logger.warn("userRenewalAuth : approvalRecord is not exist! | approvalId = $approvalId")
            throw ErrorCodeException(
                errorCode = AuthMessageCode.APPROVAL_RECORD_NOT_EXIST,
                defaultMessage = "审批记录不存在！"
            )
        }
        val managerId = approvalRecord.managerId
        val userId = approvalRecord.userId
        // 校验是否权限过期
        val userManagerRecord = managerUserDao.get(dslContext, managerId, userId)
        if (userManagerRecord == null) {
            logger.warn("userRenewalAuth : Manager permission has expired! | userId = $userId | managerId = $managerId")
            throw ErrorCodeException(
                errorCode = AuthMessageCode.MANAGER_PERMISSION_EXPIRE,
                defaultMessage = "管理员权限已经失效，请重新发起申请！"
            )
        }
        val userExpiringRecord = managerUserDao.getExpiringRecord(dslContext, managerId, userId)
        if (userExpiringRecord == null) {
            logger.info(
                "userRenewalAuth : User permissions have not expired | " +
                    "userId = $userId | managerId = $managerId"
            )
            val weworkRobotNotifyMessage = WeworkRobotNotifyMessage(
                receivers = userId,
                receiverType = WeworkReceiverType.single,
                textType = WeworkTextType.markdown,
                message = "权限还未过期，不需要操作！"
            )
            client.get(ServiceNotifyResource::class).sendWeworkRobotNotify(weworkRobotNotifyMessage)
            return true
        }
        return false
    }

    companion object {
        private val logger = LoggerFactory.getLogger(AuthManagerApprovalService::class.java)
        const val START_APPROVAL = 0
        const val USER_REFUSE_TO_RENEWAL = 1
        const val USER_AGREE_TO_RENEWAL = 2
        const val MANAGER_REFUSE_TO_APPROVAL = 3
        const val MANAGER_AGREE_TO_APPROVAL = 4
    }
}
