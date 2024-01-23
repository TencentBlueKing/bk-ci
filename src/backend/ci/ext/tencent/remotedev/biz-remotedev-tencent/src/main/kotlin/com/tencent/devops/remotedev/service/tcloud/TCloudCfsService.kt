package com.tencent.devops.remotedev.service.tcloud

import com.tencent.devops.common.api.util.PageUtil
import com.tencent.devops.remotedev.dao.ProjectTCloudCfsDao
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

@Suppress("ALL")
@Service
class TCloudCfsService @Autowired constructor(
    private val dslContext: DSLContext,
    private val projectTCloudCfsDao: ProjectTCloudCfsDao
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
            this.endpoint = "cfs.internal.tencentcloudapi.com"
        }
        val client = CfsClient(
            cred, record.region,
            ClientProfile().apply {
                this.httpProfile = profile
            }
        )

        var pgId = record.pgId
        if (pgId == null) {
            val resp = try {
                client.DescribeCfsFileSystems(
                    DescribeCfsFileSystemsRequest().apply {
                        this.fileSystemId = record.cfsId
                    }
                )
            } catch (e: Exception) {
                logger.error("fetchCfsLinkCfsPermission|DescribeCfsFileSystems error", e)
                return
            }
            if (resp.totalCount < 1 || resp.fileSystems.firstOrNull() == null) {
                logger.error("fetchCfsLinkCfsPermission|DescribeCfsFileSystems 0|${resp.requestId}")
                return
            }
            pgId = resp.fileSystems.first().pGroup.pGroupId
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

    fun addProjectCfsId(
        projectId: String,
        cfsId: String,
        region: String
    ) {
        projectTCloudCfsDao.add(dslContext, projectId, cfsId, region)
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
    }
}
