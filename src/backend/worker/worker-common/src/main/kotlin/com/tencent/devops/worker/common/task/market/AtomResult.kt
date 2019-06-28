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

package com.tencent.devops.worker.common.task.market

/**
 *  输出结果文件位置：${bk.data.dir}/${bk.data.output}
{
"status": "",     # 插件执行结果，值可以为success、failure、error
"message": "",    # 插件执行结果说明，支持markdown格式
"type": "default",# 模板类型，目前仅支持default,用于规定data的解析入库方式
"data":{          # default模板的数据格式如下：
"out_var_1": {
"type": "string",
"value": "testaaaaa"
},
"out_var_2": {
"type": "artifact",
"value": ["file_path_1", "file_path_2"] # 本地文件路径，指定后，agent自动将这些文件归档到仓库
},
"out_var_3": {
"type": "report",
"label": "",  # 报告别名，用于产出物报告界面标识当前报告
"path": "",   # 报告目录所在路径，相对于工作空间
"target": "", # 报告入口文件
}
}
}
 *
 *
 */
data class AtomResult(
    val status: String,
    val message: String?,
    val type: String,
    val data: Map<String, Map<String, Any>>?
)
