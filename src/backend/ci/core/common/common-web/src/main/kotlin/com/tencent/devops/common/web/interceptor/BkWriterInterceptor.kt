package com.tencent.devops.common.web.interceptor

import com.tencent.devops.common.api.annotation.BkInterfaceI18n
import com.tencent.devops.common.api.enums.I18nSourceEnum
import com.tencent.devops.common.api.enums.I18nTranslateTypeEnum
import com.tencent.devops.common.api.pojo.I18nFieldInfo
import com.tencent.devops.common.api.pojo.I18nMessage
import com.tencent.devops.common.api.util.JsonUtil
import com.tencent.devops.common.api.util.MessageUtil
import com.tencent.devops.common.api.util.ShaUtils
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.service.config.CommonConfig
import com.tencent.devops.common.service.utils.SpringContextUtil
import com.tencent.devops.common.web.service.ServiceI18nMessageResource
import com.tencent.devops.common.web.utils.I18nUtil
import javax.ws.rs.container.ResourceInfo
import javax.ws.rs.core.Context
import javax.ws.rs.ext.Provider
import javax.ws.rs.ext.WriterInterceptor
import javax.ws.rs.ext.WriterInterceptorContext
import org.apache.commons.collections4.ListUtils
import org.slf4j.LoggerFactory
import org.springframework.core.annotation.AnnotationUtils
import org.springframework.stereotype.Component
import org.springframework.web.context.request.RequestContextHolder
import org.springframework.web.context.request.ServletRequestAttributes

@Provider
@BkInterfaceI18n
@Component
class BkWriterInterceptor : WriterInterceptor {

    @Context
    private var resourceInfo: ResourceInfo? = null

    companion object {
        private val logger = LoggerFactory.getLogger(BkWriterInterceptor::class.java)
        private const val ARRAY_WILDCARD_TEMPLATE = "[*]"
        private const val ARRAY_REGEX_TEMPLATE = "(\\[)[0-9]+(])"
        private const val SIZE = 50
    }

    /**
     * 修改接口返回报文内容（把返回报文内容进行国际化替换）
     * @param context 拦截器上下文
     */
    override fun aroundWriteTo(context: WriterInterceptorContext?) {
        // 判断请求用户的语言是否和默认语言是否一致，如果一致则无需进行国际化替换
        val defaultLanguage = SpringContextUtil.getBean(CommonConfig::class.java).devopsDefaultLocaleLanguage
        val i18nLanguage = I18nUtil.getLanguage(I18nUtil.getRequestUserId())
        if (context == null || resourceInfo == null || i18nLanguage == defaultLanguage) {
            context?.proceed()
            return
        }
        // 1、只需拦截标上BkInterfaceI18n注解的接口
        val method = resourceInfo!!.resourceMethod
        val bkInterfaceI18nAnnotation = AnnotationUtils.findAnnotation(method, BkInterfaceI18n::class.java)
        if (bkInterfaceI18nAnnotation == null) {
            context.proceed()
            return
        }
        val keyPrefixNames = bkInterfaceI18nAnnotation.keyPrefixNames
        // 2、获取实体对象里需要进行国际化翻译的字段集合
        val entity = context.entity
        val bkI18nFieldMap = MessageUtil.getBkI18nFieldMap(entity)
        val keyPrefixMap = mutableMapOf<String, String>()
        // 3、从实体对象中获取非数组类前缀名称的值
        keyPrefixNames.filter { !it.contains(ARRAY_WILDCARD_TEMPLATE) }.forEach { keyPrefixName ->
            val keyPrefixValue = getKeyPrefixValue(keyPrefixName, entity)
            keyPrefixValue?.let { keyPrefixMap[keyPrefixName] = it }
        }
        val propertyI18nKeyMap = mutableMapOf<String, String>()
        val dbI18nKeyMap = mutableMapOf<String, String>()
        // 4、获取需要进行国际化翻译的字段的国际化key值
        bkI18nFieldMap.forEach nextBkI18nField@{ fieldPath, i18nFieldInfo ->
            val i18nKeySb = StringBuilder()
            // 获取字段的key值
            val fieldKey = getFieldKey(fieldPath, i18nFieldInfo)
            // 判断字段是否需要复用接口定义的公共前缀
            if (i18nFieldInfo.reusePrefixFlag) {
                handleInterfaceKeyPrefixNames(
                    keyPrefixNames = keyPrefixNames,
                    fieldPath = fieldPath,
                    entity = entity,
                    i18nKeySb = i18nKeySb,
                    keyPrefixMap = keyPrefixMap
                )
            }
            i18nKeySb.append(fieldKey)
            val i18nKey = i18nKeySb.toString()
            // 根据国际化信息来源把字段信息分别放入不同的集合以便进行后续处理
            if (i18nFieldInfo.source == I18nSourceEnum.PROPERTIES) {
                propertyI18nKeyMap[fieldPath] = i18nKey
            } else {
                dbI18nKeyMap[fieldPath] = i18nKey
            }
        }
        // 5、为字段设置国际化信息
        setI18nFieldValue(dbI18nKeyMap, bkI18nFieldMap, propertyI18nKeyMap)
        context.proceed()
    }

