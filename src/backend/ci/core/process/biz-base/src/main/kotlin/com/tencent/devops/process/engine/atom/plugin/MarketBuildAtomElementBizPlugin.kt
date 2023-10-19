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

package com.tencent.devops.process.engine.atom.plugin

import com.tencent.devops.common.pipeline.pojo.element.atom.AfterCreateParam
import com.tencent.devops.common.pipeline.pojo.element.atom.BeforeDeleteParam
import com.tencent.devops.common.pipeline.pojo.element.atom.BeforeUpdateParam
import com.tencent.devops.common.pipeline.pojo.element.market.MarketBuildAtomElement
import com.tencent.devops.common.pipeline.enums.AtomChangeAction
import com.tencent.devops.process.plugin.ElementBizPlugin
import com.tencent.devops.process.plugin.annotation.ElementBiz

@ElementBiz
class MarketBuildAtomElementBizPlugin : ElementBizPlugin<MarketBuildAtomElement> {

    override fun elementClass(): Class<MarketBuildAtomElement> {
        return MarketBuildAtomElement::class.java
    }

    override fun afterCreate(
        element: MarketBuildAtomElement,
        param: AfterCreateParam
    ) {
        val inputMap = element.data["input"] as Map<String, Any>
        MarketBuildUtils.changeAction(
            inputMap = inputMap,
            atomCode = element.getAtomCode(),
            atomVersion = element.version,
            param = param,
            action = AtomChangeAction.CREATE
        )
    }

    override fun beforeDelete(element: MarketBuildAtomElement, param: BeforeDeleteParam) {
        val inputMap = element.data["input"] as Map<String, Any>
        MarketBuildUtils.changeAction(
            inputMap = inputMap,
            atomCode = element.getAtomCode(),
            atomVersion = element.version,
            param = param,
            action = AtomChangeAction.DELETE
        )
    }

    override fun beforeUpdate(element: MarketBuildAtomElement, param: BeforeUpdateParam) {
        val inputMap = element.data["input"] as Map<String, Any>
        MarketBuildUtils.changeAction(
            inputMap = inputMap,
            atomCode = element.getAtomCode(),
            atomVersion = element.version,
            param = param,
            action = AtomChangeAction.UPDATE
        )
    }

    override fun check(element: MarketBuildAtomElement, appearedCnt: Int) = Unit
}
