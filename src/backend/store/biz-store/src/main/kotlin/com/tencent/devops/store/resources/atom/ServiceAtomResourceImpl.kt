package com.tencent.devops.store.resources.atom

import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.web.RestResource
import com.tencent.devops.store.api.atom.ServiceAtomResource
import com.tencent.devops.store.pojo.atom.InstalledAtom
import com.tencent.devops.store.service.atom.AtomService
import org.springframework.beans.factory.annotation.Autowired

@RestResource
class ServiceAtomResourceImpl @Autowired constructor(
    private val atomService: AtomService
) : ServiceAtomResource {

    override fun getInstalledAtoms(
        projectCode: String
    ): Result<List<InstalledAtom>> {
        return Result(atomService.listInstalledAtomByProject(projectCode))
    }
}