package com.tencent.devops.remotedev.service.tcloud

import com.tencent.devops.common.api.util.PageUtil
import com.tencent.devops.common.redis.RedisLock
import com.tencent.devops.common.redis.RedisOperation
import com.tencent.devops.remotedev.config.async.AsyncExecute
import com.tencent.devops.remotedev.dao.ProjectTCloudCfsDao
import com.tencent.devops.remotedev.dao.WorkspaceJoinDao
import com.tencent.devops.remotedev.pojo.WorkspaceStatus
import com.tencent.devops.remotedev.pojo.async.AsyncTCloudCfs
import com.tencent.devops.remotedev.pojo.tcloud.ProjectCfsData
import com.tencent.devops.remotedev.pojo.tcloud.UpdateCfsData
import com.tencentcloudapi.cfs.v20190719.CfsClient
import com.tencentcloudapi.cfs.v20190719.models.CreateCfsRuleRequest
import com.tencentcloudapi.cfs.v20190719.models.DeleteCfsRuleRequest
import com.tencentcloudapi.cfs.v20190719.models.DescribeCfsFileSystemsRequest
import com.tencentcloudapi.cfs.v20190719.models.DescribeCfsRulesRequest
import com.tencentcloudapi.common.Credential
import com.tencentcloudapi.common.profile.ClientProfile
import com.tencentcloudapi.common.profile.HttpProfile
import org.jooq.DSLContext
import org.slf4j.LoggerFactory
import org.springframework.amqp.rabbit.core.RabbitTemplate
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service

