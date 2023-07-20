/*
 * Tencent is pleased to support the open source community by making BK-CI 蓝鲸持续集成平台 available.
 *
 * Copyright (C) 2019 THL A29 Limited, a Tencent company.  All rights reserved.
 *
 * BK-CI 蓝鲸持续集成平台 is licensed under the MIT license.
 *
 * A copy of the MIT License is included in this file.
 *
 *
 * Terms of the MIT License:
 * ---------------------------------------------------
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated
 * documentation files (the "Software"), to deal in the Software without restriction, including without limitation the
 * rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to
 * permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of
 * the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT
 * LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN
 * NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE
 * SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package com.tencent.devops.store.service.common.impl

import com.tencent.devops.common.api.constant.CommonMessageCode
import com.tencent.devops.common.api.constant.DEVOPS
import com.tencent.devops.common.api.exception.ErrorCodeException
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.api.util.DateTimeUtil
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.web.utils.I18nUtil
import com.tencent.devops.model.store.tables.records.TStoreMemberRecord
import com.tencent.devops.project.api.service.ServiceProjectResource
import com.tencent.devops.store.constant.StoreMessageCode
import com.tencent.devops.store.constant.StoreMessageCode.GET_INFO_NO_PERMISSION
import com.tencent.devops.store.constant.StoreMessageCode.NO_COMPONENT_ADMIN_PERMISSION
import com.tencent.devops.store.dao.common.StoreMemberDao
import com.tencent.devops.store.dao.common.StoreProjectRelDao
import com.tencent.devops.store.pojo.common.STORE_MEMBER_ADD_NOTIFY_TEMPLATE
import com.tencent.devops.store.pojo.common.STORE_MEMBER_DELETE_NOTIFY_TEMPLATE
import com.tencent.devops.store.pojo.common.StoreMemberItem
import com.tencent.devops.store.pojo.common.StoreMemberReq
import com.tencent.devops.store.pojo.common.enums.StoreMemberTypeEnum
import com.tencent.devops.store.pojo.common.enums.StoreProjectTypeEnum
import com.tencent.devops.store.pojo.common.enums.StoreTypeEnum
import com.tencent.devops.store.service.common.StoreMemberService
import com.tencent.devops.store.service.common.StoreNotifyService
import java.util.concurrent.Executors
import org.jooq.DSLContext
import org.jooq.impl.DSL
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired

@Suppress("ALL")
abstract class StoreMemberServiceImpl : StoreMemberService {

    @Autowired
    lateinit var client: Client
    @Autowired
    lateinit var dslContext: DSLContext
    @Autowired
    lateinit var storeMemberDao: StoreMemberDao
    @Autowired
    lateinit var storeProjectRelDao: StoreProjectRelDao
    @Autowired
    lateinit var storeNotifyService: StoreNotifyService

    private val executorService = Executors.newFixedThreadPool(5)

    private val logger = LoggerFactory.getLogger(StoreMemberService::class.java)

    /**
     * store组件成员列表
     */
    override fun list(
        userId: String,
        storeCode: String,
        storeType: StoreTypeEnum,
        checkPermissionFlag: Boolean
    ): Result<List<StoreMemberItem?>> {
        if (checkPermissionFlag && !storeMemberDao.isStoreMember(
                dslContext = dslContext,
                userId = userId,
                storeCode = storeCode,
                storeType = storeType.type.toByte()
            )
        ) {
            return I18nUtil.generateResponseDataObject(
                messageCode = GET_INFO_NO_PERMISSION,
                language = I18nUtil.getLanguage(userId),
                params = arrayOf(storeCode)
            )
        }
        val records = storeMemberDao.list(
            dslContext = dslContext,
            storeCode = storeCode,
            type = null,
            storeType = storeType.type.toByte()
        )
        // 获取调试项目对应的名称
        val projectCodeList = mutableListOf<String>()
        records?.forEach {
            val testProjectCode = storeProjectRelDao.getUserStoreTestProjectCode(
                dslContext = dslContext,
                userId = it.username,
                storeCode = storeCode,
                storeType = storeType
            )
            if (null != testProjectCode) projectCodeList.add(testProjectCode)
        }
        val projectMap = client.get(ServiceProjectResource::class)
            .getNameByCode(projectCodeList.joinToString(",")).data
        val members = mutableListOf<StoreMemberItem?>()
        records?.forEach {
            val projectCode = storeProjectRelDao.getUserStoreTestProjectCode(
                dslContext = dslContext,
                userId = it.username,
                storeCode = storeCode,
                storeType = storeType
            )
            members.add(
                generateStoreMemberItem(it, projectCode ?: "", projectMap?.get(projectCode) ?: "")
            )
        }
        return Result(members)
    }

    /**
     * 查看store组件成员信息
     */
    override fun viewMemberInfo(userId: String, storeCode: String, storeType: StoreTypeEnum): Result<StoreMemberItem?> {
        logger.info("viewMemberInfo params:[$userId|$storeCode|$storeType]")
        val memberRecord = storeMemberDao.getMemberInfo(
            dslContext = dslContext,
            userId = userId,
            storeCode = storeCode,
            storeType = storeType.type.toByte()
        )
        return if (null != memberRecord) {
            // 获取调试项目对应的名称
            val projectCodeList = mutableListOf<String>()
            val projectCode = storeProjectRelDao.getUserStoreTestProjectCode(
                dslContext = dslContext,
                userId = memberRecord.username,
                storeCode = storeCode,
                storeType = storeType
            )
            if (null != projectCode) projectCodeList.add(projectCode)
            val projectMap = client.get(ServiceProjectResource::class)
                .getNameByCode(projectCodeList.joinToString(",")).data
            Result(generateStoreMemberItem(
                memberRecord = memberRecord,
                projectCode = projectCode ?: "",
                projectName = projectMap?.get(projectCode) ?: ""
            ))
        } else {
            Result(data = null)
        }
    }

    override fun batchListMember(
        storeCodeList: List<String?>,
        storeType: StoreTypeEnum
    ): Result<HashMap<String, MutableList<String>>> {
        val ret = hashMapOf<String, MutableList<String>>()
        val records = storeMemberDao.batchList(dslContext, storeCodeList, storeType.type.toByte())
        records?.forEach {
            val list = if (ret.containsKey(it.storeCode)) {
                ret[it.storeCode]!!
            } else {
                val tmp = mutableListOf<String>()
                ret[it.storeCode] = tmp
                tmp
            }
            list.add(it.username)
        }
        return Result(ret)
    }

    /**
     * 添加store组件成员
     */
    override fun add(
        userId: String,
        storeMemberReq: StoreMemberReq,
        storeType: StoreTypeEnum,
        collaborationFlag: Boolean?,
        sendNotify: Boolean,
        checkPermissionFlag: Boolean,
        testProjectCode: String?
    ): Result<Boolean> {
        val storeCode = storeMemberReq.storeCode
        val type = storeMemberReq.type.type.toByte()
        if (checkPermissionFlag && !storeMemberDao.isStoreAdmin(
                dslContext = dslContext,
                userId = userId,
                storeCode = storeCode,
                storeType = storeType.type.toByte()
            )) {
            return I18nUtil.generateResponseDataObject(
                messageCode = NO_COMPONENT_ADMIN_PERMISSION,
                language = I18nUtil.getLanguage(userId),
                params = arrayOf(storeCode)
            )
        }
        val receivers = mutableSetOf<String>()
        for (item in storeMemberReq.member) {
            if (storeMemberDao.isStoreMember(dslContext, item, storeCode, storeType.type.toByte())) {
                continue
            }
            dslContext.transaction { t ->
                val context = DSL.using(t)
                storeMemberDao.addStoreMember(context, userId, storeCode, item, type, storeType.type.toByte())
                if (null != testProjectCode) {
                    storeProjectRelDao.updateUserStoreTestProject(
                        dslContext = context,
                        userId = item,
                        storeCode = storeCode,
                        storeType = storeType,
                        projectCode = testProjectCode,
                        storeProjectType = StoreProjectTypeEnum.TEST
                    )
                } else if (null != collaborationFlag && !collaborationFlag) {
                    // 协作申请方式，添加成员时无需再添加调试项目
                    storeProjectRelDao.addStoreProjectRel(
                        dslContext = context,
                        userId = item,
                        storeCode = storeCode,
                        projectCode = storeProjectRelDao.getUserStoreTestProjectCode(
                            dslContext = context,
                            userId = userId,
                            storeCode = storeCode,
                            storeType = storeType
                        )!!,
                        type = StoreProjectTypeEnum.TEST.type.toByte(),
                        storeType = storeType.type.toByte()
                    )
                }
            }
            receivers.add(item)
        }
        if (sendNotify) {
            executorService.submit<Result<Boolean>> {
                val bodyParams = mapOf("storeAdmin" to userId, "storeName" to getStoreName(storeCode))
                storeNotifyService.sendNotifyMessage(
                    templateCode = STORE_MEMBER_ADD_NOTIFY_TEMPLATE + "_$storeType",
                    sender = DEVOPS,
                    receivers = receivers,
                    bodyParams = bodyParams
                )
            }
        }
        return Result(true)
    }

    /**
     * 删除store组件成员
     */
    override fun delete(
        userId: String,
        id: String,
        storeCode: String,
        storeType: StoreTypeEnum,
        checkPermissionFlag: Boolean
    ): Result<Boolean> {
        logger.info("deleteMember params:[$userId|$id|$storeCode|$storeType|$checkPermissionFlag")
        if (checkPermissionFlag && !storeMemberDao.isStoreAdmin(
                dslContext = dslContext,
                userId = userId,
                storeCode = storeCode,
                storeType = storeType.type.toByte()
            )
        ) {
            return I18nUtil.generateResponseDataObject(
                messageCode = NO_COMPONENT_ADMIN_PERMISSION,
                language = I18nUtil.getLanguage(userId),
                params = arrayOf(storeCode)
            )
        }
        val record = storeMemberDao.getById(dslContext, id)
        if (record != null) {
            if ((record.type).toInt() == 0) {
                val validateAdminResult = isStoreHasAdmins(storeCode, storeType)
                if (validateAdminResult.isNotOk()) {
                    return Result(validateAdminResult.status, message = validateAdminResult.message, data = false)
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
                val bodyParams = mapOf("storeAdmin" to userId, "storeName" to getStoreName(storeCode))
                storeNotifyService.sendNotifyMessage(
                    templateCode = STORE_MEMBER_DELETE_NOTIFY_TEMPLATE + "_$storeType",
                    sender = DEVOPS,
                    receivers = receivers,
                    bodyParams = bodyParams
                )
            }
        }
        return Result(true)
    }

    /**
     * 获取组件名称
     */
    abstract fun getStoreName(storeCode: String): String

    /**
     * 更改store组件成员的调试项目
     */
    override fun changeMemberTestProjectCode(
        accessToken: String,
        userId: String,
        storeMember: String,
        projectCode: String,
        storeCode: String,
        storeType: StoreTypeEnum
    ): Result<Boolean> {
        if (userId != storeMember) {
            // 如果要修改其他插件成员的调试项目，则要求修改人是插件的管理员
            if (!storeMemberDao.isStoreAdmin(dslContext, userId, storeCode, storeType.type.toByte())) {
                return I18nUtil.generateResponseDataObject(
                    messageCode = NO_COMPONENT_ADMIN_PERMISSION,
                    params = arrayOf(storeCode),
                    language = I18nUtil.getLanguage(userId)
                )
            }
        } else {
            if (!storeMemberDao.isStoreMember(dslContext, userId, storeCode, storeType.type.toByte())) {
                return I18nUtil.generateResponseDataObject(
                    messageCode = GET_INFO_NO_PERMISSION,
                    params = arrayOf(storeCode),
                    language = I18nUtil.getLanguage(userId)
                )
            }
        }
        val validateFlag: Boolean?
        try {
            // 判断用户是否项目的成员
            validateFlag = client.get(ServiceProjectResource::class).verifyUserProjectPermission(
                accessToken = accessToken,
                projectCode = projectCode,
                userId = storeMember
            ).data
        } catch (ignored: Throwable) {
            logger.warn("verifyUserProjectPermission error, params[$storeMember|$projectCode]", ignored)
            return I18nUtil.generateResponseDataObject(
                messageCode = CommonMessageCode.SYSTEM_ERROR,
                language = I18nUtil.getLanguage(userId)
            )
        }
        if (null == validateFlag || !validateFlag) {
            // 抛出错误提示
            val projectMap = client.get(ServiceProjectResource::class).getNameByCode(projectCode).data
            throw ErrorCodeException(
                errorCode = StoreMessageCode.USER_CHANGE_TEST_PROJECT_FAIL,
                params = arrayOf(storeMember, projectMap?.get(projectCode) ?: "")
            )
        }
        dslContext.transaction { t ->
            val context = DSL.using(t)
            // 更新用户的调试项目
            storeProjectRelDao.updateUserStoreTestProject(
                dslContext = context,
                userId = storeMember,
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
    override fun isStoreHasAdmins(storeCode: String, storeType: StoreTypeEnum): Result<Boolean> {
        val adminCount = storeMemberDao.countAdmin(dslContext, storeCode, storeType.type.toByte())
        if (adminCount <= 1) {
            return I18nUtil.generateResponseDataObject(
                messageCode = StoreMessageCode.USER_COMPONENT_ADMIN_COUNT_ERROR,
                language = I18nUtil.getLanguage(I18nUtil.getRequestUserId())
            )
        }
        return Result(true)
    }

    /**
     * 判断是否为成员
     */
    override fun isStoreMember(userId: String, storeCode: String, storeType: Byte): Boolean {
        return storeMemberDao.isStoreMember(dslContext, userId, storeCode, storeType)
    }

    /**
     * 判断是否为管理员
     */
    override fun isStoreAdmin(userId: String, storeCode: String, storeType: Byte): Boolean {
        return storeMemberDao.isStoreAdmin(dslContext, userId, storeCode, storeType)
    }

    private fun generateStoreMemberItem(
        memberRecord: TStoreMemberRecord,
        projectCode: String,
        projectName: String
    ): StoreMemberItem {
        return StoreMemberItem(
            id = memberRecord.id as String,
            userName = memberRecord.username as String,
            projectCode = projectCode,
            projectName = projectName,
            type = StoreMemberTypeEnum.getAtomMemberType((memberRecord.type as Byte).toInt()),
            creator = memberRecord.creator as String,
            modifier = memberRecord.modifier as String,
            createTime = DateTimeUtil.toDateTime(memberRecord.createTime),
            updateTime = DateTimeUtil.toDateTime(memberRecord.updateTime)
        )
    }
}
