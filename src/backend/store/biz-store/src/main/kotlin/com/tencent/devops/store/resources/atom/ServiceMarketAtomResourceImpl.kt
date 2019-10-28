package com.tencent.devops.store.resources.atom

import com.tencent.devops.common.api.pojo.Page
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.pipeline.enums.ChannelCode
import com.tencent.devops.common.service.utils.MessageCodeUtil
import com.tencent.devops.common.web.RestResource
import com.tencent.devops.store.api.atom.ServiceMarketAtomResource
import com.tencent.devops.store.constant.StoreMessageCode
import com.tencent.devops.store.pojo.atom.AtomPipeline
import com.tencent.devops.store.pojo.atom.AtomStatistic
import com.tencent.devops.store.pojo.atom.AtomVersion
import com.tencent.devops.store.pojo.atom.InstallAtomReq
import com.tencent.devops.store.pojo.atom.enums.AtomStatusEnum
import com.tencent.devops.store.pojo.common.enums.StoreTypeEnum
import com.tencent.devops.store.service.atom.AtomService
import com.tencent.devops.store.service.atom.MarketAtomService
import com.tencent.devops.store.service.atom.MarketAtomStatisticService
import com.tencent.devops.store.service.common.StoreMemberService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier

@RestResource
class ServiceMarketAtomResourceImpl @Autowired constructor(
    private val marketAtomService: MarketAtomService,
    private val atomService: AtomService,
    private val marketAtomStatisticService: MarketAtomStatisticService,
    @Qualifier("atomMemberService")
    private val storeMemberService: StoreMemberService
) : ServiceMarketAtomResource {

    override fun setAtomBuildStatusByAtomCode(
        atomCode: String,
        version: String,
        userId: String,
        atomStatus: AtomStatusEnum,
        msg: String?
    ): Result<Boolean> {
        return marketAtomService.setAtomBuildStatusByAtomCode(atomCode, version, userId, atomStatus, msg)
    }

    override fun getProjectElements(projectCode: String): Result<Map<String, String>> {
        return atomService.getProjectElements(projectCode)
    }

    override fun getAtomByCode(atomCode: String, username: String): Result<AtomVersion?> {
        return marketAtomService.getNewestAtomByCode(username, atomCode)
    }

    override fun getAtomStatisticByCode(atomCode: String, username: String): Result<AtomStatistic> {
        // 提供给openApi使用，暂时先使用username来验证权限，等后续openApi层统一加上具体资源访问权限后再去掉
        if (!storeMemberService.isStoreMember(username, atomCode, StoreTypeEnum.ATOM.type.toByte())) {
            return MessageCodeUtil.generateResponseDataObject(StoreMessageCode.USER_QUERY_ATOM_PERMISSION_IS_INVALID, arrayOf(atomCode))
        }

        return marketAtomStatisticService.getStatisticByCode(username, atomCode)
    }

    override fun getAtomPipelinesByCode(atomCode: String, username: String, page: Int?, pageSize: Int?): Result<Page<AtomPipeline>> {
        // 提供给openApi使用，暂时先使用username来验证权限，等后续openApi层统一加上具体资源访问权限后再去掉
        if (!storeMemberService.isStoreMember(username, atomCode, StoreTypeEnum.ATOM.type.toByte())) {
            return MessageCodeUtil.generateResponseDataObject(StoreMessageCode.USER_QUERY_ATOM_PERMISSION_IS_INVALID, arrayOf(atomCode))
        }
        return marketAtomStatisticService.getAtomPipelinesByCode(atomCode, username, page, pageSize)
    }

    override fun installAtom(userId: String, channelCode: ChannelCode?, installAtomReq: InstallAtomReq): Result<Boolean> {
        return marketAtomService.installAtom("", userId, channelCode ?: ChannelCode.BS, installAtomReq)
    }
}