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

import actions from './actions'
import getters from './getters'
import mutations from './mutations'

export default {
    namespaced: true,
    state: {
        showAtomSelectorPopup: false,
        pipeline: null,
        template: null,
        fetchingAtmoModal: false,
        fetchingContainer: false,
        commendAtomCount: 0,
        isCommendAtomPageOver: false,
        isAtomPageOver: false,
        atomList: [],
        requestAtomData: {
            recommendFlag: true,
            keyword: '',
            page: 1,
            pageSize: 50
        },
        fetchingAtomMoreLoading: false,
        fetchingAtomList: false,
        atomCodeList: [],
        fetchingAtmoVersion: false,
        atomVersionList: [],
        storeAtomData: {},
        atomClassifyCodeList: [],
        atomMap: {},
        atomClassifyMap: {},
        atomModalMap: {},
        containerModalMap: {},
        containerTypeList: [],
        editingElementPos: null,
        isPropertyPanelVisible: false,
        showPanelType: '',
        isShowCompleteLog: false,
        isStagePopupShow: false,
        isAddParallelStage: false,
        insertStageIndex: null,
        insertStageIsFinally: false,
        execDetail: null,
        hideSkipExecTask: false,
        globalEnvs: null,
        executeStatus: false,
        saveStatus: false,
        stageTagList: [],
        defaultStageTags: [],
        showReviewDialog: false,
        reviewInfo: null,
        showStageReviewPanel: {},
        importedPipelineJson: null,
        atomVersionChangedKeys: [],
        pipelineLimit: {
            stageLimit: 20,
            jobLimit: 20,
            atomLimit: 50
        },
        pipelineCommonSetting: {},
        editfromImport: false
    },
    mutations,
    actions,
    getters
}