@Suppress("ALL")
@Service
class TCloudCfsService @Autowired constructor(
    private val dslContext: DSLContext,
    private val projectTCloudCfsDao: ProjectTCloudCfsDao,
    private val workspaceJoinDao: WorkspaceJoinDao,
    private val redisOperation: RedisOperation,
    private val rabbitTemplate: RabbitTemplate
) {
    @Value("\${tcloud.apiSecretId:}")
    val secretId = ""

    @Value("\${tcloud.apiSecretKey:}")
    val secretKey = ""

    fun addOrRemoveCfsPermissionRule(
        projectId: String,
        ip: String,
        remove: Boolean
    ) {
        val record = projectTCloudCfsDao.fetchAny(dslContext, projectId)
        if (record == null) {
            logger.debug("$LOG_UPDATE_TCLOUD_CFS_RULES $projectId no cfs")
            return
        }

        val client = buildCfsClient(record.region)

        var pgId = record.pgId
        if (pgId == null) {
            pgId = getPGId(client, record.cfsId) ?: return
            projectTCloudCfsDao.updatePGId(dslContext, projectId, record.cfsId, pgId)
        }

        if (remove) {
            val gpResp = try {
                client.DescribeCfsRules(DescribeCfsRulesRequest().apply { this.pGroupId = pgId })
            } catch (e: Exception) {
                logger.error("$LOG_UPDATE_TCLOUD_CFS_RULES|DescribeCfsRules error", e)
                return
            }
            logger.info("$LOG_UPDATE_TCLOUD_CFS_RULES|will delete|${gpResp.ruleList}|$ip")
            gpResp.ruleList.forEach {
                if (it.authClientIp == ip) {
                    createOrDeleteCfsRule(
                        pgId = pgId,
                        ip = "",
                        ruleId = it.ruleId,
                        region = record.region,
                        delete = true
                    )
                }
            }
        } else {
            createOrDeleteCfsRule(
                pgId = pgId,
                ip = ip,
                ruleId = "",
                region = record.region,
                delete = false
            )
        }
    }

    private fun getPGId(
        client: CfsClient,
        cfsId: String
    ): String? {
        val resp = try {
            client.DescribeCfsFileSystems(
                DescribeCfsFileSystemsRequest().apply {
                    this.fileSystemId = cfsId
                }
            )
        } catch (e: Exception) {
            logger.error("$LOG_UPDATE_TCLOUD_CFS_RULES|DescribeCfsFileSystems error", e)
            return null
        }
        if (resp.totalCount < 1 || resp.fileSystems.firstOrNull() == null) {
            logger.error("$LOG_UPDATE_TCLOUD_CFS_RULES|DescribeCfsFileSystems 0|${resp.requestId}")
            return null
        }
        return resp.fileSystems.first().pGroup.pGroupId
    }

    fun addProjectCfsId(
        projectId: String,
        cfsId: String,
        region: String
    ) {
        val client = buildCfsClient(region)

        val pgId = getPGId(client, cfsId) ?: throw RuntimeException("获取权限组ID失败")
        projectTCloudCfsDao.add(dslContext, projectId, cfsId, region, pgId)

        // 将所有这个项目下的ip都添加到权限组
        val ips = workspaceJoinDao.fetchWindowsWorkspacesSimple(
            dslContext = dslContext,
            projectId = projectId,
            notStatus = listOf(WorkspaceStatus.DELETED, WorkspaceStatus.UNUSED)
        ).filter { !it.hostIp.isNullOrBlank() }.map {
            it.hostIp?.split(".")?.let { host ->
                host.subList(1, host.size).joinToString(separator = ".")
            }!!
        }.toSet()

        // 获取下现有的rule过滤
        val rules = try {
            client.DescribeCfsRules(
                DescribeCfsRulesRequest().apply {
                    this.pGroupId = pgId
                }
            ).ruleList.map { it.authClientIp }.toSet()
        } catch (e: Exception) {
            logger.error("$LOG_UPDATE_TCLOUD_CFS_RULES|addProjectCfsId|DescribeCfsRules error", e)
            emptySet()
        }

        val realIps = ips.subtract(rules)

        logger.info("$LOG_UPDATE_TCLOUD_CFS_RULES|will create|$projectId|$cfsId|$pgId|$ips")

        realIps.forEach { ip ->
            createOrDeleteCfsRule(pgId = pgId, ip = ip, ruleId = "", region = region, delete = false)
        }
    }

    private fun createOrDeleteCfsRule(
        pgId: String,
        ip: String,
        ruleId: String,
        region: String,
        delete: Boolean
    ) {
        logger.info("$LOG_UPDATE_TCLOUD_CFS_RULES|createOrDeleteCfsRule|$pgId|$ip|$ruleId|$region|$delete")
        AsyncExecute.dispatch(
            rabbitTemplate = rabbitTemplate,
            data = AsyncTCloudCfs(
                pgId = pgId,
                ip = ip,
                ruleId = ruleId,
                region = region,
                delete = delete
            ),
            errorLogTag = LOG_UPDATE_TCLOUD_CFS_RULES
        )
    }

    // 腾讯云对创建cfs权限组规则有频率限制，我们使用75s发送一次
    fun doCreateOrDeleteCfsRule(
        pgId: String,
        ip: String,
        ruleId: String,
        region: String,
        delete: Boolean
    ) {
        val key = "$TCLOUD_PGID_REDIS_KEY_PREFIX:$pgId"
        val redisLock = RedisLock(redisOperation, key, 76)
        redisLock.lock()

        val client = buildCfsClient(region)
        if (delete) {
            try {
                client.DeleteCfsRule(
                    DeleteCfsRuleRequest().apply {
                        this.pGroupId = pgId
                        this.ruleId = ruleId
                    }
                )
            } catch (e: Exception) {
                logger.error("$LOG_UPDATE_TCLOUD_CFS_RULES|doCreateOrDeleteCfsRule|DeleteCfsRule error", e)
                return
            }
        } else {
            try {
                client.CreateCfsRule(
                    CreateCfsRuleRequest().apply {
                        this.pGroupId = pgId
                        this.authClientIp = ip
                        this.priority = 1
                        this.rwPermission = "rw"
                        this.userPermission = "no_root_squash"
                    }
                )
            } catch (e: Exception) {
                logger.error("$LOG_UPDATE_TCLOUD_CFS_RULES|doCreateOrDeleteCfsRule|CreateCfsRule error", e)
                return
            }
        }
    }

    fun projectCfsList(
        page: Int,
        pageSize: Int
    ): List<ProjectCfsData> {
        val sqlLimit = PageUtil.convertPageSizeToSQLLimit(page, pageSize)
        return projectTCloudCfsDao.fetch(dslContext, sqlLimit).map {
            ProjectCfsData(
                projectId = it.projectId,
                cfsId = it.cfsId,
                region = it.region
            )
        }
    }

    fun deleteProjectCfs(
        projectId: String,
        cfsId: String
    ) {
        projectTCloudCfsDao.delete(dslContext, projectId, cfsId)
    }

    fun updateCfsRules4Op(
        data: UpdateCfsData
    ) {
        val record = projectTCloudCfsDao.fetchAny(dslContext, data.projectId)
        if (record == null) {
            logger.debug("$LOG_UPDATE_TCLOUD_CFS_RULES ${data.projectId} no cfs")
            return
        }

        val client = buildCfsClient(record.region)

        var pgId = record.pgId
        if (pgId == null) {
            pgId = getPGId(client, record.cfsId) ?: return
            projectTCloudCfsDao.updatePGId(dslContext, data.projectId, record.cfsId, pgId)
        }

        if (data.remove) {
            val gpResp = try {
                client.DescribeCfsRules(DescribeCfsRulesRequest().apply { this.pGroupId = pgId })
            } catch (e: Exception) {
                logger.error("$LOG_UPDATE_TCLOUD_CFS_RULES|DescribeCfsRules error", e)
                return
            }.ruleList.map { it.authClientIp to it.ruleId }
            logger.info("$LOG_UPDATE_TCLOUD_CFS_RULES|updateCfsRules4Op|will delete|${data.ips}")
            gpResp.forEach { (ip, ruleId) ->
                if (data.ips.contains(ip)) {
                    createOrDeleteCfsRule(
                        pgId = pgId,
                        ip = "",
                        ruleId = ruleId,
                        region = record.region,
                        delete = true
                    )
                }
            }
        } else {
            data.ips.forEach { ip ->
                createOrDeleteCfsRule(
                    pgId = pgId,
                    ip = ip,
                    ruleId = "",
                    region = record.region,
                    delete = false
                )
            }
        }
    }

    private fun buildCfsClient(region: String): CfsClient {
        val cred = Credential(secretId, secretKey)
        val profile = HttpProfile().apply {
            this.endpoint = TCLOUD_DOMAIN
        }
        return CfsClient(
            cred,
            region,
            ClientProfile().apply {
                this.httpProfile = profile
            }
        )
    }

    companion object {
        private val logger = LoggerFactory.getLogger(TCloudCfsService::class.java)
        private const val TCLOUD_DOMAIN = "cfs.internal.tencentcloudapi.com"
        private const val TCLOUD_PGID_REDIS_KEY_PREFIX = "remotedev:tcloud:pgid"

        // 日志标志常量，方便配置告警或者搜索日志
        private const val LOG_UPDATE_TCLOUD_CFS_RULES = "update_tcloud_project_cfs_rules"
    }
}
