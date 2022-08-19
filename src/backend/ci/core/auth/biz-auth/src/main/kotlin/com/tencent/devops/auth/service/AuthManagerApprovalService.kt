package com.tencent.devops.auth.service

import com.tencent.devops.auth.constant.AuthMessageCode
import com.tencent.devops.auth.dao.AuthManagerApprovalDao
import com.tencent.devops.auth.dao.ManagerUserDao
import com.tencent.devops.auth.pojo.enum.ApprovalType
import com.tencent.devops.common.api.exception.ErrorCodeException
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.notify.enums.WeworkReceiverType
import com.tencent.devops.common.notify.enums.WeworkTextType
import com.tencent.devops.common.service.utils.MessageCodeUtil
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
class AuthManagerApprovalService @Autowired constructor(
    val dslContext: DSLContext,
    val authManagerApprovalDao: AuthManagerApprovalDao,
    val managerUserDao: ManagerUserDao,
    private val client: Client,
) {
    fun userRenewalAuth(
        approvalId: Int,
        approvalType: ApprovalType,
    ): Boolean {
        val approvalRecord = authManagerApprovalDao.getApprovalById(dslContext, approvalId)
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
        // todo 进一步 检验是否权限是否需要续约
        val userExpiringRecord = managerUserDao.getExpiringRecord(dslContext, managerId, userId)
        if (userExpiringRecord == null) {
            logger.info("userRenewalAuth : User permissions have not expired | userId = $userId | managerId = $managerId")
            return true
        }
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
        when (approvalType) {
            ApprovalType.AGREE -> {
                authManagerApprovalDao.updateApprovalStatus(dslContext, approvalId, 2)
                val weworkRobotNotifyMessage = WeworkRobotNotifyMessage(
                    //审批人 要修改
                    receivers = "greysonfang",
                    receiverType = WeworkReceiverType.single,
                    textType = WeworkTextType.markdown,
                    message = "**蓝盾超级管理员权限续期申请审批**\\n管理员ID：$managerId\\n 用户ID：$userId" +
                        "logo+白色T\\n\\n请选择是否同意用户续期权限\\n",
                    attachments = WeworkMarkdownAttachment(
                        callbackId = "approval",
                        actions = actions
                    )
                )
                client.get(ServiceNotifyResource::class).sendWeworkRobotNotify(weworkRobotNotifyMessage)
            }
            ApprovalType.REFUSE -> {
                authManagerApprovalDao.updateApprovalStatus(dslContext, approvalId, 1)
            }
        }
        return true
    }

    fun managerApproval(
        approvalId: Int,
        approvalType: ApprovalType
    ): Boolean {
        val approvalRecord = authManagerApprovalDao.getApprovalById(dslContext, approvalId)
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
        // todo 进一步 检验是否权限是否需要续约
        val userExpiringRecord = managerUserDao.getExpiringRecord(dslContext, managerId, userId)
        if (userExpiringRecord == null) {
            logger.info("userRenewalAuth : User permissions have not expired | userId = $userId | managerId = $managerId")
            return false
        }
        when (approvalType) {
            ApprovalType.AGREE -> {
                authManagerApprovalDao.updateApprovalStatus(dslContext, approvalId, 4)
                managerUserDao.updateRecordsExpireTime(dslContext, managerId, userId)
                val weworkRobotNotifyMessage = WeworkRobotNotifyMessage(
                    receivers = userId,
                    receiverType = WeworkReceiverType.single,
                    textType = WeworkTextType.markdown,
                    message = "审批人同意了您的权限续期"
                )
                client.get(ServiceNotifyResource::class).sendWeworkRobotNotify(weworkRobotNotifyMessage)
            }
            ApprovalType.REFUSE -> {
                authManagerApprovalDao.updateApprovalStatus(dslContext, approvalId, 3)
                val weworkRobotNotifyMessage = WeworkRobotNotifyMessage(
                    receivers = userId,
                    receiverType = WeworkReceiverType.single,
                    textType = WeworkTextType.markdown,
                    message = "审批人拒绝了您的权限续期"
                )
                client.get(ServiceNotifyResource::class).sendWeworkRobotNotify(weworkRobotNotifyMessage)
            }
        }
        return true
    }

    fun checkExpiringManager() {
        val expiringRecords = managerUserDao.listExpiringRecords(dslContext) ?: return
        logger.info("sentNotifyToExpiringUser : expiringRecords = ${expiringRecords}")
        expiringRecords.map {
            val approvalRecord = authManagerApprovalDao.get(dslContext, it.managerId, it.userId)
            logger.info("approvalRecord : $approvalRecord")
            val agreeButton = WeworkMarkdownAction(
                name = "agree",
                text = "同意续期",
                type = "button",
                value = approvalRecord?.id.toString(),
                replaceText = "你已选择同意续期",
                borderColor = "2EAB49",
                textColor = "2EAB49"
            )
            val refuseButton = WeworkMarkdownAction(
                name = "refuse",
                text = "拒绝续期",
                type = "button",
                value = approvalRecord?.id.toString(),
                replaceText = "你已选择拒绝续期",
                borderColor = "2EAB49",
                textColor = "2EAB49"
            )
            val actions: MutableList<WeworkMarkdownAction> = ArrayList()
            actions.add(agreeButton)
            actions.add(refuseButton)
            val weworkRobotNotifyMessage = WeworkRobotNotifyMessage(
                receivers = it.userId,
                receiverType = WeworkReceiverType.single,
                textType = WeworkTextType.markdown,
                message = "**蓝盾超级管理员权限续期**\\n管理员ID：${it.managerId}\\n 用户ID：${it.userId}" +
                    "logo+白色T\\n\\n请选择是否需要续期权限\\n",
                attachments = WeworkMarkdownAttachment(
                    callbackId = "renewal",
                    actions = actions
                )
            )
            if (approvalRecord == null) {
                authManagerApprovalDao.createApproval(
                    dslContext = dslContext,
                    userId = it.userId,
                    managerId = it.managerId,
                    expireTime = it.endTime,
                    status = 0
                )
                client.get(ServiceNotifyResource::class).sendWeworkRobotNotify(weworkRobotNotifyMessage)
            } else {
                val now = LocalDateTime.now()
                // 审核单还未失效，不用发起审批
                if (now < approvalRecord.endTime) {
                    return@map
                } else {
                    // 表示本次审批，而且上次被审批人拒绝了，不再发送
                    if (approvalRecord.expiredTime == it.endTime && approvalRecord.status == 3)
                        return@map
                    else {
                        authManagerApprovalDao.createApproval(
                            dslContext = dslContext,
                            userId = it.userId,
                            managerId = it.managerId,
                            expireTime = it.endTime,
                            status = 0
                        )
                        client.get(ServiceNotifyResource::class).sendWeworkRobotNotify(weworkRobotNotifyMessage)
                    }
                }
            }
        }
    }

    companion object {
        private val logger = LoggerFactory.getLogger(AuthManagerApprovalService::class.java)
    }
}
