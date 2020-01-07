/*
 * Tencent is pleased to support the open source community by making BK-CI 蓝鲸持续集成平台 available.
 *
 * Copyright (C) 2019 THL A29 Limited, a Tencent company.  All rights reserved.
 *
 * BK-CI 蓝鲸持续集成平台 is licensed under the MIT license.
 *
 * A copy of the MIT License is included in this file.
 *
 *
 * Terms of the MIT License:
 * ---------------------------------------------------
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation
 * files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy,
 * modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the
 * Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT
 * LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN
 * NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE
 * SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package com.tencent.devops.common.api.util;

import java.util.Iterator;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class ObjectReplaceEnvVarUtil {

    /**
     * 把占位符替换环境变量
     *
     * @param obj    需要把占位符替换环境变量的对象(对象如果是集合，注意要选择支持增加、删除等操作的集合类型，不要选择类似SingletonMap这种)
     * @param envMap 环境变量Map
     */
    @SuppressWarnings("all")
    public static Object replaceEnvVar(Object obj, Map<String, String> envMap) {
        if (obj instanceof Map) {
            Set<Map.Entry<String, Object>> entrySet = ((Map) obj).entrySet();
            for (Map.Entry entry : entrySet) {
                Object value = entry.getValue();
                if (!isNormalReplaceEnvVar(value)) {
                    replaceEnvVar(value, envMap);
                } else {
                    entry.setValue(EnvUtils.INSTANCE.parseEnv(JsonUtil.INSTANCE.toJson(value), envMap, false, false));
                }
            }
        } else if (obj instanceof List) {
            List dataList = (List) obj;
            for (int i = 0; i < dataList.size(); i++) {
                Object value = dataList.get(i);
                if (!isNormalReplaceEnvVar(value)) {
                    replaceEnvVar(value, envMap);
                } else {
                    dataList.set(i, EnvUtils.INSTANCE.parseEnv(JsonUtil.INSTANCE.toJson(value), envMap, false, false));
                }
            }
        } else if (obj instanceof Set) {
            System.out.println(obj.getClass());
            Set objSet = (Set) obj;
            Iterator it = objSet.iterator();
            List replaceObjList = new ArrayList();
            while (it.hasNext()) {
                Object value = it.next();
                if (!isNormalReplaceEnvVar(value)) {
                    replaceEnvVar(value, envMap);
                } else {
                    // 先把需要进行占位符替换的元素放到一个集合里
                    replaceObjList.add(value);
                }
            }
            // 把需要进行占位符替换的元素替换完后再放入set集合,把替换前的元素删除
            for (Object value : replaceObjList) {
                objSet.remove(value);
                objSet.add(EnvUtils.INSTANCE.parseEnv(JsonUtil.INSTANCE.toJson(value), envMap, false, false));
            }
        } else if (isNormalReplaceEnvVar(obj)) {
            obj = EnvUtils.INSTANCE.parseEnv(JsonUtil.INSTANCE.toJson(obj), envMap, false, false);
        } else {
            try {
                Map<String, Object> dataMap = JsonUtil.INSTANCE.toMap(obj);
                replaceEnvVar(dataMap, envMap);
            } catch (Throwable e) {
                // 对象转换不了map的对象则直接替换
                obj = EnvUtils.INSTANCE.parseEnv(JsonUtil.INSTANCE.toJson(obj), envMap, false, false);
            }
        }
        return obj;
    }

    private static Boolean isNormalReplaceEnvVar(Object obj) {
        return ReflectUtil.INSTANCE.isNativeType(obj) || obj instanceof String;
    }
}
