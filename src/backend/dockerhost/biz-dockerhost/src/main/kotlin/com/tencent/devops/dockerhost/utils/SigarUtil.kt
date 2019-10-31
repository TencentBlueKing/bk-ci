package com.tencent.devops.dockerhost.utils

import org.hyperic.sigar.Sigar
import java.util.ArrayDeque
import kotlin.math.roundToInt

object SigarUtil {

    private const val MAX_MEM = 90

    private const val MAX_CPU = 90

    private val memQueue = ArrayDeque<Int>()

    private val cpuQueue = ArrayDeque<Int>()

    private const val queueMaxSize = 10

    private var queueMemValueSum = 0

    private var queueCpuValueSum = 0

    fun loadEnable(): Boolean {
        return try {
            val averageMemLoad = queueMemValueSum / memQueue.size
            averageMemLoad < MAX_MEM
        } catch (e: Exception) {
            true
        }
    }

    fun getAverageMemLoad(): Int {
        return try {
            queueMemValueSum / memQueue.size
        } catch (e: Exception) {
            0
        }
    }

    fun getAverageCpuLoad(): Int {
        return try {
            queueCpuValueSum / cpuQueue.size
        } catch (e: Exception) {
            0
        }
    }

    fun pushMem() {
        val element = getMemUsedPercent()
        if (memQueue.size >= queueMaxSize) {
            queueMemValueSum -= memQueue.pollLast()
        }

        memQueue.push(element)
        queueMemValueSum += element
    }

    fun pushCpu() {
        val element = getCpuUsedPercent()
        if (cpuQueue.size >= queueMaxSize) {
            queueCpuValueSum -= cpuQueue.pollLast()
        }

        cpuQueue.push(element)
        queueCpuValueSum += element
    }

    fun getMemQueue(): ArrayDeque<Int> {
        return memQueue
    }

    private fun getMemUsedPercent(): Int {
        val sigar = Sigar()
        val mem = sigar.mem
        val element = (mem.usedPercent).roundToInt()
        return if (element in 0..100) {
            element
        } else {
            0
        }
    }

    private fun getCpuUsedPercent(): Int {
        val sigar = Sigar()
        val cpu = sigar.cpu
        val element = (cpu.idle / cpu.total * 100).toInt()
        return if (element in 0..100) {
            element
        } else {
            0
        }
    }
}
