package com.tencent.devops.store.service.common.impl

import com.tencent.devops.auth.api.service.ServiceDeptResource
import com.tencent.devops.auth.pojo.DeptInfo
import com.tencent.devops.common.api.constant.CommonMessageCode
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.api.util.JsonUtil
import com.tencent.devops.common.api.util.UUIDUtil
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.service.utils.MessageCodeUtil
import com.tencent.devops.common.service.utils.SpringContextUtil
import com.tencent.devops.model.store.tables.records.TStorePublisherInfoRecord
import com.tencent.devops.model.store.tables.records.TStorePublisherMemberRelRecord
import com.tencent.devops.project.api.service.ServiceUserResource
import com.tencent.devops.project.pojo.user.UserDeptDetail
import com.tencent.devops.store.dao.common.PublishersDao
import com.tencent.devops.store.dao.common.StoreDockingPlatformDao
import com.tencent.devops.store.dao.common.StoreMemberDao
import com.tencent.devops.store.pojo.common.PublisherInfo
import com.tencent.devops.store.pojo.common.PublishersRequest
import com.tencent.devops.store.pojo.common.StoreDockingPlatformRequest
import com.tencent.devops.store.pojo.common.enums.PublisherType
import com.tencent.devops.store.pojo.common.enums.StoreTypeEnum
import com.tencent.devops.store.service.common.PublishersDataService
import com.tencent.devops.store.service.common.StoreMemberService
import com.tencent.devops.store.service.common.StoreUserService
import org.jooq.DSLContext
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import java.time.LocalDateTime

