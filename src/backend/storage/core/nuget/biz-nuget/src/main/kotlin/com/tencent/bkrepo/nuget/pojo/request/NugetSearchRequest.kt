package com.tencent.bkrepo.nuget.pojo.request

/**
 * nuget依赖源搜索请求
 */
data class NugetSearchRequest(
    // The search terms to used to filter packages
    // 如果为空，怎返回所有数据
    val q: String? = null,
    // The number of results to skip, for pagination
    val skip: Int = 0,
    // The number of results to return, for pagination
    val take: Int = 20,
    // true or false determining whether to include pre-release packages
    // If prerelease is not provided, pre-release packages are excluded.
    val prerelease: Boolean? = null,
    // A SemVer 1.0.0 version string
    // 如果为空，返回1.0.0的版本数据，如果semVerLevel=2.0.0, 则SemVer 1.0.0和SemVer 2.0.0 都会返回
    val semVerLevel: String? = null,
    // The package type to use to filter packages(added in SearchQueryService/3.5.0)
    val packageType: String? = null
)
