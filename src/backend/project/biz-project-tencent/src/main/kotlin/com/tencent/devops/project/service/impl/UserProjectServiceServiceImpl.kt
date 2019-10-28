package com.tencent.devops.project.service.impl

import com.tencent.devops.common.api.util.DateTimeUtil
import com.tencent.devops.common.redis.RedisOperation
import com.tencent.devops.common.service.gray.Gray
import com.tencent.devops.model.project.tables.records.TServiceRecord
import com.tencent.devops.project.dao.FavoriteDao
import com.tencent.devops.project.dao.GrayTestDao
import com.tencent.devops.project.dao.ServiceDao
import com.tencent.devops.project.dao.ServiceTypeDao
import com.tencent.devops.project.pojo.Result
import com.tencent.devops.project.pojo.ServiceUpdateUrls
import com.tencent.devops.project.pojo.service.*
import com.tencent.devops.project.service.UserProjectServiceService
import org.jooq.DSLContext
import org.jooq.impl.DSL
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
class UserProjectServiceServiceImpl @Autowired constructor(
        private val dslContext: DSLContext,
        private val serviceTypeDao: ServiceTypeDao,
        private val serviceDao: ServiceDao,
        private val grayTestDao: GrayTestDao,
        private val favoriteDao: FavoriteDao,
        private val gray: Gray,
        private val redisOperation: RedisOperation
) : UserProjectServiceService {

    override fun updateServiceUrls(
            userId: String,
            name: String,
            serviceUpdateUrls: ServiceUpdateUrls
    ): Result<Boolean> {
        return Result(serviceDao.updateUrls(dslContext, userId, name, serviceUpdateUrls))
    }

    override fun getService(userId: String, serviceId: Long): Result<ServiceVO> {
        val tServiceRecord = serviceDao.select(dslContext, serviceId)
        if (tServiceRecord != null) {
            return Result(
                    ServiceVO(
                            tServiceRecord.id ?: 0,
                            tServiceRecord.name,
                            tServiceRecord.link,
                            tServiceRecord.linkNew,
                            tServiceRecord.status, tServiceRecord.injectType,
                            tServiceRecord.iframeUrl,
                            tServiceRecord.cssUrl,
                            tServiceRecord.jsUrl,
                            tServiceRecord.grayCssUrl,
                            tServiceRecord.grayJsUrl,
                            tServiceRecord.showProjectList,
                            tServiceRecord.showNav,
                            tServiceRecord.projectIdType,
                            favoriteDao.countFavorite(dslContext, userId, tServiceRecord.id) > 0,
                            //TODO: 内部版数据库没有weight字段，此处可能会抛异常。
                            tServiceRecord.weight,
                            tServiceRecord.logoUrl,
                            tServiceRecord.webSocket
                    )
            )
        } else {
            return Result(405, "无限ID,获取服务信息失败")
        }
    }

    /**
     * 修改服务
     */
    override fun updateService(userId: String, serviceId: Long, serviceCreateInfo: ServiceCreateInfo): Result<Boolean> {
        return Result(serviceDao.update(dslContext, userId, serviceId, serviceCreateInfo))
    }

    /**
     * 删除服务
     */
    override fun deleteService(userId: String, serviceId: Long): Result<Boolean> {
        return Result(serviceDao.delete(dslContext, userId, serviceId))
    }

    /**
     * 服务列表
     */
    override fun listOPService(userId: String): Result<List<OPPServiceVO>> {
        val tServiceList = serviceDao.getServiceList(dslContext)
        val oPPServiceVOList = ArrayList<OPPServiceVO>()
        tServiceList.map { tServiceRecord ->
            oPPServiceVOList.add(
                    generateOppServiceVO(tServiceRecord)
            )
        }

        return Result(oPPServiceVOList)
    }

    private fun generateOppServiceVO(tServiceRecord: TServiceRecord): OPPServiceVO {
        return OPPServiceVO(
                tServiceRecord.id,
                tServiceRecord.name ?: "",
                tServiceRecord.serviceTypeId,
                tServiceRecord.showProjectList,
                tServiceRecord.showNav,
                tServiceRecord.status,
                tServiceRecord.link,
                tServiceRecord.linkNew,
                tServiceRecord.injectType,
                tServiceRecord.iframeUrl,
                tServiceRecord.cssUrl,
                tServiceRecord.jsUrl,
                tServiceRecord.grayCssUrl,
                tServiceRecord.grayJsUrl,
                tServiceRecord.projectIdType,
                tServiceRecord.logoUrl,
                tServiceRecord.webSocket,
                tServiceRecord.createdUser ?: "",
                DateTimeUtil.toDateTime(tServiceRecord.createdTime),
                tServiceRecord.updatedUser ?: "",
                DateTimeUtil.toDateTime(tServiceRecord.updatedTime)
        )
    }

    /**
     * 添加服务
     */
    override fun createService(userId: String, serviceCreateInfo: ServiceCreateInfo): Result<OPPServiceVO> {
        val tServiceRecord = serviceDao.create(dslContext, userId, serviceCreateInfo)
        if (tServiceRecord != null) {
            return Result(generateOppServiceVO(tServiceRecord))
        }
        return Result(500, "服务添加失败")
    }

    /**
     * 修改服务关注
     */
    override fun updateCollected(userId: String, service_id: Long, collector: Boolean): Result<Boolean> {
        if (collector) {
            if (favoriteDao.create(dslContext, userId, service_id) > 0) {
                return Result(0, "服务收藏成功", "", true)
            }
        } else {
            if (favoriteDao.delete(dslContext, userId, service_id) > 0) {
                return Result(0, "服务取消收藏成功", "", true)
            }
        }
        return Result(false)
    }

    override fun listService(userId: String, projectId: String?): Result<ArrayList<ServiceListVO>> {

        val startEpoch = System.currentTimeMillis()
        try {
            val serviceListVO = ArrayList<ServiceListVO>()

            val serviceTypeMap = serviceTypeDao.getAllIdAndTitle(dslContext).map {
                it.value1() to it.value2()
            }.toMap()

            val groupService = serviceDao.getServiceList(dslContext).groupBy { it.serviceTypeId }

            val grayTest = grayTestDao.listByUser(dslContext, userId).map { it.server_id to it.status }.toMap()

            val favorServices = favoriteDao.list(dslContext, userId).map { it.serviceId }.toList()

            serviceTypeMap.forEach { typeId, typeName ->
                val services = ArrayList<ServiceVO>()

                val s = groupService[typeId]

                s?.forEach {
                    val status = grayTest[it.id] ?: it.status
                    val favor = favorServices.contains(it.id)
                    services.add(
                            ServiceVO(
                                    it.id,
                                    it.name ?: "",
                                    it.link ?: "",
                                    it.linkNew ?: "",
                                    status,
                                    it.injectType ?: "",
                                    it.iframeUrl ?: "",
                                    getCSSUrl(it, projectId),
                                    getJSUrl(it, projectId),
                                    it.grayCssUrl ?: "",
                                    it.grayJsUrl ?: "",
                                    it.showProjectList ?: false,
                                    it.showNav ?: false,
                                    it.projectIdType ?: "",
                                    favor,
                                    it.weight ?: 0,
                                    it.logoUrl,
                                    it.webSocket
                            )
                    )
                }

                //TODO: 因内部版没有weigHt，weigHt默认给0
                serviceListVO.add(ServiceListVO(typeName, 0, services))
            }

            return Result(0, "OK", serviceListVO)
        } finally {
            logger.info("It took ${System.currentTimeMillis() - startEpoch}ms to list services")
        }
    }

    // 获取CSS URL，包括灰度的
    private fun getCSSUrl(record: TServiceRecord, projectId: String?): String {
        return if (gray.isGray() && !projectId.isNullOrBlank()) {
            if (redisOperation.isMember(gray.getGrayRedisKey(), projectId!!)) {
                record.grayCssUrl ?: record.cssUrl
            } else {
                record.cssUrl
            }
        } else {
            record.cssUrl
        } ?: ""
    }

    // 获取 JS URL， 包括灰度的
    private fun getJSUrl(record: TServiceRecord, projectId: String?): String {
        return if (gray.isGray() && !projectId.isNullOrBlank()) {
            if (redisOperation.isMember(gray.getGrayRedisKey(), projectId!!)) {
                record.grayJsUrl ?: record.jsUrl
            } else {
                record.jsUrl
            }
        } else {
            record.jsUrl
        } ?: ""
    }

    override fun syncService(userId: String, services: List<ServiceListVO>) {
        dslContext.transaction { configuration ->
            val context = DSL.using(configuration)
            services.forEach {
                val type = serviceTypeDao.create(context, userId, it.title, 0)
                it.children.forEach { s ->
                    serviceDao.create(context, userId, type.id, s)
                }
            }
        }
    }

    override fun updateServiceUrlByBatch(userId: String, serviceUrlUpdateInfoList: List<ServiceUrlUpdateInfo>?): Result<Boolean> {
        if(serviceUrlUpdateInfoList == null) {
            return Result(data = true)
        }
        serviceUrlUpdateInfoList.forEach {
            serviceDao.updateUrlByName(dslContext, it)

        }
        return Result(data = true)
    }

    companion object {
        private val logger = LoggerFactory.getLogger(UserProjectServiceServiceImpl::class.java)
    }
}