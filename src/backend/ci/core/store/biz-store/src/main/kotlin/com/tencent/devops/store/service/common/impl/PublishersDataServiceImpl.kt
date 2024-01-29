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

import com.tencent.devops.auth.api.service.ServiceDeptResource
import com.tencent.devops.auth.pojo.DeptInfo
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.api.util.JsonUtil
import com.tencent.devops.common.api.util.UUIDUtil
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.web.utils.I18nUtil
import com.tencent.devops.model.store.tables.records.TStorePublisherInfoRecord
import com.tencent.devops.model.store.tables.records.TStorePublisherMemberRelRecord
import com.tencent.devops.project.api.service.ServiceUserResource
import com.tencent.devops.store.constant.StoreMessageCode.GET_INFO_NO_PERMISSION
import com.tencent.devops.store.dao.common.PublisherMemberDao
import com.tencent.devops.store.dao.common.PublishersDao
import com.tencent.devops.store.dao.common.StoreDockingPlatformDao
import com.tencent.devops.store.dao.common.StoreDockingPlatformErrorCodeDao
import com.tencent.devops.store.dao.common.StoreMemberDao
import com.tencent.devops.store.pojo.common.PublisherInfo
import com.tencent.devops.store.pojo.common.PublishersRequest
import com.tencent.devops.store.pojo.common.StoreDockingPlatformRequest
import com.tencent.devops.store.pojo.common.enums.PublisherType
import com.tencent.devops.store.pojo.common.enums.StoreTypeEnum
import com.tencent.devops.store.service.common.PublishersDataService
import com.tencent.devops.store.service.common.StoreUserService
import java.time.LocalDateTime
import org.jooq.DSLContext
import org.jooq.impl.DSL
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
class PublishersDataServiceImpl @Autowired constructor(
    private val dslContext: DSLContext,
    private val publishersDao: PublishersDao,
    private val publisherMemberDao: PublisherMemberDao,
    private val client: Client,
    private val storeDockingPlatformDao: StoreDockingPlatformDao,
    private val storeMemberDao: StoreMemberDao,
    private val storeUserService: StoreUserService,
    private val storeDockingPlatformErrorCodeDao: StoreDockingPlatformErrorCodeDao
) : PublishersDataService {
    override fun createPublisherData(userId: String, publishers: List<PublishersRequest>): Int {
        val storePublisherInfoRecords = mutableListOf<TStorePublisherInfoRecord>()
        val storePublisherMemberRelRecords = mutableListOf<TStorePublisherMemberRelRecord>()
        publishers.forEach {
            val storePublisherInfo = getStorePublisherInfo(userId = userId, organization = it.organization)
            val storePublisherInfoId = UUIDUtil.generate()
            storePublisherInfo.id = storePublisherInfoId
            storePublisherInfo.publisherCode = it.publishersCode
            storePublisherInfo.publisherName = it.name
            storePublisherInfo.publisherType = it.publishersType.name
            storePublisherInfo.owners = it.owners[0]
            storePublisherInfo.helper = it.helper
            storePublisherInfo.organizationName = it.organization
            storePublisherInfo.bgName = it.bgName
            storePublisherInfo.certificationFlag = it.certificationFlag
            storePublisherInfo.storeType = it.storeType.type.toByte()
            storePublisherInfo.creator = userId
            storePublisherInfo.modifier = userId
            storePublisherInfo.createTime = LocalDateTime.now()
            storePublisherInfo.updateTime = LocalDateTime.now()
            storePublisherInfoRecords.add(storePublisherInfo)
            if (it.publishersType == PublisherType.ORGANIZATION) {
                //  生成可使用组织发布者进行发布的成员关联
                it.members.forEach { memberId ->
                    storePublisherMemberRelRecords.add(
                        publisherMemberDao.createPublisherMemberRel(storePublisherInfoId, memberId, userId)
                    )
                }
            }
        }
        val batchCreateCount = publishersDao.batchCreate(dslContext, storePublisherInfoRecords)
        publisherMemberDao.batchCreatePublisherMemberRel(dslContext, storePublisherMemberRelRecords)
        return batchCreateCount
    }

    override fun updatePublisherData(userId: String, publishers: List<PublishersRequest>): Int {
        val storePublisherInfoRecords = mutableListOf<TStorePublisherInfoRecord>()
        val addStorePublisherMemberRelRecords = mutableListOf<TStorePublisherMemberRelRecord>()
        val delStorePublisherMemberRelRecords = mutableListOf<TStorePublisherMemberRelRecord>()
        val createPublisherData = mutableListOf<PublishersRequest>()
        publishers.forEach {
            val publisherId = publishersDao.getPublisherId(dslContext, it.publishersCode)
            if (publisherId != null) {
                val records = getStorePublisherInfo(userId = userId, organization = it.organization)
                records.id = publisherId
                records.publisherCode = it.publishersCode
                records.publisherName = it.name
                records.publisherType = it.publishersType.name
                records.owners = JsonUtil.toJson(it.owners)
                records.certificationFlag = it.certificationFlag
                records.organizationName = it.organization
                records.modifier = userId
                records.bgName = it.bgName
                records.helper = it.helper
                records.storeType = it.storeType.type.toByte()
                records.updateTime = LocalDateTime.now()
                storePublisherInfoRecords.add(records)
                updateMembers(
                    userId = userId,
                    publisherId = publisherId,
                    newMembers = it.members,
                    addRecords = addStorePublisherMemberRelRecords,
                    delRecords = delStorePublisherMemberRelRecords
                )
            } else {
                createPublisherData.add(it)
            }
        }
        var count = 0
        dslContext.transaction { t ->
            val context = DSL.using(t)
            count = publishersDao.batchUpdate(context, storePublisherInfoRecords)
            publisherMemberDao.batchCreatePublisherMemberRel(context, addStorePublisherMemberRelRecords)
            publisherMemberDao.batchDeletePublisherMemberByMemberIds(context, delStorePublisherMemberRelRecords)
        }
        count += createPublisherData(userId, createPublisherData)
        return count
    }

    override fun deletePublisherData(userId: String, publishers: List<PublishersRequest>): Int {

        val organizePublishers = mutableListOf<String>()
        publishers.map {
            //  获取删除的组织发布者
            if (it.publishersType == PublisherType.ORGANIZATION) {
                organizePublishers.add(it.publishersCode)
            }
        }
        if (organizePublishers.isNotEmpty()) {
            //  删除组织发布者关联的组织成员关联
            val organizePublishersIds = publishersDao.getPublisherIdsByCode(dslContext, organizePublishers)
            publisherMemberDao.batchDeletePublisherMemberRelByPublisherId(dslContext, organizePublishersIds)
        }
        return publishersDao.batchDelete(dslContext, publishers)
    }

    private fun updateMembers(
        userId: String,
        publisherId: String,
        newMembers: List<String>,
        addRecords: MutableList<TStorePublisherMemberRelRecord>,
        delRecords: MutableList<TStorePublisherMemberRelRecord>
    ) {
        val members = publisherMemberDao.getPublisherMemberRelMemberIdsByPublisherId(dslContext, publisherId)
        val intersection = members.intersect(newMembers.toSet())
        members.forEach { member ->
            if (!intersection.contains(member)) {
                val storePublisherMemberRel = TStorePublisherMemberRelRecord()
                storePublisherMemberRel.publisherId = publisherId
                storePublisherMemberRel.memberId = member
                delRecords.add(storePublisherMemberRel)
            }
        }
        newMembers.forEach { newMember ->
            if (!intersection.contains(newMember)) {
                addRecords.add(publisherMemberDao.createPublisherMemberRel(publisherId, newMember, userId))
            }
        }
    }

    override fun savePlatformsData(
        userId: String,
        storeDockingPlatformRequests: List<StoreDockingPlatformRequest>
    ): Int {
        storeDockingPlatformRequests.forEach {
            dslContext.transaction { t ->
                val context = DSL.using(t)
                storeDockingPlatformDao.add(context, userId, it)
                if (!it.errorCodeInfo.isNullOrEmpty()) {
                    storeDockingPlatformErrorCodeDao.batchSaveErrorCodeInfo(
                        dslContext = context,
                        platformCode = it.platformCode,
                        errorCodeInfos = it.errorCodeInfo
                    )
                }
            }
        }
        return storeDockingPlatformRequests.size
    }

    override fun deletePlatformsData(
        userId: String,
        storeDockingPlatformRequests: List<StoreDockingPlatformRequest>
    ): Int {
        storeDockingPlatformRequests.forEach {
            dslContext.transaction { t ->
                val context = DSL.using(t)
                if (it.errorCodeInfo != null) {
                    storeDockingPlatformErrorCodeDao.deletePlatformErrorCodeInfo(
                        dslContext = context,
                        platformCode = it.platformCode,
                        errorCodes = it.errorCodeInfo!!.map { errorCodeInfo -> errorCodeInfo.errorCode }
                    )
                }
                storeDockingPlatformDao.delete(context, it)
            }
        }
        return storeDockingPlatformRequests.size
    }

    override fun getPublishers(
        userId: String,
        storeCode: String,
        storeType: StoreTypeEnum
    ): Result<List<PublisherInfo>> {
        val publishersInfos = mutableListOf<PublisherInfo>()
        if (!storeMemberDao.isStoreMember(
                dslContext = dslContext,
                userId = userId,
                storeCode = storeCode,
                storeType = storeType.type.toByte()
            )) {
            return I18nUtil.generateResponseDataObject(
                messageCode = GET_INFO_NO_PERMISSION,
                params = arrayOf(storeCode),
                language = I18nUtil.getLanguage(userId)
            )
        }
        val organizationPublisherIds =
            publisherMemberDao.getPublisherMemberRelByMemberId(dslContext, userId)
        if (organizationPublisherIds.isNotEmpty()) {
            // 获取组织发布者信息
            organizationPublisherIds.forEach { id ->
                val organizationPublisherInfo = publishersDao.getPublisherInfoById(dslContext, id)
                organizationPublisherInfo?.let { it -> publishersInfos.add(it) }
            }
        }
        var personPublisherInfo = publishersDao.getPublisherInfoByCode(dslContext, userId, storeType)
        if (personPublisherInfo == null) {
            // 如果未注册发布者则自动注册并返回
            val userDeptInfo = client.get(ServiceUserResource::class).getDetailFromCache(userId).data
            userDeptInfo?.let {
                personPublisherInfo = TStorePublisherInfoRecord()
                    personPublisherInfo!!.id = UUIDUtil.generate()
                    personPublisherInfo!!.publisherCode = userId
                    personPublisherInfo!!.publisherName = userId
                    personPublisherInfo!!.publisherType = PublisherType.PERSON.name
                    personPublisherInfo!!.owners = userId
                    personPublisherInfo!!.helper = userId
                    personPublisherInfo!!.firstLevelDeptId = it.bgId.toLong()
                    personPublisherInfo!!.firstLevelDeptName = it.bgName
                    personPublisherInfo!!.secondLevelDeptId = it.deptId.toLong()
                    personPublisherInfo!!.secondLevelDeptName = it.deptName
                    personPublisherInfo!!.thirdLevelDeptId = it.centerId.toLong()
                    personPublisherInfo!!.thirdLevelDeptName = it.centerName
                    personPublisherInfo!!.fourthLevelDeptId = it.groupId.toLong()
                    personPublisherInfo!!.fourthLevelDeptName = it.groupName
                    personPublisherInfo!!.organizationName = storeUserService.getUserFullDeptName(userId).data ?: ""
                    personPublisherInfo!!.bgName = it.bgName
                    personPublisherInfo!!.certificationFlag = false
                    personPublisherInfo!!.storeType = storeType.type.toByte()
                    personPublisherInfo!!.creator = userId
                    personPublisherInfo!!.modifier = userId
                    personPublisherInfo!!.createTime = LocalDateTime.now()
                    personPublisherInfo!!.updateTime = LocalDateTime.now()

                publishersDao.batchCreate(dslContext, listOf(personPublisherInfo!!))
            }
        }
        personPublisherInfo!!.let {
            publishersInfos.add(
                PublisherInfo(
                    id = UUIDUtil.generate(),
                    publisherCode = userId,
                    publisherName = userId,
                    publisherType = PublisherType.PERSON,
                    owners = userId,
                    helper = userId,
                    firstLevelDeptId = it.firstLevelDeptId.toInt(),
                    firstLevelDeptName = it.firstLevelDeptName,
                    secondLevelDeptId = it.secondLevelDeptId.toInt(),
                    secondLevelDeptName = it.secondLevelDeptName,
                    thirdLevelDeptId = it.thirdLevelDeptId.toInt(),
                    thirdLevelDeptName = it.thirdLevelDeptName,
                    fourthLevelDeptId = it.fourthLevelDeptId.toInt(),
                    fourthLevelDeptName = it.fourthLevelDeptName,
                    organizationName = storeUserService.getUserFullDeptName(userId).data ?: "",
                    bgName = it.bgName,
                    certificationFlag = false,
                    storeType = storeType,
                    creator = userId,
                    modifier = userId,
                    createTime = LocalDateTime.now(),
                    updateTime = LocalDateTime.now()
                )
            )
        }
        return Result(publishersInfos)
    }

    override fun updatePlatformsLogoInfo(userId: String, platformCode: String, logoUrl: String): Boolean {
        storeDockingPlatformDao.getStoreDockingPlatformByCode(dslContext, platformCode)?.let {
            storeDockingPlatformDao.updateStoreDockingPlatformLogoUrl(dslContext, it, logoUrl)
            return true
        }
        return false
    }

    private fun getStorePublisherInfo(
        userId: String,
        storePublisherInfo: TStorePublisherInfoRecord? = null,
        organization: String
    ): TStorePublisherInfoRecord {
        val publisherInfo = storePublisherInfo ?: TStorePublisherInfoRecord()
        //  根据解析组织名称获取组织ID
        val deptNames = organization.split("/")
        val deptInfos = mutableListOf<DeptInfo>()
        deptNames.forEachIndexed() { index, deptName ->
            val deptVo = client.get(ServiceDeptResource::class).getDeptByName(userId, deptName).data
            if (!deptVo?.results.isNullOrEmpty()) {
                deptInfos.add(index, deptVo!!.results[0])
            }
        }
        if (deptInfos.isEmpty()) {
            publisherInfo.firstLevelDeptId = 0
            publisherInfo.firstLevelDeptName = ""
            publisherInfo.secondLevelDeptId = 0
            publisherInfo.secondLevelDeptName = ""
            publisherInfo.thirdLevelDeptId = 0
            publisherInfo.thirdLevelDeptName = ""
            publisherInfo.fourthLevelDeptId = 0
            publisherInfo.fourthLevelDeptName = ""
        } else {
            // 最多存4级组织ID
            val levelCount = minOf(3, deptInfos.size)
            for (i in 0 until levelCount) {
                when (i) {
                    0 -> {
                        publisherInfo.firstLevelDeptId = deptInfos[i].id.toLong()
                        publisherInfo.firstLevelDeptName = deptInfos[i].name
                    }
                    1 -> {
                        publisherInfo.secondLevelDeptId = deptInfos[i].id.toLong()
                        publisherInfo.secondLevelDeptName = deptInfos[i].name
                    }
                    2 -> {
                        publisherInfo.thirdLevelDeptId = deptInfos[i].id.toLong()
                        publisherInfo.thirdLevelDeptName = deptInfos[i].name
                    }
                }
            }
            if (deptInfos.size > 3) {
                val fourthLevelDept = deptInfos[3]
                publisherInfo.fourthLevelDeptId = fourthLevelDept.id.toLong()
                publisherInfo.fourthLevelDeptName = fourthLevelDept.name
            }
        }
        return publisherInfo
    }
}
