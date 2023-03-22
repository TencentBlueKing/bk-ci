package com.tencent.devops.common.web.interceptor

import com.tencent.devops.common.api.annotation.BkI18n
import com.tencent.devops.common.api.auth.AUTH_HEADER_DEVOPS_SERVICE_NAME
import com.tencent.devops.common.api.enums.SystemModuleEnum
import com.tencent.devops.common.api.util.JsonUtil
import com.tencent.devops.common.api.util.MessageUtil
import com.tencent.devops.common.api.util.ShaUtils
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.service.utils.SpringContextUtil
import com.tencent.devops.common.web.service.ServiceI18nMessageResource
import org.apache.commons.collections4.ListUtils
import org.slf4j.LoggerFactory
import org.springframework.web.context.request.RequestContextHolder
import org.springframework.web.context.request.ServletRequestAttributes
import javax.ws.rs.ext.Provider
import javax.ws.rs.ext.WriterInterceptor
import javax.ws.rs.ext.WriterInterceptorContext

@Provider
@BkI18n
class BkWriterInterceptor : WriterInterceptor {

    companion object {
        private val logger = LoggerFactory.getLogger(BkWriterInterceptor::class.java)
        private const val ARRAY_WILDCARD_TEMPLATE = "[*]"
        private const val ARRAY_REGEX_TEMPLATE = "(\\[)[0-9]+(\\])"
    }

    /**
     * 修改接口返回报文内容（把返回报文内容进行国际化替换）
     * @param context 拦截器上下文
     */
    override fun aroundWriteTo(context: WriterInterceptorContext?) {
        if (context == null) {
            return
        }
        val bkI18nAnnotation = context.annotations.find {
            it is BkI18n
        } as? BkI18n
        // 只需拦截标上BkI18n注解的接口
        if (bkI18nAnnotation == null) {
            context.proceed()
            return
        }
        val attributes = RequestContextHolder.getRequestAttributes() as? ServletRequestAttributes
        val moduleCode = getModuleCode(attributes)
        val keyPrefixNames = bkI18nAnnotation.keyPrefixNames
        // 获取实体对象里需要进行国际化翻译的字段集合
        val entity = context.entity
        val bkI18nFieldMap = MessageUtil.getBkI18nFieldMap(entity)
        // 蓝盾的接口返回报文对象都是Result对象，故可以直接将entity转换为map
        val dataMap = JsonUtil.toMap(entity)
        val keyPrefixMap = mutableMapOf<String, String>()
        // 从实体对象map中获取固定前缀名称的值
        keyPrefixNames.filter { !it.contains(ARRAY_WILDCARD_TEMPLATE) }.forEach { keyPrefixName ->
            val keyPrefixValue = getKeyPrefixValue(keyPrefixName, dataMap)
            keyPrefixValue?.let {
                keyPrefixMap[keyPrefixName] = it
            }
        }
        val i18nKeyMap = mutableMapOf<String, String>()
        // 获取需要进行国际化翻译的字段的国际化key值
        bkI18nFieldMap.forEach nextBkI18nField@{ fieldPath, i18nFieldInfo ->
            val i18nKeySb = StringBuilder()
            val fieldPathTemplateLastIndex = fieldPath.lastIndexOf(".")
            keyPrefixNames.forEach nextKeyPrefixName@{ keyPrefixName ->
                if (keyPrefixName.contains(ARRAY_WILDCARD_TEMPLATE)) {
                    val keyPrefixTemplateLastIndex = keyPrefixName.lastIndexOf(".")
                    // 如果前缀名称不是固定的，需要将前缀名称往上退一级作为模板进行正则匹配替换
                    val keyPrefixTemplate = if (keyPrefixTemplateLastIndex > 0) {
                        keyPrefixName.substring(0, keyPrefixTemplateLastIndex)
                    } else {
                        keyPrefixName
                    }
                    // 把前缀名称中数组的通配符换成正则表达式
                    val regex = keyPrefixTemplate.replace(ARRAY_WILDCARD_TEMPLATE, ARRAY_REGEX_TEMPLATE).toRegex()
                    // 如果前缀名称不是固定的，需要将字段路径往上退一级作为模板进行正则匹配替换
                    val fieldPathTemplate = if (fieldPathTemplateLastIndex > 0) {
                        fieldPath.substring(0, fieldPathTemplateLastIndex)
                    } else {
                        fieldPath
                    }
                    // 通过字段路径模板获取真实的前缀名称
                    val matchResult = regex.find(fieldPathTemplate)
                    // 如果通过正则表达式获取不到真实的前缀名称则中断流程
                    val convertKeyPrefixTemplate = matchResult?.groupValues?.get(0) ?: return@nextKeyPrefixName
                    // 获取前缀名称的叶子节点
                    val keyPrefixLastNodeName = keyPrefixName.substring(keyPrefixTemplateLastIndex + 1)
                    // 生成真实的前缀名称
                    val convertKeyPrefixName = "$convertKeyPrefixTemplate.$keyPrefixLastNodeName"
                    // 从实体对象map中获取
                    val keyPrefixValue = getKeyPrefixValue(convertKeyPrefixName, dataMap)
                    keyPrefixValue?.let {
                        i18nKeySb.append("$it.")
                    }
                } else {
                    keyPrefixMap[keyPrefixName]?.let {
                        i18nKeySb.append("$it.")
                    }
                }
            }
            val fieldName = fieldPath.substring(fieldPathTemplateLastIndex + 1)
            i18nKeyMap[fieldPath] = i18nKeySb.append(fieldName).toString()
        }
        val client = SpringContextUtil.getBean(Client::class.java)
        val i18nKeys = i18nKeyMap.values.toList()
        ListUtils.partition(i18nKeys, 50).forEach { rids ->
            val ruleObj = client.get(ServiceI18nMessageResource::class).getI18nMessages(
                keys = rids,
                moduleCode = moduleCode,
                locale = ""
            )
        }
        i18nKeyMap.forEach { (fieldPath, i18nKey) ->
            val i18nFieldInfo = bkI18nFieldMap[fieldPath]
            i18nFieldInfo?.let {
                val field = it.field
                if (!field.isAccessible) {
                    // 设置字段为可访问
                    field.isAccessible = true
                }
                field.set(it.entity, i18nKey)
            }
        }
        context.proceed()
    }

