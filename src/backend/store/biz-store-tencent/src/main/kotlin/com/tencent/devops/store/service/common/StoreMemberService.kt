package com.tencent.devops.store.service.common

import com.tencent.devops.common.api.constant.CommonMessageCode
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.service.utils.MessageCodeUtil
import com.tencent.devops.model.atom.tables.records.TStoreMemberRecord
import com.tencent.devops.notify.api.ServiceNotifyMessageTemplateResource
import com.tencent.devops.notify.model.SendNotifyMessageTemplateRequest
import com.tencent.devops.project.api.ServiceProjectResource
import com.tencent.devops.store.constant.StoreMessageCode
import com.tencent.devops.store.dao.common.StoreMemberDao
import com.tencent.devops.store.dao.common.StoreProjectRelDao
import com.tencent.devops.store.pojo.common.STORE_MEMBER_ADD_NOTIFY_TEMPLATE
import com.tencent.devops.store.pojo.common.STORE_MEMBER_DELETE_NOTIFY_TEMPLATE
import com.tencent.devops.store.pojo.common.StoreMemberItem
import com.tencent.devops.store.pojo.common.StoreMemberReq
import com.tencent.devops.store.pojo.common.enums.StoreMemberTypeEnum
import com.tencent.devops.store.pojo.common.enums.StoreProjectTypeEnum
import com.tencent.devops.store.pojo.common.enums.StoreTypeEnum
import org.jooq.DSLContext
import org.jooq.impl.DSL
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import java.time.format.DateTimeFormatter
import java.time.temporal.TemporalAccessor
import java.util.concurrent.Executors

@Service
abstract class StoreMemberService {

    @Autowired
    lateinit var client: Client
    @Autowired
    lateinit var dslContext: DSLContext
    @Autowired
    lateinit var storeMemberDao: StoreMemberDao
    @Autowired
    lateinit var storeProjectRelDao: StoreProjectRelDao

    private val executorService = Executors.newFixedThreadPool(5)

    private val logger = LoggerFactory.getLogger(StoreMemberService::class.java)

    /**
     * store组件成员列表
     */
    fun list(userId: String, storeCode: String, storeType: StoreTypeEnum): Result<List<StoreMemberItem?>> {
        logger.info("getStoreMemberList userId is:$userId,storeCode is:$storeCode,storeType is:$storeType")
        if (! storeMemberDao.isStoreMember(dslContext, userId, storeCode, storeType.type.toByte())) {
            return MessageCodeUtil.generateResponseDataObject(CommonMessageCode.PERMISSION_DENIED)
        }
        val records = storeMemberDao.list(dslContext, storeCode, null, storeType.type.toByte())
        logger.info("getStoreMemberList records is:$records")
        // 获取调试项目对应的名称
        val projectCodeList = mutableListOf<String>()
        records?.forEach {
            val testProjectCode = storeProjectRelDao.getUserStoreTestProjectCode(dslContext, it.username, storeCode, storeType)
            if (null != testProjectCode) projectCodeList.add(testProjectCode)
        }
        logger.info("getStoreMemberList projectCodeList is:$projectCodeList")
        val projectMap = client.get(ServiceProjectResource::class).getNameByCode(projectCodeList.joinToString(",")).data
        val members = mutableListOf<StoreMemberItem?>()
        records?.forEach {
            val projectCode = storeProjectRelDao.getUserStoreTestProjectCode(dslContext, it.username, storeCode, storeType)
            members.add(
                generateStoreMemberItem(it, projectMap?.get(projectCode) ?: "")
            )
        }
        return Result(members)
    }

    /**
     * 查看store组件成员信息
     */
    fun viewMemberInfo(userId: String, storeCode: String, storeType: StoreTypeEnum): Result<StoreMemberItem?> {
        logger.info("viewMemberInfo userId is:$userId,storeCode is:$storeCode,storeType is:$storeType")
        val memberRecord = storeMemberDao.getMemberInfo(dslContext, userId, storeCode, storeType.type.toByte())
        logger.info("viewMemberInfo memberRecord is:$memberRecord")
        return if (null != memberRecord) {
            // 获取调试项目对应的名称
            val projectCodeList = mutableListOf<String>()
            val projectCode = storeProjectRelDao.getUserStoreTestProjectCode(dslContext, memberRecord.username, storeCode, storeType)
            if (null != projectCode) projectCodeList.add(projectCode)
            logger.info("getStoreMemberList projectCodeList is:$projectCodeList")
            val projectMap = client.get(ServiceProjectResource::class).getNameByCode(projectCodeList.joinToString(",")).data
            Result(generateStoreMemberItem(memberRecord, projectMap?.get(projectCode) ?: ""))
        } else {
            Result(data = null)
        }
    }

