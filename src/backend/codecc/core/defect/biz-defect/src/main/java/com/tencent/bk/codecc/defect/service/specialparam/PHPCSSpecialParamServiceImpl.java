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
 * PHPCS特殊参数处理器实现类
 *
 * @version V4.0
 * @date 2019/3/12
 */
@Service("PHPCSSpecialParamBizService")
public class PHPCSSpecialParamServiceImpl extends AbstractSpecialParamServiceImpl
{
    /**
     * 修改工具配置影响规则时清空规则集
     * @param reqParamJsonObj
     * @param currentParamJsonObj
     */
    @Override
    public boolean paramJsonModified(JSONObject reqParamJsonObj, JSONObject currentParamJsonObj)
    {
        return !reqParamJsonObj.getString(ComConstants.KEY_PHPCS_STANDARD).equals(currentParamJsonObj.getString(ComConstants.KEY_PHPCS_STANDARD));
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
        return isSameParam(paramJsonObj1, paramJsonObj2, ComConstants.KEY_PHPCS_STANDARD);
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
        if (parseObj.has(ComConstants.KEY_PHPCS_STANDARD))
        {
            String phpcsStandard = parseObj.getString(ComConstants.KEY_PHPCS_STANDARD);
            int standCode = ComConstants.PHPCSStandardCode.valueOf(phpcsStandard).code();
            if ((standCode & checkerModel.getStandard()) == 0)
            {
                return false;
            }
        }
        return true;
    }

    /**
     * 向对象中写入特殊参数
     *
     * @param toolName
     * @param paramJson
     * @param dest
     * @param cls
     */
    @Override
    public <T> void setParam2Object(String toolName, String paramJson, T dest, Class<T> cls)
    {
        Map<String, String> paramJsonMap = Maps.newHashMap();
        JSONObject paramJsonObj = new JSONObject(paramJson);
        String destParamKey = CaseFormat.LOWER_UNDERSCORE.to(CaseFormat.LOWER_CAMEL, ComConstants.KEY_PHPCS_STANDARD);
        paramJsonMap.put(destParamKey, paramJsonObj.getString(ComConstants.KEY_PHPCS_STANDARD));
        T newDest = ObjectDynamicCreator.setFieldValueBySetMethod(paramJsonMap, cls);
        ObjectDynamicCreator.copyNonNullPropertiesBySetMethod(newDest, dest, cls);
    }
}
