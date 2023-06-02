package com.tencent.devops.dispatch.macos.resources

import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.web.RestResource
import com.tencent.devops.dispatch.macos.api.UserSystemVersionResource
import com.tencent.devops.dispatch.macos.pojo.MacOsVersionVO
import com.tencent.devops.dispatch.macos.service.MacVmTypeService
import org.springframework.beans.factory.annotation.Autowired

@RestResource
class UserSystemVersionResourceImpl @Autowired constructor(
    private val macVmTypeService: MacVmTypeService
) : UserSystemVersionResource {

    override fun list(): Result<List<String>> {
        return Result(macVmTypeService.listSystemVersion())
    }

    override fun listV2(): Result<MacOsVersionVO> {
        return Result(
            MacOsVersionVO(
                defaultVersion = DEFAULT_SYSTEM_VERSION,
                versionList = macVmTypeService.listSystemVersion()
            )
        )
    }

    companion object {
        private const val DEFAULT_SYSTEM_VERSION = "BigSur11.4"
    }
}
