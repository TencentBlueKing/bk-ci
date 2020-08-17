package com.tencent.bk.codecc.defect.service.specialparam;

import com.google.common.base.CaseFormat;
import com.google.common.collect.Maps;
import com.tencent.bk.codecc.defect.model.CheckerDetailEntity;
import com.tencent.devops.common.constant.ComConstants;
import com.tencent.devops.common.util.ObjectDynamicCreator;
import org.apache.commons.lang.StringUtils;
import org.json.JSONObject;
import org.springframework.stereotype.Service;

import java.util.Map;

/**
 * EsLint特殊参数处理实现类
 *
 * @version V4.0
 * @date 2019/3/16
 */
@Service("ESLINTSpecialParamBizService")
public class ESLINTSpecialParamServiceImpl extends AbstractSpecialParamServiceImpl
{
    /**
     * 修改工具配置影响规则时清空规则集
     *
     * @param reqParamJsonObj
     * @param currentParamJsonObj
     */
    @Override
    public boolean paramJsonModified(JSONObject reqParamJsonObj, JSONObject currentParamJsonObj)
    {
        return !reqParamJsonObj.getString(ComConstants.PARAM_ESLINT_RC).equals(currentParamJsonObj.getString(ComConstants.PARAM_ESLINT_RC));
    }

    /**
     * 特殊参数是否相同
     *
     * @param toolName
     * @param paramJson1
     * @param paramJson2
     */
    @Override
    public boolean isSameParam(String toolName, String paramJson1, String paramJson2)
    {
        JSONObject paramJsonObj1 = new JSONObject(paramJson1);
        JSONObject paramJsonObj2 = new JSONObject(paramJson2);
        return isSameParam(paramJsonObj1, paramJsonObj2, ComConstants.PARAM_ESLINT_RC);
    }

    /**
     * 规则与参数是否相符
     *
     * @param toolName
     * @param checkerModel
     * @param paramJson
     * @return
     */
    @Override
    public boolean checkerMatchParam(String toolName, CheckerDetailEntity checkerModel, String paramJson)
    {
        boolean match = true;
        JSONObject parseObj = StringUtils.isBlank(paramJson) ? new JSONObject() : new JSONObject(paramJson);
        if (parseObj.has(ComConstants.PARAM_ESLINT_RC))
        {
            String eslintRc = parseObj.getString(ComConstants.PARAM_ESLINT_RC);

            //NPE修改：因toolConfig由getSvnConfig方法得来一定不为空，故只要调用equals的顺序
            if (ComConstants.EslintFrameworkType.standard.name().equals(eslintRc))
            {
                if (!checkerModel.getFrameworkType().equals(ComConstants.EslintFrameworkType.standard.name()))
                {
                    match = false;
                }
            }
            else if (ComConstants.EslintFrameworkType.vue.name().equals(eslintRc))
            {
                if (checkerModel.getFrameworkType().equals(ComConstants.EslintFrameworkType.react.name()))
                {
                    match = false;
                }
            }
            else if (ComConstants.EslintFrameworkType.react.name().equals(eslintRc))
            {
                if (checkerModel.getFrameworkType().equals(ComConstants.EslintFrameworkType.vue.name()))
                {
                    match = false;
                }
            }
        }
        return match;
    }

    /**
     * 向对象中写入特殊参数
     *
     * @param toolName
     * @param paramJson
     * @param dest
     * @param cls
     * @param <T>
     */
    @Override
    public <T> void setParam2Object(String toolName, String paramJson, T dest, Class<T> cls)
    {
        Map<String, String> paramJsonMap = Maps.newHashMap();
        JSONObject paramJsonObj = new JSONObject(paramJson);
        String destParamKey = CaseFormat.LOWER_UNDERSCORE.to(CaseFormat.LOWER_CAMEL, ComConstants.PARAM_ESLINT_RC);
        paramJsonMap.put(destParamKey, paramJsonObj.getString(ComConstants.PARAM_ESLINT_RC));
        T newDest = ObjectDynamicCreator.setFieldValueBySetMethod(paramJsonMap, cls);
        ObjectDynamicCreator.copyNonNullPropertiesBySetMethod(newDest, dest, cls);
    }
}
