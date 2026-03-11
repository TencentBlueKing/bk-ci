package com.tencent.devops.dispatch.windows.resources

import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.web.RestResource
import com.tencent.devops.dispatch.windows.api.UserSystemVersionResource
import com.tencent.devops.dispatch.windows.pojo.VMType
import com.tencent.devops.dispatch.windows.service.WindowsTypeService
import org.springframework.beans.factory.annotation.Autowired
@RestResource
class UserWindowsVersionResourceImpl @Autowired constructor(
    private val windowsTypeService: WindowsTypeService
) : UserSystemVersionResource {

    override fun list(): Result<List<VMType>> {
        return Result(windowsTypeService.listSystemVersion())
    }
}