    private fun setI18nFieldValue(
        dbI18nKeyMap: MutableMap<String, String>,
        bkI18nFieldMap: MutableMap<String, I18nFieldInfo>,
        propertyI18nKeyMap: MutableMap<String, String>
    ) {
        if (dbI18nKeyMap.isNotEmpty()) {
            setDbI18nFieldValue(dbI18nKeyMap, bkI18nFieldMap)
        }
        if (propertyI18nKeyMap.isNotEmpty()) {
            setPropertyI18nFieldValue(propertyI18nKeyMap, bkI18nFieldMap)
        }
    }

    /**
     * 获取字段国际化key
     * @param fieldPath 字段路径
     * @param i18nFieldInfo 国际化字段信息
     * @return 字段国际化key
     */
    private fun getFieldKey(
        fieldPath: String,
        i18nFieldInfo: I18nFieldInfo
    ): String {
        val fieldPathTemplateLastIndex = fieldPath.lastIndexOf(".")
        // 获取字段路径最后一个节点的名称
        val lastNodeName = fieldPath.substring(fieldPathTemplateLastIndex + 1)
        val fieldKeyPrefixName = i18nFieldInfo.keyPrefixName
        // 依据翻译类型生成字段的后缀名
        val fieldKeySuffixName = if (i18nFieldInfo.translateType == I18nTranslateTypeEnum.NAME) {
            // 如果注解上配置了字段转换名称，则使用转换名称进行国际化翻译
            i18nFieldInfo.convertName.ifBlank {
                lastNodeName
            }
        } else {
            MessageUtil.getFieldValue(i18nFieldInfo.field, i18nFieldInfo.entity)?.toString() ?: ""
        }
        val fieldKey = if (fieldKeyPrefixName.isNotBlank()) {
            // 如果字段有单独配置前缀，应该生成带有前缀的字段名称
            "$fieldKeyPrefixName.$fieldKeySuffixName"
        } else {
            fieldKeySuffixName
        }
        return fieldKey
    }

    /**
     * 处理接口前缀名称列表
     * @param keyPrefixNames 前缀名称列表
     * @param fieldPath 字段路径
     * @param entity 实体对象
     * @param i18nKeySb 国际化key的StringBuilder对象
     * @param keyPrefixMap 前缀名称与名称值的map集合
     */
    private fun handleInterfaceKeyPrefixNames(
        keyPrefixNames: Array<String>,
        fieldPath: String,
        entity: Any,
        i18nKeySb: StringBuilder,
        keyPrefixMap: MutableMap<String, String>
    ) {
        keyPrefixNames.forEach nextKeyPrefixName@{ keyPrefixName ->
            if (keyPrefixName.contains(ARRAY_WILDCARD_TEMPLATE)) {
                // 判断前缀名称是否是动态参数，如果是动态参数前缀名称的值要从实体对象获取
                val keyPrefixNameVar = MessageUtil.getPrefixNameVar(keyPrefixName)
                val keyPrefixValue = if (keyPrefixNameVar.isNullOrBlank()) {
                    keyPrefixName
                } else {
                    val keyPrefixTemplateLastIndex = keyPrefixNameVar.lastIndexOf(".")
                    // 如果前缀名称不是固定的，需要将前缀名称往上退一级作为模板进行正则匹配替换
                    val keyPrefixTemplate = getVarTemplate(keyPrefixTemplateLastIndex, keyPrefixNameVar)
                    // 把前缀名称中数组的通配符换成正则表达式
                    val regex = keyPrefixTemplate.replace(ARRAY_WILDCARD_TEMPLATE, ARRAY_REGEX_TEMPLATE).toRegex()
                    // 如果前缀名称不是固定的，需要将字段路径往上退一级作为模板进行正则匹配替换
                    val fieldPathTemplate = getVarTemplate(fieldPath.lastIndexOf("."), fieldPath)
                    // 通过字段路径模板获取真实的前缀名称
                    val matchResult = regex.find(fieldPathTemplate)
                    // 如果通过正则表达式获取不到真实的前缀名称则中断流程
                    val convertKeyPrefixTemplate = matchResult?.groupValues?.get(0) ?: return@nextKeyPrefixName
                    // 获取前缀名称的叶子节点
                    val keyPrefixLastNodeName = keyPrefixNameVar.substring(keyPrefixTemplateLastIndex + 1)
                    // 生成真实的前缀名称
                    val convertKeyPrefixName = "{$convertKeyPrefixTemplate.$keyPrefixLastNodeName}"
                    // 从实体对象map中获取
                    getKeyPrefixValue(convertKeyPrefixName, entity)
                }
                appendI18nKeyNodeName(keyPrefixValue, i18nKeySb)
            } else {
                appendI18nKeyNodeName(keyPrefixMap[keyPrefixName], i18nKeySb)
            }
        }
    }

