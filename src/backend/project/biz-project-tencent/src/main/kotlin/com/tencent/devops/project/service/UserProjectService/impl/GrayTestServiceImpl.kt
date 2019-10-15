package com.tencent.devops.project.service.UserProjectService.impl

import com.tencent.devops.project.dao.GrayTestDao
import com.tencent.devops.project.pojo.service.GrayTestInfo
import com.tencent.devops.project.pojo.service.GrayTestListInfo
import com.tencent.devops.project.service.GrayTestService
import org.jooq.DSLContext
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

/**
 * @author eltons,  Date on 2018-12-05.
 */
@Service
class GrayTestServiceImpl @Autowired constructor(
    private val dslContext: DSLContext,
    private val grayTestDao: GrayTestDao
) : GrayTestService {
    override fun create(userId: String, grayTestInfo: GrayTestInfo): GrayTestInfo {
        return grayTestDao.create(dslContext, userId, grayTestInfo.server_id, grayTestInfo.status)
    }

    override fun update(userId: String, id: Long, grayTestInfo: GrayTestInfo) {
        grayTestDao.update(dslContext, userId, grayTestInfo.server_id, grayTestInfo.status, id)
    }

    override fun delete(id: Long) {
        grayTestDao.delete(dslContext, id)
    }

    override fun get(id: Long): GrayTestInfo {
        return grayTestDao.get(dslContext, id)
    }

    override fun listByUser(userId: String): List<GrayTestInfo> {
        return grayTestDao.listByUser(dslContext, userId)
    }

    // 以下两个实现，只有企业版才有，内部版没有。
    override fun listByCondition(userNameList: List<String>, serviceIdList: List<String>, statusList: List<String>, pageSize: Int, pageNum: Int): List<GrayTestListInfo> {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }


    override fun listAllUsers(): Map<String, List<Any>> {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.

    }
}