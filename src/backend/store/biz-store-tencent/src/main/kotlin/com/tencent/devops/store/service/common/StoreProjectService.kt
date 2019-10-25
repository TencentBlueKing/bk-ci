package com.tencent.devops.store.service.common

import com.tencent.devops.common.api.constant.CommonMessageCode
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.service.utils.MessageCodeUtil
import com.tencent.devops.project.api.ServiceProjectResource
import com.tencent.devops.quality.api.v2.ServiceQualityIndicatorResource
import com.tencent.devops.store.constant.StoreMessageCode
import com.tencent.devops.store.dao.common.StoreProjectRelDao
import com.tencent.devops.store.dao.common.StoreStatisticDao
import com.tencent.devops.store.pojo.common.InstalledProjRespItem
import com.tencent.devops.store.pojo.common.enums.StoreTypeEnum
import org.jooq.DSLContext
import org.jooq.impl.DSL
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

/**
 * store项目通用业务逻辑类
 * author: carlyin
 * since: 2019-03-22
 */
@Service
class StoreProjectService @Autowired constructor(
    private val dslContext: DSLContext,
    private val client: Client,
    private val storeProjectRelDao: StoreProjectRelDao,
    private val storeStatisticDao: StoreStatisticDao,
    private val storeUserService: StoreUserService
) {
    private val logger = LoggerFactory.getLogger(StoreProjectService::class.java)

    /**
     * 根据商城组件标识获取已安装的项目列表
     */
    fun getInstalledProjects(accessToken: String, storeCode: String, storeType: StoreTypeEnum): Result<List<InstalledProjRespItem>> {
        logger.info("accessToken is :$accessToken, storeCode is :$storeCode, storeType is :$storeType")
        // 获取用户有权限的项目列表
        val projectList = client.get(ServiceProjectResource::class).list(accessToken).data
        logger.info("projectList is :$projectList")
        if (projectList?.count() == 0) {
            return Result(mutableListOf())
        }
        val projectCodeMap = projectList?.map { it.project_code to it }?.toMap()!!
        val records = storeProjectRelDao.getInstalledProject(dslContext, storeCode, storeType.type.toByte(), projectCodeMap.keys)
        val result = mutableListOf<InstalledProjRespItem>()
        records?.forEach {
            result.add(
                    InstalledProjRespItem(
                            projectCode = it.projectCode,
                            projectName = projectCodeMap[it.projectCode]?.project_name
                    )
            )
        }
        return Result(result)
    }

    fun installStoreComponent(
        accessToken: String,
        userId: String,
        projectCodeList: ArrayList<String>,
        storeId: String,
        storeCode: String,
        storeType: StoreTypeEnum,
        publicFlag: Boolean
    ): Result<Boolean> {
        logger.info("installStoreComponent accessToken is :$accessToken, userId is :$userId, projectCodeList is :$projectCodeList")
        logger.info("installStoreComponent storeId is :$storeId, storeCode is :$storeCode, storeType is :$storeType")
        var increment = 0
        dslContext.transaction { t ->
            val context = DSL.using(t)
            for (projectCode in projectCodeList) {
                // 判断是否已安装
                val relCount = storeProjectRelDao.countInstalledProject(context, projectCode, storeCode, storeType.type.toByte())
                logger.info("relCount is :$relCount")
                if (relCount > 0) {
                    continue
                }
                // 未安装则入库
                storeProjectRelDao.addStoreProjectRel(context, userId, storeCode, projectCode, 1, storeType.type.toByte())
                increment += 1
            }
            logger.info("increment: $increment")
            // 更新安装量
            if (increment > 0) {
                storeStatisticDao.updateDownloads(context, userId, storeId, storeCode, storeType.type.toByte(), increment)
            }
        }
        // 更新质量红线指标可见范围
        client.get(ServiceQualityIndicatorResource::class).appendRangeByElement(storeCode, projectCodeList)
        return Result(true)
    }

    fun validateInstallPermission(
        publicFlag: Boolean,
        userId: String,
        storeCode: String,
        storeType: StoreTypeEnum,
        accessToken: String,
        projectCodeList: ArrayList<String>
    ): Result<Boolean> {
        val installFlag = storeUserService.isCanInstallStoreComponent(publicFlag, userId, storeCode, storeType) // 是否能安装
        // 判断用户是否有权限安装
        if (!installFlag) {
            return MessageCodeUtil.generateResponseDataObject(CommonMessageCode.PERMISSION_DENIED, false)
        }
        // 获取用户有权限的项目列表
        val projectList = client.get(ServiceProjectResource::class).list(accessToken).data
        logger.info("validateInstallPermission projectList is :$projectList")
        // 判断用户是否有权限安装到对应的项目
        val privilegeProjectCodeList = mutableListOf<String>()
        projectList?.map {
            privilegeProjectCodeList.add(it.project_code)
        }
        val dataList = mutableListOf<String>()
        dataList.addAll(projectCodeList)
        dataList.removeAll(privilegeProjectCodeList)
        if (dataList.isNotEmpty()) {
            // 存在用户没有安装权限的项目，抛出错误提示
            return MessageCodeUtil.generateResponseDataObject(StoreMessageCode.USER_PROJECT_IS_NOT_ALLOW_INSTALL, arrayOf(dataList.toString()), false)
        }
        return Result(true)
    }

    fun uninstall(storeType: StoreTypeEnum, storeCode: String, projectCode: String): Result<Boolean> {
        storeProjectRelDao.deleteRel(dslContext, storeCode, storeType.type.toByte(), projectCode)
        return Result(true)
    }
}
