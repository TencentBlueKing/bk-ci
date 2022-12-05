package com.tencent.bkrepo.nuget.pojo.nuspec

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement

@JacksonXmlRootElement(localName = "file")
data class NuspecFile(
    val serialVersionUID: String,
    @JacksonXmlProperty(isAttribute = true)
    val src: String,
    @JacksonXmlProperty(isAttribute = true)
    val target: String?,
    @JacksonXmlProperty(isAttribute = true)
    val exclude: String?
)
