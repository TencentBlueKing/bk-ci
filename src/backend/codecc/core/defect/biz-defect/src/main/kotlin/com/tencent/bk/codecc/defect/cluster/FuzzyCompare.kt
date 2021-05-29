package com.tencent.bk.codecc.defect.cluster

import com.tencent.bk.codecc.defect.pojo.FuzzyHashInfoModel
import org.slf4j.LoggerFactory
import kotlin.math.max
import kotlin.math.min

/**
 * 模糊匹配工具类，用于文本相似度匹配
 */
object FuzzyCompare {

    private val logger = LoggerFactory.getLogger(FuzzyCompare::class.java)
    private const val SPAMSUM_LENGTH = 64
    private const val ROLLING_WINDOW = 7
    private const val MIN_BLOCKSIZE = 3

    /**
     * 特征哈希值比较入口主方法
     * @param fuzzyHashInfoModel1 第一个哈希值经处理后的信息对象，包括blocksize，一段和二段哈希值字符串，一段和二段哈希值并行处理映射
     * @param fuzzyHashInfoModel2 第二个哈希值经处理后的信息对象
     */
    @ExperimentalUnsignedTypes
    fun fuzzyCompare(
        fuzzyHashInfoModel1: FuzzyHashInfoModel,
        fuzzyHashInfoModel2: FuzzyHashInfoModel
    ): Int {
        //校验参数，如果不符合规范的直接返回0相似度
        if ((null != fuzzyHashInfoModel1.valid && !fuzzyHashInfoModel1.valid) ||
            (null != fuzzyHashInfoModel2.valid && !fuzzyHashInfoModel2.valid)
        ) {
            logger.info("one of the input param is false!")
            return 0
        }
        val blockSize1: ULong = fuzzyHashInfoModel1.blockSize ?: return 0
        val blockSize2: ULong = fuzzyHashInfoModel2.blockSize ?: return 0

        //如果分片长度不对，则直接返回0
        if (blockSize1 != blockSize2 && (blockSize1 * 2UL != blockSize2 || blockSize1 > ULong.MAX_VALUE / 2UL) &&
            (blockSize1 / 2UL != blockSize2 || blockSize1 or 1UL == 1UL)
        ) {
            return 0
        }
        val s1b1 = fuzzyHashInfoModel1.b1 ?: return 0
        val s1b1Length = fuzzyHashInfoModel1.b1Length ?: return 0
        val s1b2 = fuzzyHashInfoModel1.b2 ?: return 0
        val s1b2Length = fuzzyHashInfoModel1.b2Length ?: return 0
        val s2b1 = fuzzyHashInfoModel2.b1 ?: return 0
        val s2b1Length = fuzzyHashInfoModel2.b1Length ?: return 0
        val s2b2 = fuzzyHashInfoModel2.b2 ?: return 0
        val s2b2Length = fuzzyHashInfoModel2.b2Length ?: return 0

        //如果输入的两个字符串完全相同，则返回100相似度
        if (blockSize1 == blockSize2 && s1b1 == s2b1 && s1b2 == s2b2) {
            return 100
        }

        val score: Int

        //按照输入的两个特征值的blocksize不同的场景进行比较，算出相似度
        if (blockSize1 < ULong.MAX_VALUE / 2UL) {
            score = when {
                blockSize1 == blockSize2 -> {
                    //缓存位并行运算的数组值，防止每次比较时都要对字符串进行处理（**优化点）
                    var s1b1ParArray = fuzzyHashInfoModel1.b1ParArray
                    if (s1b1ParArray.isNullOrEmpty()) {
                        s1b1ParArray = mutableMapOf()
                        s1b1.forEachIndexed { t, c ->
                            s1b1ParArray!!.compute(c) { _, v ->
                                (v ?: 0UL) or (1UL shl t)
                            }
                        }
                    }
                    fuzzyHashInfoModel1.b1ParArray = s1b1ParArray

                    var s1b2ParArray = fuzzyHashInfoModel1.b2ParArray
                    if (s1b2ParArray.isNullOrEmpty()) {
                        s1b2ParArray = mutableMapOf()
                        s1b2.forEachIndexed { t, c ->
                            s1b2ParArray.compute(c) { _, v ->
                                (v ?: 0UL) or (1UL shl t)
                            }
                        }
                    }
                    fuzzyHashInfoModel1.b2ParArray = s1b2ParArray
                    val score1 = scoreStrings(
                        s1b1Length,
                        s1b1ParArray,
                        s2b1,
                        s2b1Length,
                        blockSize1
                    )
                    val score2 = scoreStrings(
                        s1b2Length,
                        s1b2ParArray,
                        s2b2,
                        s2b2Length,
                        blockSize1 * 2UL
                    )
                    max(score1, score2)
                }
                blockSize1 * 2UL == blockSize2 -> {
                    var s2b1ParArray = fuzzyHashInfoModel2.b1ParArray
                    if (s2b1ParArray.isNullOrEmpty()) {
                        s2b1ParArray = mutableMapOf()
                        s2b1.forEachIndexed { t, c ->
                            s2b1ParArray.compute(c) { _, v ->
                                (v ?: 0UL) or (1UL shl t)
                            }
                        }
                    }
                    fuzzyHashInfoModel2.b1ParArray = s2b1ParArray
                    scoreStrings(
                        s2b1Length,
                        s2b1ParArray,
                        s1b2,
                        s1b2Length,
                        blockSize2
                    )
                }
                else -> {
                    var s1b1ParArray = fuzzyHashInfoModel1.b1ParArray
                    if (s1b1ParArray.isNullOrEmpty()) {
                        s1b1ParArray = mutableMapOf()
                        s1b1.forEachIndexed { t, c ->
                            s1b1ParArray!!.compute(c) { _, v ->
                                (v ?: 0UL) or (1UL shl t)
                            }
                        }
                    }
                    fuzzyHashInfoModel1.b1ParArray = s1b1ParArray
                    scoreStrings(
                        s1b1Length,
                        s1b1ParArray,
                        s2b2,
                        s2b2Length,
                        blockSize1
                    )
                }
            }
        } else {
            score = if (blockSize1 == blockSize2) {
                var s1b1ParArray = fuzzyHashInfoModel1.b1ParArray
                if (s1b1ParArray.isNullOrEmpty()) {
                    s1b1ParArray = mutableMapOf()
                    s1b1.forEachIndexed { t, c ->
                        s1b1ParArray!!.compute(c) { _, v ->
                            (v ?: 0UL) or (1UL shl t)
                        }
                    }
                }
                fuzzyHashInfoModel1.b1ParArray = s1b1ParArray
                scoreStrings(
                    s1b1Length,
                    s1b1ParArray,
                    s2b1,
                    s2b1Length,
                    blockSize1
                )
            } else if (blockSize1 % 2UL == 0UL && blockSize1 / 2UL == blockSize2) {
                var s1b1ParArray = fuzzyHashInfoModel1.b1ParArray
                if (s1b1ParArray.isNullOrEmpty()) {
                    s1b1ParArray = mutableMapOf()
                    s1b1.forEachIndexed { t, c ->
                        s1b1ParArray.compute(c) { _, v ->
                            (v ?: 0UL) or (1UL shl t)
                        }
                    }
                }
                fuzzyHashInfoModel1.b1ParArray = s1b1ParArray
                scoreStrings(
                    s1b1Length,
                    s1b1ParArray,
                    s2b2,
                    s2b2Length,
                    blockSize1
                )
            } else {
                0
            }
        }
        return score
    }

