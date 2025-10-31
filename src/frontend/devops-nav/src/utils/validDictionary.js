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
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

const dictionary = {
    'zh-CN': {
        messages: {
            string: field => '字段只能包含数字，字母和下划线',
            regex: (field, regex) => {
                return `字段不符合(${regex})正则表达式规则`
            },
            aliasUnique: field => '代码库别名不能重复。', // 较验代码库别名
            projectNameUnique: field => '项目名称已存在。', // 较验项目名称是否重复
            projectEnglishNameUnique: field => '英文缩写已存在。', // 较验项目英文名称是否重复
            projectEnglishNameReg: field => '英文缩写必须由小写字母+数字+中划线组成，以小写字母开头，长度限制32字符！' // 较验项目英文名称格式
        }
    },
    'en-US': {
        messages: {
            string: field => '字段只能包含数字，字母和下划线',
            regex: (field, regex) => {
                return `字段不符合(${regex})正则表达式规则`
            },
            aliasUnique: field => 'Codelib alias already exists. ', // 较验代码库别名
            projectNameUnique: field => 'Project name already exists. ', // 较验项目名称是否重复
            projectEnglishNameUnique: field => 'English Name already exists. ', // 较验项目英文名称是否重复
            projectEnglishNameReg: field => 'English name must consist of lowercase letters + numbers + middle lines, starting with a lowercase letter and limiting the length to 32 characters' // 较验项目英文名称格式
        }
    },
    'ja-JP': {
        messages: {
            string: field => 'フィールドは数字、文字、アンダースコアのみを含めることができます',
            regex: (field, regex) => {
                return `フィールドは(${regex})正規表現ルールに準拠していません`
            },
            aliasUnique: field => 'コードライブラリのエイリアスが重複しています。', // 较验代码库别名
            projectNameUnique: field => 'プロジェクト名が既に存在します。', // 较验项目名称是否重复
            projectEnglishNameUnique: field => '英語の略称が既に存在します。', // 较验项目英文名称是否重复
            projectEnglishNameReg: field => '英語の略称は小文字のアルファベット+数字+中線で構成され、小文字のアルファベットで始まり、長さは32文字以内です！' // 较验项目英文名称格式
        }
    }
}

export default dictionary
