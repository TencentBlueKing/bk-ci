package com.tencent.devops.plugin.resources

import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.web.RestResource
import com.tencent.devops.plugin.api.UserZhiyunResource
import com.tencent.devops.plugin.pojo.zhiyun.ZhiyunProduct
import com.tencent.devops.plugin.service.ZhiyunService
import org.springframework.beans.factory.annotation.Autowired

@RestResource
class UserZhiyunResourceImpl @Autowired constructor(private val zhiyunService: ZhiyunService) : UserZhiyunResource {
    override fun getProduct(userId: String): Result<List<ZhiyunProduct>> {
        return Result(zhiyunService.getList())
    }
}