    /**
     * 获取模块标识
     * @param attributes 属性列表
     * @return 模块标识
     */
    private fun getModuleCode(attributes: ServletRequestAttributes?): SystemModuleEnum {
        val moduleCode = if (null != attributes) {
            val request = attributes.request
            // 从请求头中获取服务名称
            val serviceName = request.getHeader(AUTH_HEADER_DEVOPS_SERVICE_NAME)
            SystemModuleEnum.valueOf(serviceName.uppercase())
        } else {
            // 默认从公共模块获取国际化信息
            SystemModuleEnum.COMMON
        }
        return moduleCode
    }


    /**
     * 获取前缀名称对应的值
     * @param keyPrefixName 前缀名称
     * @param entity 实体对象
     * @return 前缀名称对应的值
     */
    private fun getKeyPrefixValue(keyPrefixName: String, entity: Any): String? {
        // 把前缀名称按照点切割成数组
        val partNames = keyPrefixName.split(".")
        var nodeObj: Any? = null
        // 按切割的数组取出前缀名称对应的值
        partNames.forEachIndexed { index, partName ->
            if (index == 0) {
                // 首个节点需要通过实体对象取出节点的值
                nodeObj = getNodeObj(partName, entity)
            } else {
                nodeObj?.let { nodeObj = getNodeObj(partName, it) }
            }
        }
        return nodeObj?.toString()
    }

    /**
     * 根据节点名称获取节点对象
     * @param nodeName 节点名称
     * @param entity 实体对象
     * @return 字段值
     */
    private fun getNodeObj(
        nodeName: String,
        entity: Any
    ): Any? {
        val matchResult = ARRAY_REGEX_TEMPLATE.toRegex().find(nodeName)
        // 通过正则表达式获取节点名称数组标识
        val indexStr = matchResult?.groupValues?.get(0)
        return if (!indexStr.isNullOrBlank()) {
            val convertNodeName = nodeName.removeSuffix(indexStr)
            // 获取数组下标
            val nodeIndex = indexStr.removePrefix("[").removeSuffix("]").toInt()
            // 通过节点名称获取字段对象
            val field = entity.javaClass.getDeclaredField(convertNodeName)
            // 获取字段值
            var fieldVale = MessageUtil.getFieldValue(field, entity)
            fieldVale = if (fieldVale != null && fieldVale is Set<*>) {
                // 如果字段值是set集合，为了保证字段有序，需先将其转换为有序的list集合然后进行国际化翻译处理
                fieldVale.toList().sortedBy {
                    it?.let {
                        ShaUtils.sha1InputStream(JsonUtil.toJson(it, false).byteInputStream())
                    }
                }
            } else {
                // 字段值不是set集合则直接将其转换为list集合
                fieldVale as? List<*>
            }
            fieldVale?.get(nodeIndex)
        } else {
            // 如果该节点是普通节点可以直接通过节点名称获取节点对象
            val field = entity.javaClass.getDeclaredField(nodeName)
            MessageUtil.getFieldValue(field, entity)
        }
    }
}
