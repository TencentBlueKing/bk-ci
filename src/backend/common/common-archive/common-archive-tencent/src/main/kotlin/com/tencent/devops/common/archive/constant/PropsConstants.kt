package com.tencent.devops.common.archive.constant

object PropsConstants {
    val propsMap = mapOf(
            ARCHIVE_PROPS_PROJECT_ID to "项目id",
            ARCHIVE_PROPS_PIPELINE_ID to "流水线id",
            ARCHIVE_PROPS_PIPELINE_NAME to "流水线名称",
            ARCHIVE_PROPS_BUILD_ID to "构建id",
            ARCHIVE_PROPS_BUILD_NO to "构建num",
            ARCHIVE_PROPS_USER_ID to "启动用户id",
            ARCHIVE_PROPS_CREATOR_ID to "创建者id",
            ARCHIVE_PROPS_APP_VERSION to "apk或者ipa版本号",
            ARCHIVE_PROPS_APP_BUNDLE_IDENTIFIER to "ipa应用id",
            ARCHIVE_PROPS_APP_APP_TITLE to "ipa应用名字",
            ARCHIVE_PROPS_APP_IMAGE to "ipa图标",
            ARCHIVE_PROPS_APP_FULL_IMAGE to "ipa全尺寸图标",
            ARCHIVE_PROPS_SOURCE to "创建来源",

            ARCHIVE_PROPS_CODECC_VS_LEAK_COUNT to "遗留告警数",
            ARCHIVE_PROPS_CODECC_VS_LEAK_HIGH_COUNT to "遗留严重告警数",
            ARCHIVE_PROPS_CODECC_VS_LEAK_HIGH_AND_LIGHT_COUNT to "遗留严重+一般告警数",

            ARCHIVE_PROPS_VS_LEAK_HIGH_COUNT to "遗留严重漏洞数",
            ARCHIVE_PROPS_VS_LEAK_MIDDLE_COUNT to "遗留中危漏洞数",
            ARCHIVE_PROPS_VS_LEAK_LIGHT_COUNT to "遗留轻危漏洞数",
            ARCHIVE_PROPS_VS_LEAK_COUNT to "遗留漏洞数",
            ARCHIVE_PROPS_VS_RISK_COUNT to "风险数",

            ARCHIVE_PROPS_APK_SHELL_STATUS to "APK已加固",

            ARCHIVE_PROPS_IPA_SIGN_STATUS to "已企业签名"
    )
}

// 归档元数据
const val ARCHIVE_PROPS_PROJECT_ID = "projectId"
const val ARCHIVE_PROPS_PIPELINE_ID = "pipelineId"
const val ARCHIVE_PROPS_PIPELINE_NAME = "pipelineName"
const val ARCHIVE_PROPS_BUILD_ID = "buildId"
const val ARCHIVE_PROPS_BUILD_NO = "buildNo"
const val ARCHIVE_PROPS_USER_ID = "userId"
const val ARCHIVE_PROPS_CREATOR_ID = "creatorId"
const val ARCHIVE_PROPS_APP_VERSION = "appVersion"
const val ARCHIVE_PROPS_APP_BUNDLE_IDENTIFIER = "bundleIdentifier"
const val ARCHIVE_PROPS_APP_APP_TITLE = "appTitle"
const val ARCHIVE_PROPS_APP_IMAGE = "image"
const val ARCHIVE_PROPS_APP_FULL_IMAGE = "fullImage"
const val ARCHIVE_PROPS_SOURCE = "source"
const val ARCHIVE_PROPS_FILE_NAME = "fileName"

// Codecc元数据
const val ARCHIVE_PROPS_CODECC_VS_LEAK_COUNT = "codecc.coverity.warning.count"
const val ARCHIVE_PROPS_CODECC_VS_LEAK_HIGH_COUNT = "codecc.coverity.warning.high.count"
const val ARCHIVE_PROPS_CODECC_VS_LEAK_HIGH_AND_LIGHT_COUNT = "codecc.coverity.warning.highAndLight.count"

// 漏洞扫描元数据
const val ARCHIVE_PROPS_VS_LEAK_HIGH_COUNT = "vs.leak.high.count"
const val ARCHIVE_PROPS_VS_LEAK_MIDDLE_COUNT = "vs.leak.middle.count"
const val ARCHIVE_PROPS_VS_LEAK_LIGHT_COUNT = "vs.leak.light.count"
const val ARCHIVE_PROPS_VS_LEAK_COUNT = "vs.leak.count"
const val ARCHIVE_PROPS_VS_RISK_COUNT = "vs.risk.count"

// apk加固元数据
const val ARCHIVE_PROPS_APK_SHELL_STATUS = "apk.shell.status"

// 企业签名元数据
const val ARCHIVE_PROPS_IPA_SIGN_STATUS = "ipa.sign.status"