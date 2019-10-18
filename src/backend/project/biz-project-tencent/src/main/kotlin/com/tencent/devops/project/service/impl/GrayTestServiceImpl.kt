package com.tencent.devops.project.service.impl

import com.tencent.devops.project.dao.GrayTestDao
import com.tencent.devops.project.dao.ServiceDao
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
    private val grayTestDao: GrayTestDao,
    private val serviceDao: ServiceDao
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
        val notNullUsers = userNameList.filterNot { it == "" }
        val notNullIds = serviceIdList.filterNot { it == "" }
        val notNullStatus = statusList.filterNot { it == "" }

        val grayList =
                grayTestDao.listByCondition(dslContext, notNullUsers, notNullIds, notNullStatus, pageSize, pageNum)
        val totalRecord = grayTestDao.getSum(dslContext)
        val serviceList = serviceDao.getServiceList(dslContext)
        return grayList.map {
            val server = serviceList.filter { it2 -> it.server_id == it2.id }[0]
            GrayTestListInfo(it.id, it.server_id, server.name, it.userName, it.status, totalRecord)
        }
    }


    override fun listAllUsers(): Map<String, List<Any>> {
        val allUsers = grayTestDao.listAllUsers(dslContext)
        val allService = grayTestDao.listAllService(dslContext)
        val map = HashMap<String, List<Any>>()
        map.put("users", allUsers)
        map.put("services", allService)
        return map
    }
}