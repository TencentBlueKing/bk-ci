package com.tencent.devops.remotedev.service.tcloud

import com.tencent.devops.common.api.util.PageUtil
import com.tencent.devops.remotedev.dao.ProjectTCloudCfsDao
import com.tencent.devops.remotedev.dao.WorkspaceJoinDao
import com.tencent.devops.remotedev.pojo.WorkspaceSearch
import com.tencent.devops.remotedev.pojo.WorkspaceSystemType
import com.tencent.devops.remotedev.pojo.common.QueryType
import com.tencent.devops.remotedev.pojo.tcloud.ProjectCfsData
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
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import java.util.concurrent.Executors

@Suppress("ALL")
@Service
class TCloudCfsService @Autowired constructor(
    private val dslContext: DSLContext,
    private val projectTCloudCfsDao: ProjectTCloudCfsDao,
    private val workspaceJoinDao: WorkspaceJoinDao
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
            logger.debug("fetchCfsLinkCfsPermission $projectId no cfs")
            return
        }

        val cred = Credential(secretId, secretKey)
        val profile = HttpProfile().apply {
            this.endpoint = TCLOUD_DOMAIN
        }
        val client = CfsClient(
            cred, record.region,
            ClientProfile().apply {
                this.httpProfile = profile
            }
        )

        var pgId = record.pgId
        if (pgId == null) {
            pgId = getPGId(client, record.cfsId) ?: return
            projectTCloudCfsDao.updatePGId(dslContext, projectId, record.cfsId, pgId)
        }

        if (remove) {
            val gpResp = try {
                client.DescribeCfsRules(DescribeCfsRulesRequest().apply { this.pGroupId = pgId })
            } catch (e: Exception) {
                logger.error("fetchCfsLinkCfsPermission|DescribeCfsRules error", e)
                return
            }
            logger.debug("fetchCfsLinkCfsPermission|willdelete|${gpResp.ruleList}|$ip")
            gpResp.ruleList.forEach {
                if (it.authClientIp == ip) {
                    try {
                        client.DeleteCfsRule(
                            DeleteCfsRuleRequest().apply {
                                this.pGroupId = pgId
                                this.ruleId = it.ruleId
                            }
                        )
                    } catch (e: Exception) {
                        logger.error("fetchCfsLinkCfsPermission|DeleteCfsRule error", e)
                        return
                    }
                }
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
                logger.error("fetchCfsLinkCfsPermission|CreateCfsRule error", e)
                return
            }
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
            logger.error("fetchCfsLinkCfsPermission|DescribeCfsFileSystems error", e)
            return null
        }
        if (resp.totalCount < 1 || resp.fileSystems.firstOrNull() == null) {
            logger.error("fetchCfsLinkCfsPermission|DescribeCfsFileSystems 0|${resp.requestId}")
            return null
        }
        return resp.fileSystems.first().pGroup.pGroupId
    }

    private val executor = Executors.newCachedThreadPool()

    fun addProjectCfsId(
        projectId: String,
        cfsId: String,
        region: String
    ) {
        executor.execute {
            val cred = Credential(secretId, secretKey)
            val profile = HttpProfile().apply {
                this.endpoint = TCLOUD_DOMAIN
            }
            val client = CfsClient(
                cred, region,
                ClientProfile().apply {
                    this.httpProfile = profile
                }
            )

            val pgId = getPGId(client, cfsId) ?: throw RuntimeException("获取权限组ID失败")
            projectTCloudCfsDao.add(dslContext, projectId, cfsId, region, pgId)

            // 将所有这个项目下的ip都添加到权限组
            val ips = workspaceJoinDao.limitFetchProjectWorkspace(
                dslContext = dslContext,
                null,
                queryType = QueryType.WEB,
                search = WorkspaceSearch(
                    projectId = listOf(projectId),
                    workspaceSystemType = listOf(WorkspaceSystemType.WINDOWS_GPU),
                    onFuzzyMatch = false
                )
            )?.filter { !it.hostName.isNullOrBlank() }?.map {
                it.hostName?.split(".")?.let { host ->
                    host.subList(1, host.size).joinToString(separator = ".")
                }!!
            }?.toSet() ?: return@execute

            ips.forEach { ip ->
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
                    logger.error("fetchCfsLinkCfsPermission|CreateCfsRule error", e)
                    return@execute
                }
                // 中间休眠下，防止快速操作腾讯云报错
                Thread.sleep(5000)
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

    companion object {
        private val logger = LoggerFactory.getLogger(TCloudCfsService::class.java)
        private const val TCLOUD_DOMAIN = "cfs.internal.tencentcloudapi.com"
    }
}
