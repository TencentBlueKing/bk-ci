package com.tencent.bk.codecc.defect.cluster

import com.tencent.bk.codecc.defect.pojo.AggregateDefectOutputModelV2
import com.tencent.bk.codecc.defect.pojo.FuzzyHashInfoModel
import org.slf4j.LoggerFactory
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

abstract class AbstractClusterCompareProcess<T> {

    companion object {
        private val logger = LoggerFactory.getLogger(AbstractClusterCompareProcess::class.java)
        private const val lowThreadLimit = 5000
        private const val lowThreadCount = 3
        private const val normalThreadLimit = 10000
        private const val normalThreadCount = 6
        private const val highThreadCount = 10
        private const val similarityThreshold = 85
    }

    /**
     * 聚类方法
     */
    abstract fun clusterMethod(
        inputDefectList: List<T>,
        md5SameMap: MutableMap<String, Boolean>
    ): CopyOnWriteArrayList<AggregateDefectOutputModelV2<T>>

    /**
     * 根据清单大小获取并发线程池
     */
    protected fun getThreadPool(inputListSize: Int): ExecutorService {
        //配置线程池及闭锁，线程池大小根据输入告警的size，经过测试确定
        val threadCount = when {
            inputListSize < lowThreadLimit -> lowThreadCount
            inputListSize < normalThreadLimit -> normalThreadCount
            else -> highThreadCount
        }
        logger.info("thread count: $threadCount")
        return Executors.newFixedThreadPool(threadCount)
    }


    /**
     * 对于每个输入元素，进行配置并进行比较
     * @param aggregateDefectInputList 按文件名和规则分组后，每组的告警入参信息值
     */
    @ExperimentalUnsignedTypes
    protected fun configAndCompare(aggregateDefectInputList: List<T>): Map<Int, List<T>> {
        var index = 0
        //1. 根据pinpointhash进行分组，保证不因为同样的pinpointhash重复比较
        val pinpointHashMap = getPinpointHashMap(aggregateDefectInputList)

        //2. 初始化并查集数组
        val unionFindClass = UnionFindClass<T>()
        unionFindClass.initArray(pinpointHashMap.size)

        //3. 开始逐个比较，相似的告警分在一组
        pinpointHashMap.forEach lit@{ t, u ->
            val fuzzyHashInfoModel =
                configNodeProperties(
                    u,
                    t,
                    index,
                    unionFindClass
                ) ?: return@lit
            for (i in 0 until index) {
                try {
                    val preElement = unionFindClass.getArrayElement(i) ?: continue
                    //校验两者在并查集中处的位置，如果两者已经是一类了，则不需要比较（**优化点）
                    if (unionFindClass.findRoot(i) == unionFindClass.findRoot(index)) {
                        continue
                    }
                    val preFuzzyHashInfoModel = preElement.fuzzyHashInfoModel ?: continue
                    if (FuzzyCompare.fuzzyCompare(
                            preFuzzyHashInfoModel,
                            fuzzyHashInfoModel
                        ) >= similarityThreshold
                    ) {
                        unionFindClass.unionCollection(i, index)
                    }
                } catch (e: Exception) {
                    logger.info("compare pinpoint hash fail! continue to next")
                }
            }
            index++
        }

        //4. 对于比较结果，进行输出
        val outputFileMap = mutableMapOf<Int, MutableList<T>>()
        for (i in aggregateDefectInputList.indices) {
            val unionFindNodeInfo = unionFindClass.getArrayElement(i) ?: continue
            if (null == unionFindNodeInfo.aggregateDefectInputModel) {
                continue
            }
            //如果是根节点，则用自身的
            if (unionFindNodeInfo.parentIndex < 0) {
                outputFileMap.compute(i) { _, u ->
                    if (u.isNullOrEmpty()) {
                        unionFindNodeInfo.aggregateDefectInputModel!!.toMutableList()
                    } else {
                        u.addAll(unionFindNodeInfo.aggregateDefectInputModel!!)
                        u
                    }
                }
            } else {
                outputFileMap.compute(unionFindClass.findRoot(i)) { _, u ->
                    if (u.isNullOrEmpty()) {
                        unionFindNodeInfo.aggregateDefectInputModel!!.toMutableList()
                    } else {
                        u.addAll(unionFindNodeInfo.aggregateDefectInputModel!!)
                        u
                    }
                }
            }
        }
        return outputFileMap
    }

