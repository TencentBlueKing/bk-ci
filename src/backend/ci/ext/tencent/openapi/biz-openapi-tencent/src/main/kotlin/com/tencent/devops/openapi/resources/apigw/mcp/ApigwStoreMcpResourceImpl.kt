package com.tencent.devops.openapi.resources.apigw.mcp

import com.tencent.devops.common.api.enums.FrontendTypeEnum
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.web.RestResource
import com.tencent.devops.openapi.api.apigw.mcp.ApigwStoreMcpResource
import com.tencent.devops.openapi.api.apigw.mcp.pojo.MarketAtomCreateRequestMCP
import com.tencent.devops.openapi.api.apigw.mcp.pojo.MarketAtomUpdateRequestMCP
import com.tencent.devops.repository.pojo.enums.VisibilityLevelEnum
import com.tencent.devops.store.api.atom.ServiceAtomResource
import com.tencent.devops.store.api.atom.ServiceMarketAtomResource
import com.tencent.devops.store.api.atom.TxServiceAtomReleaseResource
import com.tencent.devops.store.api.common.ServiceStoreResource
import com.tencent.devops.store.pojo.atom.AtomRebuildRequest
import com.tencent.devops.store.pojo.atom.MarketAtomCreateRequest
import com.tencent.devops.store.pojo.atom.MarketAtomUpdateRequest
import com.tencent.devops.store.pojo.atom.MyAtomResp
import com.tencent.devops.store.pojo.atom.enums.AtomCategoryEnum
import com.tencent.devops.store.pojo.atom.enums.AtomStatusEnum
import com.tencent.devops.store.pojo.atom.enums.JobTypeEnum
import com.tencent.devops.store.pojo.common.enums.PackageSourceTypeEnum
import com.tencent.devops.store.pojo.common.enums.ReleaseTypeEnum
import org.springframework.beans.factory.annotation.Autowired

@RestResource
class ApigwStoreMcpResourceImpl @Autowired constructor(private val client: Client) :
    ApigwStoreMcpResource {

    override fun addMarketAtom(userId: String, marketAtomCreateRequest: MarketAtomCreateRequestMCP): Result<String> {
        return client.get(ServiceStoreResource::class).addMarketAtom(userId, with(marketAtomCreateRequest) {
            MarketAtomCreateRequest(
                projectCode = projectCode,
                atomCode = atomCode,
                name = name,
                language = language,
                authType = "OAUTH",
                visibilityLevel = VisibilityLevelEnum.LOGIN_PUBLIC,
                packageSourceType = PackageSourceTypeEnum.REPO
            )
        })
    }

    override fun updateMarketAtomTest(
        userId: String,
        marketAtomUpdateRequest: MarketAtomUpdateRequestMCP
    ): Result<String?> {
        val atomInfo = client.get(ServiceMarketAtomResource::class)
            .getAtomByCode(marketAtomUpdateRequest.atomCode, userId).data
            ?: return Result(message = "没有找到atomCode对应的插件", data = null)
        val version = marketAtomUpdateRequest.branch.take(29)
        // 获取当前分支是否在测试中，如果是，则直接重试
        val test = client.get(ServiceAtomResource::class)
            .getAtomVersionInfo(
                atomCode = marketAtomUpdateRequest.atomCode,
                version = version
            ).data
        if (test != null) {
            when (test.atomStatus) {
                AtomStatusEnum.BUILDING.name -> return Result(
                    message = "插件测试版本正在构建中，请勿重复触发",
                    data = null
                )

                else -> {
                    val res = client.get(TxServiceAtomReleaseResource::class)
                        .rebuild(
                            userId = userId,
                            projectId = atomInfo.projectCode ?: "",
                            atomId = test.id,
                            atomRebuildRequest = AtomRebuildRequest(true)
                        )
                    return if (res.data != null && res.data == true) {
                        Result(test.id)
                    } else
                        Result(message = res.message, data = null)
                }
            }
        }

        return client.get(ServiceStoreResource::class)
            .updateMarketAtomTest(
                userId,
                MarketAtomUpdateRequest(
                    atomCode = marketAtomUpdateRequest.atomCode,
                    name = atomInfo.name,
                    category = AtomCategoryEnum.TASK,
                    classifyCode = marketAtomUpdateRequest.classifyCode ?: atomInfo.classifyCode ?: "",
                    jobType = marketAtomUpdateRequest.jobType ?: JobTypeEnum.valueOf(atomInfo.jobType ?: "AGENT_LESS"),
                    os = marketAtomUpdateRequest.os ?: atomInfo.os?.let { ArrayList(it) } ?: arrayListOf("LINUX"),
                    summary = marketAtomUpdateRequest.summary ?: atomInfo.summary,
                    description = marketAtomUpdateRequest.description ?: atomInfo.description,
                    logoUrl = atomInfo.logoUrl,
                    version = version,
                    releaseType = ReleaseTypeEnum.BRANCH_TEST,
                    versionContent = "build atom branch version",
                    publisher = userId,
                    labelIdList = null,
                    frontendType = atomInfo.frontendType ?: FrontendTypeEnum.NORMAL,
                    branch = marketAtomUpdateRequest.branch
                )
            )
    }

    override fun listMyAtoms(
        userId: String,
        atomName: String?,
        page: Int,
        pageSize: Int
    ): Result<MyAtomResp?> {
        return client.get(ServiceMarketAtomResource::class)
            .listMyAtoms(userId, atomName, page, pageSize)
    }
}
