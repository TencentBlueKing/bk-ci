package com.tencent.devops.store.api.op

import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.web.RestResource
import com.tencent.devops.store.pojo.atom.ApproveReq
import com.tencent.devops.store.pojo.atom.Atom
import com.tencent.devops.store.pojo.atom.AtomCreateRequest
import com.tencent.devops.store.pojo.atom.AtomResp
import com.tencent.devops.store.pojo.atom.AtomUpdateRequest
import com.tencent.devops.store.pojo.atom.enums.AtomStatusEnum
import com.tencent.devops.store.pojo.common.StoreVisibleDeptResp
import com.tencent.devops.store.pojo.common.VisibleApproveReq
import com.tencent.devops.store.pojo.atom.enums.AtomTypeEnum
import com.tencent.devops.store.pojo.common.enums.StoreTypeEnum
import com.tencent.devops.store.pojo.enums.OpSortTypeEnum
import com.tencent.devops.store.service.atom.AtomService
import com.tencent.devops.store.service.atom.MarketAtomService
import com.tencent.devops.store.service.common.StoreVisibleDeptService
import org.springframework.beans.factory.annotation.Autowired

@RestResource
class OpAtomResourceImpl @Autowired constructor(
    private val atomService: AtomService,
    private val storeVisibleDeptService: StoreVisibleDeptService,
    private val marketAtomService: MarketAtomService
) : OpAtomResource {

    override fun moveGitProjectToGroup(userId: String, atomCode: String, groupCode: String?): Result<Boolean> {
        return atomService.moveGitProjectToGroup(userId, groupCode, atomCode)
    }

    override fun add(userId: String, atomCreateRequest: AtomCreateRequest): Result<Boolean> {
        return atomService.savePipelineAtom(userId, atomCreateRequest)
    }

    override fun update(userId: String, id: String, atomUpdateRequest: AtomUpdateRequest): Result<Boolean> {
        return atomService.updatePipelineAtom(userId, id, atomUpdateRequest)
    }

    override fun listAllPipelineAtoms(
        atomName: String?,
        atomType: AtomTypeEnum?,
        serviceScope: String?,
        os: String?,
        category: String?,
        classifyId: String?,
        atomStatus: AtomStatusEnum?,
        sortType: OpSortTypeEnum?,
        desc: Boolean?,
        page: Int?,
        pageSize: Int?
    ): Result<AtomResp<Atom>?> {
        return atomService.getOpPipelineAtoms(atomName, atomType, serviceScope, os, category, classifyId, atomStatus, sortType, desc, page, pageSize)
    }

    override fun getPipelineAtomById(id: String): Result<Atom?> {
        return atomService.getPipelineAtom(id)
    }

    override fun deletePipelineAtomById(id: String): Result<Boolean> {
        return atomService.deletePipelineAtom(id)
    }

    override fun approveAtom(atomId: String, approveReq: ApproveReq): Result<Boolean> {
        return marketAtomService.approveAtom(atomId, approveReq)
    }

    override fun approveVisibleDept(userId: String, atomCode: String, visibleApproveReq: VisibleApproveReq): Result<Boolean> {
        return storeVisibleDeptService.approveVisibleDept(userId, atomCode, visibleApproveReq, StoreTypeEnum.ATOM)
    }

    override fun getVisibleDept(atomCode: String): Result<StoreVisibleDeptResp?> {
        return storeVisibleDeptService.getVisibleDept(atomCode, StoreTypeEnum.ATOM, null)
    }
}