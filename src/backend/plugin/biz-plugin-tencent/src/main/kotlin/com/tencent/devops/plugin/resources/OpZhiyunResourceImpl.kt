package com.tencent.devops.plugin.resources

import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.web.RestResource
import com.tencent.devops.plugin.api.OpZhiyunResource
import com.tencent.devops.plugin.pojo.zhiyun.ZhiyunProduct
import com.tencent.devops.plugin.service.ZhiyunService
import org.springframework.beans.factory.annotation.Autowired

@RestResource
class OpZhiyunResourceImpl @Autowired constructor(
    private val zhiyunService: ZhiyunService
) : OpZhiyunResource {
    override fun create(zhiyunProduct: ZhiyunProduct): Result<Boolean> {
        zhiyunService.createProduct(zhiyunProduct)
        return Result(true)
    }

    override fun delete(productId: String): Result<Boolean> {
        zhiyunService.deleteProduct(productId)
        return Result(true)
    }

    override fun getList(): Result<List<ZhiyunProduct>> {
        return Result(zhiyunService.getList())
    }
}