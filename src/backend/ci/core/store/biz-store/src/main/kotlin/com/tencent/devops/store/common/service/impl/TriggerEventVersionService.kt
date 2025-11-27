package com.tencent.devops.store.common.service.impl

import com.tencent.devops.common.api.constant.KEY_VERSION
import com.tencent.devops.common.web.utils.I18nUtil
import com.tencent.devops.store.common.service.AbstractComponentVersionService
import com.tencent.devops.store.common.utils.VersionUtils
import com.tencent.devops.store.pojo.common.KEY_ATOM_STATUS
import com.tencent.devops.store.pojo.common.STORE_ATOM_STATUS
import com.tencent.devops.store.pojo.common.enums.StoreStatusEnum
import com.tencent.devops.store.pojo.common.version.VersionInfo
import org.jooq.Record
import org.jooq.Result
import org.springframework.stereotype.Service

@Service("TRIGGER_EVENT_VERSION_SERVICE")
class TriggerEventVersionService : AbstractComponentVersionService() {

    override fun convertVersionList(records: Result<out Record>): List<VersionInfo> {
        val versionList = mutableListOf<VersionInfo>()
        var tmpVersionPrefix = ""
        records.forEach {
            val atomVersion = it[KEY_VERSION] as String
            val index = atomVersion.indexOf(".")
            val versionPrefix = atomVersion.substring(0, index + 1)
            var versionName = atomVersion
            var latestVersionName = VersionUtils.convertLatestVersionName(atomVersion)
            val atomStatus = StoreStatusEnum.valueOf(it[KEY_ATOM_STATUS] as String)
            val atomVersionStatusList = listOf(
                StoreStatusEnum.TESTING,
                StoreStatusEnum.UNDERCARRIAGING,
                StoreStatusEnum.UNDERCARRIAGED
            )
            if (atomVersionStatusList.contains(atomStatus)) {
                // 处于测试中、下架中、已下架的插件版本的版本名称加下说明
                val storeAtomStatusPrefix = STORE_ATOM_STATUS + "_"
                val atomStatusMsg = I18nUtil.getCodeLanMessage(
                    messageCode = "$storeAtomStatusPrefix${atomStatus.name}"
                )
                versionName = "$atomVersion ($atomStatusMsg)"
                latestVersionName = "$latestVersionName ($atomStatusMsg)"
            }
            if (tmpVersionPrefix != versionPrefix) {
                versionList.add(VersionInfo(latestVersionName, "$versionPrefix*"))
                // 添加大版本号的通用最新模式（如1.*）
                tmpVersionPrefix = versionPrefix
            }
            versionList.add(VersionInfo(versionName, atomVersion)) // 添加具体的版本号
        }
        return versionList
    }

    override fun convertVersion(version: String): String {
        val index = version.indexOf(".")
        val versionPrefix = version.substring(0, index + 1)
        return "V$versionPrefix"
    }
}