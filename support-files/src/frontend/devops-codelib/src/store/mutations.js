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

import Vue from 'vue'
import {
    SET_CODELIBS_MUTATION,
    SET_TICKETS_MUTATION,
    UPDATE_CODE_LIB_MUTATION,
    TOGGLE_CODE_LIB_DIALOG,
    DIALOG_LOADING_MUTATION,
    SET_OAUTH_MUTATION,
    SET_T_GIT_OAUTH_MUTATION
} from './constants'
const mutations = {
    [SET_CODELIBS_MUTATION]: (state, {
        codelibs
    }) => {
        Vue.set(state, 'codelibs', codelibs)
    },
    [SET_TICKETS_MUTATION]: (state, {
        tickets
    }) => {
        Vue.set(state, 'tickets', tickets)
    },
    [UPDATE_CODE_LIB_MUTATION]: (state, {
        codelib,
        replace
    }) => {
        Vue.set(state, 'codelib', {
            ...(state.codelib && !replace ? state.codelib : {}),
            ...codelib
        })
    },
    [TOGGLE_CODE_LIB_DIALOG]: (state, {
        showCodelibDialog
    }) => {
        Vue.set(state, 'showCodelibDialog', showCodelibDialog)
    },
    [DIALOG_LOADING_MUTATION]: (state, fetchingCodelibDetail) => {
        Vue.set(state, 'fetchingCodelibDetail', fetchingCodelibDetail)
    },
    [SET_OAUTH_MUTATION]: (state, {
        type,
        oAuth
    }) => {
        Vue.set(state, `${type}OAuth`, oAuth)
    },
    [SET_T_GIT_OAUTH_MUTATION]: (state, {
        type,
        oAuth
    }) => {
        Vue.set(state, `tGitOAuth`, oAuth)
    }
}

export default mutations
