package com.tencent.devops.openapi.utils.markdown

import com.tencent.devops.openapi.utils.markdown.MarkdownCharacter.TEXT_FILL
import com.tencent.devops.openapi.utils.markdown.MarkdownCharacter.WHITESPACE

class Text(
    var level: Int,
    var body: String,
    override val key: String = ""
) : MarkdownElement(key) {
    companion object {
        const val classType = "text"
    }

    override fun toString(): String {
        return WHITESPACE.padStart(level + 1, TEXT_FILL) + body + '\n'
    }
}