    /**
     * 计算两个字符串模糊哈希的编辑距离（采用gene myers位并行算法代替传统动态归纳方法 **优化点）
     * 原理可参考论文 《Explaining and Extending the Bit-parallel Approximate String Matching Algorithm of Myers》
     * @param pMap 字符串处理后的位并行映射
     * @param s1Length 第一个字符串长度
     * @param s2 第二个字符串
     * @param s2Length 第二个字符串的长度
     */
    @ExperimentalUnsignedTypes
    private fun editDistancePa(
        pMap: MutableMap<Char, ULong>,
        s1Length: Int,
        s2: String,
        s2Length: Int
    ): Int {
        var pv = ULong.MAX_VALUE
        var nv: ULong = 0UL
        var ph: ULong
        var nh: ULong
        var zd: ULong
        var mt: ULong
        var x: ULong
        var y: ULong
        val msb: ULong = 1UL shl (s1Length - 1)
        var cur = s1Length
        for (i in 0 until s2Length) {
            mt = pMap[s2[i]] ?: 0UL
            zd = (((mt and pv) + pv) xor pv) or mt or nv
            nh = pv and zd
            if ((nh and msb) != 0UL) {
                --cur
            }
            x = nv or (pv or zd).inv() or (pv and mt.inv() and 1UL)
            y = (pv - nh) shr 1
            ph = (x + y) xor y
            if ((ph and msb) != 0UL) {
                ++cur
            }
            x = (ph shl 1) or 1UL
            nv = x and zd
            pv = (nh shl 1) or (x or zd).inv() or (x and (pv - nh))
        }
        return cur
    }

