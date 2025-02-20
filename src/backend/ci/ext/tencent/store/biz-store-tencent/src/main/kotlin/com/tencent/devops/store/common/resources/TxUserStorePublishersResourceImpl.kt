package com.tencent.devops.store.common.resources

import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.web.RestResource
import com.tencent.devops.store.api.common.TxUserStorePublishersResource
import com.tencent.devops.store.common.service.TxUserStorePublishersService
import org.springframework.beans.factory.annotation.Autowired

@RestResource
class TxUserStorePublishersResourceImpl @Autowired constructor(
    private val txUserStorePublishersService: TxUserStorePublishersService
) : TxUserStorePublishersResource {
    override fun updateComponentFirstPublisher(userId: String): Result<Boolean> {
        return Result(txUserStorePublishersService.updateComponentFirstPublisher(userId))
    }


}


