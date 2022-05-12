package com.tencent.bk.codecc.defect.cluster

import com.fasterxml.jackson.core.type.TypeReference
import com.tencent.bk.codecc.defect.pojo.AggregateDefectInputModel
import com.tencent.bk.codecc.defect.pojo.AggregateDefectOutputModel
import com.tencent.bk.codecc.defect.pojo.AggregateDispatchFileName
import com.tencent.bk.codecc.defect.pojo.FuzzyHashInfoModel
import com.tencent.devops.common.api.codecc.util.JsonUtil
import org.slf4j.LoggerFactory
import java.io.File
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

/**
 * 告警跟踪聚类工具，基于ssdeep文本相似度比较原理，将相似的告警归于一类，从而将前后两次扫描上报的相似告警归并为同一个告警，
 * 达到告警全生命周期跟踪的效果，跟踪流程如下（ssdeep原理可参考论文https://www.sciencedirect.com/science/article/pii/S1742287606000764?via%3Dihub）：
 * 1.工具侧客户端根据每个告警对应的代码文本生成告警文本特征值（后续的pinpointHash字段，特征值用两个冒号分为了三部分，样例 -- 3:haKIlJuRoGKhcdJy6A+Eyyct6NMfE7oc2iP3RPqgdXQ2xHct0:hpiGKhcjXEyyct9EUOPJXQlt0）
 * 2.工具将本次扫描的告警特征值上报至服务端，服务端取出原有存量告警，与本次上报告警特征值进行比较，利用并查集归纳比较结果，并进行归并处理入库
 *
 * 该类主要针对第2步中的大规模告警特征值比较进行了优化，耗时较原有比较方法减少一半以上，优化点包括：
 * -- 计算莱温斯坦距离时采用位并行算法（时间复杂度O(ceil(m/w) * n)）代替传统动态归纳方法O(n2)
 * -- 缓存位并行算法的位标识数组，不用每次比较时重复处理特征值字符串，节约时间
 * -- 根据文件路径及规则分组，进行并行比较节约耗时
 * -- 比较前校验并查集的分组情况，如果两个待比较告警已经在同一组了，则可以略过本次比较，继续往下进行
 * -- 拥有相同特征hash值的告警归为一组，统一与其他告警进行比较，避免重复比较造成的浪费
 */
object ClusterCompareProcess {

    private val logger = LoggerFactory.getLogger(ClusterCompareProcess::class.java)
    private const val lowThreadLimit = 5000
    private const val lowThreadCount = 3
    private const val normalThreadLimit = 10000
    private const val normalThreadCount = 6
    private const val highThreadCount = 10
    private const val similarityThreshold = 85

    /**
     * 聚类主方法
     * @param aggregateDispatchModel 标识输入文件名和输出文件名
     */
    @ExperimentalUnsignedTypes
    fun clusterMethod(aggregateDispatchModel: AggregateDispatchFileName) {
        logger.info("cluster method begin! input file name: ${aggregateDispatchModel.inputFileName}, output file name: ${aggregateDispatchModel.outputFileName}")
        val startTime = System.currentTimeMillis()
        //1.读取输入文件内容
        val inputFileName = aggregateDispatchModel.inputFileName

        val inputFile = File(inputFileName)
        var inputDefects =
            JsonUtil.to(inputFile.readText(), object : TypeReference<List<AggregateDefectInputModel>>() {})
        if (inputDefects.isNullOrEmpty()) {
            logger.info("input defect is empty!")
            return
        }
        println("input defect size: ${inputDefects.size}")
        inputDefects = inputDefects.distinctBy { it.id }
        val outputFileList = CopyOnWriteArrayList<AggregateDefectOutputModel>()
        //2.根据文件名和规则名分组，不同的文件名和规则名不聚类在一起
        logger.info("start to distinct input defect list!")
        val inputDefectMap = inputDefects.groupBy { "${it.filePath}_${it.checkerName}" }

        //3.配置线程池及闭锁，线程池大小根据输入告警的size，经过测试确定
        val threadCount = when {
            inputDefectMap.size < lowThreadLimit -> lowThreadCount
            inputDefectMap.size < normalThreadLimit -> normalThreadCount
            else -> highThreadCount
        }
        logger.info("thread count: $threadCount")
        val executor = Executors.newFixedThreadPool(threadCount)
        val lock = CountDownLatch(inputDefectMap.size)

        //4. 根据分组分别进行配置属性和并且利用线程池并行比较（**优化点）
        logger.info("start to config properties and compare!")
        inputDefectMap.forEach { (t, u) ->
            executor.execute {
                try {
                    val outputFileMap = configAndCompare(u)
                    outputFileList.addAll(outputFileMap.map {
                        AggregateDefectOutputModel(
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
        //输出结果至输出文件
        val outputFile = File(aggregateDispatchModel.outputFileName)
        if (outputFile.exists()) {
            outputFile.delete()
        }
        outputFile.writeText(JsonUtil.toJson(outputFileList))
        logger.info("finish cluster! total time cost: ${System.currentTimeMillis() - startTime}")
    }


    /**
     * 对于每个输入元素，进行配置并进行比较
     * @param aggregateDefectInputList 按文件名和规则分组后，每组的告警入参信息值
     */
    @ExperimentalUnsignedTypes
    private fun configAndCompare(aggregateDefectInputList: List<AggregateDefectInputModel>): Map<Int, MutableList<AggregateDefectInputModel>> {
        var index = 0
        //1. 根据pinpointhash进行分组，保证不因为同样的pinpointhash重复比较
        val pinpointHashMap = aggregateDefectInputList.groupBy { it.pinpointHash }

        //2. 初始化并查集数组
        val unionFindClass = UnionFindClass<AggregateDefectInputModel>()
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
        val outputFileMap = mutableMapOf<Int, MutableList<AggregateDefectInputModel>>()
        for (i in 0 until aggregateDefectInputList.size) {
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
     * 对于每个告警进行配置属性，配置好的属性放进并查集数组的字段中
     * @param aggregateDefectInputModel 按特征hash值分组后的告警清单
     * @param pinpointHash 该分组的特征hash值
     * @param index 该分组的索引值
     * @param unionFindClass 并查集工具类
     */
    @ExperimentalUnsignedTypes
    private fun configNodeProperties(
        aggregateDefectInputModel: List<AggregateDefectInputModel>,
        pinpointHash: String,
        index: Int,
        unionFindClass: UnionFindClass<AggregateDefectInputModel>
    ): FuzzyHashInfoModel? {
        val unionFindNodeInfo = unionFindClass.getArrayElement(index) ?: return null
        unionFindNodeInfo.aggregateDefectInputModel = aggregateDefectInputModel
        unionFindNodeInfo.pinpointHash = pinpointHash
        val emptyFuzzyHashInfoModel = FuzzyHashInfoModel(null, null, null, null, null, null, null, false)
        //如果hash为空，则置为false
        if (pinpointHash.isBlank()) {
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
            logger.error("get block size fail!, message: ${e.message}", e)
            unionFindNodeInfo.fuzzyHashInfoModel = emptyFuzzyHashInfoModel
            return unionFindNodeInfo.fuzzyHashInfoModel!!
        }
        //如果获取hash字符串失败，则置为false
        val hashStr = try {
            pinpointHash.substringAfter(":")
        } catch (e: Exception) {
            logger.error("get hash str fail!, message: ${e.message}", e)
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