@Service
class PublishersDataServiceImpl @Autowired constructor(
    private val dslContext: DSLContext,
    private val publishersDao: PublishersDao,
    private val client: Client,
    private val storeDockingPlatformDao: StoreDockingPlatformDao,
    private val storeMemberDao: StoreMemberDao,
    private val storeUserService: StoreUserService
) : PublishersDataService {
    override fun createPublisherData(userId: String, publishers: List<PublishersRequest>): Int  {
        val storePublisherInfoRecords = mutableListOf<TStorePublisherInfoRecord>()
        val storePublisherMemberRelRecords = mutableListOf<TStorePublisherMemberRelRecord>()
        publishers.forEach {
            val deptInfos = analysisDept(userId, it.organization)
            val storePublisherInfo = TStorePublisherInfoRecord()
            val storePublisherInfoId = UUIDUtil.generate()
            storePublisherInfo.id = storePublisherInfoId
            storePublisherInfo.publisherCode = it.publishersCode
            storePublisherInfo.publisherName = it.name
            storePublisherInfo.publisherType = it.publishersType.name
            storePublisherInfo.owners = it.owners[0]
            storePublisherInfo.helper = it.helper
            storePublisherInfo.firstLevelDeptId = deptInfos[0].id.toLong()
            storePublisherInfo.firstLevelDeptName = deptInfos[0].name
            storePublisherInfo.secondLevelDeptId = deptInfos[1].id.toLong()
            storePublisherInfo.secondLevelDeptName = deptInfos[1].name
            storePublisherInfo.thirdLevelDeptId = deptInfos[2].id.toLong()
            storePublisherInfo.thirdLevelDeptName = deptInfos[2].name
            if (deptInfos.size > 3) {
                storePublisherInfo.fourthLevelDeptId = deptInfos[3].id.toLong()
                storePublisherInfo.fourthLevelDeptName = deptInfos[3].name
            }
            storePublisherInfo.organizationName = it.organization
            storePublisherInfo.ownerDeptName = it.ownerDeptName
            storePublisherInfo.certificationFlag = it.certificationFlag
            storePublisherInfo.storeType = it.storeType.type.toByte()
            storePublisherInfo.creator = userId
            storePublisherInfo.modifier = userId
            storePublisherInfo.createTime = LocalDateTime.now()
            storePublisherInfo.updateTime = LocalDateTime.now()
            storePublisherInfoRecords.add(storePublisherInfo)
            if (it.publishersType == PublisherType.ORGANIZATION) {
                //  生成可使用组织发布者进行发布的成员关联
                logger.debug("CreatePublisherMemberRel publisherCode is ${it.publishersCode}, members is ${it.members}")
                getStoreMemberService(it.storeType)
                    .getMemberId(it.publishersCode, it.storeType, it.members)
                    .data?.map { memberId ->
                    val storePublisherMemberRel = TStorePublisherMemberRelRecord()
                    storePublisherMemberRel.id = UUIDUtil.generate()
                    storePublisherMemberRel.publisherId = storePublisherInfoId
                    storePublisherMemberRel.memberId = memberId
                    storePublisherMemberRel.creator = userId
                    storePublisherMemberRel.createTime = LocalDateTime.now()
                    storePublisherMemberRel.modifier = userId
                    storePublisherMemberRel.updateTime = LocalDateTime.now()
                    storePublisherMemberRelRecords.add(storePublisherMemberRel)
                }
            }
        }
        val batchCreateCount = publishersDao.batchCreate(dslContext, storePublisherInfoRecords)
        publishersDao.batchCreatePublisherMemberRel(dslContext, storePublisherMemberRelRecords)
        return batchCreateCount
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
            publishersDao.batchDeletePublisherMemberRelById(dslContext, organizePublishersIds)
        }
        return publishersDao.batchDelete(dslContext, publishers)
    }

    override fun updatePublisherData(userId: String, publishers: List<PublishersRequest>): Int  {
        val storePublisherInfoRecords = mutableListOf<TStorePublisherInfoRecord>()
        publishers.forEach {
            val deptInfos = analysisDept(userId, it.organization)
            val records = TStorePublisherInfoRecord()
            records.publisherCode = it.publishersCode
            records.publisherName = it.name
            records.firstLevelDeptName = deptInfos[0].name
            records.firstLevelDeptId = deptInfos[0].id.toLong()
            records.secondLevelDeptName = deptInfos[1].name
            records.secondLevelDeptId = deptInfos[1].id.toLong()
            records.thirdLevelDeptId = deptInfos[2].id.toLong()
            records.thirdLevelDeptName = deptInfos[2].name
            if (deptInfos.size > 3) {
                records.fourthLevelDeptId = deptInfos[3].id.toLong()
                records.fourthLevelDeptName = deptInfos[3].name
            }
            records.publisherType = it.publishersType.name
            records.owners = JsonUtil.toJson(it.owners)
            records.certificationFlag = it.certificationFlag
            records.organizationName = it.organization
            records.modifier = userId
            records.ownerDeptName = it.ownerDeptName
            records.helper = it.helper
            records.storeType = it.storeType.type.toByte()
            records.updateTime = LocalDateTime.now()
            storePublisherInfoRecords.add(records)
        }
        return publishersDao.batchUpdate(dslContext, storePublisherInfoRecords)
    }

    override fun createPlatformsData(
        userId: String,
        storeDockingPlatformRequests: List<StoreDockingPlatformRequest>
    ): Int {
        return storeDockingPlatformDao.batchCreate(dslContext, userId, storeDockingPlatformRequests)
    }

    override fun deletePlatformsData(
        userId: String,
        storeDockingPlatformRequests: List<StoreDockingPlatformRequest>
    ): Int  {
        return storeDockingPlatformDao.batchDelete(dslContext, userId, storeDockingPlatformRequests)
    }

    override fun updatePlatformsData(
        userId: String,
        storeDockingPlatformRequests: List<StoreDockingPlatformRequest>
    ): Int {
        return storeDockingPlatformDao.batchUpdate(dslContext, userId, storeDockingPlatformRequests)
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
            return MessageCodeUtil.generateResponseDataObject(CommonMessageCode.PERMISSION_DENIED)
        }
        // 查看是否能使用组织发布者进行发布
        val viewMemberInfo = getStoreMemberService(storeType).viewMemberInfo(userId, storeCode, storeType).data
        val organizationPublisherId =
            publishersDao.getPublisherMemberRelById(dslContext, storeCode, viewMemberInfo!!.id)
        if (!organizationPublisherId.isNullOrBlank()) {
            // 获取组织发布者信息
            val organizationPublisherInfo = publishersDao.getPublisherInfoById(dslContext, organizationPublisherId)
            publishersInfos.add(organizationPublisherInfo!!)
        }
        var personPublisherInfo = publishersDao.getPublisherInfoByCode(dslContext, userId)
        logger.debug("getPublishers personPublisherInfo is $personPublisherInfo")
        if (personPublisherInfo == null) {
            // 如果未注册发布者则自动注册并返回
            val userDeptInfo = client.get(ServiceUserResource::class).getDetailFromCache(userId).data
            userDeptInfo?.let {
                personPublisherInfo = PublisherInfo(
                    id = UUIDUtil.generate(),
                    publisherCode = userId,
                    publisherName = userId,
                    publisherType = PublisherType.PERSON,
                    owners = userId,
                    helper = userId,
                    firstLevelDeptId = it.bgId.toInt(),
                    firstLevelDeptName = it.bgName,
                    secondLevelDeptId = it.deptId.toInt(),
                    secondLevelDeptName = it.deptName,
                    thirdLevelDeptId = it.centerId.toInt(),
                    thirdLevelDeptName = it.centerName,
                    fourthLevelDeptId = it.groupId.toInt(),
                    fourthLevelDeptName = it.groupName,
                    organizationName = storeUserService.getUserFullDeptName(userId).data ?: "",
                    ownerDeptName = it.deptName,
                    certificationFlag = false,
                    storeType = storeType,
                    creator = userId,
                    modifier = userId,
                    createTime = LocalDateTime.now(),
                    updateTime = LocalDateTime.now()
                )
                publishersDao.create(dslContext, personPublisherInfo!!)
            }
        }
        personPublisherInfo?.let { publishersInfos.add(it) }
        logger.debug("getPublishers $publishersInfos")
        return Result(publishersInfos)
    }

    private fun analysisDept(userId: String, organization: String): List<DeptInfo> {
        //  根据解析组织名称获取组织ID
        val deptNames = organization.split("/")
        val deptInfos = mutableListOf<DeptInfo>()
        deptNames.forEachIndexed(){ index, deptName ->
            val result = client.get(ServiceDeptResource::class).getDeptByName(userId, deptName).data
            result?.let { it -> deptInfos.add(index, it.results[0] ) }
        }
        return deptInfos
    }

    private fun getStoreMemberService(storeType: StoreTypeEnum): StoreMemberService {
        return SpringContextUtil.getBean(StoreMemberService::class.java,
            "${storeType.name.toLowerCase()}MemberService")
    }
    companion object {
        private val logger = LoggerFactory.getLogger(PublishersDataServiceImpl::class.java)
    }
}