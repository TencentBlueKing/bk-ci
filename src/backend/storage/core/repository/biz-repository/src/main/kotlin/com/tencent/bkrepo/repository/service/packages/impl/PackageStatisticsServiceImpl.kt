package com.tencent.bkrepo.repository.service.packages.impl

import com.tencent.bkrepo.common.query.util.MongoEscapeUtils
import com.tencent.bkrepo.repository.dao.PackageDao
import com.tencent.bkrepo.repository.model.TPackage
import com.tencent.bkrepo.repository.pojo.software.CountResult
import com.tencent.bkrepo.repository.pojo.software.ProjectPackageOverview
import com.tencent.bkrepo.repository.service.packages.PackageStatisticsService
import org.springframework.data.mongodb.core.aggregation.Aggregation
import org.springframework.data.mongodb.core.query.Criteria
import org.springframework.stereotype.Service

@Service
class PackageStatisticsServiceImpl(
    private val packageDao: PackageDao
) : PackageStatisticsService {

    override fun packageOverview(
        repoType: String,
        projectId: String,
        packageName: String?
    ): List<ProjectPackageOverview> {
        val criteria = Criteria.where(TPackage::type.name).`is`(repoType.toUpperCase())
        projectId.let { criteria.and(TPackage::projectId.name).`is`(projectId) }
        packageName?.let {
            val escapedValue = MongoEscapeUtils.escapeRegexExceptWildcard(packageName)
            val regexPattern = escapedValue.replace("*", ".*")
            criteria.and(TPackage::name.name).regex("^$regexPattern$")
        }
        val aggregation = Aggregation.newAggregation(
            TPackage::class.java,
            Aggregation.match(criteria),
            Aggregation.group("\$${TPackage::repoName.name}").count().`as`("count")
        )
        val result = packageDao.aggregate(aggregation, CountResult::class.java).mappedResults
        return transTree(projectId, result)
    }

    private fun transTree(projectId: String, list: List<CountResult>): List<ProjectPackageOverview> {
        val projectSet = mutableSetOf<ProjectPackageOverview>()
        projectSet.add(
            ProjectPackageOverview(
                projectId = projectId,
                repos = mutableSetOf(),
                sum = 0L
            )
        )
        list.map { pojo ->
            val repoOverview = ProjectPackageOverview.RepoPackageOverview(
                repoName = pojo.id,
                packages = pojo.count
            )
            projectSet.first().repos.add(repoOverview)
            projectSet.first().sum += pojo.count
        }
        return projectSet.toList()
    }
}
