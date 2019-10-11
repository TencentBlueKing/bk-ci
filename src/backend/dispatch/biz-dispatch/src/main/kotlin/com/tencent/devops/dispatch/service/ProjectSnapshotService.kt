package com.tencent.devops.dispatch.service

import com.tencent.devops.dispatch.dao.ProjectSnapshotDao
import org.jooq.DSLContext
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
class ProjectSnapshotService @Autowired constructor(
    private val dslContext: DSLContext,
    private val projectSnapshotDao: ProjectSnapshotDao
) {

    fun getProjectStartupSnapshot(projectId: String): String? {
        val record = projectSnapshotDao.findSnapshot(dslContext, projectId) ?: return null
        return record.vmStartupSnapshot
    }
}