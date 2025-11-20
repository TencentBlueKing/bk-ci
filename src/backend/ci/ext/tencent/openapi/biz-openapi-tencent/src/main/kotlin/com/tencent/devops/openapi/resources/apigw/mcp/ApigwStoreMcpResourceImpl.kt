package com.tencent.devops.openapi.resources.apigw.mcp

import com.tencent.devops.common.api.enums.FrontendTypeEnum
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.web.RestResource
import com.tencent.devops.openapi.api.apigw.mcp.ApigwStoreMcpResource
import com.tencent.devops.openapi.api.apigw.mcp.pojo.MarketAtomCreateRequestMCP
import com.tencent.devops.openapi.api.apigw.mcp.pojo.MarketAtomUpdateRequestMCP
import com.tencent.devops.repository.pojo.enums.VisibilityLevelEnum
import com.tencent.devops.store.api.atom.ServiceMarketAtomResource
import com.tencent.devops.store.api.common.ServiceStoreResource
import com.tencent.devops.store.pojo.atom.MarketAtomCreateRequest
import com.tencent.devops.store.pojo.atom.MarketAtomUpdateRequest
import com.tencent.devops.store.pojo.atom.enums.AtomCategoryEnum
import com.tencent.devops.store.pojo.atom.enums.JobTypeEnum
import com.tencent.devops.store.pojo.common.enums.PackageSourceTypeEnum
import com.tencent.devops.store.pojo.common.enums.ReleaseTypeEnum
import java.text.SimpleDateFormat
import java.util.Date
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
        val version = "test-${marketAtomUpdateRequest.branch}-${SimpleDateFormat("yyyyMMdd").format(Date())}"
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
}
