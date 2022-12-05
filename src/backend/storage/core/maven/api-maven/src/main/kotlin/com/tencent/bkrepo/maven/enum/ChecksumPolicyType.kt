package com.tencent.bkrepo.maven.enum

/**
 * Checksum policy determines how Artifactory behaves when a client checksum for a deployed
 * resource is missing or conflicts with the locally calculated checksum (bad checksum).
 * default: VERIFY_AGAINST_CLIENT_CHECKSUMS
 */
enum class ChecksumPolicyType(val nick: Int) {
    // 默认值
    VERIFY_AGAINST_CLIENT_CHECKSUMS(0),
    TRUST_SERVER_GENERATED_CHECKSUMS(1)
}
