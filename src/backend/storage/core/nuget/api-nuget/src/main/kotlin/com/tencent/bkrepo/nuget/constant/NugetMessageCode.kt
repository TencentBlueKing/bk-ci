package com.tencent.bkrepo.nuget.constant

import com.tencent.bkrepo.common.api.message.MessageCode

enum class NugetMessageCode(private val key: String) : MessageCode {
    PACKAGE_CONTENT_INVALID("nuget.package.content.invalid"),
    VERSION_EXISTED("nuget.version.existed"),
    PACKAGE_VERSIONS_NOT_EXISTED("nuget.versions.not.existed"),
    PACKAGE_METADATA_LIST_NOT_FOUND("nuget.metadata.list.not.fount"),
    RESOURCE_FEED_NOT_FOUND("resource.feed.not.found")
    ;

    override fun getBusinessCode() = ordinal + 1
    override fun getKey() = key
    override fun getModuleCode() = 11
}
