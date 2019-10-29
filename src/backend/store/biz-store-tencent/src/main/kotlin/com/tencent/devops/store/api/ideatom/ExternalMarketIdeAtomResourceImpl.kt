package com.tencent.devops.store.api.ideatom

import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.web.RestResource
import com.tencent.devops.store.api.ideatom.ExternalMarketIdeAtomResource
import com.tencent.devops.store.pojo.ideatom.ExternalIdeAtomResp
import com.tencent.devops.store.pojo.ideatom.IdeAtom
import com.tencent.devops.store.pojo.ideatom.InstallIdeAtomReq
import com.tencent.devops.store.pojo.ideatom.InstallIdeAtomResp
import com.tencent.devops.store.pojo.ideatom.enums.IdeAtomTypeEnum
import com.tencent.devops.store.pojo.ideatom.enums.MarketIdeAtomSortTypeEnum
import com.tencent.devops.store.service.ideatom.ExternalMarketIdeAtomService
import com.tencent.devops.store.service.ideatom.IdeAtomService
import org.springframework.beans.factory.annotation.Autowired

@RestResource
class ExternalMarketIdeAtomResourceImpl @Autowired constructor(
    private val ideAtomService: IdeAtomService,
    private val externalMarketIdeAtomService: ExternalMarketIdeAtomService
) : ExternalMarketIdeAtomResource {

    override fun list(
        categoryCode: String,
        atomName: String?,
        classifyCode: String?,
        labelCodes: String?,
        score: Int?,
        rdType: IdeAtomTypeEnum?,
        sortType: MarketIdeAtomSortTypeEnum?,
        page: Int?,
        pageSize: Int?
    ): Result<ExternalIdeAtomResp> {
        return Result(externalMarketIdeAtomService.list(categoryCode, atomName, classifyCode, labelCodes, score, rdType, sortType, page, pageSize))
    }

    override fun getIdeAtomsByCode(atomCode: String): Result<IdeAtom?> {
        return ideAtomService.getIdeAtomByCode(atomCode)
    }

    override fun installIdeAtom(installIdeAtomReq: InstallIdeAtomReq): Result<InstallIdeAtomResp?> {
        return externalMarketIdeAtomService.installIdeAtom(installIdeAtomReq)
    }
}