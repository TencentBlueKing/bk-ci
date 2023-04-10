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

const dictionary = {
    'zh-CN': {
        messages: {
            alpha: field => '字段只能包含字母',
            unique: field => `${field}字段不能重复`,
            required: field => '字段不能为空',
            excludeComma: field => '字段不能包含英文逗号',
            string: field => '字段只能包含数字，字母和下划线',
            varRule: field => `${field}只能以字母和下划线开头，同时只包含字母，数字以及下划线`,
            numeric: field => '字段只能包含数字',
            regex: (field, regex) => {
                return `字段不符合(${regex})正则表达式规则`
            },
            max: (field, args) => {
                return `字段长度不能超过${args[0]}个字符`
            },
            min: (field, args) => {
                return `字段长度不能少于${args[0]}个字符`
            },
            max_value: (field, args) => {
                return `最大不能超过${args[0]}`
            },
            min_value: (field, args) => {
                return `最小不能少于${args[0]}`
            },
            pullmode: field => '字段不能为空',
            excludeEmptyCapital: field => '字段不能为空，只支持英文小写、数字、下划线以及/',
            mutualGroup: field => '字段不能为空，只支持英文、数字或填写变量',
            nonVarRule: field => '该字段不需要包含${{}}',
            notStartWithBKCI: field => '该字段不能以BK_CI开头',
            paramsRule: field => '字段只能包含英文字母、数字及下划线',
            timeoutsRule: field => '请输入1-10080之间的正整数，支持引用流水线变量'
        }
    },
    'en-US': {
        messages: {
            unique: field => `${field} field cannot be repeated`,
            excludeComma: field => 'Field cannot contain English commas',
            string: field => 'Fields can only contain numbers, letters, and underscores',
            numeric: field => 'Fields can only contain numbers',
            regex: (field, regex) => {
                return `Field does not match (${regex}) regular expression rules`
            },
            max: (field, args) => {
                return `Field length cannot exceed ${args[0]} characters`
            },
            min: (field, args) => {
                return `Field length cannot be less than ${args[0]} characters`
            },
            max_value: (field, args) => {
                return `Maximum cannot exceed ${args[0]}`
            },
            min_value: (field, args) => {
                return `Minimum must not be less than ${args[0]}`
            },
            varRule: field => `${field} only begin with letters and underscores, and only contain letters, numbers, and underscores`,
            pullmode: field => 'Field cannot be empty',
            excludeEmptyCapital: field => 'The field must be filled and only supports English letter, numbers or variables ',
            mutualGroup: field => 'Field only supports English, numbers or fill in variables',
            nonVarRule: field => 'This field does not need to be included ${{}}',
            notStartWithBKCI: field => "Field can not start with 'BK_CI'",
            paramsRule: () => 'Field only support English letter, numbers and underscores',
            timeoutsRule: field => 'Please enter an integer between 1-10080, or a pipeline variable'
        }
    }
}

export default dictionary