    /**
     * 获取特征值映射方法，每个类型方法不一致
     */
    abstract fun getPinpointHashMap(aggregateDefectInputList: List<T>): Map<String?, List<T>>

    /**
     * 对于每个告警进行配置属性，配置好的属性放进并查集数组的字段中
     * @param aggregateDefectInputModel 按特征hash值分组后的告警清单
     * @param pinpointHash 该分组的特征hash值
     * @param index 该分组的索引值
     * @param unionFindClass 并查集工具类
     */
    @ExperimentalUnsignedTypes
    private fun configNodeProperties(
        aggregateDefectInputModel: List<T>,
        pinpointHash: String?,
        index: Int,
        unionFindClass: UnionFindClass<T>
    ): FuzzyHashInfoModel? {
        val unionFindNodeInfo = unionFindClass.getArrayElement(index) ?: return null
        unionFindNodeInfo.aggregateDefectInputModel = aggregateDefectInputModel
        unionFindNodeInfo.pinpointHash = pinpointHash
        val emptyFuzzyHashInfoModel = FuzzyHashInfoModel(null, null, null, null, null, null, null, false)
        //如果hash为空，则置为false
        if (pinpointHash.isNullOrBlank()) {
            logger.info("pinpoint hash is empty!")
            unionFindNodeInfo.fuzzyHashInfoModel = emptyFuzzyHashInfoModel
            return unionFindNodeInfo.fuzzyHashInfoModel!!
        }
        //如果hash格式不对，则置为false
        if (!pinpointHash.contains(":")) {
            logger.info("str lack of :")
            unionFindNodeInfo.fuzzyHashInfoModel = emptyFuzzyHashInfoModel
            return unionFindNodeInfo.fuzzyHashInfoModel!!
        }
        //如果获取blockSize失败，则置为false
        val blockSize = try {
            pinpointHash.substringBefore(":").toULong()
        } catch (e: Exception) {
            logger.info("get block size fail!, message: ${e.message}", e)
            unionFindNodeInfo.fuzzyHashInfoModel = emptyFuzzyHashInfoModel
            return unionFindNodeInfo.fuzzyHashInfoModel!!
        }
        //如果获取hash字符串失败，则置为false
        val hashStr = try {
            pinpointHash.substringAfter(":")
        } catch (e: Exception) {
            logger.info("get hash str fail!, message: ${e.message}", e)
            unionFindNodeInfo.fuzzyHashInfoModel = emptyFuzzyHashInfoModel
            return unionFindNodeInfo.fuzzyHashInfoModel!!
        }
        if(hashStr.isBlank()){
            logger.info("hash str is empty!")
            unionFindNodeInfo.fuzzyHashInfoModel = emptyFuzzyHashInfoModel
            return unionFindNodeInfo.fuzzyHashInfoModel!!
        }
        //消除连续重复字符串
        val hashResult = FuzzyCompare.eliminateDupSequence(hashStr)
        val hashStrArray = hashResult.split(":")
        if (hashStrArray.size >= 3) {
            logger.info("more than 1 : found in str")
            unionFindNodeInfo.fuzzyHashInfoModel = emptyFuzzyHashInfoModel
            return unionFindNodeInfo.fuzzyHashInfoModel!!
        }
        //处理hash值拆分的字符串，不合规的字符串则置为false
        val strb1 = hashStrArray[0]
        if (strb1.isBlank()) {
            logger.info("block 1 str is blank!")
            unionFindNodeInfo.fuzzyHashInfoModel = emptyFuzzyHashInfoModel
            return unionFindNodeInfo.fuzzyHashInfoModel!!
        }
        val strb1Length = strb1.length
        val strb2 = hashStrArray[1]
        if (strb2.isBlank()) {
            logger.info("block 2 str is blank!")
            unionFindNodeInfo.fuzzyHashInfoModel = emptyFuzzyHashInfoModel
            return unionFindNodeInfo.fuzzyHashInfoModel!!
        }
        val strb2Length = strb2.length
        val fuzzyHashInfoModel = FuzzyHashInfoModel(
            blockSize = blockSize,
            b1 = strb1,
            b1Length = strb1Length,
            b1ParArray = null,
            b2 = strb2,
            b2Length = strb2Length,
            b2ParArray = null,
            valid = true
        )
        unionFindNodeInfo.fuzzyHashInfoModel = fuzzyHashInfoModel
        return fuzzyHashInfoModel
    }
}
