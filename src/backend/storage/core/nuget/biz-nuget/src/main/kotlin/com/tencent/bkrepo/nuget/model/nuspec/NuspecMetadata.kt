package com.tencent.bkrepo.nuget.model.nuspec

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement
import com.tencent.bkrepo.common.api.constant.StringPool

class NuspecMetadata {
    @JacksonXmlProperty(isAttribute = true)
    val minClientVersion: String? = null

    // is required
    val id: String = StringPool.EMPTY
    val version: String = StringPool.EMPTY
    val description: String = StringPool.EMPTY
    val authors: String = StringPool.EMPTY

    // optional
    val owners: String? = null
    val projectUrl: String? = null
    val licenseUrl: String? = null

    // supported with NuGet 4.9.0 and above
    val license: String? = null

    // deprecated, use icon instead
    val iconUrl: String? = null

    // supported with NuGet 5.3.0 and above
    val icon: String? = null
    val requireLicenseAcceptance: Boolean? = null
    val developmentDependency: Boolean? = null

    // is being deprecated
    val summary: String? = null
    val releaseNotes: String? = null
    val copyright: String? = null
    val language: String? = null

    // a space-delimited
    val tags: String? = null
    val serviceable: String? = null
    val repository: Repository? = null
    val title: String? = null

    // collection elements
    val packageTypes: MutableList<PackageType>? = null
    val dependencies: List<Dependency>? = null
    val frameworkAssemblies: MutableList<FrameworkAssembly>? = null
    val references: MutableList<Any>? = null

    fun isValid(): Boolean {
        return id.isNotBlank() && version.isNotBlank()
    }
}

class Repository {
    @JacksonXmlProperty(isAttribute = true)
    val type: String? = null

    @JacksonXmlProperty(isAttribute = true)
    val url: String? = null

    @JacksonXmlProperty(isAttribute = true)
    val branch: String? = null

    @JacksonXmlProperty(isAttribute = true)
    val commit: String? = null
}

data class PackageType(
    @JacksonXmlProperty(isAttribute = true)
    val name: String?,
    @JacksonXmlProperty(isAttribute = true)
    val version: String?
)

data class Dependency(
    @JacksonXmlProperty(isAttribute = true)
    val id: String,
    @JacksonXmlProperty(isAttribute = true)
    val version: String,
    @JacksonXmlProperty(isAttribute = true)
    val include: String?,
    @JacksonXmlProperty(isAttribute = true)
    val exclude: String?
)

@JacksonXmlRootElement(localName = "group")
data class DependencyGroup(
    @JacksonXmlProperty(isAttribute = true)
    val targetFramework: String,
    @JacksonXmlElementWrapper(localName = "dependency")
    val dependencies: MutableList<Dependency>?
)

data class FrameworkAssembly(
    // required
    @JacksonXmlProperty(isAttribute = true)
    val assemblyName: String,
    // optional
    @JacksonXmlProperty(isAttribute = true)
    val targetFramework: String?
)

data class Reference(
    @JacksonXmlProperty(isAttribute = true)
    val file: String
)

@JacksonXmlRootElement(localName = "group")
data class ReferenceGroup(
    @JacksonXmlProperty(isAttribute = true)
    val targetFramework: String,
    @JacksonXmlElementWrapper(localName = "reference")
    val references: MutableList<Reference>?
)
