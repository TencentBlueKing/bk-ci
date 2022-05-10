package com.tencent.bkrepo.repository.service.packages.impl

import com.tencent.bkrepo.common.api.pojo.Page
import com.tencent.bkrepo.common.mongo.dao.util.Pages
import com.tencent.bkrepo.repository.dao.PackageDao
import com.tencent.bkrepo.repository.model.TPackage
import com.tencent.bkrepo.repository.pojo.packages.VersionListOption
import com.tencent.bkrepo.repository.service.packages.PackageRepairService
import com.tencent.bkrepo.repository.service.packages.PackageService
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.data.domain.Sort
import org.springframework.data.mongodb.core.query.Query
import org.springframework.stereotype.Service
import java.time.Duration
import java.time.LocalDateTime

@Service
class PackageRepairServiceImpl(
    private val packageService: PackageService,
    private val packageDao: PackageDao
) : PackageRepairService {
    override fun repairHistoryVersion() {
        // 查询仓库下面的所有package的包
        var successCount = 0L
        var failedCount = 0L
        var totalCount = 0L
        val startTime = LocalDateTime.now()

        // 查询所有的包
        logger.info("starting repair package history version.")
        // 分页查询包信息
        var page = 1
        val packagePage = queryPackage(page)
        var packageList = packagePage.records
        val total = packagePage.totalRecords
        if (packageList.isEmpty()) {
            logger.info("no package found, return.")
            return
        }
        while (packageList.isNotEmpty()) {
            packageList.forEach {
                logger.info(
                    "Retrieved $total packages to repair history version," +
                        " process: $totalCount/$total"
                )
                val projectId = it.projectId
                val repoName = it.repoName
                val key = it.key
                try {
                    // 添加包管理
                    doRepairPackageHistoryVersion(it)
                    logger.info("Success to repair history version for [$key] in repo [$projectId/$repoName].")
                    successCount += 1
                } catch (exception: RuntimeException) {
                    logger.error(
                        "Failed to repair history version for [$key] in repo [$projectId/$repoName]," +
                            " message: $exception"
                    )
                    failedCount += 1
                } finally {
                    totalCount += 1
                }
            }
            page += 1
            packageList = queryPackage(page).records
        }
        val durationSeconds = Duration.between(startTime, LocalDateTime.now()).seconds
        logger.info(
            "Repair package history version, " +
                "total: $totalCount, success: $successCount, failed: $failedCount," +
                " duration $durationSeconds s totally."
        )
    }

    private fun doRepairPackageHistoryVersion(tPackage: TPackage) {
        with(tPackage) {
            val allVersion = packageService.listAllVersion(projectId, repoName, key, VersionListOption())
                .map { it.name }
            historyVersion = historyVersion.toMutableSet().apply { addAll(allVersion) }
            packageDao.save(this)
        }
    }

    private fun queryPackage(page: Int): Page<TPackage> {
        val query = Query().with(
            Sort.by(Sort.Direction.DESC, TPackage::projectId.name, TPackage::repoName.name, TPackage::key.name)
        )
        val totalRecords = packageDao.count(query)
        val pageRequest = Pages.ofRequest(page, 10000)
        val records = packageDao.find(query.with(pageRequest))
        return Pages.ofResponse(pageRequest, totalRecords, records)
    }

    companion object {
        private val logger: Logger = LoggerFactory.getLogger(PackageRepairServiceImpl::class.java)
    }
}
