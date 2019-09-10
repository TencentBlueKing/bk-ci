package com.tencent.devops.store.resources.atom

import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.web.RestResource
import com.tencent.devops.store.api.atom.ServiceMarketAtomResource
import com.tencent.devops.store.service.atom.AtomService
import org.springframework.beans.factory.annotation.Autowired

@RestResource
class ServiceMarketAtomResourceImpl @Autowired constructor(
    private val atomService: AtomService
) : ServiceMarketAtomResource {

    override fun getProjectAtomNames(projectCode: String): Result<Map<String, String>> {
        return atomService.getProjectAtomNames(projectCode)
    }
}