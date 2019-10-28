package com.tencent.devops.store.api.ideatom

import com.tencent.devops.common.api.pojo.Page
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.web.RestResource
import com.tencent.devops.store.api.ideatom.OpIdeAtomResource
import com.tencent.devops.store.pojo.ideatom.IdeAtom
import com.tencent.devops.store.pojo.ideatom.IdeAtomCreateRequest
import com.tencent.devops.store.pojo.ideatom.IdeAtomReleaseRequest
import com.tencent.devops.store.pojo.ideatom.IdeAtomUpdateRequest
import com.tencent.devops.store.pojo.ideatom.OpIdeAtomItem
import com.tencent.devops.store.pojo.ideatom.enums.IdeAtomTypeEnum
import com.tencent.devops.store.service.ideatom.IdeAtomService
import com.tencent.devops.store.service.ideatom.OpIdeAtomService
import org.springframework.beans.factory.annotation.Autowired

@RestResource
class OpIdeAtomResourceImpl @Autowired constructor(
    private val ideAtomService: IdeAtomService,
    private val opIdeAtomService: OpIdeAtomService
) : OpIdeAtomResource {

    override fun addIdeAtom(userId: String, ideAtomCreateRequest: IdeAtomCreateRequest): Result<Boolean> {
        return opIdeAtomService.addIdeAtom(userId, ideAtomCreateRequest)
    }

    override fun updateIdeAtom(userId: String, atomId: String, ideAtomUpdateRequest: IdeAtomUpdateRequest): Result<Boolean> {
        return opIdeAtomService.updateIdeAtom(userId, atomId, ideAtomUpdateRequest)
    }

    override fun deleteIdeAtomById(atomId: String): Result<Boolean> {
        return opIdeAtomService.deleteIdeAtomById(atomId)
    }

    override fun getIdeAtomById(atomId: String): Result<IdeAtom?> {
        return ideAtomService.getIdeAtomById(atomId)
    }

    override fun getIdeAtomsByCode(atomCode: String, version: String?): Result<IdeAtom?> {
        return ideAtomService.getIdeAtomByCode(atomCode, version)
    }

    override fun getIdeAtomVersionsByCode(
        atomCode: String,
        page: Int?,
        pageSize: Int?
    ): Result<Page<OpIdeAtomItem>?> {
        return opIdeAtomService.getIdeAtomVersionsByCode(atomCode, page, pageSize)
    }

    override fun listIdeAtoms(
        atomName: String?,
        atomType: IdeAtomTypeEnum?,
        classifyCode: String?,
        categoryCodes: String?,
        labelCodes: String?,
        processFlag: Boolean?,
        page: Int,
        pageSize: Int
    ): Result<Page<OpIdeAtomItem>?> {
        return opIdeAtomService.listIdeAtoms(atomName, atomType, classifyCode, categoryCodes, labelCodes, processFlag, page, pageSize)
    }

    override fun releaseIdeAtom(userId: String, atomId: String, ideAtomReleaseRequest: IdeAtomReleaseRequest): Result<Boolean> {
        return opIdeAtomService.releaseIdeAtom(userId, atomId, ideAtomReleaseRequest)
    }

    override fun offlineIdeAtom(userId: String, atomCode: String, version: String?, reason: String?): Result<Boolean> {
        return opIdeAtomService.offlineIdeAtom(userId, atomCode, version, reason)
    }
}