package com.tencent.bkrepo.maven.enum

/**
 * Specifies the naming convention for Maven SNAPSHOT versions.
The options are -
Unique: Version number is based on a time-stamp (default)
Non-unique: Version number uses a self-overriding naming pattern of artifactId-version-SNAPSHOT.type
Deployer: Respects the settings in the Maven client that is deploying the artifact.
default: UNIQUE
 */
enum class SnapshotBehaviorType(val nick: Int) {
    // 默认值, SNAPSHOT 接受client 传过来的时间戳, 当客户端未传时间戳时，服务端生成时间戳。
    UNIQUE(0),

    // 无视接受client 传过来的时间戳，强制为-SNAPSHOT.jar。
    NON_UNIQUE(1),
    // 服务器完全信任客户端的请求, 不对任何请求做处理
    DEPLOYER(2)
}