    /**
     * 消除连续3个以上的重复字符（3个以上连续字符会影响编辑距离的比较结果）
     * @param inputStr 输入字符串
     */
    fun eliminateDupSequence(inputStr: String): String {
        val dupPair: MutableMap<Char, Long> = mutableMapOf()
        var result = ""
        inputStr.forEach {
            if (dupPair.isNullOrEmpty()) {
                dupPair[it] = 1L
                result = result.plus(it.toString())
            } else {
                if (dupPair.containsKey(it)) {
                    dupPair[it] = dupPair[it]!! + 1L
                    if (dupPair[it]!! <= 3L) {
                        result = result.plus(it.toString())
                    }
                } else {
                    dupPair.clear()
                    dupPair[it] = 1L
                    result = result.plus(it.toString())
                }
            }
        }
        return result
    }

    /**
     * 利用位并行方法判断是否有连续相同的字符串
     * （如果两比较字符串连连续相同字符串都不存在，则不需要比较了），用于提高比较效率
     * @param parArray 第一个字符串经过位并行处理后的映射
     * @param s2 第二个字符串内容
     * @param s2Length 第二个字符串长度
     */
    @ExperimentalUnsignedTypes
    private fun hasCommonStringPa(
        parArray: MutableMap<Char, ULong>,
        s2: String,
        s2Length: Int
    ): Boolean {
        var D: ULong
        var r = ROLLING_WINDOW - 1
        var l: Int
        var ch: Char
        while (r < s2Length) {
            l = r - (ROLLING_WINDOW - 1)
            var originalPos = s2Length - 1 - r
            ch = s2[originalPos]
            D = parArray[ch] ?: 0UL
            while (D != 0UL) {
                r--
                originalPos++
                D = (D shl 1) and (parArray[s2[originalPos]] ?: 0UL)
                if (r == l && D != 0UL) {
                    return true
                }
            }
            r += ROLLING_WINDOW
        }
        return false
    }

    /**
     * 相似度计分
     * @param s1Length 第一个字符串的长度
     * @param s1ParArray 第一个字符串经过位并行处理的映射
     * @param s2 第二个字符串
     * @param s2Length 第二个字符串的长度
     * @param blockSize 特征哈希值的的长度标识
     */
    @ExperimentalUnsignedTypes
    private fun scoreStrings(
        s1Length: Int,
        s1ParArray: MutableMap<Char, ULong>,
        s2: String,
        s2Length: Int,
        blockSize: ULong
    ): Int {
        var score: Int
        if (s1Length < ROLLING_WINDOW) {
            return 0
        }
        if (s2Length < ROLLING_WINDOW) {
            return 0
        }

        if (!hasCommonStringPa(s1ParArray, s2, s2Length)) {
            return 0
        }
        score = editDistancePa(s1ParArray, s1Length, s2, s2Length)
        //取相对编辑长度
        score = (score * SPAMSUM_LENGTH) / (s1Length + s2Length)
        //scale到 0 至100范围
        score = (100 * score) / SPAMSUM_LENGTH
        score = 100 - score
        if (blockSize >= ((99 + ROLLING_WINDOW) / ROLLING_WINDOW * MIN_BLOCKSIZE).toUInt()) {
            return score
        }
        if (score > blockSize.toLong() / MIN_BLOCKSIZE * min(s1Length, s2Length)) {
            score = blockSize.toInt() / MIN_BLOCKSIZE * min(s1Length, s2Length)
        }
        return score
    }
}