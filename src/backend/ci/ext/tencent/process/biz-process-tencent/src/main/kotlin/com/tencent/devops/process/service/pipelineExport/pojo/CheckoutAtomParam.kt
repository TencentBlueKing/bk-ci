package com.tencent.devops.process.service.pipelineExport.pojo

import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonProperty
import com.tencent.devops.common.api.enums.RepositoryConfig
import com.tencent.devops.common.api.enums.RepositoryType

@JsonInclude(JsonInclude.Include.NON_NULL)
data class CheckoutAtomParam(
    /**
     * 代码库, 必选, 默认: ID, options: 按代码库选择[ID] | 按代码库别名输入[NAME] | 按仓库URL输入[URL]
     */
    var repositoryType: CheckoutRepositoryType? = null,

    /**
     * 按代码库选择, 当 [repositoryType] = [ID] 时必选
     */
    var repositoryHashId: String? = null,

    /**
     * 按代码库别名输入, 当 [repositoryType] = [NAME] 时必选
     */
    var repositoryName: String? = null,

    /**
     * 代码库链接, 当 [repositoryType] = [URL] 时必选
     */
    var repositoryUrl: String? = null,

    /**
     * 授权类型, 默认: TICKET,
     * 当 [repositoryType] = [URL] 时必选, single,
     *
     * options:
     *
     * EMPTY[空] | TICKET[凭证] | ACCESS_TOKEN[access token] | USERNAME_PASSWORD[username/password] |
     * START_USER_TOKEN[流水线启动人token] | PERSONAL_ACCESS_TOKEN[工蜂personal_access_token]
     */
    var authType: AuthType? = null,
    var authUserId: String? = null,

    /**
     * 代码库凭证, 当 [repositoryType] = [URL] 和 [authType] = [TICKET] 时必选
     */
    var ticketId: String? = null,

    /**
     * access token, 当 [repositoryType] = [URL] 和 [authType] = [ACCESS_TOKEN] 时必选
     */
    var accessToken: String? = null,

    /**
     * 工蜂personal_access_token, 当 [repositoryType] = [URL] 和 [authType] = [PERSONAL_ACCESS_TOKEN] 时必选
     */
    var personalAccessToken: String? = null,

    /**
     * username, 当 [repositoryType] = [URL] 和 [authType] = [USERNAME_PASSWORD] 时必选
     */
    var username: String? = null,

    /**
     * password, 当 [repositoryType] = [URL] 和 [authType] = [USERNAME_PASSWORD] 时必选
     */
    var password: String? = null,

    /**
     * 指定拉取方式, 默认: BRANCH, single, options: BRANCH[BRANCH] | TAG[TAG] | COMMIT_ID[COMMIT_ID]
     */
    var pullType: String? = null,

    /**
     * 分支/TAG/COMMIT, 必选, 默认: master
     */
    var refName: String? = null,

    /**
     * 代码保存路径
     */
    var localPath: String? = null,

    /**
     * 拉取策略, 默认: REVERT_UPDATE,
     *
     * options:
     *
     * Revert Update[REVERT_UPDATE] | Fresh Checkout[FRESH_CHECKOUT] | Increment Update[INCREMENT_UPDATE]
     */
    var strategy: String? = null,

    /**
     * git fetch的depth参数值
     */
    var fetchDepth: Int? = null,

    /**
     * 启用拉取指定分支, 默认: false
     */
    val enableFetchRefSpec: Boolean? = null,

    /**
     * 插件配置的分支不需要设置，默认会设置.配置的分支必须存在，否则会报错, 当 [enableFetchRefSpec] = [true] 时必选
     */
    val fetchRefSpec: String? = null,

    /**
     * 是否开启Git Lfs, 默认: true
     */
    var enableGitLfs: Boolean? = null,

    /**
     * lfs并发上传下载的数量
     */
    val lfsConcurrentTransfers: Int? = null,

    /**
     * MR事件触发时执行Pre-Merge, 必选, 默认: true
     */
    var enableVirtualMergeBranch: Boolean? = null,

    /**
     * 启用子模块, 默认: true
     */
    var enableSubmodule: Boolean? = null,

    /**
     * 子模块路径当 [enableSubmodule] = [true] 时必选
     */
    var submodulePath: String? = null,

    /**
     * 执行git submodule update后面是否带上--remote参数, 默认: false, 当 [enableSubmodule] = [true] 时必选
     */
    var enableSubmoduleRemote: Boolean? = null,

    /**
     * 执行git submodule update后面是否带上--recursive参数, 默认: true, 当 [enableSubmodule] = [true] 时必选
     */
    var enableSubmoduleRecursive: Boolean? = null,

    /**
     * AutoCrlf配置值, 默认: false, single, options: false[false] | true[true] | input[input]
     */
    var autoCrlf: String? = null,

    /**
     * 是否开启Git Clean, 必选, 默认: true, 当 [strategy] = [REVERT_UPDATE] 时必选
     */
    var enableGitClean: Boolean? = null,

    /**
     * 清理没有版本跟踪的ignored文件, 必选, 默认: true, 当 [strategy] = [REVERT_UPDATE] 和 [enableGitClean] = [true] 时必选
     */
    val enableGitCleanIgnore: Boolean? = null,

    /**
     * 清理没有版本跟踪的嵌套仓库, 必选, 默认: false, 当 [strategy] = [REVERT_UPDATE] 和 [enableGitClean] = [true] 时必选
     */
    val enableGitCleanNested: Boolean? = null,

    /**
     * 拉取代码库以下路径
     */
    var includePath: String? = null,

    /**
     * 排除代码库以下路径
     */
    var excludePath: String? = null,

    // 非前端传递的参数
    @JsonProperty("pipeline.start.type")
    val pipelineStartType: String? = null,
    val hookEventType: String? = null,
    val hookSourceBranch: String? = null,
    val hookTargetBranch: String? = null,
    val hookSourceUrl: String? = null,
    val hookTargetUrl: String? = null,

    @JsonProperty("git_mr_number")
    val gitMrNumber: String? = null,

// 重试时检出的commitId
    var retryStartPoint: String? = null,

    /**
     * 是否持久化凭证, 默认: true
     */
    var persistCredentials: Boolean? = null,

    /**
     * 是否开启调试, 必选, 默认: false
     */
    var enableTrace: Boolean? = null,

    /**
     * 是否开启部分克隆,部分克隆只有git版本大于2.22.0才可以使用
     */
    var enablePartialClone: Boolean? = null,

    /**
     * 归档的缓存路径
     */
    val cachePath: String? = null
) {
    constructor(input: GitCodeRepoAtomParam) : this(
        pullType = input.pullType?.name,
        refName = input.getBranch(),
        localPath = input.localPath,
        includePath = input.includePath,
        excludePath = input.excludePath,
        fetchDepth = input.fetchDepth,
        strategy = input.strategy?.name,
        enableSubmodule = input.enableSubmodule,
        submodulePath = input.submodulePath,
        enableSubmoduleRemote = input.enableSubmoduleRemote,
        enableSubmoduleRecursive = input.enableSubmoduleRecursive,
        enableVirtualMergeBranch = input.enableVirtualMergeBranch,
        autoCrlf = input.autoCrlf,
        enableFetchRefSpec = null,
        enableGitClean = input.enableGitClean,
        enableGitLfs = input.enableGitLfs,
        authType = null,
        username = null,
        password = null,
        ticketId = null,
        personalAccessToken = null,
        accessToken = null,
        persistCredentials = null,
        fetchRefSpec = null,
        enablePartialClone = null,
        cachePath = null,
        lfsConcurrentTransfers = null,
        enableGitCleanIgnore = input.enableGitCleanIgnore,
        enableGitCleanNested = null,
        enableTrace = null
    )

    constructor(input: GitCodeRepoCommonAtomParam) : this(
        pullType = input.pullType?.name,
        refName = input.getBranch(),
        localPath = input.localPath,
        includePath = input.includePath,
        excludePath = input.excludePath,
        fetchDepth = input.fetchDepth,
        strategy = input.strategy?.name,
        enableSubmodule = input.enableSubmodule,
        submodulePath = null,
        enableSubmoduleRemote = input.enableSubmoduleRemote,
        enableSubmoduleRecursive = null,
        enableVirtualMergeBranch = input.enableVirtualMergeBranch,
        autoCrlf = null,
        enableFetchRefSpec = null,
        enableGitClean = input.enableGitClean,
        enableGitLfs = input.enableGitLfs,
        authType = null,
        username = input.username,
        password = input.password,
        ticketId = input.ticketId,
        personalAccessToken = null,
        accessToken = input.accessToken,
        persistCredentials = null,
        fetchRefSpec = null,
        enablePartialClone = null,
        cachePath = null,
        lfsConcurrentTransfers = null,
        enableGitCleanIgnore = null,
        enableGitCleanNested = null,
        enableTrace = null
    )

    enum class AuthType {
        TICKET,
        ACCESS_TOKEN,
        USERNAME_PASSWORD,
        START_USER_TOKEN,

        // 工蜂专有授权类型
        PERSONAL_ACCESS_TOKEN,
        EMPTY,

        // 指定授权用户
        AUTH_USER_TOKEN
    }

    enum class CheckoutRepositoryType {
        ID,
        NAME,
        URL
    }

    @JsonIgnore
    fun getRepositoryConfig(): RepositoryConfig {
        return RepositoryConfig(
            repositoryHashId = repositoryHashId,
            repositoryName = repositoryName,
            repositoryType = kotlin.runCatching { RepositoryType.valueOf(repositoryType?.name ?: "ID") }
                .getOrDefault(RepositoryType.ID)
        )
    }
}
