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
        validate: function (value, args) {
            return /^[\w,\d,\-_\(\)]+$/i.test(value)
        }
    },
    unique: {
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
    },
    // 不同时为空
    atlestNotEmpty: {
        validate: function (value, args) {
            console.log(args, 'not')
            let notEmptyNum = 0
            for (const i in args) {
                if (args[i]) {
                    notEmptyNum++
                }
            }
            return notEmptyNum > 0
        }
    },
    pullmode: {
        validate: function (value, args) {
            return typeof value === 'object' && value.type !== '' && value.value !== ''
        }
    },
    excludeComma: {
        validate: function (value) {
            return !/\,/gm.test(value)
        }
    },
    varRule: {
        validate: function (value, args) {
            return /^[a-z_][a-z_\d]*$/gi.test(value)
        }
    },
    excludeEmptyCapital: {
        validate: function (value, args) {
            return /^[a-z0-9_\/]+$/g.test(value)
        }
    },
    mutualGroup: {
        validate: function (value, args) {
            return /^[A-Za-z0-9]+$/g.test(value) || (typeof value === 'string' && value.isBkVar())
        }
    },
    nonVarRule: {
        validate: function (value, args) {
            return !(value.isBkVar())
        }
    },
    notStartWithBKCI: {
        validate: function (value, args) {
            return !/^BK_CI/.test(value)
        }
    },
    paramsRule: {
        validate: function (value, args) {
            return /^[a-zA-Z0-9_]+$/g.test(value)
        }
    },
    buildNumRule: {
        validate: function (value, args) {
            return /^[\w-{}() +?.:$"]{1,256}$/.test(value)
        }
    },
    timeoutsRule: {
        validate: function (value, args) {
            return /\b([1-9]|[1-9]\d{1,3}|10080|100[0-7][0-9]|10079|10000)\b/.test(value) || value.isBkVar()
        }
    }
}

function ExtendsCustomRules (_extends) {
    if (typeof _extends !== 'function') {
        console.warn('VeeValidate.Validator.extend must be a function')
        return
    }
    for (const key in customeRules) {
        if (Object.prototype.hasOwnProperty.call(customeRules, key)) {
            _extends(key, customeRules[key])
        }
    }
}

export default ExtendsCustomRules