    fun batchListMember(storeCodeList: List<String?>, storeType: StoreTypeEnum): Result<HashMap<String, MutableList<String>>> {
        val ret = hashMapOf<String, MutableList<String>>()
        val records = storeMemberDao.batchList(dslContext, storeCodeList, storeType.type.toByte())
        records?.forEach {
            val list = if (ret.containsKey(it["STORE_CODE"] as String)) {
                ret[it["STORE_CODE"] as String]!!
            } else {
                val tmp = mutableListOf<String>()
                ret[it["STORE_CODE"] as String] = tmp
                tmp
            }
            list.add(it["USERNAME"] as String)
        }
        return Result(ret)
    }

    /**
     * 添加store组件成员
     */
    fun add(userId: String, storeMemberReq: StoreMemberReq, storeType: StoreTypeEnum, collaborationFlag: Boolean? = false): Result<Boolean> {
        logger.info("addMember userId is:$userId,storeMemberReq is:$storeMemberReq,storeType is:$storeType")
        val storeCode = storeMemberReq.storeCode
        val type = storeMemberReq.type.type.toByte()
        if (!storeMemberDao.isStoreAdmin(dslContext, userId, storeCode, storeType.type.toByte())) {
            return MessageCodeUtil.generateResponseDataObject(CommonMessageCode.PERMISSION_DENIED)
        }
        val receivers = mutableSetOf<String>()
        for (item in storeMemberReq.member) {
            if (storeMemberDao.isStoreMember(dslContext, item, storeCode, storeType.type.toByte())) {
                continue
            }
            dslContext.transaction { t ->
                val context = DSL.using(t)
                storeMemberDao.addStoreMember(context, userId, storeCode, item, type, storeType.type.toByte())
                if (null != collaborationFlag && !collaborationFlag) {
                    // 协作申请方式，添加成员时无需再添加调试项目
                    val testProjectCode = storeProjectRelDao.getUserStoreTestProjectCode(context, userId, storeCode, storeType)
                    storeProjectRelDao.addStoreProjectRel(context, item, storeCode, testProjectCode!!, StoreProjectTypeEnum.TEST.type.toByte(), storeType.type.toByte())
                }
            }
            receivers.add(item)
        }
        executorService.submit<Result<Boolean>> {
            sendNotifyMessage(userId, storeCode, STORE_MEMBER_ADD_NOTIFY_TEMPLATE + "_$storeType", receivers)
        }
        return Result(true)
    }

    /**
     * 删除store组件成员
     */
    fun delete(userId: String, id: String, storeCode: String, storeType: StoreTypeEnum): Result<Boolean> {
        logger.info("deleteMember userId is:$userId,id is:$id,storeCode is:$storeCode,storeType is:$storeType")
        if (!storeMemberDao.isStoreAdmin(dslContext, userId, storeCode, storeType.type.toByte())) {
            return MessageCodeUtil.generateResponseDataObject(CommonMessageCode.PERMISSION_DENIED)
        }
        val record = storeMemberDao.getById(dslContext, id)
        if (record != null) {
            if ((record.type).toInt() == 0) {
                val validateAdminResult = isStoreHasAdmins(storeCode, storeType)
                if (validateAdminResult.isNotOk()) {
                    return Result(status = validateAdminResult.status, message = validateAdminResult.message, data = false)
                }
            }
            dslContext.transaction { t ->
                val context = DSL.using(t)
                storeMemberDao.delete(context, id)
                // 删除成员对应的调试项目
                storeProjectRelDao.deleteUserStoreTestProject(
                    dslContext = context,
                    userId = record.username,
                    storeProjectType = StoreProjectTypeEnum.TEST,
                    storeCode = storeCode,
                    storeType = storeType
                )
            }
            executorService.submit<Result<Boolean>> {
                val receivers = mutableSetOf(record.username)
                sendNotifyMessage(userId, storeCode, STORE_MEMBER_DELETE_NOTIFY_TEMPLATE + "_$storeType", receivers)
            }
        }
        return Result(true)
    }

