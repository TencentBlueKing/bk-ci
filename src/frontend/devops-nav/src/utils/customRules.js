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

import {
  REPOSITORY_API_URL_PREFIX,
  PROJECT_API_URL_PREFIX
} from '../store/constants'
import eventBus from './eventBus'

const customeRules = {
  string: {
    getMessage: field => `非法的${field}`,
    validate: function (value, args) {
      return /^[\w,\d,\-_\(\)]+$/i.test(value)
    }
  },
  aliasUnique: { // 较验代码库别名
    getMessage: field => '代码库别名不能重复',
    validate: function (value, [projectId, repositoryHashId]) {
      return new Promise(async (resolve, reject) => {
        try {
          const response = await eventBus.$ajax.get(`${REPOSITORY_API_URL_PREFIX}/user/repositories/${projectId}/hasAliasName?aliasName=${value}${repositoryHashId ? `&repositoryHashId=${repositoryHashId}` : ''}`)
          resolve({
            valid: !response
          })
        } catch (e) {
          console.log(e)
          reject(e)
        }
      })
    }
  },
  projectNameUnique: { // 较验项目名称是否重复
    getMessage: field => '项目名称已存在',
    validate: function (value, [projectId]) {
      return new Promise(async (resolve, reject) => {
        try {
          const response = await eventBus.$ajax.put(`${PROJECT_API_URL_PREFIX}/user/projects/project_name/names/${value}/validate/${projectId ? `?project_id=${projectId}` : ''}`)
          console.log(response)
          resolve({
            valid: response
          })
        } catch (e) {
          console.log(e)
          resolve({
            valid: false
          })
        }
      })
    }
  },

  projectEnglishNameUnique: { // 较验项目英文名称是否重复
    getMessage: field => '英文缩写已存在',
    validate: function (value) {
      return new Promise(async (resolve, reject) => {
        try {
          const response = await eventBus.$ajax.put(`${PROJECT_API_URL_PREFIX}/user/projects/english_name/names/${value}/validate/`)
          resolve({
            valid: response
          })
        } catch (e) {
          console.log(e)
          resolve({
            valid: false
          })
        }
      })
    }
  },

  projectEnglishNameReg: { // 较验项目英文名称格式
    getMessage: field => '英文缩写必须由小写字母+数字组成，以小写字母开头，长度限制32字符！',
    validate: function (value) {
      return /^[a-z][a-z0-9]{1,32}$/.test(value)
    }
  }
}

function ExtendsCustomRules (_extends) {
  if (typeof _extends !== 'function') {
    console.warn('VeeValidate.Validator.extend必须是一个函数！')
    return
  }
  for (const key in customeRules) {
    if (customeRules.hasOwnProperty(key)) {
      _extends(key, customeRules[key])
    }
  }
}

export default ExtendsCustomRules
