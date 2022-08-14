package com.tencent.devops.openapi.utils.markdown

import com.tencent.devops.openapi.utils.markdown.MarkdownCharacter.CODE_FILL

class Code(
    var language: String,
    var body: String,
    override val key: String = ""
) : MarkdownElement(key) {
    companion object {
        const val classType = "code"
    }

    override fun toString(): String {
        return "$CODE_FILL$language\n$body\n$CODE_FILL\n"
    }
}
