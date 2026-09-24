package com.tencent.devops.common.webhook.pojo.code.git

import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.databind.JsonDeserializer
import com.fasterxml.jackson.databind.JsonNode

class EmptyGitCommitAsNullDeserializer : JsonDeserializer<GitCommit?>() {
    override fun deserialize(p: JsonParser, ctxt: DeserializationContext): GitCommit? {
        val node = p.codec.readTree<JsonNode>(p)
        if (node == null || node.isNull || (node.isObject && node.size() == 0)) {
            return null
        }
        return p.codec.treeToValue(node, GitCommit::class.java)
    }
}
