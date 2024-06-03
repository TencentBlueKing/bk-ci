package com.tencent.devops.openapi.utils.markdown

class Link(
    var name: String,
    var url: String,
    override val key: String = ""
) : MarkdownElement(key) {
    companion object {
        const val classType = "link"
    }

    override fun toString(): String {
        return "[$name]($url)"
    }
}
