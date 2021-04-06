/*
 * Tencent is pleased to support the open source community by making BK-CI 蓝鲸持续集成平台 available.
 *
 * Copyright (C) 2021 THL A29 Limited, a Tencent company.  All rights reserved.
 *
 * BK-CI 蓝鲸持续集成平台 is licensed under the MIT license.
 *
 * A copy of the MIT License is included in this file.
 *
 *
 * Terms of the MIT License:
 * ---------------------------------------------------
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package com.tencent.bkrepo.repository.service.node

import com.tencent.bkrepo.repository.pojo.node.service.NodeCopyRequest
import com.tencent.bkrepo.repository.pojo.node.service.NodeMoveRequest

/**
 * 节点移动/拷贝接口
 */
interface NodeMoveCopyOperation {

    /**
     * 移动文件或者文件夹
     * 采用fast-failed模式，移动过程中出现错误则抛异常，剩下的文件不会再移动
     * 行为类似linux mv命令
     * mv 文件名 文件名	将源文件名改为目标文件名
     * mv 文件名 目录名	将文件移动到目标目录
     * mv 目录名 目录名	目标目录已存在，将源目录（目录本身及子文件）移动到目标目录；目标目录不存在则改名
     * mv 目录名 文件名	出错
     */
    fun moveNode(moveRequest: NodeMoveRequest)

    /**
     * 拷贝文件或者文件夹
     * 采用fast-failed模式，拷贝过程中出现错误则抛异常，剩下的文件不会再拷贝
     * 行为类似linux cp命令
     * cp 文件名 文件名	将源文件拷贝到目标文件
     * cp 文件名 目录名	将文件移动到目标目录下
     * cp 目录名 目录名	目标目录已存在，将源目录（目录本身及子文件）拷贝到目标目录；目标目录不存在则将源目录下文件拷贝到目标目录
     * cp 目录名 文件名	出错
     */
    fun copyNode(copyRequest: NodeCopyRequest)
}
