package com.tencent.devops.common.web.utils

import com.tencent.devops.common.redis.RedisOperation

object AtomRuntimeUtil {

    fun getRunningAtomValue(
        redisOperation: RedisOperation,
        buildId: String,
        vmSeqId: String
    ): Pair<String /* atomCode */, String /* signToken */>? {
        val atomValue = redisOperation.get(
            getRunningAtomKey(buildId = buildId, vmSeqId = vmSeqId)
        ) ?: return null
        val atomValueSplit = atomValue.split(":")
        return if (atomValueSplit.size == 2) {
            Pair(atomValueSplit[0], atomValueSplit[1])
        } else {
            Pair(atomValueSplit[0], "")
        }
    }

    fun setRunningAtomValue(
        redisOperation: RedisOperation,
        buildId: String,
        vmSeqId: String,
        atomCode: String,
        signToken: String,
        expiredInSecond: Long
    ) {
        redisOperation.set(
            key = getRunningAtomKey(buildId = buildId, vmSeqId = vmSeqId),
            value = "$atomCode:$signToken",
            expiredInSecond = expiredInSecond
        )
    }

    fun deleteRunningAtom(
        redisOperation: RedisOperation,
        buildId: String,
        vmSeqId: String
    ) {
        redisOperation.delete(getRunningAtomKey(buildId = buildId, vmSeqId = vmSeqId))
    }

    private fun getRunningAtomKey(buildId: String, vmSeqId: String): String {
        return "sensitiveApi:atomCode:$buildId:$vmSeqId"
    }
}