    /**
     * 获取变量名称模板
     * @param lastIndex 点号在变量名称中最后位置
     * @param varName 变量名称
     * @return 变量名称模板
     */
    private fun getVarTemplate(lastIndex: Int, varName: String): String {
        val keyPrefixTemplate = if (lastIndex > 0) {
            varName.substring(0, lastIndex)
        } else {
            varName
        }
        return keyPrefixTemplate
    }

    /**
     * 为国际化字段key添加节点名称
     * @param nodeName 节点名称
     * @param i18nKeySb 国际化key对应的StringBuilder对象
     */
    private fun appendI18nKeyNodeName(nodeName: String?, i18nKeySb: StringBuilder) {
        nodeName?.let {
            i18nKeySb.append("$it.")
        }
    }

    /**
     * 为翻译来源是DB类型的字段设置国际化信息
     * @param dbI18ndbKeyMap 国际化字段路径与国际化key映射map
     * @param bkI18nFieldMap 国际化字段路径与字段信息映射map
     */
    private fun setDbI18nFieldValue(
        dbI18ndbKeyMap: MutableMap<String, String>,
        bkI18nFieldMap: MutableMap<String, I18nFieldInfo>
    ) {
        val attributes = RequestContextHolder.getRequestAttributes() as? ServletRequestAttributes
        // 获取模块标识
        val moduleCode = I18nUtil.getModuleCode(attributes)
        // 获取用户ID
        val userId = I18nUtil.getRequestUserId()
        // 根据用户ID获取语言信息
        val language = I18nUtil.getLanguage(userId)
        val i18nMessageMap = mutableMapOf<String, String>()
        val client = SpringContextUtil.getBean(Client::class.java)
        val i18nKeys = dbI18ndbKeyMap.values.toList()
        // 切割国际化key列表，分批获取key的国际化信息
        ListUtils.partition(i18nKeys, SIZE).forEach { rids ->
            var i18nMessages: List<I18nMessage>? = null
            try {
                i18nMessages = client.get(ServiceI18nMessageResource::class).getI18nMessages(
                    keys = rids,
                    moduleCode = moduleCode,
                    language = language
                ).data
            } catch (ignored: Throwable) {
                logger.warn("Fail to get i18nMessages of keys[$rids]", ignored)
            }
            i18nMessages?.forEach { i18nMessage ->
                i18nMessageMap[i18nMessage.key] = i18nMessage.value
            }
        }
        dbI18ndbKeyMap.forEach { (fieldPath, i18nKey) ->
            val i18nFieldInfo = bkI18nFieldMap[fieldPath]
            val i18nFieldValue = i18nMessageMap[i18nKey]
            if (i18nFieldInfo != null && i18nFieldValue != null) {
                val field = i18nFieldInfo.field
                if (!field.isAccessible) {
                    // 设置字段为可访问
                    field.isAccessible = true
                }
                // 为需要进行国际化翻译的字段设置国际化信息
                field.set(i18nFieldInfo.entity, i18nFieldValue)
            }
        }
    }

    /**
     * 为翻译来源是PROPERTY类型的字段设置国际化信息
     * @param propertyI18nKeyMap 国际化字段路径与国际化key映射map
     * @param bkI18nFieldMap 国际化字段路径与字段信息映射map
     */
    private fun setPropertyI18nFieldValue(
        propertyI18nKeyMap: MutableMap<String, String>,
        bkI18nFieldMap: MutableMap<String, I18nFieldInfo>
    ) {
        propertyI18nKeyMap.forEach { (fieldPath, i18nKey) ->
            val i18nFieldInfo = bkI18nFieldMap[fieldPath]
            // 从资源文件中获取国际化信息
            val i18nFieldValue = I18nUtil.getCodeLanMessage(
                messageCode = i18nKey
            )
            if (i18nFieldInfo != null && i18nFieldValue.isNotBlank()) {
                val field = i18nFieldInfo.field
                if (!field.isAccessible) {
                    // 设置字段为可访问
                    field.isAccessible = true
                }
                // 为需要进行国际化翻译的字段设置国际化信息
                field.set(i18nFieldInfo.entity, i18nFieldValue)
            }
        }
    }

    /**
     * 获取前缀名称对应的值
     * @param keyPrefixName 前缀名称
     * @param entity 实体对象
     * @return 前缀名称对应的值
     */
    private fun getKeyPrefixValue(keyPrefixName: String, entity: Any): String? {
        // 判断前缀名称是否是动态参数，如果是动态参数那么前缀名称的值要从实体对象获取
        val keyPrefixNameVar = MessageUtil.getPrefixNameVar(keyPrefixName) ?: return keyPrefixName
        // 把前缀名称按照点切割成数组
        val partNames = keyPrefixNameVar.split(".")
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
