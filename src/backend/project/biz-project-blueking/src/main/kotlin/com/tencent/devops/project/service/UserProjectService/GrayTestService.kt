package com.tencent.devops.project.service.UserProjectService

import com.tencent.devops.project.pojo.service.GrayTestInfo

/**
 * @author eltons,  Date on 2018-12-05.
 */

interface GrayTestService {
    fun create(userId: String, grayTestInfo: GrayTestInfo): GrayTestInfo
    fun update(userId: String, id: Long, grayTestInfo: GrayTestInfo)
    fun delete(id: Long)
    fun get(id: Long): GrayTestInfo
    fun listByUser(userId: String): List<GrayTestInfo>
}