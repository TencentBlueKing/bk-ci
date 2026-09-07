package com.tencent.devops.process.enums

enum class CreativeFlowShareScene { TALENT_FOLLOW }

/**
 * 分享形态。当前只有 COPY（源为个人项目，复制出独立副本）。
 * 团队创作流的「授权执行原件」形态未来会加，见设计文档 §14；届时在此追加枚举值，
 * 并把 CreativeFlowShareGrantService 里拒绝团队项目的分支改为按形态分流。
 */
enum class CreativeFlowShareMode { COPY }

enum class CreativeFlowShareVersionScope { LATEST, PINNED }

enum class CreativeFlowShareGrantStatus { ENABLED, REVOKED }

enum class CreativeFlowCopyConflictPolicy { SKIP, OVERWRITE, FAIL }

enum class CreativeFlowCopyStatus { CREATED, OVERWRITTEN, SKIPPED }
