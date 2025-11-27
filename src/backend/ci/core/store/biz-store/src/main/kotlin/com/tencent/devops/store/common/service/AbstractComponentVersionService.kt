package com.tencent.devops.store.common.service

import com.tencent.devops.common.api.constant.KEY_BRANCH_TEST_FLAG
import com.tencent.devops.common.api.constant.KEY_VERSION
import com.tencent.devops.common.web.utils.I18nUtil
import com.tencent.devops.store.common.utils.VersionUtils
import com.tencent.devops.store.pojo.atom.enums.AtomStatusEnum
import com.tencent.devops.store.pojo.common.KEY_ATOM_STATUS
import com.tencent.devops.store.pojo.common.STORE_ATOM_STATUS
import com.tencent.devops.store.pojo.common.version.VersionInfo
import org.jooq.Record
import org.jooq.Result

/**
 * 组件版本号服务
 */
abstract class AbstractComponentVersionService {
    /**
     * 转化组件版本信息，并按照主版本号进行分组
     */
    open fun convertVersionList(records: Result<out Record>): List<VersionInfo> {
        val versionList = mutableListOf<VersionInfo>()
        var tmpVersionPrefix = ""
        records.forEach {
            val atomVersion = it[KEY_VERSION] as String
            val index = atomVersion.indexOf(".")
            val versionPrefix = atomVersion.substring(0, index + 1)
            var versionName = atomVersion
            var latestVersionName = VersionUtils.convertLatestVersionName(atomVersion)
            val atomStatus = it[KEY_ATOM_STATUS] as Byte
            val atomVersionStatusList = listOf(
                AtomStatusEnum.TESTING.status.toByte(),
                AtomStatusEnum.UNDERCARRIAGING.status.toByte(),
                AtomStatusEnum.UNDERCARRIAGED.status.toByte()
            )
            if (atomVersionStatusList.contains(atomStatus)) {
                // 处于测试中、下架中、已下架的插件版本的版本名称加下说明
                val atomStatusName = AtomStatusEnum.getAtomStatus(atomStatus.toInt())
                val storeAtomStatusPrefix = STORE_ATOM_STATUS + "_"
                val atomStatusMsg = I18nUtil.getCodeLanMessage(
                    messageCode = "$storeAtomStatusPrefix$atomStatusName"
                )
                versionName = "$atomVersion ($atomStatusMsg)"
                latestVersionName = "$latestVersionName ($atomStatusMsg)"
            }
            // 处理分支版本
            if (
                    tmpVersionPrefix != versionPrefix &&
                    (it.indexOf(KEY_BRANCH_TEST_FLAG) == -1 || (it[KEY_BRANCH_TEST_FLAG] as Boolean?) != true)
            ) {
                versionList.add(VersionInfo(latestVersionName, "$versionPrefix*"))
                // 添加大版本号的通用最新模式（如1.*）
                tmpVersionPrefix = versionPrefix
            }
            versionList.add(VersionInfo(versionName, atomVersion)) // 添加具体的版本号
        }
        return versionList
    }

    /**
     * 将具体版本号转换为版本名称
     */
    open fun convertVersion(version: String) = version
}