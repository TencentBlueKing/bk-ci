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
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

const customeRules = {
    string: {
        getMessage: field => `非法的${field}`,
        validate: function (value, args) {
            return /^[\w,\d,\-_\(\)]+$/i.test(value)
        }
    },
    unique: {
        getMessage: field => `${field}不能重复`,
        validate: function (value, args) {
            let repeatNum = 0
            for (let i = 0; i < args.length; i++) {
                if (repeatNum > 2) return false
                if (args[i] === value) {
                    repeatNum++
                }
            }
            return repeatNum <= 1
        }
    }
}

function ExtendsCustomRules (_extends) {
    if (typeof _extends !== 'function') {
        console.warn('VeeValidate.Validator.extend必须是一个函数！')
        return
    }
    for (let key in customeRules) {
        if (customeRules.hasOwnProperty(key)) {
            _extends(key, customeRules[key])
        }
    }
}

export default ExtendsCustomRules
