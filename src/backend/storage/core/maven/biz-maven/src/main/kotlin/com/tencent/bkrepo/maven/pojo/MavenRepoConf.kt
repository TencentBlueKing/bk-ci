package com.tencent.bkrepo.maven.pojo

import com.tencent.bkrepo.maven.enum.SnapshotBehaviorType

/**
 * maven 仓库配置属性
 * 默认值设置:@see [com.tencent.bkrepo.maven.util.MavenConfiguration]
 * [checksumPolicy] 暂未启用, 默认值:0,
 * [mavenSnapshotVersionBehavior] 默认值: 0, 当值为1时,服务端会强制将客户端上传`SNAPSHOT`包时设置的时间戳转换为`SNAPSHOT`
 * [maxUniqueSnapshots] 暂未启用, 无默认值, 设计上为限制`SNAPSHOT`包同一版本下的包数量。
 */
data class MavenRepoConf(

    val checksumPolicy: Int?,
    val mavenSnapshotVersionBehavior: SnapshotBehaviorType? = SnapshotBehaviorType.UNIQUE,
    /**
     * The maximum number of unique snapshots of a single artifact to store.
     Once the number of snapshots exceeds this setting, older versions are removed.
     A value of 0 (default) indicates there is no limit, and unique snapshots are not cleaned up.
     */
    val maxUniqueSnapshots: Int?
)
