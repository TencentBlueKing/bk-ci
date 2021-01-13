package com.tencent.devops.process.api.op

import com.tencent.devops.common.api.pojo.Page
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.web.RestResource
import com.tencent.devops.process.pojo.op.GitCiMarketAtom
import com.tencent.devops.process.pojo.op.GitCiMarketAtomReq
import com.tencent.devops.process.service.op.GitCiMarketAtomService
import org.springframework.beans.factory.annotation.Autowired

@RestResource
class OpGitCiMarketAtomResourceImpl @Autowired constructor(
    private val gitCiMarketAtomService: GitCiMarketAtomService
) : OpGitCiMarketAtomResource {
    override fun list(atomCode: String?, page: Int?, pageSize: Int?): Result<Page<GitCiMarketAtom>> {
        val result = gitCiMarketAtomService.list(atomCode, page ?: 1, pageSize ?: 20)
        return Result(
            Page(
                count = result.count,
                page = page ?: 1,
                pageSize = pageSize ?: 20,
                records = result.records
            )
        )
    }

    override fun add(userId: String, gitCiMarketAtomReq: GitCiMarketAtomReq): Result<Boolean> {
        return Result(gitCiMarketAtomService.add(userId, gitCiMarketAtomReq))
    }

    override fun delete(atomCode: String): Result<Boolean> {
        return Result(gitCiMarketAtomService.delete(atomCode))
    }
}