    private fun sendNotifyMessage(userId: String, storeCode: String, templateCode: String, receivers: MutableSet<String>): Result<Boolean> {
        val bodyParams = mapOf("storeAdmin" to userId, "storeName" to getStoreName(storeCode))
        val sendNotifyMessageTemplateRequest = SendNotifyMessageTemplateRequest(
            templateCode = templateCode,
            sender = "DevOps",
            receivers = receivers,
            bodyParams = bodyParams
        )
        val sendNotifyResult = client.get(ServiceNotifyMessageTemplateResource::class).sendNotifyMessageByTemplate(sendNotifyMessageTemplateRequest)
        logger.info("sendNotifyResult is:$sendNotifyResult")
        return Result(true)
    }

    /**
     * 获取组件名称
     */
    abstract fun getStoreName(storeCode: String): String

    /**
     * 更改store组件成员的调试项目
     */
    fun changeMemberTestProjectCode(accessToken: String, userId: String, projectCode: String, storeCode: String, storeType: StoreTypeEnum): Result<Boolean> {
        logger.info("changeMemberTestProjectCode userId is:$userId,accessToken is:$accessToken")
        logger.info("changeMemberTestProjectCode projectCode is:$projectCode,storeCode is:$storeCode,storeType is:$storeType")
        if (!storeMemberDao.isStoreMember(dslContext, userId, storeCode, storeType.type.toByte())) {
            return MessageCodeUtil.generateResponseDataObject(CommonMessageCode.PERMISSION_DENIED)
        }
        val validateFlag: Boolean?
        try {
            // 判断用户是否项目的成员
            validateFlag = client.get(ServiceProjectResource::class).verifyUserProjectPermission(accessToken, projectCode, userId).data
        } catch (e: Exception) {
            logger.error("verifyUserProjectPermission error is :$e")
            return MessageCodeUtil.generateResponseDataObject(CommonMessageCode.SYSTEM_ERROR)
        }
        logger.info("the validateFlag is :$validateFlag")
        if (null == validateFlag || !validateFlag) {
            // 抛出错误提示
            return MessageCodeUtil.generateResponseDataObject(CommonMessageCode.PERMISSION_DENIED)
        }
        dslContext.transaction { t ->
            val context = DSL.using(t)
            // 更新用户的调试项目
            storeProjectRelDao.updateUserStoreTestProject(
                dslContext = context,
                userId = userId,
                projectCode = projectCode,
                storeProjectType = StoreProjectTypeEnum.TEST,
                storeCode = storeCode,
                storeType = storeType
            )
        }
        return Result(true)
    }

    /**
     * 判断store组件是否有超过一个管理员
     */
    fun isStoreHasAdmins(storeCode: String, storeType: StoreTypeEnum): Result<Boolean> {
        val adminCount = storeMemberDao.countAdmin(dslContext, storeCode, storeType.type.toByte())
        if (adminCount <= 1) {
            return MessageCodeUtil.generateResponseDataObject(StoreMessageCode.USER_COMPONENT_ADMIN_COUNT_ERROR)
        }
        return Result(true)
    }

    /**
     * 判断是否为成员
     */
    fun isStoreMember(userId: String, storeCode: String, storeType: Byte): Boolean {
        return storeMemberDao.isStoreMember(dslContext, userId, storeCode, storeType)
    }

    /**
     * 判断是否为管理员
     */
    fun isStoreAdmin(userId: String, storeCode: String, storeType: Byte): Boolean {
        return storeMemberDao.isStoreAdmin(dslContext, userId, storeCode, storeType)
    }

    private fun generateStoreMemberItem(memberRecord: TStoreMemberRecord, projectName: String): StoreMemberItem {
        val df = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
        return StoreMemberItem(
            id = memberRecord.id as String,
            userName = memberRecord.username as String,
            projectName = projectName,
            type = StoreMemberTypeEnum.getAtomMemberType((memberRecord.type as Byte).toInt()),
            creator = memberRecord.creator as String,
            modifier = memberRecord.modifier as String,
            createTime = df.format(memberRecord.createTime as TemporalAccessor),
            updateTime = df.format(memberRecord.updateTime as TemporalAccessor)
        )
    }
}