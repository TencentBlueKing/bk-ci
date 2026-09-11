package com.tencent.devops.environment.service

import com.tencent.devops.common.api.pojo.OS
import com.tencent.devops.environment.dao.DynamicEnvTagRule
import com.tencent.devops.environment.dao.EnvTagDao
import com.tencent.devops.environment.dao.NodeTagDao
import com.tencent.devops.environment.pojo.thirdpartyagent.InstallEnvItem
import com.tencent.devops.environment.pojo.thirdpartyagent.InstallEnvPreview
import org.jooq.DSLContext
import org.springframework.stereotype.Component

@Component
class DynamicEnvMatcher(
    private val dslContext: DSLContext,
    private val envTagDao: EnvTagDao,
    private val nodeTagDao: NodeTagDao
) {
    /**
     * Different tag keys are AND conditions, while values under the same key are OR conditions.
     * An environment is pending only when every currently unmatched key is explicitly marked unresolved.
     */
    fun match(
        projectId: String,
        candidateTags: Map<Long, Set<Long>>,
        unresolvedInternalTagKeyIds: Set<Long> = emptySet()
    ): InstallEnvPreview {
        return match(
            rules = envTagDao.fetchDynamicEnvTagRules(dslContext, projectId),
            candidateTags = candidateTags,
            unresolvedInternalTagKeyIds = unresolvedInternalTagKeyIds
        )
    }

    /**
     * Before a new agent starts, its OS is known but its architecture is not. The OS is mapped to the built-in tag;
     * environments whose only missing condition is the built-in arch key remain pending instead of being matched.
     */
    fun previewFirstImport(
        projectId: String,
        os: OS,
        candidateTags: Map<Long, Set<Long>> = emptyMap()
    ): InstallEnvPreview {
        val internalTags = nodeTagDao.fetchInternalTag(dslContext)
        val knownTags = candidateTags.mapValuesTo(mutableMapOf()) { (_, values) -> values.toMutableSet() }
        internalTags[OS_TAG_KEY]?.let { osTag ->
            osTag.tagValues.firstOrNull { it.tagValueName == os.name.lowercase() }?.let { osValue ->
                knownTags.getOrPut(osTag.tagKeyId) { mutableSetOf() }.add(osValue.tagValueId)
            }
        }
        val unresolvedArchKeyIds = setOfNotNull(internalTags[ARCH_TAG_KEY]?.tagKeyId)
        return match(
            projectId = projectId,
            candidateTags = knownTags,
            unresolvedInternalTagKeyIds = unresolvedArchKeyIds
        )
    }

    /**
     * Computes membership changes for dynamic environments only. Static environment membership is intentionally
     * absent from the leaving set because it is not derived from tags.
     */
    fun diff(
        projectId: String,
        currentTags: Map<Long, Set<Long>>,
        proposedTags: Map<Long, Set<Long>>
    ): InstallEnvPreview {
        val rules = envTagDao.fetchDynamicEnvTagRules(dslContext, projectId)
        val current = matchRules(rules, currentTags).associateBy { it.envHashId }
        val proposed = matchRules(rules, proposedTags).associateBy { it.envHashId }
        return InstallEnvPreview(
            unchangedEnvironments = proposed.filterKeys { it in current }.values.toList(),
            joiningEnvironments = proposed.filterKeys { it !in current }.values.toList(),
            leavingEnvironments = current.filterKeys { it !in proposed }.values.toList()
        )
    }

    private fun match(
        rules: List<DynamicEnvTagRule>,
        candidateTags: Map<Long, Set<Long>>,
        unresolvedInternalTagKeyIds: Set<Long>
    ): InstallEnvPreview {
        val matched = mutableListOf<InstallEnvItem>()
        val pending = mutableListOf<InstallEnvItem>()
        rules.sortedBy { it.envId }.forEach { rule ->
            val unmatchedKeys = rule.tags.filterNot { (keyId, acceptedValueIds) ->
                candidateTags[keyId]?.any { it in acceptedValueIds } == true
            }.keys
            val env = rule.toInfo()
            when {
                unmatchedKeys.isEmpty() -> matched.add(env)
                unmatchedKeys.all { it in unresolvedInternalTagKeyIds } -> pending.add(env)
            }
        }
        return InstallEnvPreview(
            matchedEnvironments = matched,
            pendingEnvironments = pending
        )
    }

    private fun matchRules(
        rules: List<DynamicEnvTagRule>,
        candidateTags: Map<Long, Set<Long>>
    ): List<InstallEnvItem> {
        return rules.sortedBy { it.envId }.filter { rule ->
            rule.tags.all { (keyId, acceptedValueIds) ->
                candidateTags[keyId]?.any { it in acceptedValueIds } == true
            }
        }.map { it.toInfo() }
    }

    private fun DynamicEnvTagRule.toInfo() = InstallEnvItem(envHashId = envHashId, name = envName)

    companion object {
        private const val OS_TAG_KEY = "os"
        private const val ARCH_TAG_KEY = "arch"
    }
}
