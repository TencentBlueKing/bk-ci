package com.tencent.bk.codecc.defect.cluster

import com.tencent.bk.codecc.defect.model.CCNDefectEntity
import com.tencent.bk.codecc.defect.pojo.AggregateDefectOutputModelV2
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

@Component("clusterCCNCompareProcess")
class ClusterCCNCompareProcess : AbstractClusterCompareProcess<CCNDefectEntity>() {

    companion object {
        private val logger = LoggerFactory.getLogger(ClusterCompareProcess::class.java)
    }

    /**
     * 聚类主方法
     * @param inputDefectList 输入告警清单
     * @param md5SameMap md5相同映射
     */
    @ExperimentalUnsignedTypes
    override fun clusterMethod(
        inputDefectList: List<CCNDefectEntity>,
        md5SameMap: MutableMap<String, Boolean>
    ): CopyOnWriteArrayList<AggregateDefectOutputModelV2<CCNDefectEntity>> {
        println("input defect size: ${inputDefectList.size}")
        val outputFileList = CopyOnWriteArrayList<AggregateDefectOutputModelV2<CCNDefectEntity>>()
        //2.根据文件名和规则名分组，不同的文件名和规则名不聚类在一起
        logger.info("start to distinct input defect list!")
        val inputDefectMap = inputDefectList.groupBy { it.filePath to it.functionName }
        val executor = getThreadPool(inputDefectMap.size)
        try {
            val lock = CountDownLatch(inputDefectMap.size)
            //4. 根据分组分别进行配置属性和并且利用线程池并行比较（**优化点）
            logger.info("start to config properties and compare!")
            inputDefectMap.forEach { (t, u) ->
                executor.execute {
                    try {
                        val filePath = t.first
                        val md5Same = md5SameMap[filePath]
                        val outputFileMap = if (null != md5Same && md5Same) {
                            u.groupBy { it.startLines }
                        } else {
                            configAndCompare(u)
                        }
                        outputFileList.addAll(outputFileMap.map {
                            AggregateDefectOutputModelV2(
                                defects = it.value
                            )
                        })
                    } catch (e: Exception) {
                        logger.info("config and compare elements fail! index is $t, msg is${e.message}")
                    } finally {
                        lock.countDown()
                    }
                }
            }
            //定时,超时2个小时自动超时
            lock.await(2, TimeUnit.HOURS)
        } catch (e: Exception) {
            logger.info("execute ccn cluster process fail!")
        } finally {
            executor.shutdownNow()
        }

        return outputFileList
    }

    override fun getPinpointHashMap(aggregateDefectInputList: List<CCNDefectEntity>): Map<String?, List<CCNDefectEntity>> {
        return aggregateDefectInputList.groupBy { it.pinpointHash }
    }
}
