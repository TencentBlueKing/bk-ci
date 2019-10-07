package com.tencent.devops.store.resources.atom

import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.web.RestResource
import com.tencent.devops.store.api.atom.ServiceMarketAtomResource
import com.tencent.devops.store.pojo.atom.AtomProcessInfo
import com.tencent.devops.store.service.atom.AtomService
import com.tencent.devops.store.service.atom.MarketAtomService
import org.springframework.beans.factory.annotation.Autowired

@RestResource
class ServiceMarketAtomResourceImpl @Autowired constructor(
    private val atomService: AtomService,
    private val marketAtomService: MarketAtomService
) : ServiceMarketAtomResource {

    override fun getProjectAtomNames(projectCode: String): Result<Map<String, String>> {
        return atomService.getProjectAtomNames(projectCode)
    }

    override fun getProcessInfo(atomId: String): Result<AtomProcessInfo> {
        return marketAtomService.getProcessInfo(atomId)
    }
}