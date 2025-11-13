package com.tencent.devops.remotedev.service

import com.tencent.devops.common.api.constant.SYSTEM
import com.tencent.devops.common.api.exception.ErrorCodeException
import com.tencent.devops.remotedev.common.exception.ErrorCodeEnum
import com.tencent.devops.remotedev.dao.WhiteListDao
import com.tencent.devops.remotedev.dao.WorkspaceDao
import com.tencent.devops.remotedev.pojo.CdsMeshStatus
import com.tencent.devops.remotedev.pojo.IWhiteList
import com.tencent.devops.remotedev.pojo.WhiteList
import com.tencent.devops.remotedev.pojo.WhiteListType
import com.tencent.devops.remotedev.pojo.WorkspaceStatus
import com.tencent.devops.remotedev.pojo.WorkspaceSystemType
import com.tencent.devops.remotedev.service.redis.ConfigCacheService
import org.jooq.DSLContext
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
class WhiteListService @Autowired constructor(
    private val dslContext: DSLContext,
    private val cacheService: ConfigCacheService,
    private val whiteListDao: WhiteListDao,
    private val workspaceDao: WorkspaceDao
) {

    companion object {
        private val logger = LoggerFactory.getLogger(WorkspaceService::class.java)
        private const val CONFIG_CDS_DOMAIN_DEFAULT_KEY = "remotedev:cdsDomainDefault"
        private const val CONFIG_CDS_DOMAIN_PROJECT_KEY_PREFIX = "remotedev:cdsDomainProject:"
        const val CONFIG_CDS_DOMAIN_WORKSPACE_KEY_PREFIX = "remotedev:cdsDomainWorkspace:"
        private const val taiUser = "@tai"
    }

    fun opFetch(userId: String, whiteListType: WhiteListType): List<WhiteList> {
        logger.info("userId($userId) wants to fetch whiteListType($whiteListType)")
        return whiteListDao.fetch(dslContext, whiteListType)
    }

    fun opCreateOrUpdateWhiteList(userId: String, whiteList: WhiteList): Boolean {
        logger.info("userId($userId) wants to add whiteList($whiteList)")
        return whiteListDao.addOrUpdate(dslContext, whiteList) == 1
    }

    fun opDeleteWhiteList(userId: String, whiteList: WhiteList): Boolean {
        logger.info("userId($userId) wants to delete whiteList($whiteList)")
        return whiteListDao.delete(dslContext, whiteList.name, whiteList.type) == 1
    }

    fun apiSetWhiteList(
        userId: String,
        type: WhiteListType,
        delete: Boolean,
        body: Map<String, String>
    ): Boolean {
        logger.info("userId($userId) wants to set whiteList($type, $delete, $body)")
        val whiteList = when (type) {
            WhiteListType.PROJECT_ACCESS_DEVICE -> {
                requireNotNull(body["projectId"]) { "projectId is required in body" }
                requireNotNull(body["userId"]) { "userId is required in body" }
                WhiteList(name = "${body["projectId"]}::${body["userId"]}", type, userId)
            }

            else -> null
        } ?: return false
        return if (delete) {
            opDeleteWhiteList(userId, whiteList)
        } else {
            opCreateOrUpdateWhiteList(userId, whiteList)
        }
    }

    fun apiGetWhiteList(
        userId: String,
        type: WhiteListType?,
        body: Map<String, String>
    ): List<IWhiteList> {
        logger.info("userId($userId) wants to get whiteList($type, $body)")
        val types = type?.let { listOf(type) } ?: listOf(WhiteListType.PROJECT_ACCESS_DEVICE)
        return types.flatMap { t ->
            when (t) {
                WhiteListType.PROJECT_ACCESS_DEVICE -> {
                    requireNotNull(body["projectId"]) { "projectId is required in body" }
                    whiteListDao.fetchProjectAccessDevice(dslContext, body["projectId"]!!)
                }

                else -> emptyList()
            }
        }
    }

    fun shareWorkspace(userId: String, whiteListUser: String) {
        addWhiteListUser(operator = userId, whiteListUser = whiteListUser)
        addGPUWhiteListUser(
            operator = userId,
            whiteListUser = whiteListUser,
            limit = if (whiteListUser.contains(taiUser)) 0 else 1
        )
    }

    // 添加客户端白名单用户。目前对接redis配置，后续需要对接权限系统。
    fun addWhiteListUser(operator: String, whiteListUser: String): Boolean {
        logger.info("userId($operator) wants to add whiteListUser($whiteListUser)")
        // whiteListUser支持多个用;分隔，需要解析。
        if (whiteListUser.isEmpty()) return false
        val whiteListUserArray = whiteListUser.split(";")
        for (user in whiteListUserArray) {
            if (whiteListDao.add(dslContext, WhiteList(user, WhiteListType.API, operator)) == 1) {
                logger.info("whiteListUser($user) in the whiteList has add.")
            } else {
                logger.info("whiteListUser($user) in the whiteList already exists.")
            }
        }
        return true
    }

    fun checkInWhiteList(user: String): Boolean {
        return cacheService.checkApiWhiteList(user)
    }

    fun removeWhiteListUser(operator: String, whiteListUser: String): Boolean {
        logger.info("userId($operator) wants to remove whiteListUser($whiteListUser)")
        // whiteListUser支持多个用;分隔，需要解析。
        if (whiteListUser.isEmpty()) return false
        val whiteListUserArray = whiteListUser.split(";")
        for (user in whiteListUserArray) {
            if (whiteListDao.delete(dslContext, user, WhiteListType.API) == 1) {
                logger.info("whiteListUser($user) in the whiteList has removed.")
            } else {
                logger.info("whiteListUser($user) in the whiteList already removed.")
            }
        }
        return true
    }

    fun updateAndGetWindowsLimit(userId: String, limit: Int): Int {
        val get = whiteListDao.get(dslContext, userId, WhiteListType.WINDOWS_GPU)?.windowsGpuLimit ?: 0
        if (limit != 0) {
            whiteListDao.addOrUpdate(
                dslContext,
                WhiteList(
                    name = userId,
                    type = WhiteListType.WINDOWS_GPU,
                    creator = SYSTEM,
                    windowsGpuLimit = limit + get
                )
            )
        }
        return limit + get
    }

    fun checkInCdsMeshWhiteList(
        projectId: String,
        workspaceName: String
    ): Int {

        if (whiteListDao.get(dslContext, workspaceName, WhiteListType.NOT_CDS_MESH_WORKSPACE) != null) {
            return CdsMeshStatus.DISABLED.value
        }
        if (whiteListDao.get(dslContext, projectId, WhiteListType.CDS_MESH_PROJECT) != null) {
            return CdsMeshStatus.MESH.value
        }
        if (whiteListDao.get(dslContext, workspaceName, WhiteListType.CDS_MESH_WORKSPACE) != null) {
            return CdsMeshStatus.MESH.value
        }
        // 检查是否启用 SSL 模式
        if (whiteListDao.get(dslContext, workspaceName, WhiteListType.CDS_SSL_WORKSPACE) != null) {
            return CdsMeshStatus.SSL.value
        }
        return CdsMeshStatus.DISABLED.value
    }

    fun getCdsDomain(projectId: String, workspaceName: String): String? {
        return cacheService.get(CONFIG_CDS_DOMAIN_WORKSPACE_KEY_PREFIX + workspaceName)
            ?: cacheService.get(CONFIG_CDS_DOMAIN_PROJECT_KEY_PREFIX + projectId)
            ?: cacheService.get(CONFIG_CDS_DOMAIN_DEFAULT_KEY)
    }

    fun addGPUWhiteListUser(
        operator: String,
        whiteListUser: String,
        limit: Int = 1,
        override: Boolean = false
    ): Boolean {
        logger.info("userId($operator) wants to add GPU whiteListUser($whiteListUser)")
        // whiteListUser支持多个用;分隔，需要解析。
        whiteListUser.apply {
            val whiteListUserArray = this.split(";")
            for (user in whiteListUserArray) {
                if (override && whiteListDao.addOrUpdate(
                        dslContext,
                        WhiteList(
                            name = user,
                            type = WhiteListType.WINDOWS_GPU,
                            creator = operator,
                            windowsGpuLimit = limit
                        )
                    ) == 1
                ) {
                    logger.info("whiteListUser($user) in the gpu whiteList has add.(override)")
                }
                if (!override && whiteListDao.add(
                        dslContext,
                        WhiteList(
                            name = user,
                            type = WhiteListType.WINDOWS_GPU,
                            creator = operator,
                            windowsGpuLimit = limit
                        )
                    ) == 1
                ) {
                    logger.info("whiteListUser($user) in the gpu whiteList has add.(not override)")
                }
            }
        }

        return true
    }

    fun removeGPUWhiteListUser(userId: String, whiteListUser: String): Boolean {
        logger.info("userId($userId) wants to remove GPU whiteListUser($whiteListUser)")
        // whiteListUser支持多个用;分隔，需要解析。
        whiteListUser.apply {
            val whiteListUserArray = this.split(";")
            for (user in whiteListUserArray) {
                if (whiteListDao.delete(dslContext, user, WhiteListType.WINDOWS_GPU) == 1) {
                    logger.info("whiteListUser($user) in the gpu whiteList has removed.")
                } else {
                    logger.info("whiteListUser($user) in the gpu whiteList already removed.")
                }
            }
        }

        return true
    }

    fun checkInGPUWhiteList(user: String): Boolean {
        return cacheService.checkWindowsGpuLimit(user) > 0
    }

    fun windowsGpuCheck(userId: String, count: Int) {
        windowsNumberLimit(
            userId = userId,
            value = workspaceDao.countUserWorkspace(
                dslContext = dslContext,
                userId = userId,
                status = WorkspaceStatus.Types.USING.status(),
                systemType = WorkspaceSystemType.WINDOWS_GPU
            ) + count
        )
    }

    /* 有关数量的限制:
        如果value大于指定key中id规定的数量，则抛出异常
        如果没有白名单，则抛出异常
        如果白名单中没有对应id，则抛出异常
     */
    fun windowsNumberLimit(userId: String, value: Long) {
        val limit = cacheService.checkWindowsGpuLimit(userId)
        logger.info("numberLimit|$value|$limit")
        if (limit != null && value <= limit) {
            // 没有达到限制，直接return
            return
        }
        throw ErrorCodeException(
            errorCode = ErrorCodeEnum.FORBIDDEN.errorCode,
            params = arrayOf("We're sorry but User($userId) exceeding the limit($limit)")
        )
    }
}
