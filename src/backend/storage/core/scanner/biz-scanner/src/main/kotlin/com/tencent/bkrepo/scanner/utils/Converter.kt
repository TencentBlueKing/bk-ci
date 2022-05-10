/*
 * Tencent is pleased to support the open source community by making BK-CI 蓝鲸持续集成平台 available.
 *
 * Copyright (C) 2022 THL A29 Limited, a Tencent company.  All rights reserved.
 *
 * BK-CI 蓝鲸持续集成平台 is licensed under the MIT license.
 *
 * A copy of the MIT License is included in this file.
 *
 *
 * Terms of the MIT License:
 * ---------------------------------------------------
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated
 * documentation files (the "Software"), to deal in the Software without restriction, including without limitation the
 * rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to
 * permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of
 * the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT
 * LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN
 * NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE
 * SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package com.tencent.bkrepo.scanner.utils

import com.tencent.bkrepo.common.api.exception.SystemErrorException
import com.tencent.bkrepo.common.api.pojo.Page
import com.tencent.bkrepo.common.api.util.readJsonString
import com.tencent.bkrepo.common.artifact.pojo.RepositoryType
import com.tencent.bkrepo.common.mongo.dao.util.Pages
import com.tencent.bkrepo.common.query.model.PageLimit
import com.tencent.bkrepo.common.scanner.pojo.scanner.Scanner
import com.tencent.bkrepo.common.scanner.pojo.scanner.arrowhead.ArrowheadScanner
import com.tencent.bkrepo.common.scanner.pojo.scanner.arrowhead.CveSecItem
import com.tencent.bkrepo.scanner.pojo.request.LoadResultArguments
import com.tencent.bkrepo.scanner.pojo.request.ArrowheadLoadResultArguments
import com.tencent.bkrepo.scanner.model.TScanPlan
import com.tencent.bkrepo.scanner.model.TScanTask
import com.tencent.bkrepo.scanner.model.TSubScanTask
import com.tencent.bkrepo.scanner.pojo.ScanTask
import com.tencent.bkrepo.scanner.pojo.ScanTriggerType
import com.tencent.bkrepo.scanner.pojo.SubScanTask
import com.tencent.bkrepo.scanner.pojo.request.ArtifactVulnerabilityRequest
import com.tencent.bkrepo.scanner.pojo.request.BatchScanRequest
import com.tencent.bkrepo.scanner.pojo.request.MatchPlanSingleScanRequest
import com.tencent.bkrepo.scanner.pojo.request.ScanRequest
import com.tencent.bkrepo.scanner.pojo.request.SingleScanRequest
import com.tencent.bkrepo.scanner.pojo.response.ArtifactVulnerabilityInfo
import org.springframework.data.domain.PageRequest
import java.time.format.DateTimeFormatter

object Converter {
    fun convert(subScanTask: TSubScanTask, scanner: Scanner): SubScanTask = with(subScanTask) {
        SubScanTask(
            taskId = id!!,
            parentScanTaskId = parentScanTaskId,
            scanner = scanner,
            sha256 = sha256,
            size = size,
            credentialsKey = credentialsKey
        )
    }

    fun convert(scanTask: TScanTask, scanPlan: TScanPlan? = null, force: Boolean = false): ScanTask = with(scanTask) {
        ScanTask(
            taskId = id!!,
            createdBy = createdBy,
            lastModifiedDateTime = lastModifiedDate.format(DateTimeFormatter.ISO_DATE_TIME),
            triggerDateTime = createdDate.format(DateTimeFormatter.ISO_DATE_TIME),
            startDateTime = startDateTime?.format(DateTimeFormatter.ISO_DATE_TIME),
            finishedDateTime = finishedDateTime?.format(DateTimeFormatter.ISO_DATE_TIME),
            status = status,
            scanPlan = scanPlan?.let { ScanPlanConverter.convert(it) },
            rule = scanTask.rule?.readJsonString(),
            total = total,
            scanning = scanning,
            failed = failed,
            scanned = scanned,
            scanner = scanner,
            scannerType = scannerType,
            scannerVersion = scannerVersion,
            scanResultOverview = scanResultOverview,
            force = force
        )
    }

    fun convert(request: BatchScanRequest): ScanRequest {
        with(request) {
            val rule = if (repoNames.isEmpty() && artifactRules.isEmpty()) {
                null
            } else {
                RuleConverter.convert(projectId, repoNames, artifactRules)
            }
            return ScanRequest(null, request.planId, rule)
        }
    }

    fun convert(request: SingleScanRequest, planType: String): ScanRequest {
        with(request) {
            require(fullPath != null || packageKey != null && version != null)

            // 创建rule
            val rule = if (planType == RepositoryType.GENERIC.name) {
                RuleConverter.convert(projectId, repoName, fullPath!!)
            } else {
                RuleConverter.convert(projectId, repoName, packageKey!!, version!!)
            }

            return ScanRequest(planId = planId, rule = rule)
        }
    }

    fun convert(request: MatchPlanSingleScanRequest, scanPlan: TScanPlan): ScanRequest {
        with(request) {
            // 创建rule
            val rule = if (fullPath != null) {
                RuleConverter.convert(projectId, repoName, fullPath!!)
            } else {
                RuleConverter.convert(projectId, repoName, packageKey!!, version!!)
            }
            return ScanRequest(planId = scanPlan.id, rule = rule)
        }
    }

    fun convertToLoadArguments(request: ArtifactVulnerabilityRequest, scannerType: String): LoadResultArguments? {
        if (scannerType == ArrowheadScanner.TYPE) {
            return ArrowheadLoadResultArguments(
                vulnerabilityLevels = request.leakType?.let { listOf(it) } ?: emptyList(),
                vulIds = request.vulId?.let { listOf(it) } ?: emptyList(),
                reportType = request.reportType,
                pageLimit = PageLimit(request.pageNumber, request.pageSize)
            )
        }
        return null
    }

    @Suppress("UNCHECKED_CAST")
    fun convert(
        detailReport: Any?,
        scannerType: String,
        reportType: String,
        pageNumber: Int,
        pageSize: Int
    ): Page<ArtifactVulnerabilityInfo> {
        val pageRequest = PageRequest.of(pageNumber, pageSize)
        if (scannerType == ArrowheadScanner.TYPE && reportType == CveSecItem.TYPE && detailReport != null) {
            detailReport as Page<CveSecItem>
            val reports = detailReport.records.mapTo(HashSet(detailReport.records.size)) {
                ArtifactVulnerabilityInfo(
                    vulId = getVulId(it),
                    severity = ScanPlanConverter.convertToLeakLevel(it.cvssRank),
                    pkgName = it.component,
                    installedVersion = it.versions,
                    title = it.name,
                    vulnerabilityName = it.name,
                    description = it.description,
                    officialSolution = it.officialSolution.ifEmpty { it.defenseSolution },
                    reference = it.references
                )
            }.toList()
            return Pages.ofResponse(pageRequest, detailReport.totalRecords, reports)
        }
        return Pages.ofResponse(pageRequest, 0L, emptyList())
    }

    fun convert(triggerType: String): ScanTriggerType {
        return when (triggerType) {
            "MANUAL" -> ScanTriggerType.MANUAL
            "AUTOM" -> ScanTriggerType.ON_NEW_ARTIFACT
            else -> throw SystemErrorException()
        }
    }

    fun convert(overview: Map<String, Any?>): Map<String, Number> {
        val numberOverview = HashMap<String, Number>(overview.size)
        overview.forEach {
            if (it.value is Number) {
                numberOverview[it.key] = it.value as Number
            }
        }
        return numberOverview
    }

    private fun getVulId(cveSecItem: CveSecItem): String {
        with(cveSecItem) {
            if (cveId.isNotEmpty()) {
                return cveId
            }

            if (cnnvdId.isNotEmpty()) {
                return cnnvdId
            }

            if (cnvdId.isNotEmpty()) {
                return cnvdId
            }
            return pocId
        }
    }
}
