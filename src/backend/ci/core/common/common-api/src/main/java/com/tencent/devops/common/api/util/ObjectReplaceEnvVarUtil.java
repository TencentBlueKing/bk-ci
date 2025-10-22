/*
 * Tencent is pleased to support the open source community by making BK-CI 蓝鲸持续集成平台 available.
 *
 * Copyright (C) 2019 Tencent.  All rights reserved.
 *
 * BK-CI 蓝鲸持续集成平台 is licensed under the MIT license.
 *
 * A copy of the MIT License is included in this file.
 *
 *
 * Terms of the MIT License:
 * ---------------------------------------------------
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated
 * documentation files (the "Software"), to deal in the Software without restriction, including without limitation the
 * rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to
 * permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of
 * the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT
 * LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN
 * NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE
 * SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package com.tencent.devops.common.api.util;

import java.util.Iterator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class ObjectReplaceEnvVarUtil {

    /**
     * 把对象字段值中的占位符替换成环境变量
     * @param obj 需要把占位符替换环境变量的对象(对象如果是集合对象，注意要选择支持增加、删除等操作的集合类型，不要选择类似SingletonMap这种)
     * @param envMap 环境变量Map
     * @return 变量替换后的对象
     */
    @SuppressWarnings("all")
    public static Object replaceEnvVar(Object obj, Map<String, String> envMap) {
        if (obj instanceof Map) {
            // 递归替换map对象中的变量
            Set<Map.Entry<String, Object>> entrySet = ((Map) obj).entrySet();
            for (Map.Entry entry : entrySet) {
                Object value = entry.getValue();
                if (!isNormalReplaceEnvVar(value)) {
                    entry.setValue(replaceEnvVar(value, envMap));
                } else {
                    entry.setValue(handleNormalEnvVar(value, envMap));
                }
            }
        } else if (obj instanceof List) {
            // 递归替换list对象中的变量
            List dataList = (List) obj;
            for (int i = 0; i < dataList.size(); i++) {
                Object value = dataList.get(i);
                if (!isNormalReplaceEnvVar(value)) {
                    dataList.set(i, replaceEnvVar(value, envMap));
                } else {
                    dataList.set(i, handleNormalEnvVar(value, envMap));
                }
            }
        } else if (obj instanceof Set) {
            // 递归替换set对象中的变量
            Set objSet = (Set) obj;
            Set replaceObjSet = new HashSet(objSet);
            Iterator it = replaceObjSet.iterator();
            while (it.hasNext()) {
                Object value = it.next();
                objSet.remove(value);
                if (!isNormalReplaceEnvVar(value)) {
                    objSet.add(replaceEnvVar(value, envMap));
                } else {
                    objSet.add(handleNormalEnvVar(value, envMap));
                }
            }
        } else if (isNormalReplaceEnvVar(obj)) {
            // 替换基本类型对象或字符串对象中的变量
            obj = handleNormalEnvVar(obj, envMap);
        } else {
            try {
                // 把对象转换成map后进行递归替换变量
                Map<String, Object> dataMap = JsonUtil.INSTANCE.toMap(obj);
                replaceEnvVar(dataMap, envMap);
                obj = JsonUtil.INSTANCE.to(JsonUtil.INSTANCE.toJson(dataMap, true), obj.getClass());
            } catch (Throwable e) {
                // 转换不了map的对象则进行直接替换
                obj = EnvUtils.INSTANCE.parseEnv(JsonUtil.INSTANCE.toJson(obj, true), envMap, false, false);
            }
        }
        return obj;
    }

    private static Object handleNormalEnvVar(Object obj, Map<String, String> envMap) {
        // 只有字符串参数才需要进行变量替换，其它基本类型参数无需进行变量替换
        if (obj instanceof String) {
            String objStr = ((String) obj).trim();
            if (objStr.startsWith("{") && objStr.endsWith("}") && JsonSchemaUtil.INSTANCE.validateJson(objStr)) {
                try {
                    Object dataObj = JsonUtil.INSTANCE.to((String) obj, Map.class);
                    // string能正常转换成map，则说明是json串，那么把dataObj进行递归替换变量后再转成json串
                    dataObj = replaceEnvVar(dataObj, envMap);
                    obj = JsonUtil.INSTANCE.toJson(dataObj, true);
                } catch (Throwable e) {
                    // 转换不了map的字符串对象则直接替换
                    obj = EnvUtils.INSTANCE.parseEnv(JsonUtil.INSTANCE.toJson(obj, true), envMap, false, false);
                }
            } else if (objStr.startsWith("[") && objStr.endsWith("]") && JsonSchemaUtil.INSTANCE.validateJson(objStr)) {
                try {
                    Object dataObj = JsonUtil.INSTANCE.to((String) obj, List.class);
                    // string能正常转成list，说明是json串，把dataObj进行递归替换变量后再转成json串
                    dataObj = replaceEnvVar(dataObj, envMap);
                    obj = JsonUtil.INSTANCE.toJson(dataObj, true);
                } catch (Throwable e1) {
                    // 转换不了list的字符串对象则直接替换
                    obj = EnvUtils.INSTANCE.parseEnv(JsonUtil.INSTANCE.toJson(obj, true), envMap, false, false);
                }
            } else {
                // 转换不了map或者list的字符串对象则直接替换
                obj = EnvUtils.INSTANCE.parseEnv(JsonUtil.INSTANCE.toJson(obj, true), envMap, false, false);
            }
        }
        return obj;
    }

    /**
     * 判断对象是否是普通替换对象
     * @param obj 需要把占位符替换环境变量的对象(对象如果是集合对象，注意要选择支持增加、删除等操作的集合类型，不要选择类似SingletonMap这种)
     * @return 是否是普通替换对象
     */
    private static Boolean isNormalReplaceEnvVar(Object obj) {
        return obj == null || ReflectUtil.INSTANCE.isNativeType(obj) || obj instanceof String;
    }
}
