package com.tencent.devops.common.util

object LoopUtil {

    const val DEFAULT_THRESHOLD_COUNT = 10000  // 默认最大循环次数 10000
    const val DEFAULT_THRESHOLD_MILLS = 300 * 1000L // 最大循环耗时300s，每循环完成1次计算1次，超过此耗时立即直接返回
    const val SLEEP_INTERVAL = 5 * 1000L // 间隔5秒休息
    private const val SLEEP_MILLS = 5L // 强制休息5ms

    /**
     * 以下函数本意是解决从数据库全表拉改成循环方式按数据库表自增ID方式按升序通过[runFunc]函数加载
     * 注意，本函数并非线程安全，数据结构[LoopMetrics]和[vo]均非线程安全，如有多线程并发相同[vo]对象者，需要自行做并发控制
     *
     * [runFunc]函数会循环运行N次，循环次数取决如下：
     *
     * 函数接受数据库ID起始值（通常从0开始），并返回执行后的下一个数据库记录ID值，当返回的ID值与起始ID值相等则表示执行结束，不再继续循环执行
     *
     * 为防止[runFunc]函数返回值不当导致死循环，会限制循环次数达到N次调用或者耗时超过M毫秒都会提前结束后续循环，见[vo]定义
     * 所以调用该方法要自行判断是否真正完成循环，否则还需要再调用一次本方法
     * @return 返回整个循环中的度量数据
     */
    fun <ID, DATA> doLoop(vo: LoopVo<ID, DATA>, runFunc: (vo: LoopVo<ID, DATA>) -> Unit): LoopMetrics {
        vo.correctCtrlArgs() // 为保护系统，将错误的控制参数修正为默认值
        vo.finish = false // 重置状态
        val metrics = LoopMetrics(startTime = System.currentTimeMillis(), lastSleepTime = System.currentTimeMillis())
        do {
            // 防止因runFunc长时间循环导致CPU拉满, 当耗时累计达到强制休息间隔时，休息SLEEP_MILLS毫秒，并重新累计耗时
            metrics.sleepATime(vo.sleepIntervalMills)

            runFunc(vo) // 真正的业务逻辑由使用者传入

            metrics.calculateTotalTime()
            // ID不相等表示有拿到新数据，并且循环次数未超限以及 总共耗时小于循环的最大耗时
        } while (++metrics.loopCount < vo.thresholdCount && !vo.finish && metrics.totalTime < vo.thresholdMills)

        return metrics
    }

    /**
     * 参与循环定义的值对象，有循环控制参数和返回值
     * 注意：非线程安全
     */
    data class LoopVo<ID, DATA>(
        // 控制参数：代表要循环的ID，比如数据库表中的id字段，通常类型Long，由业务自定
        var id: ID,
        // 循环返回的数据，通常是一个List<xxx>结构，由业务自定, 如果没有要返回的值，则随便设置一个类型
        var data: DATA,
        // 控制参数：是否完成目标，由执行体runFunc自行决定判断是否完成，避免再次进入循环
        var finish: Boolean = false,
        // 控制参数：循环耗时ms时会退出，不一定精确相等一定是大于该值，差异取决于执行体runFunc耗时
        var thresholdMills: Long = DEFAULT_THRESHOLD_MILLS,
        // 控制参数：最大循环次数退出,默认1000次
        var thresholdCount: Int = DEFAULT_THRESHOLD_COUNT,
        // 控制参数：间隔多个ms会被休息100ms， 默认5000ms
        var sleepIntervalMills: Long = SLEEP_INTERVAL,
    ) {
        fun correctCtrlArgs() {
            if (thresholdCount <= 0 || thresholdCount > DEFAULT_THRESHOLD_COUNT) {
                thresholdCount = DEFAULT_THRESHOLD_COUNT
            }
            if (thresholdMills <= 0 || thresholdMills > DEFAULT_THRESHOLD_MILLS) {
                thresholdMills = DEFAULT_THRESHOLD_MILLS
            }
            if (sleepIntervalMills <= 0 || sleepIntervalMills > SLEEP_INTERVAL) {
                sleepIntervalMills = SLEEP_INTERVAL
            }
        }
    }

    /**
     * 循环的度量对象，非线程安全
     */
    data class LoopMetrics(
        var loopCount: Int = 0, // 一共循环次数
        var sleepCount: Int = 0, // 一共被强制休息次数
        var sleepTime: Long = 0, // 一共被强制休息耗时ms
        var totalTime: Long = 0, // 一共耗时
        val startTime: Long,
        var lastSleepTime: Long = 0, // 上一次休息后的时间
    ) {
        constructor(startTime: Long) : this(startTime = startTime, lastSleepTime = startTime)

        fun add(right: LoopMetrics) {
            this.loopCount += right.loopCount
            this.sleepCount += right.sleepCount
            this.sleepTime += right.sleepTime
            this.totalTime += right.totalTime
        }

        fun calculateTotalTime() {
            this.totalTime = System.currentTimeMillis() - this.startTime
        }

        /**
         * 将当前时间戳减去上一次休息时间戳所累计耗时达到强制休息间隔[sleepIntervalMills]时，休息SLEEP_MILLS毫秒，并重新累计耗时
         */
        fun sleepATime(sleepIntervalMills: Long): Boolean {
            return if ((System.currentTimeMillis() - lastSleepTime) >= sleepIntervalMills) {
                Thread.sleep(SLEEP_MILLS)
                sleepTime += SLEEP_MILLS // 累计休息时间 ms
                ++sleepCount // 累计休息次数
                lastSleepTime = System.currentTimeMillis() // 重置计时点
                true
            } else {
                false
            }
        }
    }
}
