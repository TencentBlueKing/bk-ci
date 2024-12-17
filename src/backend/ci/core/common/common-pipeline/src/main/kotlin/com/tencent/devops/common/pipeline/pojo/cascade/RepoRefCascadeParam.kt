package com.tencent.devops.common.pipeline.pojo.cascade

import com.tencent.devops.common.api.enums.ScmType
import com.tencent.devops.common.pipeline.enums.BuildFormPropertyType
import com.tencent.devops.common.pipeline.pojo.BuildFormValue

class RepoRefCascadeParam : CascadeParam(
    type = BuildFormPropertyType.REPO_REF,
    chain = listOf(SELECTOR_KEY_REPO_NAME, SELECTOR_KEY_BRANCH)
) {
    override fun chainHandler(): Map<String, CascadeParamPropsHandler> {
        return mapOf(
            SELECTOR_KEY_REPO_NAME to repoNameHandler,
            SELECTOR_KEY_BRANCH to branchHandler
        )
    }

    private val repoNameHandler = object : CascadeParamPropsHandler {
        override fun handle(key: String, defaultValue: String, projectId: String): BuildCascadeProps {
            val repositoryTypes = SUPPORT_REPO_TYPE.joinToString(separator = ",") { it.name }
            return BuildCascadeProps(
                id = key,
                options = listOf(BuildFormValue(defaultValue, defaultValue)),
                searchUrl = "process/api/user/buildParam/repository/$projectId/aliasName?aliasName={words}&" +
                        "permission=LIST&repositoryType=$repositoryTypes",
                replaceKey = "{words}"
            )
        }
    }

    private val branchHandler = object : CascadeParamPropsHandler {
        override fun handle(key: String, defaultValue: String, projectId: String) =
            BuildCascadeProps(
                id = key,
                options = listOf(BuildFormValue(defaultValue, defaultValue)),
                searchUrl = "/process/api/user/buildParam/$projectId/repository/refs?search={branch}&" +
                        "repositoryType=NAME&repositoryId={parentValue}",
                replaceKey = "{branch}"
            )
    }

    companion object {
        const val SELECTOR_KEY_REPO_NAME = "repo-name"
        const val SELECTOR_KEY_BRANCH = "branch"
        private val SUPPORT_REPO_TYPE = listOf(
            ScmType.CODE_GIT,
            ScmType.GITHUB,
            ScmType.CODE_SVN,
            ScmType.CODE_TGIT,
            ScmType.CODE_GITLAB
        )

        fun variableKeyMap(key: String) = mapOf(
            SELECTOR_KEY_REPO_NAME to "$key.$SELECTOR_KEY_REPO_NAME",
            SELECTOR_KEY_BRANCH to "$key.$SELECTOR_KEY_BRANCH"
        )

        fun defaultValue() = mapOf(
            SELECTOR_KEY_REPO_NAME to "",
            SELECTOR_KEY_BRANCH to ""
        )
    }
}
