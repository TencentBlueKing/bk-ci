package com.tencent.bk.codecc.quartz.strategy.router.impl

import com.tencent.bk.codecc.quartz.hash.FnvHashObj
import com.tencent.bk.codecc.quartz.model.JobInstanceEntity
import com.tencent.bk.codecc.quartz.pojo.OperationType
import com.tencent.bk.codecc.quartz.pojo.ShardInfo
import com.tencent.bk.codecc.quartz.pojo.ShardingResult
import com.tencent.bk.codecc.quartz.strategy.router.AbstractRouterStrategy
import com.tencent.devops.common.api.exception.CodeCCException
import com.tencent.devops.common.constant.CommonMessageCode
import java.util.*

class ConsistentHashRouterStrategy : AbstractRouterStrategy() {

    private val hashCircle = TreeMap<Int, ShardInfo>()

    //虚拟节点个数
    private val replicaNum = 8

    /**
     * 初始化分片信息及任务信息
     */
    override fun initJobInstances(
        shardingResult: ShardingResult?,
        jobInstances: List<JobInstanceEntity>
    ): List<JobInstanceEntity> {
        if (null == shardingResult) {
            return listOf()
        }
        initShardToCircle(shardingResult.shardList)
        //过滤出当前分片下的job实例
        return jobInstances.filter {
            val jobHashCode = getHashCode(it.jobName)
            val currentShardInfo = getShardForKey(jobHashCode)
            currentShardInfo?.tag ?: "" == shardingResult.currentShard.tag
        }
    }

    /**
     * 根据分片信息的变动
     */
    override fun rescheduleJobInstances(
        shardInfo: ShardInfo, shardingResult: ShardingResult,
        operType: OperationType, currentJobInstances: List<JobInstanceEntity>,
        totalJobInstances: List<JobInstanceEntity>
    ): List<JobInstanceEntity> {
        when (operType) {
            OperationType.ADD -> {
                addShardToCircle(shardInfo)
                return currentJobInstances.filter {
                    getShardForKey(getHashCode(it.jobName))?.tag == shardInfo.tag
                }
            }
            OperationType.REMOVE -> {
                val addJobInstance = totalJobInstances.filter {
                    getShardForKey(getHashCode(it.jobName))?.tag == shardInfo.tag
                }
                removeShardFromCircle(shardInfo)
                return addJobInstance.filter {
                    getShardForKey(getHashCode(it.jobName))?.tag == shardingResult.currentShard.tag
                }
            }
            else -> return listOf()
        }
    }

    /**
     * 添加去除job
     */
    override fun addOrRemoveJobInstance(
        shardingResult: ShardingResult?,
        jobInstance: JobInstanceEntity
    ): Boolean {
        if (null == shardingResult)
            return false
        return getShardForKey(getHashCode(jobInstance.jobName))?.tag == shardingResult.currentShard.tag
    }


    /**
     * 获取散列hash值,采用FNV1_32_HASH方法
     */
    private fun getHashCode(key: String): Int {
        return FnvHashObj(key).hashCode()
    }

    /**
     * 将各个分片加入到哈希环上
     */
    private fun initShardToCircle(shardInfoList: List<ShardInfo>) {
        shardInfoList.forEach {
            //加入虚拟节点
            for (i in 0 until replicaNum) {
                val hashCode = getHashCode("$i#${it.tag}")
                hashCircle[hashCode] = it
            }
        }
    }

    /**
     * 给哈希环添加分片
     */
    private fun addShardToCircle(shardInfo: ShardInfo) {
        for (i in 0 until replicaNum) {
            val hashCode = getHashCode("$i#${shardInfo.tag}")
            hashCircle[hashCode] = shardInfo
        }
    }

    /**
     * 给哈希环去除分片
     */
    private fun removeShardFromCircle(shardInfo: ShardInfo) {
        for (i in 0 until replicaNum) {
            val hashCode = getHashCode("$i#${shardInfo.tag}")
            hashCircle.remove(hashCode)
        }
    }

    /**
     * 获取对应hash值所属的分片,即顺时针数第一个分片
     */
    private fun getShardForKey(hashKey: Int): ShardInfo? {
        //hash环尚未填充
        return if (hashCircle.isEmpty())
            throw CodeCCException(CommonMessageCode.SCHEDULE_TASK_ERROR)
        else if (hashCircle.containsKey(hashKey))
            hashCircle[hashKey]
        else {
            val tailMap = hashCircle.tailMap(hashKey)
            if (tailMap.isEmpty()) hashCircle[hashCircle.firstKey()] else hashCircle[tailMap.firstKey()]
        }
    }
}