package com.tencent.devops.process.service.op

import com.tencent.devops.process.dao.op.GitCiMarketAtomDao
import com.tencent.devops.process.pojo.op.GitCiMarketAtom
import org.jooq.DSLContext
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
class GitCiMarketAtomService @Autowired constructor(
    private val dslContext: DSLContext,
    private val gitCiMarketAtomDao: GitCiMarketAtomDao
) {
    fun list(): List<GitCiMarketAtom> {
        val results = mutableListOf<GitCiMarketAtom>()
        val records = gitCiMarketAtomDao.list(dslContext)
        if (records.isEmpty()) {
            return results
        }
        records.forEach {
            results.add(
                GitCiMarketAtom(
                    id = it.id,
                    atomCode = it.atomCode,
                    desc = it.desc
                )
            )
        }
        return results
    }

    fun add(
        gitCiMarketAtom: GitCiMarketAtom
    ): Boolean {
        val recordNum = gitCiMarketAtomDao.create(
            dslContext = dslContext,
            atomCode = gitCiMarketAtom.atomCode,
            desc = gitCiMarketAtom.desc
        )
        return recordNum > 0
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
