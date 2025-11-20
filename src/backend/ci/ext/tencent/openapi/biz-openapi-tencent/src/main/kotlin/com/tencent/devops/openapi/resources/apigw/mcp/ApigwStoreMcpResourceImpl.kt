package com.tencent.devops.openapi.resources.apigw.mcp

import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.web.RestResource
import com.tencent.devops.openapi.api.apigw.mcp.ApigwStoreMcpResource
import com.tencent.devops.openapi.api.apigw.mcp.pojo.MarketAtomCreateRequestMCP
import com.tencent.devops.openapi.api.apigw.mcp.pojo.MarketAtomUpdateRequestMCP
import com.tencent.devops.repository.pojo.enums.VisibilityLevelEnum
import com.tencent.devops.store.api.common.ServiceStoreResource
import com.tencent.devops.store.pojo.atom.MarketAtomCreateRequest
import com.tencent.devops.store.pojo.atom.MarketAtomUpdateRequest
import com.tencent.devops.store.pojo.atom.enums.AtomCategoryEnum
import com.tencent.devops.store.pojo.common.enums.PackageSourceTypeEnum
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

    override fun updateMarketAtom(
        userId: String,
        marketAtomUpdateRequest: MarketAtomUpdateRequestMCP
    ): Result<String?> {
        val projectCode = ""// todo 调试项目
        val logoUrl = "" // todo 调试logo
        val version = "" // todo 通过后台计算出需要的版本
        return client.get(ServiceStoreResource::class)
            .updateMarketAtom(userId, projectCode, with(marketAtomUpdateRequest) {
                MarketAtomUpdateRequest(
                    atomCode = atomCode,
                    name = name,
                    category = AtomCategoryEnum.TASK,
                    classifyCode = classifyCode,
                    jobType = jobType,
                    os = os,
                    summary = summary,
                    description = description,
                    logoUrl = logoUrl,
                    version = version,
                    releaseType = releaseType,
                    versionContent = versionContent,
                    publisher = userId,
                    labelIdList = null
                )
            })
    }
}
