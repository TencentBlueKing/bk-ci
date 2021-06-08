package com.tencent.bk.codecc.defect.service

import com.tencent.devops.common.api.clusterresult.BaseClusterResultVO

/**
 * 告警数据聚类接口：
 * 扫描完成后根据 cluster 方法对工具分类并聚类告警
 */
interface ClusterDefectService {
    /**
     * 告警聚类统计，按照告警类型计算当前构建的告警数
     *
     * @param taskId
     * @param buildId
     * @param toolList 当前分类的工具集合
     */
    fun cluster(taskId: Long, buildId: String, toolList: List<String>)

    /**
     * 获取指定构建的聚类结果
     *
     * @param taskId
     * @param buildId
     */
    fun getClusterStatistic(taskId: Long, buildId: String): BaseClusterResultVO
}
