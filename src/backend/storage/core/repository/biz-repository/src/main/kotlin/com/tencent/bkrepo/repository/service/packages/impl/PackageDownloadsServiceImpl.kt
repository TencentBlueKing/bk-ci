/*
 * Tencent is pleased to support the open source community by making BK-CI 蓝鲸持续集成平台 available.
 *
 * Copyright (C) 2021 THL A29 Limited, a Tencent company.  All rights reserved.
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

package com.tencent.bkrepo.repository.service.packages.impl

import com.tencent.bkrepo.common.api.exception.ErrorCodeException
import com.tencent.bkrepo.common.api.message.CommonMessageCode
import com.tencent.bkrepo.common.artifact.message.ArtifactMessageCode
import com.tencent.bkrepo.common.mongo.dao.AbstractMongoDao.Companion.ID
import com.tencent.bkrepo.repository.dao.PackageDao
import com.tencent.bkrepo.repository.dao.PackageDownloadsDao
import com.tencent.bkrepo.repository.dao.PackageVersionDao
import com.tencent.bkrepo.repository.model.TPackage
import com.tencent.bkrepo.repository.model.TPackageDownloads
import com.tencent.bkrepo.repository.model.TPackageVersion
import com.tencent.bkrepo.repository.pojo.download.DetailsQueryRequest
import com.tencent.bkrepo.repository.pojo.download.DownloadsMigrationRequest
import com.tencent.bkrepo.repository.pojo.download.PackageDownloadRecord
import com.tencent.bkrepo.repository.pojo.download.PackageDownloadsDetails
import com.tencent.bkrepo.repository.pojo.download.PackageDownloadsSummary
import com.tencent.bkrepo.repository.pojo.download.SummaryQueryRequest
import com.tencent.bkrepo.repository.service.packages.PackageDownloadsService
import com.tencent.bkrepo.repository.util.PackageQueryHelper
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.data.mongodb.core.aggregation.Aggregation.group
import org.springframework.data.mongodb.core.aggregation.Aggregation.match
import org.springframework.data.mongodb.core.aggregation.Aggregation.newAggregation
import org.springframework.data.mongodb.core.aggregation.Aggregation.project
import org.springframework.data.mongodb.core.query.Criteria
import org.springframework.data.mongodb.core.query.Query
import org.springframework.data.mongodb.core.query.Update
import org.springframework.data.mongodb.core.query.and
import org.springframework.data.mongodb.core.query.isEqualTo
import org.springframework.data.mongodb.core.query.where
import org.springframework.stereotype.Service
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.ChronoUnit

@Service
class PackageDownloadsServiceImpl(
    private val packageDao: PackageDao,
    private val packageVersionDao: PackageVersionDao,
    private val packageDownloadsDao: PackageDownloadsDao
) : PackageDownloadsService {

    override fun record(record: PackageDownloadRecord) {
        with(record) {
            val tPackage = checkPackage(projectId, repoName, packageKey)
            checkPackageVersion(tPackage.id.orEmpty(), packageVersion)

            // update package downloads
            val downloadsCriteria = downloadsCriteria(
                projectId = projectId,
                repoName = repoName,
                packageKey = packageKey,
                packageVersion = packageVersion,
                eqDate = LocalDate.now()
            )
            val downloadsQuery = Query(downloadsCriteria)
            val downloadsUpdate = Update().inc(TPackageDownloads::count.name, 1)
                .setOnInsert(TPackageDownloads::name.name, tPackage.name)
            packageDownloadsDao.upsert(downloadsQuery, downloadsUpdate)

            // update package version
            val versionQuery = PackageQueryHelper.versionQuery(tPackage.id.orEmpty(), name = packageVersion)
            val versionUpdate = Update().inc(TPackageVersion::downloads.name, 1)
            packageVersionDao.updateFirst(versionQuery, versionUpdate)

            // update package
            val packageQuery = PackageQueryHelper.packageQuery(projectId, repoName, packageKey)
            val packageUpdate = Update().inc(TPackage::downloads.name, 1)
            packageDao.updateFirst(packageQuery, packageUpdate)

            if (logger.isDebugEnabled) {
                logger.debug("Create artifact download statistics [$record] success.")
            }
        }
    }

    override fun migrate(request: DownloadsMigrationRequest) {
        with(request) {
            val criteria = downloadsCriteria(
                projectId = projectId,
                repoName = repoName,
                packageKey = packageKey,
                packageVersion = packageVersion,
                eqDate = date
            )
            val query = Query(criteria)
            if (packageDownloadsDao.findOne(query) != null) {
                return
            }
            val downloads = TPackageDownloads(
                projectId = projectId,
                repoName = repoName,
                key = packageKey,
                name = packageName,
                version = packageVersion,
                date = date.toString(),
                count = count
            )
            packageDownloadsDao.save(downloads)
        }
    }

    override fun queryDetails(request: DetailsQueryRequest): PackageDownloadsDetails {
        // 最早查询前三个月
        with(request) {
            val tPackage = checkPackage(projectId, repoName, packageKey)
            packageVersion?.let { checkPackageVersion(tPackage.id!!, it) }

            val today = LocalDate.now()
            val earliestDate = today.minusMonths(MAX_MONTH_TIME_SPAN)

            val normalizedFromDate = if (fromDate?.isAfter(earliestDate) == true) fromDate!! else today
            val normalizedToDate = if (toDate?.isBefore(today) == true) toDate!! else today
            val downloadsCriteria = downloadsCriteria(
                projectId = projectId,
                repoName = repoName,
                packageKey = packageKey,
                packageVersion = packageVersion,
                fromDate = normalizedFromDate,
                toDate = normalizedToDate
            )
            val dateRangeMap = buildDateRangeMap(normalizedFromDate, normalizedToDate)
            getAggregationList(downloadsCriteria).forEach {
                dateRangeMap[it.date] = it.count
            }
            return PackageDownloadsDetails(dateRangeMap)
        }
    }

    override fun querySummary(request: SummaryQueryRequest): PackageDownloadsSummary {
        with(request) {
            val tPackage = checkPackage(projectId, repoName, packageKey)
            packageVersion?.let { checkPackageVersion(tPackage.id!!, it) }

            // today
            val today = LocalDate.now()
            val todayCriteria = downloadsCriteria(
                projectId = projectId,
                repoName = repoName,
                packageKey = packageKey,
                packageVersion = packageVersion,
                eqDate = today
            )
            val todayCount = getAggregationCount(todayCriteria)

            // this week
            val thisWeekCriteria = downloadsCriteria(
                projectId = projectId,
                repoName = repoName,
                packageKey = packageKey,
                packageVersion = packageVersion,
                fromDate = today.with(DayOfWeek.MONDAY),
                toDate = today
            )
            val thisWeekCount = getAggregationCount(thisWeekCriteria)

            // this month
            val thisMonthCriteria = downloadsCriteria(
                projectId = projectId,
                repoName = repoName,
                packageKey = packageKey,
                packageVersion = packageVersion,
                fromDate = today.withDayOfMonth(1),
                toDate = today
            )
            val thisMonthCount = getAggregationCount(thisMonthCriteria)

            return PackageDownloadsSummary(
                total = tPackage.downloads,
                today = todayCount,
                thisWeek = thisWeekCount,
                thisMonth = thisMonthCount
            )
        }
    }

    private fun getAggregationList(downloadsCriteria: Criteria): MutableList<DateCount> {
        val aggregation = newAggregation(
            match(downloadsCriteria),
            group(DATE).sum(COUNT).`as`(COUNT),
            project(COUNT).and(ID).`as`(DATE).andExclude(ID)
        )
        return packageDownloadsDao.aggregate(aggregation, DateCount::class.java).mappedResults
    }

    private fun getAggregationCount(downloadsCriteria: Criteria): Long {
        val aggregation = newAggregation(
            match(downloadsCriteria),
            group().sum(COUNT).`as`(COUNT),
            project(COUNT).andExclude(ID)
        )
        return packageDownloadsDao.aggregate(aggregation, Count::class.java).uniqueMappedResult?.count ?: 0L
    }

    private fun downloadsCriteria(
        projectId: String,
        repoName: String,
        packageKey: String,
        packageVersion: String? = null,
        eqDate: LocalDate? = null,
        fromDate: LocalDate? = null,
        toDate: LocalDate? = null
    ): Criteria {
        val criteria = where(TPackageDownloads::projectId).isEqualTo(projectId)
            .and(TPackageDownloads::repoName).isEqualTo(repoName)
            .and(TPackageDownloads::key).isEqualTo(packageKey)
            .apply {
                packageVersion?.let { and(TPackageDownloads::version).isEqualTo(it) }
                eqDate?.let { and(DATE).isEqualTo(eqDate.toString()) }
            }
        if (fromDate != null && toDate != null) {
            if (fromDate.isAfter(toDate)) {
                throw ErrorCodeException(CommonMessageCode.PARAMETER_INVALID, "date range")
            }
            val fromDateString = fromDate.toString()
            val toDateString = toDate.toString()
            if (fromDate.isEqual(toDate)) {
                criteria.and(DATE).isEqualTo(fromDateString)
            } else {
                criteria.and(DATE).gte(fromDateString).lte(toDateString)
            }
        }
        return criteria
    }

    private fun buildDateRangeMap(from: LocalDate, to: LocalDate): HashMap<String, Long> {
        var current = to
        val days = ChronoUnit.DAYS.between(from, to).toInt() + 1
        val dateRangeMap = LinkedHashMap<String, Long>(days)
        repeat(days) {
            dateRangeMap[current.toString()] = 0
            current = current.minusDays(1)
        }
        return dateRangeMap
    }

    /**
     * 查找包，不存在则抛异常
     */
    private fun checkPackage(projectId: String, repoName: String, packageKey: String): TPackage {
        return packageDao.findByKey(projectId, repoName, packageKey)
            ?: throw ErrorCodeException(ArtifactMessageCode.PACKAGE_NOT_FOUND, packageKey)
    }

    /**
     * 查找版本，不存在则抛异常
     */
    private fun checkPackageVersion(packageId: String, versionName: String): TPackageVersion {
        return packageVersionDao.findByName(packageId, versionName)
            ?: throw ErrorCodeException(ArtifactMessageCode.VERSION_NOT_FOUND, versionName)
    }

    companion object {

        private val logger: Logger = LoggerFactory.getLogger(PackageDownloadsServiceImpl::class.java)

        /**
         * 最大的时间跨度（月份数量）
         */
        private const val MAX_MONTH_TIME_SPAN = 3L

        private const val DATE = "date"
        private const val COUNT = "count"
    }

    data class DateCount(
        val date: String,
        val count: Long
    )

    data class Count(
        val count: Long
    )
}
