package com.tencent.bk.codecc.defect.service.specialparam;

import com.google.common.base.CaseFormat;
import com.google.common.collect.Maps;
import com.tencent.bk.codecc.defect.model.CheckerDetailEntity;
import com.tencent.devops.common.constant.ComConstants;
import com.tencent.devops.common.util.ObjectDynamicCreator;
import org.json.JSONObject;
import org.springframework.stereotype.Service;

import java.util.Map;

/**
 * 圈复杂度特殊参数处理
 *
 * @version V4.0
 * @date 2019/8/5
 */
@Service("CCNSpecialParamBizService")
public class CCNSpecialParamServiceImpl extends AbstractSpecialParamServiceImpl
{
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

        return isSameParam(paramJsonObj1, paramJsonObj2, ComConstants.KEY_CCN_THRESHOLD);
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
        String destParamKey = CaseFormat.LOWER_UNDERSCORE.to(CaseFormat.LOWER_CAMEL, ComConstants.KEY_CCN_THRESHOLD);
        paramJsonMap.put(destParamKey, paramJsonObj.getString(ComConstants.KEY_CCN_THRESHOLD));
        T newDest = ObjectDynamicCreator.setFieldValueBySetMethod(paramJsonMap, cls);
        ObjectDynamicCreator.copyNonNullPropertiesBySetMethod(newDest, dest, cls);
    }
}
