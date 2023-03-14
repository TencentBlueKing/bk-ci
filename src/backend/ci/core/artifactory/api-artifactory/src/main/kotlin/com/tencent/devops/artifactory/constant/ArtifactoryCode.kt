package com.tencent.devops.artifactory.constant

object ArtifactoryCode {
    const val BK_GRANT_DOWNLOAD_PERMISSION = "GrantDownloadPermission" //请联系流水线负责人授予下载构件权限
    const val BK_GRANT_PIPELINE_PERMISSION = "GrantPipelinePermission"//访问件构请联系流水线负责人：\n{0} 授予流水线权限。
    const val BK_METADATA_NOT_EXIST_DOWNLOAD_FILE_BY_SHARING = "MetadataNotExistDownloadFileBySharing"//元数据({0})不存在，请通过共享下载文件
    const val BK_NO_EXPERIENCE_PERMISSION = "NoExperiencePermission"//您没有该体验的权限 _
    const val BK_FILE_NOT_EXIST = "FileNotExist"//文件{0}不存在
    const val BK_DESTINATION_PATH_SHOULD_BE_FOLDER = "DestinationPathShouldBeFolder"//目标路径应为文件夹
    const val BK_CANNOT_COPY_TO_CURRENT_DIRECTORY = "CannotCopyToCurrentDirectory"//不能在拷贝到当前目录
    const val BK_CANNOT_MOVE_TO_CURRENT_DIRECTORY = "CannotMoveToCurrentDirectory"//不能移动到当前目录
    const val BK_CANNOT_MOVE_PARENT_DIRECTORY_TO_SUBDIRECTORY = "CannotMoveParentDirectoryToSubdirectory"//不能将父目录移动到子目录
    const val BK_METADATA_NOT_EXIST = "MetadataNotExist"//元数据({0})不存在
    const val BK_BUILD_NOT_EXIST = "BuildNotExist"//构建不存在({0})
    const val BK_USER_NO_PIPELINE_PERMISSION_UNDER_PROJECT = "UserNoPipelinePermissionUnderProject"//用户({0})在工程({1})下没有流水线{2}权限
    const val BK_BLUE_SHIELD_SHARE_FILES_WITH_YOU = "UserNoPipelinePermissionUnderProject"//【蓝盾版本仓库通知】{0}与你共享{1}文件
    const val BK_BLUE_SHIELD_SHARE_AND_OTHER_FILES_WITH_YOU = "BlueShieldShareAndOtherFilesWithYou"//【蓝盾版本仓库通知】{0}与你共享{1}等{2}个文件
    const val BK_SHARE_FILES_PLEASE_DOWNLOAD_FILES_IN_TIME = "ShareFilesPleaseDownloadFilesInTime"//{0}与你共享以下文件，请在有效期（{1}}天）内及时下载：
    const val BK_FILE_NAME = "FileName"//文件名
    const val BK_BELONG_TO_THE_PROJECT = "BelongToTheProject"//所属项目
    const val BK_OPERATING = "Operating"//操作
    const val BK_DOWNLOAD = "Download"//下载
    const val BK_PUSH_FROM_BLUE_SHIELD_DEVOPS_PLATFORM = "PushFromBlueShieldDevopsPlatform"//来自蓝盾DevOps平台的推送
    const val BK_TABLE_CONTENTS = "TableContents"//表格内容
    const val BK_PLEASE_FEEL_TO_CONTACT_BLUE_SHIELD_ASSISTANT = "PleaseFeelToContactBlueShieldAssistant"//如有任何问题，可随时联系蓝盾助手
    const val BK_RECEIVED_THIS_EMAIL_BECAUSE_YOU_FOLLOWED_PROJECT = "BkReceivedThisEmailBecauseYouFollowedProject"//你收到此邮件，是因为你关注了 {0} 项目，或其它人@了你
    const val BK_ILLEGAL_PATH = "IllegalPath"//非法路径

}
