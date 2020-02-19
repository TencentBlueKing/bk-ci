package com.tencent.devops.store.service

import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.api.util.DateTimeUtil
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.pipeline.enums.ChannelCode
import com.tencent.devops.project.api.service.ServiceProjectResource
import com.tencent.devops.store.dao.ExtServiceDao
import com.tencent.devops.store.dao.ExtServiceFeatureDao
import com.tencent.devops.store.dao.common.StoreProjectRelDao
import com.tencent.devops.store.pojo.common.InstalledProjRespItem
import com.tencent.devops.store.pojo.common.enums.StoreTypeEnum
import com.tencent.devops.store.service.common.StoreProjectService
import org.jooq.DSLContext
import org.springframework.stereotype.Service
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.util.StopWatch
import java.lang.RuntimeException

@Service
class ExtServiceProjectService @Autowired constructor(
    val extServiceDao: ExtServiceDao,
    val extServiceFeatureDao: ExtServiceFeatureDao,
    private val storeProjectRelDao: StoreProjectRelDao,
    val dslContext: DSLContext,
    val client: Client,
    private val storeProjectService: StoreProjectService
) {

    /**
     * 安装扩展服务到项目
     */
    fun installService(
        userId: String,
        projectCodeList: ArrayList<String>,
        serviceCode: String,
        channelCode: ChannelCode
    ): Result<Boolean> {
        logger.info("installService:Input:($userId,$projectCodeList,$serviceCode)")
        // 判断扩展服务标识是否合法
        val serviceRecord = extServiceDao.getServiceLatestByCode(dslContext, serviceCode)
            ?: throw RuntimeException("serviceCode=$serviceCode")
        val serviceFeature = extServiceFeatureDao.getServiceByCode(dslContext, serviceCode)
        val validateInstallResult = storeProjectService.validateInstallPermission(
            publicFlag = serviceFeature?.publicFlag ?: false,
            userId = userId,
            storeCode = serviceRecord.serviceCode,
            storeType = StoreTypeEnum.SERVICE,
            projectCodeList = projectCodeList
        )
        if (validateInstallResult.isNotOk()) {
            return validateInstallResult
        }
        logger.info("installService:Inner:service.id=${serviceRecord.id},serviceFeature.publicFlag=${serviceFeature?.publicFlag}")
        return storeProjectService.installStoreComponent(
            userId = userId,
            projectCodeList = projectCodeList,
            storeId = serviceRecord.id,
            storeCode = serviceRecord.serviceCode,
            storeType = StoreTypeEnum.SERVICE,
            publicFlag = serviceFeature?.publicFlag ?: false,
            channelCode = channelCode
        )
    }

    fun getInstalledProjects(
        accessToken: String,
        userId: String,
        storeCode: String,
        storeType: StoreTypeEnum
    ): Result<List<InstalledProjRespItem>> {
        logger.info("getInstalledProjects accessToken is :$accessToken, userId is :$userId, storeCode is :$storeCode, storeType is :$storeType")
        val watch = StopWatch()
        // 获取用户有权限的项目列表
        watch.start("get accessible projects")
        val projectList = client.get(ServiceProjectResource::class).list(userId).data
        watch.stop()
        logger.info("$userId accessible projectList is :size=${projectList?.size},$projectList")
        if (projectList?.count() == 0) {
            return Result(mutableListOf())
        }
        watch.start("projectCodeMap")
        val projectCodeMap = projectList?.map { it.projectCode to it }?.toMap()!!
        watch.stop()
        watch.start("getInstalledProject")
        val records =
            storeProjectRelDao.getInstalledProject(dslContext, storeCode, storeType.type.toByte(), projectCodeMap.keys)
        watch.stop()
        watch.start("generate InstalledProjRespItem")
        val result = mutableListOf<InstalledProjRespItem>()
        records?.forEach {
            result.add(
                InstalledProjRespItem(
                    projectCode = it.projectCode,
                    projectName = projectCodeMap[it.projectCode]?.projectName,
                    creator = it.creator,
                    createTime = DateTimeUtil.toDateTime(it.createTime)
                )
            )
        }
        watch.stop()
        logger.info("getInstalledProjects:watch:$watch")
        return Result(result)
    }

    companion object {
        val logger = LoggerFactory.getLogger(this::class.java)
    }
}