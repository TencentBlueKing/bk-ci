package com.tencent.devops.remotedev.service

import com.tencent.devops.remotedev.dao.ClientTipsDao
import com.tencent.devops.remotedev.dao.ClientTipsRecordM
import com.tencent.devops.remotedev.pojo.ClientTips
import com.tencent.devops.remotedev.pojo.ClientTipsInfo
import org.jooq.DSLContext
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
class ClientTipsService @Autowired constructor(
    private val dslContext: DSLContext,
    private val clientTipsDao: ClientTipsDao
) {
    /**
     * 量比较少，所以全量搜出来过滤
     * 生效项目和生效人员如果不填就是所有都生效，如果填了就是当前项目或者人员下生效
     * 优先级 人员 > 项目
     */
    fun fetchTips(
        projectId: String?,
        userId: String?
    ): List<ClientTips> {
        val records = clientTipsDao.fetchAll(dslContext)
        val result = mutableListOf<ClientTips>()
        records.forEach { record ->
            if (record.effectiveUsers.isNullOrEmpty() && record.effectiveProjects.isNullOrEmpty()) {
                result.add(genTips(record))
                return@forEach
            }
            if (!record.effectiveUsers.isNullOrEmpty()) {
                if (!userId.isNullOrBlank() && userId in record.effectiveUsers) {
                    result.add(genTips(record))
                    return@forEach
                } else {
                    return@forEach
                }
            }
            if (!record.effectiveProjects.isNullOrEmpty()) {
                if (!projectId.isNullOrBlank() && projectId in record.effectiveProjects) {
                    result.add(genTips(record))
                    return@forEach
                } else {
                    return@forEach
                }
            }
        }

        return result.sortedByDescending { it.weight }
    }

    fun fetchAll(): List<ClientTipsInfo> {
        val records = clientTipsDao.fetchAll(dslContext)
        return records.map {
            ClientTipsInfo(
                id = it.id,
                title = it.title,
                content = it.content,
                weight = it.weight,
                effectiveUsers = it.effectiveUsers,
                effectiveProjects = it.effectiveProjects
            )
        }
    }

    fun createOrUpdateTips(
        id: Long?,
        info: ClientTipsInfo
    ) {
        if (id == null) {
            clientTipsDao.create(dslContext, info)
        } else {
            clientTipsDao.update(dslContext, id, info)
        }
    }

    fun deleteTips(
        ids: Set<Long>
    ) {
        clientTipsDao.delete(dslContext, ids)
    }

    companion object {
        private fun genTips(r: ClientTipsRecordM): ClientTips {
            return ClientTips(
                id = r.id,
                title = r.title,
                content = r.content,
                weight = r.weight
            )
        }
    }
}
