/*
 * Tencent is pleased to support the open source community by making BK-CI 蓝鲸持续集成平台 available.
 *
 * Copyright (C) 2020 THL A29 Limited, a Tencent company.  All rights reserved.
 *
 * BK-CI 蓝鲸持续集成平台 is licensed under the MIT license.
 *
 * A copy of the MIT License is included in this file.
 *
 *
 * Terms of the MIT License:
 * ---------------------------------------------------
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package com.tencent.bkrepo.rds.utils

import com.tencent.bkrepo.common.api.util.readYamlString
import com.tencent.bkrepo.common.artifact.api.ArtifactFile
import com.tencent.bkrepo.rds.constants.TGZ_SUFFIX
import com.tencent.bkrepo.rds.exception.RdsFileNotFoundException
import com.tencent.bkrepo.rds.pojo.metadata.RdsChartMetadata
import com.tencent.bkrepo.rds.pojo.metadata.RdsIndexYamlMetadata
import com.tencent.bkrepo.rds.utils.DecompressUtil.getArchivesContent
import java.io.InputStream
import java.time.LocalDateTime
import java.util.SortedSet

object ChartParserUtil {

    fun parseChartFileInfo(artifactFile: ArtifactFile, extension: String = TGZ_SUFFIX): RdsChartMetadata {
        val inputStream = artifactFile.getInputStream()
        return parseChartInputStream(inputStream, extension)
    }

    private fun parseChartInputStream(inputStream: InputStream, extension: String): RdsChartMetadata {
        val result = inputStream.getArchivesContent(extension)
        val rdsChartMetadata: RdsChartMetadata = result.byteInputStream().readYamlString()
        rdsChartMetadata.extension = extension
        return rdsChartMetadata
    }

    /**
     * 将新增加的Chart包信息加入到index.yaml中
     */
    fun addIndexEntries(indexYamlMetadata: RdsIndexYamlMetadata, chartMetadata: RdsChartMetadata) {
        val chartName = chartMetadata.code
        val chartVersion = chartMetadata.version
        val isFirstChart = !indexYamlMetadata.entries.containsKey(chartMetadata.code)
        indexYamlMetadata.entries.let {
            if (isFirstChart) {
                it[chartMetadata.code] = sortedSetOf(chartMetadata)
            } else {
                // force upload
                run stop@{
                    it[chartName]?.forEachIndexed { _, rdsChartMetadata ->
                        if (chartVersion == rdsChartMetadata.version) {
                            it[chartName]?.remove(rdsChartMetadata)
                            return@stop
                        }
                    }
                }
                it[chartName]?.add(chartMetadata)
            }
        }
    }

    /**
     * 根据创建时间过滤chart信息
     */
    fun filterByCreateTime(
        indexYamlMetadata: RdsIndexYamlMetadata,
        startTime: LocalDateTime = LocalDateTime.MIN
    ): Map<String, SortedSet<RdsChartMetadata>> {
        with(indexYamlMetadata) {
            when (startTime) {
                LocalDateTime.MIN -> entries
                else -> {
                    val nonMatchingPredicate: (Int, RdsChartMetadata) -> Boolean =
                        { _, it -> compareTime(startTime, it.created) }
                    entries.values.forEachIndexed { _, list ->
                        list.removeAll(list.filterIndexed(nonMatchingPredicate))
                    }
                }
            }
            return convertUtcTime(entries)
        }
    }

    /**
     * 比较两个时间大小
     */
    private fun compareTime(startTime: LocalDateTime, createTime: String?): Boolean {
        createTime?.let {
            return startTime.isAfter(TimeFormatUtil.convertToLocalTime(it))
        }
        return false
    }

    /**
     * 根据名字从index.yaml中找出对应chart信息，可能会包含多个版本
     */
    fun filterChart(
        indexYamlMetadata: RdsIndexYamlMetadata,
        startTime: LocalDateTime = LocalDateTime.MIN,
        name: String,
        version: String? = null
    ): Any {
        val chartList = indexYamlMetadata.entries[name]
        chartList?.let {
            when (startTime) {
                LocalDateTime.MIN -> chartList
                else -> {
                    val nonMatchingPredicate: (Int, RdsChartMetadata) -> Boolean =
                        { _, chart -> compareTime(startTime, chart.created) }
                    chartList.removeAll(chartList.filterIndexed(nonMatchingPredicate))
                }
            }
            version?.let {
                return filterByVersion(chartList, version, startTime)
            }
            chartList.forEach { convertUtcTime(it) }
        }
        return chartList ?: throw RdsFileNotFoundException("chart not found")
    }

    /**
     * 根据名字-版本从index.yaml中找出对应chart信息
     */
    private fun filterByVersion(
        chartList: SortedSet<RdsChartMetadata>,
        chartVersion: String,
        startTime: LocalDateTime = LocalDateTime.MIN
    ): RdsChartMetadata {
        val helmChartMetadataList = chartList.filter {
            chartVersion == it.version
        }.toList()
        return if (helmChartMetadataList.isNotEmpty()) {
            require(helmChartMetadataList.size == 1) {
                "find more than one version [$chartVersion] in package."
            }
            when (startTime) {
                LocalDateTime.MIN -> convertUtcTime(helmChartMetadataList.first())
                else -> {
                    if (!compareTime(startTime, helmChartMetadataList.first().created)) {
                        convertUtcTime(helmChartMetadataList.first())
                    } else {
                        throw RdsFileNotFoundException("chart version:[$chartVersion] can not be found")
                    }
                }
            }
        } else {
            throw RdsFileNotFoundException("chart version:[$chartVersion] can not be found")
        }
    }

    /**
     * 查询对应的chart信息
     */
    fun searchJson(
        indexYamlMetadata: RdsIndexYamlMetadata,
        urls: String,
        startTime: LocalDateTime = LocalDateTime.MIN
    ): Any {
        val urlList = urls.removePrefix("/").split("/").filter { it.isNotBlank() }
        when (urlList.size) {
            // Without name and version
            0 -> {
                return filterByCreateTime(indexYamlMetadata, startTime)
            }
            // query with name
            1 -> {
                return filterChart(indexYamlMetadata, startTime, urlList[0])
            }
            // query with name and version
            2 -> {
                return filterChart(indexYamlMetadata, startTime, urlList[0], urlList[1])
            }
            else -> {
                // ERROR_NOT_FOUND
                throw RdsFileNotFoundException("chart not found")
            }
        }
    }

    fun convertUtcTime(indexYamlMetadata: RdsIndexYamlMetadata): RdsIndexYamlMetadata {
        convertUtcTime(indexYamlMetadata.entries)
        return indexYamlMetadata
    }

    fun convertUtcTime(helmChartMetadata: RdsChartMetadata): RdsChartMetadata {
        helmChartMetadata.created?.let {
            helmChartMetadata.created = TimeFormatUtil.formatLocalTime(TimeFormatUtil.convertToLocalTime(it))
        }
        return helmChartMetadata
    }

    fun convertUtcTime(entries: Map<String, SortedSet<RdsChartMetadata>>):
        Map<String, SortedSet<RdsChartMetadata>> {
        entries.forEach {
            val chartMetadataSet = it.value
            chartMetadataSet.forEach { chartMetadata ->
                convertUtcTime(chartMetadata)
            }
        }
        return entries
    }
}
