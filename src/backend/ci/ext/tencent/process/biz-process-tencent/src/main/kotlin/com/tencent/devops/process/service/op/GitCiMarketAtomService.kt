package com.tencent.devops.process.service.op

import com.tencent.devops.common.api.model.SQLPage
import com.tencent.devops.common.api.util.DateTimeUtil
import com.tencent.devops.process.dao.op.GitCiMarketAtomDao
import com.tencent.devops.process.pojo.op.GitCiMarketAtom
import com.tencent.devops.process.pojo.op.GitCiMarketAtomReq
import org.jooq.DSLContext
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
class GitCiMarketAtomService @Autowired constructor(
    private val dslContext: DSLContext,
    private val gitCiMarketAtomDao: GitCiMarketAtomDao
) {
    fun list(
        atomCode: String?,
        page: Int?,
        pageSize: Int?
    ): SQLPage<GitCiMarketAtom> {
        val results = mutableListOf<GitCiMarketAtom>()
        val count = gitCiMarketAtomDao.getCount(dslContext, atomCode)
        val records = gitCiMarketAtomDao.list(dslContext, atomCode, page, pageSize)
        if (records.isEmpty()) {
            return SQLPage(count = 0, records = results)
        }
        records.forEach {
            results.add(
                GitCiMarketAtom(
                    id = it.id,
                    atomCode = it.atomCode,
                    desc = it.desc,
                    updateTime = DateTimeUtil.toDateTime(it.updateTime),
                    modifyUser = it.modifyUser
                )
            )
        }
        return SQLPage(count = count, records = results)
    }

    fun add(
        userId: String,
        gitCiMarketAtomReq: GitCiMarketAtomReq
    ): Boolean {
        gitCiMarketAtomDao.batchAdd(
            dslContext = dslContext,
            userId = userId,
            gitCiMarketAtomReq = gitCiMarketAtomReq
        )
        return true
    }

    fun delete(
        atomCode: String
    ): Boolean {
        val recordNum = gitCiMarketAtomDao.delete(
            dslContext = dslContext,
            atomCode = atomCode
        )
        return recordNum > 0
    }
}
