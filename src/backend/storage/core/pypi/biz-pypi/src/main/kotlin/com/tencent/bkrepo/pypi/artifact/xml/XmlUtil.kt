/*
 * Tencent is pleased to support the open source community by making BK-CI 蓝鲸持续集成平台 available.
 *
 * Copyright (C) 2020 THL A29 Limited, a Tencent company.  All rights reserved.
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

package com.tencent.bkrepo.pypi.artifact.xml

object XmlUtil {

    /**
     * null response
     */
    fun getEmptyMethodResponse(): MethodResponse {
        return MethodResponse(
            Params(
                listOf(
                    Param(
                        Value(
                            null,
                            null,
                            null,
                            Array(
                                Data(
                                    mutableListOf()
                                )
                            ),
                            null
                        )
                    )
                )
            )
        )
    }

    fun nodeLis2Values(nodeList: List<Map<String, Any?>>): MutableList<Value> {
        val values: MutableList<Value> = ArrayList()
        // 过滤掉重复节点，每个节点对应一个Struct
        for (node in nodeList) {
            values.add(
                Value(
                    null,
                    null,
                    Struct(getMembers(node["metadata"] as Map<String, String>)),
                    null,
                    null
                )
            )
        }
        return values
    }

    fun getXmlMethodResponse(nodeList: List<Map<String, Any>>): String {
        val values: MutableList<Value> = ArrayList()
        // 过滤掉重复节点，每个节点对应一个Struct
        for (node in nodeList) {
            values.add(
                Value(
                    null,
                    null,
                    Struct(getMembers(node["metadata"] as Map<String, String>)),
                    null,
                    null
                )
            )
        }

        val methodResponse =
            MethodResponse(
                Params(
                    listOf(
                        Param(
                            Value(
                                null,
                                null,
                                null,
                                Array(
                                    Data(
                                        // 按版本分段
                                        values
                                    )
                                ),
                                null
                            )
                        )
                    )
                )
            )
        return (XmlConvertUtil.methodResponse2Xml(methodResponse))
    }

    private fun getMembers(metadata: Map<String, String>): List<Member> {
        val members: MutableList<Member> = ArrayList()
        members.add(
            Member(
                "_pypi_ordering",
                Value(
                    null,
                    0,
                    null,
                    null,
                    null
                )
            )
        )
        members.add(
            Member(
                "version",
                Value(
                    metadata["version"],
                    null,
                    // 填入子节点name
                    null,
                    null,
                    null
                )
            )
        )
        members.add(
            Member(
                "name",
                Value(
                    metadata["name"],
                    null,
                    null,
                    null,
                    null
                )
            )
        )
        members.add(
            Member(
                "summary",
                Value(
                    metadata["summary"],
                    null,
                    null,
                    null,
                    null
                )
            )
        )
        return members
    }
}
