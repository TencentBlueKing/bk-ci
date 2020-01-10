package com.tencent.devops.store.resources

import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.web.RestResource
import com.tencent.devops.store.api.UserExtServiceResource
import com.tencent.devops.store.pojo.atom.enums.MarketAtomSortTypeEnum
import com.tencent.devops.store.pojo.enums.ExtServiceSortTypeEnum
import com.tencent.devops.store.pojo.vo.ExtServiceMainItemVo
import com.tencent.devops.store.pojo.vo.SearchExtServiceVO
import com.tencent.devops.store.service.ExtServiceSearchService
import org.springframework.beans.factory.annotation.Autowired

@RestResource
class UserExtServiceResourceImpl @Autowired constructor(
    val extServiceSearchService: ExtServiceSearchService
) : UserExtServiceResource {
    override fun mainPageList(userId: String, page: Int?, pageSize: Int?): Result<List<ExtServiceMainItemVo>> {
        return extServiceSearchService.mainPageList(userId, page, pageSize)
    }

    override fun list(
        userId: String,
        serviceName: String?,
        classifyCode: String?,
        labelCode: String?,
        score: Int?,
        sortType: ExtServiceSortTypeEnum?,
        page: Int?,
        pageSize: Int?
    ): Result<SearchExtServiceVO> {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }
}