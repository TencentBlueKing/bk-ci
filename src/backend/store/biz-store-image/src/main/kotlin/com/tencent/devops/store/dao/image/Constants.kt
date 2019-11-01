package com.tencent.devops.store.dao.image

/**
 * @Description
 * @Date 2019/9/23
 * @Version 1.0
 */
object Constants {
    // 公共字段
    const val KEY_PUB_TIME = "pubTime"
    const val KEY_PUBLISHER = "publisher"
    const val KEY_CREATOR = "creator"
    const val KEY_MODIFIER = "modifier"
    const val KEY_CREATE_TIME = "createTime"
    const val KEY_UPDATE_TIME = "updateTime"
    // T_IMAGE表
    const val KEY_IMAGE_ID = "imageId"
    const val KEY_IMAGE_CODE = "imageCode"
    const val KEY_IMAGE_NAME = "imageName"
    const val KEY_IMAGE_SOURCE_TYPE = "imageSourceType"
    const val KEY_IMAGE_REPO_URL = "imageRepoUrl"
    const val KEY_IMAGE_REPO_NAME = "imageRepoName"
    const val KEY_IMAGE_VERSION = "imageVersion"
    const val KEY_IMAGE_TAG = "imageTag"
    const val KEY_IMAGE_SIZE = "imageSize"
    const val KEY_IMAGE_STATUS = "imageStatus"
    const val KEY_IMAGE_LOGO_URL = "imageLogoUrl"
    const val KEY_IMAGE_SUMMARY = "imageSummary"
    const val KEY_IMAGE_DESCRIPTION = "imageDescription"
    const val KEY_IMAGE_LATEST_FLAG = "imageLatestFlag"
    const val KEY_IMAGE_FEATURE_PUBLIC_FLAG = "publicFlag"
    const val KEY_IMAGE_FEATURE_RECOMMEND_FLAG = "recommendFlag"
    const val KEY_IMAGE_FEATURE_WEIGHT = "weight"
    // VersionLog表
    const val KEY_VERSION_LOG_CONTENT = "versionLogContent"
    // T_CLASSIFY表
    const val KEY_CLASSIFY_ID = "classifyId"
    const val KEY_CLASSIFY_CODE = "classifyCode"
    const val KEY_CLASSIFY_NAME = "classifyName"
    // T_CATEGORY表
    const val KEY_CATEGORY_ID = "categoryId"
    const val KEY_CATEGORY_CODE = "categoryCode"
    const val KEY_CATEGORY_NAME = "categoryName"
    const val KEY_CATEGORY_ICON_URL = "categoryIconUrl"
    const val KEY_CATEGORY_TYPE = "categoryType"
    // Label表
    const val KEY_LABEL_ID = "labelId"
    const val KEY_LABEL_CODE = "labelCode"
    const val KEY_LABEL_NAME = "labelName"
    const val KEY_LABEL_TYPE = "labelType"
}