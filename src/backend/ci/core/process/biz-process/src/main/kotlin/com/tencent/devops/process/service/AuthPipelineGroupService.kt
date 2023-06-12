package com.tencent.devops.process.service

import com.tencent.bk.sdk.iam.constants.CallbackMethodEnum
import com.tencent.bk.sdk.iam.dto.callback.request.CallbackRequestDTO
import com.tencent.bk.sdk.iam.dto.callback.response.CallbackBaseResponseDTO
import com.tencent.bk.sdk.iam.dto.callback.response.FetchInstanceInfoResponseDTO
import com.tencent.bk.sdk.iam.dto.callback.response.InstanceInfoDTO
import com.tencent.bk.sdk.iam.dto.callback.response.ListInstanceResponseDTO
import com.tencent.devops.common.api.util.HashUtil
import com.tencent.devops.common.auth.api.AuthTokenApi
import com.tencent.devops.common.auth.callback.FetchInstanceInfo
import com.tencent.devops.common.auth.callback.ListInstanceInfo
import com.tencent.devops.common.auth.callback.SearchInstanceInfo
import com.tencent.devops.process.dao.label.PipelineViewDao
import com.tencent.devops.process.engine.dao.PipelineInfoDao
import org.jooq.DSLContext
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
class AuthPipelineGroupService @Autowired constructor(
    val authTokenApi: AuthTokenApi,
    val pipelineListFacadeService: PipelineListFacadeService,
    val pipelineViewDao: PipelineViewDao,
    val pipelineInfoDao: PipelineInfoDao,
    val dslContext: DSLContext
) {
    fun pipelineGroupInfo(
        callBackInfo: CallbackRequestDTO,
        token: String
    ): CallbackBaseResponseDTO? {
        logger.info("iam流水线组回调信息:$callBackInfo")
        val method = callBackInfo.method
        val page = callBackInfo.page
        val projectId = callBackInfo.filter.parent?.id ?: "" // FETCH_INSTANCE_INFO场景下iam不会传parentId
        when (method) {
            CallbackMethodEnum.LIST_INSTANCE -> {
                return getPipelineGroup(
                    projectId = projectId,
                    offset = page.offset.toInt(),
                    limit = page.limit.toInt(),
                    token = token
                )
            }
            CallbackMethodEnum.FETCH_INSTANCE_INFO -> {
                val ids = callBackInfo.filter.idList.map { HashUtil.decodeIdToLong(it.toString()) }
                return getPipelineGroupInfo(ids.toSet(), token)
            }
            CallbackMethodEnum.SEARCH_INSTANCE -> {
                return searchPipelineGroupInfo(
                    projectId = projectId,
                    keyword = callBackInfo.filter.keyword,
                    limit = page.limit.toInt(),
                    offset = page.offset.toInt(),
                    token = token
                )
            }
            else -> {}
        }
        return null
    }

    private fun getPipelineGroup(
        projectId: String,
        offset: Int,
        limit: Int,
        token: String
    ): ListInstanceResponseDTO {
        authTokenApi.checkToken(token)
        val pipelineGroupList = pipelineViewDao.listByPage(
            dslContext = dslContext,
            projectId = projectId,
            isProject = true,
            limit = limit,
            offset = offset
        )
        val result = ListInstanceInfo()
        if (pipelineGroupList.isEmpty()) {
            logger.info("$projectId 项目下无流水线组")
            return result.buildListInstanceFailResult()
        }
        val entityInfo = mutableListOf<InstanceInfoDTO>()
        pipelineGroupList.map {
            val entity = InstanceInfoDTO()
            entity.id = HashUtil.encodeLongId(it.id)
            entity.displayName = it.name
            entityInfo.add(entity)
        }
        logger.info("entityInfo $entityInfo, count ${entityInfo.size}")
        return result.buildListInstanceResult(entityInfo, entityInfo.size.toLong())
    }

    private fun getPipelineGroupInfo(
        ids: Set<Long>,
        token: String
    ): FetchInstanceInfoResponseDTO {
        authTokenApi.checkToken(token)
        val pipelineGroupList = pipelineViewDao.list(
            dslContext = dslContext,
            viewIds = ids
        )
        val result = FetchInstanceInfo()
        if (pipelineGroupList.isEmpty()) {
            logger.info("$ids 未匹配到启用流水线组")
            return result.buildFetchInstanceFailResult()
        }

        val entityInfo = mutableListOf<InstanceInfoDTO>()
        pipelineGroupList.map {
            val entity = InstanceInfoDTO()
            entity.id = HashUtil.encodeLongId(it.id)
            entity.iamApprover = arrayListOf(it.createUser)
            entity.displayName = it.name
            entityInfo.add(entity)
        }
        logger.info("entityInfo $entityInfo, count ${entityInfo.size.toLong()}")
        return result.buildFetchInstanceResult(entityInfo)
    }

    private fun searchPipelineGroupInfo(
        projectId: String,
        keyword: String,
        limit: Int,
        offset: Int,
        token: String
    ): SearchInstanceInfo {
        authTokenApi.checkToken(token)
        val pipelineGroupInfo = pipelineViewDao.listByPage(
            dslContext = dslContext,
            projectId = projectId,
            isProject = true,
            viewName = keyword,
            limit = limit,
            offset = offset
        )
        val result = SearchInstanceInfo()
        if (pipelineGroupInfo.isEmpty()) {
            logger.info("$projectId 项目下无流水线组")
            return result.buildSearchInstanceFailResult()
        }
        val entityInfo = mutableListOf<InstanceInfoDTO>()
        pipelineGroupInfo.map {
            val entity = InstanceInfoDTO()
            entity.id = HashUtil.encodeLongId(it.id)
            entity.displayName = it.name
            entityInfo.add(entity)
        }
        logger.info("entityInfo $entityInfo, count ${pipelineGroupInfo.size}")
        return result.buildSearchInstanceResult(entityInfo, pipelineGroupInfo.size.toLong())
    }

    companion object {
        private val logger = LoggerFactory.getLogger(AuthPipelineGroupService::class.java)
    }
}
