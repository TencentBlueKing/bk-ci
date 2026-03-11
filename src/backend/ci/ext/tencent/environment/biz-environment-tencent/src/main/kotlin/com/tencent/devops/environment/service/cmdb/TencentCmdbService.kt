package com.tencent.devops.environment.service.cmdb

import com.tencent.devops.environment.pojo.cmdb.common.CmdbServerDTO

/**
 * 对接公司CMDB接口，提供服务器信息查询服务
 */
interface TencentCmdbService {
    /**
     * 根据ServerId批量查询服务器信息
     * @param serverIdSet ServerId集合
     */
    fun queryServerByServerId(serverIdSet: Set<Long>): Map<Long, CmdbServerDTO>

    /**
     * 根据IP批量查询服务器信息
     * @param ipSet IP集合
     */
    fun queryServerByIp(ipSet: Set<String>): Map<String, CmdbServerDTO>
}
