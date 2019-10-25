package com.tencent.devops.store.api.ideatom

import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.web.RestResource
import com.tencent.devops.store.pojo.ideatom.IdeAtomDetail
import com.tencent.devops.store.pojo.ideatom.MarketIdeAtomMainItem
import com.tencent.devops.store.pojo.ideatom.MarketIdeAtomResp
import com.tencent.devops.store.pojo.ideatom.enums.IdeAtomTypeEnum
import com.tencent.devops.store.pojo.ideatom.enums.MarketIdeAtomSortTypeEnum
import com.tencent.devops.store.service.ideatom.MarketIdeAtomService
import org.springframework.beans.factory.annotation.Autowired

@RestResource
class UserMarketIdeAtomResourceImpl @Autowired constructor(
    private val marketIdeAtomService: MarketIdeAtomService
) : UserMarketIdeAtomResource {

    override fun mainPageList(userId: String, page: Int?, pageSize: Int?): Result<List<MarketIdeAtomMainItem>> {
        return marketIdeAtomService.mainPageList(userId, page, pageSize)
    }

    override fun queryIdeAtomList(
        userId: String,
        atomName: String?,
        categoryCode: String?,
        classifyCode: String?,
        labelCode: String?,
        score: Int?,
        rdType: IdeAtomTypeEnum?,
        sortType: MarketIdeAtomSortTypeEnum?,
        page: Int?,
        pageSize: Int?
    ): Result<MarketIdeAtomResp> {
        return Result(marketIdeAtomService.list(userId, atomName, categoryCode, classifyCode, labelCode, score, rdType, sortType, page, pageSize))
    }

    override fun getIdeAtomByCode(userId: String, atomCode: String): Result<IdeAtomDetail?> {
        return marketIdeAtomService.getAtomByCode(userId, atomCode)
    }
}