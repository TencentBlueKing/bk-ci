package com.tencent.devops.dispatch.macos.resources

import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.web.RestResource
import com.tencent.devops.dispatch.macos.api.UserXcodeVersionResource
import com.tencent.devops.dispatch.macos.pojo.MacOsVersionVO
import com.tencent.devops.dispatch.macos.service.MacVmTypeService
import org.springframework.beans.factory.annotation.Autowired

@RestResource
class UserXcodeVersionResourceImpl @Autowired constructor(
    private val macVmTypeService: MacVmTypeService
) : UserXcodeVersionResource {
    override fun list(): Result<List<String>> {
        return Result(macVmTypeService.listXcodeVersion())
    }

    override fun listV2(systemVersion: String?): Result<MacOsVersionVO> {
        val versionList = macVmTypeService.listXcodeVersion(systemVersion)
        var defaultVersion = DEFAULT_XCODE_VERSION
        if (!versionList.contains(DEFAULT_XCODE_VERSION)) {
            defaultVersion = versionList.first()
        }

        return Result(
            MacOsVersionVO(
                defaultVersion = defaultVersion,
                versionList = versionList
            )
        )
    }

    companion object {
        private const val DEFAULT_XCODE_VERSION = "13.2.1"
    }
}
