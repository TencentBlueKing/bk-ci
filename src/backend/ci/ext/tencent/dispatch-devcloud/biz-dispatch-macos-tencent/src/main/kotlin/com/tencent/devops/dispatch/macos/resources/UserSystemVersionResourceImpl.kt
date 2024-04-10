package com.tencent.devops.dispatch.macos.resources

import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.web.RestResource
import com.tencent.devops.dispatch.macos.api.UserSystemVersionResource
import com.tencent.devops.dispatch.macos.pojo.MacOsVersionVO
import com.tencent.devops.dispatch.macos.service.MacVmTypeService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value

@RestResource
class UserSystemVersionResourceImpl @Autowired constructor(
    private val macVmTypeService: MacVmTypeService
) : UserSystemVersionResource {

    @Value("\${macos.defaultSystemVersion:Monterey12.4}")
    private lateinit var defaultSystemVersion: String

    override fun list(): Result<List<String>> {
        return Result(macVmTypeService.listSystemVersion())
    }

    override fun listV2(): Result<MacOsVersionVO> {
        return Result(
            MacOsVersionVO(
                defaultVersion = defaultSystemVersion,
                versionList = macVmTypeService.listSystemVersion()
            )
        )
    }
}
