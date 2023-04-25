<template>
    <article class="pipeline-list" v-bkloading="{ isLoading: pageLoading, title: loading.title }">
        <section class="loading-wrapper">
            <div class="pipeline-list-wrapper" v-if="showContent && !pageLoading">
                <template>
                    <template v-if="pipelineList.length <= 0">
                        <empty-tips v-if="hasFilter"
                            :title="filterNoResultTipsConfig.title"
                            :desc="filterNoResultTipsConfig.desc"
                            :btns="filterNoResultTipsConfig.btns">
                        </empty-tips>
                        <empty-tips
                            v-else
                            :title="mypipelineEmptyTipsConfig.title"
                            :desc="mypipelineEmptyTipsConfig.desc"
                            :btns="mypipelineEmptyTipsConfig.btns">
                        </empty-tips>
                    </template>
                    <section v-else>
                        <list-create-header
                            :layout="layout"
                            :has-filter="hasFilter"
                            :num="pipelineList.length"
                            @showSlide="showSlide"
                            @changeLayout="changeLayoutType"
                            @changeOrder="changeOrderType"
                            @showCreate="toggleTemplatePopup">
                        </list-create-header>

                        <section class="pipeline-list-content">
                            <div class="pipeline-list-cards clearfix" v-if="layout === 'card'">
                                <task-card
                                    v-for="(card, index) of pipelineList"
                                    :has-permission="card.hasPermission"
                                    :config="card.feConfig"
                                    :index="index"
                                    :key="`taskCard${card.pipelineId}`"
                                    :can-manual-startup="card.canManualStartup">
                                </task-card>
                            </div>

                            <div class="pipeline-list-table" v-if="layout === 'table'">
                                <task-table
                                    :list="pipelineList">
                                </task-table>
                            </div>
                        </section>
                    </section>
                </template>
            </div>

            <mooc-pipeline-popup :toggle-popup="toggleTemplatePopup" :is-show="templatePopupShow"></mooc-pipeline-popup>
            <pipeline-filter v-if="slideShow" :is-show="slideShow" @showSlide="showSlide" :is-disabled="isDisabled" :selected-filter="currentFilter" @filter="filterCommit" class="pipeline-filter"></pipeline-filter>
            <bk-dialog
                width="800"
                v-model="copyDialogConfig.isShow"
                :title="copyDialogConfig.title"
                :mask-close="false"
                :close-icon="false"
                :auto-close="false"
                header-position="left"
                @confirm="copyConfirmHandler"
                @cancel="copyCancelHandler"
            >
                <template>
                    <section class="copy-pipeline bk-form" v-bkloading="{ isLoading: copyConfig.loading }">
                        <div class="bk-form-item">
                            <label class="bk-label">{{ $t('name') }}：</label>
                            <div class="bk-form-content">
                                <input type="text" class="bk-form-input" :placeholder="$t('pipelineNameInputTips')"
                                    name="newPipelineName"
                                    v-validate="&quot;required|max:40&quot;"
                                    v-model="copyConfig.newPipelineName"
                                    :class="{
                                        'is-danger': errors.has('newPipelineName')
                                    }"
                                >
                                <p class="error-tips" v-if="errors.has('newPipelineName')">{{ $t('pipelineNameInputTips') }}</p>
                            </div>
                        </div>

                        <div class="bk-form-item">
                            <label class="bk-label">{{ $t('desc') }}：</label>
                            <div class="bk-form-content">
                                <input type="text" class="bk-form-input" :placeholder="$t('pipelineDescInputTips')"
                                    name="newPipelineDesc"
                                    v-model="copyConfig.newPipelineDesc"
                                    v-validate.initial="&quot;max:100&quot;"
                                    :class="{
                                        'is-danger': errors.has('newPipelineDesc')
                                    }"
                                >
                                <p class="error-tips" v-if="errors.has('newPipelineDesc')"> {{ errors.first("newPipelineDesc") }}</p>
                            </div>
                        </div>
                    </section>
                </template>
            </bk-dialog>

            <bk-dialog
                width="800"
                v-model="saveAsTemp.isShow"
                :title="saveAsTemp.title"
                :close-icon="false"
                :mask-close="false"
                :auto-close="false"
                header-position="left"
                @confirm="saveAsConfirmHandler"
                @cancel="saveAsCancelHandler">
                <section class="copy-pipeline bk-form" ref="saveAsTemp">
                    <div class="bk-form-item">
                        <label class="bk-label">{{ $t('template.name') }}</label>
                        <div class="bk-form-content">
                            <input type="text"
                                class="bk-form-input"
                                :placeholder="$t('template.nameInputTips')"
                                v-model="saveAsTemp.templateName"
                                :class="{ 'is-danger': errors.has('saveTemplateName') }"
                                name="saveTemplateName"
                                v-validate="&quot;required|max:30&quot;"
                                maxlength="30"
                            >
                        </div>
                        <div v-if="errors.has('saveTemplateName')" class="error-tips err-name">{{ $t('template.nameInputTips') }}</div>
                    </div>

                    <div class="bk-form-item">
                        <label class="bk-label tip-bottom">{{ $t('template.applySetting') }}
                            <span v-bk-tooltips.bottom="$t('template.tipsSetting')" class="bottom-start">
                                <i class="bk-icon icon-info-circle"></i>
                            </span>
                        </label>
                        <div class="bk-form-content">
                            <bk-radio-group v-model="saveAsTemp.isCopySetting">
                                <bk-radio v-for="(entry, key) in copySettings" :key="key" :value="entry.value" class="auth-radio">{{ entry.label }}</bk-radio>
                            </bk-radio-group>
                        </div>
                    </div>
                </section>
            </bk-dialog>
        </section>
    </article>
</template>

<script>
    // import pipelineWebsocket from '@/utils/pipelineWebSocket'
    import webSocketMessage from '@/utils/webSocketMessage'
    import { mapGetters, mapState } from 'vuex'
    import moocPipelinePopup from '@/components/pipelineList/moocPipelinePopup'
    import { bus } from '@/utils/bus'
    import taskCard from '@/components/pipelineList/taskCard'
    import taskTable from '@/components/pipelineList/taskTable'
    import emptyTips from '@/components/pipelineList/imgEmptyTips'
    import listCreateHeader from '@/components/pipelineList/listCreateHeader'
    import pipelineFilter from '@/components/pipelineList/PipelineFilter'
    import {
        convertMStoString,
        convertMStoStringByRule,
        navConfirm
    } from '@/utils/util'

    export default {
        components: {
            taskCard,
            taskTable,
            emptyTips,
            listCreateHeader,
            moocPipelinePopup,
            pipelineFilter
        },

        data () {
            const layout = localStorage.getItem('pipelineLayout') || 'card'
            const sortType = localStorage.getItem('pipelineSortType') || 'CREATE_TIME'
            return {
                hasTemplatePermission: true,
                templatePopupShow: false,
                loading: {
                    isLoading: false,
                    title: ''
                },
                showContent: false,
                responsiveConfig: {
                    wrapper: null,
                    width: 0,
                    cardRealWidth: 0,
                    cardMaxWidth: 0,
                    standardMarginRight: 25,
                    allowCalc: true
                },
                copyDialogConfig: {
                    isShow: false,
                    title: this.$t('newlist.copyPipeline'),
                    closeIcon: false,
                    quickClose: false,
                    padding: '0 20px',
                    pipelineId: ''
                },
                copyConfig: {
                    newPipelineName: '',
                    newPipelineDesc: '',
                    loading: false,
                    newPipelineGroup: '',
                    config: {
                        data: this.groupList,
                        onChange: this.pipelineGroupChange
                    }
                },
                saveAsTemp: {
                    isShow: false,
                    title: this.$t('newlist.saveAsTemp'),
                    closeIcon: false,
                    quickClose: false,
                    padding: '0 20px',
                    pipelineId: '',
                    templateName: '',
                    isCopySetting: true
                },
                copySettings: [
                    { label: this.$t('true'), value: true },
                    { label: this.$t('false'), value: false }
                ],
                tipsSetting: {
                    content: this.$t('template.tipsSetting'),
                    placements: ['right']
                },
                layout,
                sortType,
                slideShow: false,
                isDisabled: false,
                filter: {},
                currentFilter: {},
                currentViewId: 'myPipeline',
                mypipelineEmptyTipsConfig: {
                    title: '欢迎来到新T lab 蓝盾流水线体验任务',
                    desc: '您将在这里按照指引构建一条具备基本功能的项目流水线，体验从代码拉取到编译的整个过程。请点击"创建流水线"，开始您的课程任务。',
                    imgType: '',
                    btns: [
                        {
                            theme: 'primary',
                            size: 'normal',
                            handler: () => this.toggleTemplatePopup(true),
                            text: '创建流水线'
                        },
                        {
                            theme: 'default',
                            size: 'normal',
                            handler: () => window.open('https://iwiki.oa.tencent.com/pages/viewpage.action?pageId=10718789', '_blank'),
                            text: '学习基础知识'
                        }
                    ]
                },
                filterNoResultTipsConfig: {
                    title: this.$t('newlist.knowMore'),
                    desc: this.$t('newlist.knowMore'),
                    imgType: '',
                    btns: [
                        {
                            theme: 'primary',
                            size: 'normal',
                            handler: () => this.showSlide(true),
                            text: this.$t('newlist.filterAgain')
                        }
                    ]
                }
            }
        },

        computed: {
            ...mapGetters({
                pipelineList: 'pipelines/getAllPipelineList',
                statusMap: 'pipelines/getStatusMap',
                tagGroupList: 'pipelines/getTagGroupList'
            }),
            ...mapState('pipelines', [
                'pageLoading',
                'hasCreatePermission'
            ]),
            projectId () {
                return this.$route.params.projectId
            },
            pageType () {
                return this.$route.params.type
            },
            hasFilter () {
                const res = (this.filter && Object.keys(this.filter).length && (this.filter.filterByCreator !== '' || this.filter.filterByPipelineName !== '' || this.filter.filterByLabels !== ''))
                return res
            }
        },

        watch: {
            '$route.params.type' (val) {
                if (val) {
                    this.filter = {}
                    this.currentFilter = {}
                    this.initPage()
                    this.showContent = true
                }
            }
            // projectId: {
            //     handler (val) {
            //         this.initWebSocket(val)
            //     },
            //     immediate: true
            // }
        },

        created () {
            bus.$off('title-click')
            bus.$on('title-click', (pipelineId) => {
                this.titleClickHandler(pipelineId)
            })

            bus.$off('error-noticed')
            bus.$on('error-noticed', (pipelineId) => {
                this.errorNoticed(pipelineId)
            })

            bus.$off('triggers-exec')
            bus.$on('triggers-exec', (params, pipelineId) => {
                this.triggersExec(params, pipelineId)
            })

            bus.$off('terminate-pipeline')
            bus.$on('terminate-pipeline', (pipelineId) => {
                this.terminatePipeline(pipelineId)
            })

            bus.$off('resume-pipeline')
            bus.$on('resume-pipeline', (pipelineId) => {
                this.resumePipeline(pipelineId)
            })

            bus.$off('set-permission')
            bus.$on('set-permission', this.setPermissionConfig)
            this.sendMoocQuerySearch()
        },

        mounted () {
            this.initPage()
            webSocketMessage.installWsMessage(this.updatePipelineStatus)
        },

        beforeDestroy () {
            // pipelineWebsocket.disconnect()
            webSocketMessage.unInstallWsMessage()
        },

        methods: {
            async sendMoocQuerySearch () {
                try {
                    const searchJSON = {}
                    for (const [key, value] of new URLSearchParams(location.search)) {
                        searchJSON[key] = value
                    }
                    this.$ajax.post('/plugin/api/user/mooc/add', searchJSON)
                } catch (e) {
                    console.warn(e)
                    this.$showTips({
                        message: e.message,
                        theme: 'error'
                    })
                }
            },
            async changeLayoutType (val) {
                localStorage.setItem('pipelineLayout', val)
                this.layout = val
            },
            async changeOrderType (val) {
                localStorage.setItem('pipelineSortType', val)
                this.sortType = val
                await this.requestPipelineList(val)
            },
            async filterCommit (data, currentFilter, needLoad = true) { // needLoad 阻止重复请求流水线列表
                try {
                    this.filter = data
                    this.currentFilter = currentFilter
                    if (needLoad) {
                        this.togglePageLoading(true)
                        await this.requestPipelineList(this.sortType)
                    }
                } catch (e) {
                    console.error('filter error', e)
                } finally {
                    this.togglePageLoading(false)
                    this.isDisabled = false
                }
            },
            toggleTemplatePopup (templatePopupShow) {
                if (!this.hasCreatePermission) {
                    this.toggleCreatePermission()
                } else {
                    this.templatePopupShow = templatePopupShow
                }
            },
            toggleCreatePermission () {
                this.$showAskPermissionDialog({
                    noPermissionList: [{
                        actionId: this.$permissionActionMap.create,
                        resourceId: this.$permissionResourceMap.pipeline,
                        instanceId: [],
                        projectId: this.projectId
                    }],
                    applyPermissionUrl: `/backend/api/perm/apply/subsystem/?client_id=pipeline&project_code=${this.projectId}&service_code=pipeline&role_creator=pipeline:`
                })
            },
            localConvertMStoString (num) {
                return convertMStoString(num)
            },
            showSlide (val) {
                this.slideShow = val
            },
            togglePageLoading (val) {
                this.$store.commit('pipelines/showPageLoading', val)
            },
            calcLatestStartBuildTime (row) {
                if (row.latestBuildStartTime) {
                    try {
                        if (window.pipelineVue.$i18n && window.pipelineVue.$i18n.locale === 'en-US') {
                            return this.localConvertMStoString(row.currentTimestamp - row.latestBuildStartTime)
                        } else {
                            let result = this.localConvertMStoString(row.currentTimestamp - row.latestBuildStartTime).match(/^[0-9]{1,}([\u4e00-\u9fa5]){1,}/)[0]
                            if (result.indexOf('分') > 0) {
                                result += '钟'
                            }
                            return `${result}前`
                        }
                    } catch (err) {
                        return '---'
                    }
                } else {
                    return '--'
                }
            },
            /**
             * 计算执行中任务的百分比
             */
            calcPercentage (item) {
                const {
                    latestBuildStartTime,
                    latestBuildEstimatedExecutionSeconds
                } = item
                const time = +new Date()

                if (!latestBuildStartTime || !latestBuildEstimatedExecutionSeconds) {
                    return '0%'
                }

                const calcTime = Math.ceil((time - latestBuildStartTime) / (latestBuildEstimatedExecutionSeconds * 100 * 1000))

                if (this.statusMap[item.latestBuildStatus] === 'error') {
                    return '100%'
                }

                if (calcTime > 99) {
                    return '99%'
                } if (calcTime < 1) {
                    return '1%'
                } else {
                    return `${calcTime}%`
                }
            },
            /**
             *  初始化页面数据
             */
            async init () {
                await this.requestTemplatePermission()
                await this.requestHasCreatePermission()
                await this.requestPipelineList(this.sortType)
                this.requestGrouptLists()
            },

            async initPage () {
                console.time('init')
                this.togglePageLoading(true)
                await this.init()

                this.showContent = true
                console.timeEnd('init')
                this.togglePageLoading(false)
            },

            requestTemplatePermission () {
                this.$store.dispatch('pipelines/requestTemplatePermission', this.projectId).then((res) => {
                    this.hasTemplatePermission = res
                })
            },

            /**
             *  请求pipeline列表
             */
            async requestPipelineList (sortType) {
                const {
                    $store,
                    filter
                } = this
                let response
                try {
                    // response = await $store.dispatch('pipelines/requestAllPipelinesList', {
                    response = await $store.dispatch('pipelines/requestAllPipelinesListByFilter', {
                        projectId: this.projectId,
                        page: 1,
                        pageSize: -1,
                        sortType: sortType || 'CREATE_TIME',
                        filterByLabels: filter.filterByLabels,
                        filterByPipelineName: filter.filterByPipelineName,
                        filterByCreator: filter.filterByCreator,
                        viewId: this.currentViewId
                    })

                    let res

                    if (response.count) {
                        const $data = response.records
                        let obj = {}

                        res = $data.map((item, index) => {
                            obj = {
                                name: item.pipelineName,
                                desc: typeof item.pipelineDesc === 'string' && item.pipelineDesc.trim(),
                                isRunning: false,
                                status: '',
                                content: [
                                    {
                                        key: this.$t('lastBuildNum'),
                                        value: `${item.latestBuildNum ? `#${item.latestBuildNum}` : '--'}`
                                    },
                                    {
                                        key: this.$t('lastExecTime'),
                                        value: ''
                                    }
                                ],
                                runningInfo: {
                                    time: 0,
                                    percentage: this.calcPercentage(item),
                                    log: '',
                                    buildCount: item.runningBuildCount || 0
                                },
                                footer: [
                                    {
                                        upperText: item.taskCount,
                                        lowerText: this.$t('newlist.totalAtomNums'),
                                        handler: this.goEditPipeline
                                    },
                                    {
                                        upperText: item.buildCount,
                                        lowerText: this.$t('newlist.execTimes'),
                                        handler: this.goHistory
                                    }
                                ],
                                marginRight: 0,
                                width: '352px',
                                customBtns: [],
                                buttonAllow: {
                                    confirmFailure: true,
                                    terminatePipeline: true,
                                    exec: true,
                                    continuePipeline: true
                                },
                                pipelineId: item.pipelineId,
                                buildId: item.latestBuildId || 0,
                                extMenu: [],
                                isInstanceTemplate: item.instanceFromTemplate
                            }

                            item.feConfig = obj
                            return item
                        })
                    } else {
                        res = []
                    }
                    $store.commit('pipelines/updateAllPipelineList', res)
                    this.updatePipelineStatus(res, true)
                } catch (err) {
                    $store.commit('pipelines/updateAllPipelineList', [])
                    this.$showTips({
                        message: err.message || err,
                        theme: 'error'
                    })
                }
            },
            /** *
             * 获取标签及其分组
             */
            async requestGrouptLists () {
                const { $store } = this
                let res
                try {
                    res = await $store.dispatch('pipelines/requestGetGroupLists', {
                        projectId: this.projectId
                    })
                    $store.commit('pipelines/updateGroupLists', res)
                } catch (err) {
                    this.$showTips({
                        message: err.message || err,
                        theme: 'error'
                    })
                }
            },
            async requestHasCreatePermission () {
                const { $store } = this
                let res
                try {
                    res = await $store.dispatch('pipelines/requestHasCreatePermission', {
                        projectId: this.projectId
                    })
                    $store.commit('pipelines/updateCreatePermission', res)
                } catch (err) {
                    this.$showTips({
                        message: err.message || err,
                        theme: 'error'
                    })
                }
            },
            /**
             *  处理收藏和取消收藏
             */
            async togglePipelineCollect (pipelineId) {
                const pipeline = this.pipelineList.find(item => item.pipelineId === pipelineId)
                if (!pipeline) return
                const isCollect = !pipeline.hasCollect
                try {
                    await this.$store.dispatch('pipelines/requestToggleCollect', {
                        projectId: this.projectId,
                        pipelineId,
                        isCollect
                    })
                    pipeline.hasCollect = isCollect
                    pipeline.feConfig.extMenu[1].text = isCollect ? this.$t('uncollect') : this.$t('collect')
                    if (this.currentViewId === 'collect' && !isCollect) {
                        this.$store.commit('pipelines/removePipelineById', pipelineId)
                    }

                    this.$showTips({
                        message: isCollect ? this.$t('collectSuc') : this.$t('uncollectSuc'),
                        theme: 'success'
                    })
                } catch (err) {
                    this.$showTips({
                        message: err.message || err,
                        theme: 'error'
                    })
                }
            },
            /**
             *  跳转编辑
             */
            goEditPipeline (pipelineId) {
                this.$router.push({
                    name: 'pipelinesEdit',
                    params: {
                        projectId: this.projectId,
                        pipelineId: pipelineId
                    }
                })
            },
            /**
             *  跳转执行历史
             */
            goHistory (pipelineId) {
                this.$router.push({
                    name: 'pipelinesHistory',
                    params: {
                        projectId: this.projectId,
                        pipelineId: pipelineId
                    }
                })
            },
            /**
             *  请求pipeline列表状态
             */
            async requestPipelineStatus (isAll) {
                const {
                    $store,
                    pipelineList,
                    projectId
                } = this

                const pipelineId = isAll
                    ? pipelineList.map((item, index) => {
                        return item.pipelineId
                    })
                    : pipelineList
                        .filter(item => item.feConfig && item.feConfig.status === 'running')
                        .map(item => item.pipelineId)

                if (pipelineId.length === 0) return false

                try {
                    const data = await $store.dispatch('pipelines/requestPipelineStatus', {
                        projectId,
                        pipelineId
                    })

                    this.updatePipelineStatus(data)
                } catch (err) {
                    this.$showTips({
                        message: err.message || err,
                        theme: 'error'
                    })
                }
                return true
            },
            updatePipelineStatus (data, isFirst = false) {
                const {
                    $store,
                    pipelineList,
                    statusMap
                } = this
                const knownErrorList = JSON.parse(localStorage.getItem('pipelineKnowError'))

                pipelineList.forEach(pipeline => {
                    const cur = isFirst ? pipeline : data[pipeline.pipelineId]
                    if (cur) {
                        const customBtns = []
                        let isRunning = false
                        const status = statusMap[cur.latestBuildStatus]
                        let mappedStatus

                        // 是否已经确认错误
                        if (status === 'error') {
                            if (knownErrorList) {
                                if (knownErrorList[`${pipeline.projectId}_${pipeline.pipelineId}_${pipeline.latestBuildId}`]) {
                                    mappedStatus = 'known_error'
                                } else {
                                    mappedStatus = 'error'
                                }
                            } else {
                                mappedStatus = 'error'
                            }
                        } else {
                            mappedStatus = status || 'not_built'
                        }

                        // 单独修改当前任务是否在执行的状态
                        switch (mappedStatus) {
                            case 'known_error':
                            case 'success':
                                isRunning = false
                                break
                            case 'running':
                            case 'warning':
                            case 'error':
                                isRunning = true
                                break
                            default:
                                isRunning = false
                        }

                        $store.commit('pipelines/updatePipelineValueById', {
                            pipelineId: pipeline.pipelineId,
                            obj: {
                                isRunning
                            }
                        })

                        // 拼接右下角按钮
                        switch (mappedStatus) {
                            case 'error':
                                customBtns.splice(0, customBtns.length, {
                                    icon: 'check-1',
                                    text: this.$t('newlist.known'),
                                    handler: 'error-noticed'
                                })
                                break
                            case 'running':
                                customBtns.splice(0, customBtns.length, {
                                    text: this.$t('terminate'),
                                    handler: 'terminate-pipeline'
                                })
                                break
                            case 'warning': {
                                const tmpArr = [
                                    {
                                        text: this.$t('resume'),
                                        handler: 'resume-pipeline'
                                    },
                                    {
                                        text: this.$t('terminate'),
                                        handler: 'terminate-pipeline'
                                    }
                                ]
                                customBtns.splice(0, customBtns.length, ...tmpArr)
                                break
                            }
                        }

                        const obj = {
                            status: mappedStatus,
                            content: {
                                index: [0, 1],
                                key: ['value', 'value'],
                                value: [cur.latestBuildNum ? `#${cur.latestBuildNum}` : '--', cur.latestBuildStartTime ? this.calcLatestStartBuildTime(cur) : this.$t('newlist.noExecution')]
                            },
                            footer: {
                                index: [0, 1],
                                key: ['upperText', 'upperText'],
                                value: [cur.taskCount, cur.buildCount]
                            },
                            runningInfo: {
                                key: ['time', 'percentage', 'log', 'buildCount'],
                                value: [convertMStoStringByRule(status === 'error' ? (cur.latestBuildEndTime - cur.latestBuildStartTime) : (cur.currentTimestamp - cur.latestBuildStartTime)), this.calcPercentage(cur), cur.latestBuildTaskName, cur.runningBuildCount || 0]
                            },
                            customBtns,
                            buildId: cur.latestBuildId || 0
                        }
                        const cPipeline = this.pipelineList.find(item => item.pipelineId === pipeline.pipelineId)
                        if (!pipeline.feConfig.extMenu.length) {
                            obj.extMenu = [
                                {
                                    text: this.$t('edit'),
                                    handler: this.goEditPipeline
                                },
                                {
                                    text: (cPipeline.hasCollect ? this.$t('uncollect') : this.$t('collect')),
                                    // text: cur.favor,
                                    handler: this.togglePipelineCollect
                                },
                                {
                                    text: this.$t('newlist.copyAs'),
                                    handler: this.copyPipeline
                                },
                                {
                                    text: this.$t('newlist.saveAsTemp'),
                                    disable: !this.hasTemplatePermission,
                                    handler: this.copyAsTemplate
                                },
                                {
                                    text: this.$t('delete'),
                                    handler: this.deletePipeline
                                }
                            ]
                        }

                        $store.commit('pipelines/updatePipelineValueById', {
                            pipelineId: pipeline.pipelineId,
                            obj
                        })
                    }
                })
            },
            /**
             * 触发执行流水线
             */
            async triggersExec (params, pipelineId) {
                const {
                    $store,
                    projectId,
                    pipelineList
                } = this
                const curPipeline = pipelineList.find(item => item.pipelineId === pipelineId)
                const {
                    feConfig
                } = curPipeline
                let message = ''
                let theme = ''

                try {
                    // 请求执行构建
                    const res = await $store.dispatch('pipelines/requestExecPipeline', {
                        projectId,
                        pipelineId: pipelineId,
                        params
                    })

                    message = this.$t('newlist.sucToStartBuild')
                    theme = 'success'
                    curPipeline.latestBuildId = res.id

                    // 重置执行进度
                    const runningInfo = feConfig.runningInfo
                    runningInfo.percentage = '1%'
                    runningInfo.time = '0秒'
                    runningInfo.log = ''
                    runningInfo.buildCount = 1

                    feConfig.status = 'running'
                    feConfig.isRunning = true
                } catch (err) {
                    if (err.code === 403) { // 没有权限执行
                        this.setPermissionConfig(this.$permissionResourceMap.pipeline, this.$permissionActionMap.execute, [{
                            id: curPipeline.pipelineId,
                            name: curPipeline.pipelineName
                        }], projectId, this.getPermUrlByRole(projectId, curPipeline.pipelineId, this.roleMap.executor))
                        return
                    } else {
                        feConfig.status = 'known_error'
                        feConfig.isRunning = false

                        message = err.message || err
                        theme = 'error'
                    }
                } finally {
                    message && this.$showTips({
                        message,
                        theme
                    })
                }
            },
            /**
             *  任务执行错误，点击我知道了的回调函数
             */
            async errorNoticed (pipelineId) {
                let knownErrorList = JSON.parse(localStorage.getItem('pipelineKnowError'))
                // let target = pipelineList[index]
                const target = this.pipelineList.find(item => item.pipelineId === pipelineId)
                const key = `${target.projectId}_${target.pipelineId}_${target.latestBuildId}`

                if (!knownErrorList) {
                    knownErrorList = {
                        [key]: 1
                    }
                } else {
                    knownErrorList[key] = 1
                }

                localStorage.setItem('pipelineKnowError', JSON.stringify(knownErrorList))
                // 更新DOM节点的样式
                if (target.feConfig) {
                    target.feConfig.status = 'known_error'
                    target.feConfig.isRunning = false
                }
            },
            /**
             *  终止任务
             */
            async terminatePipeline (pipelineId) {
                const { $store, projectId } = this
                const target = this.pipelineList.find(item => item.pipelineId === pipelineId)
                const {
                    feConfig
                } = target

                if (!feConfig.buttonAllow.terminatePipeline) return

                feConfig.buttonAllow.terminatePipeline = false

                try {
                    await $store.dispatch('pipelines/requestTerminatePipeline', {
                        projectId,
                        pipelineId: target.pipelineId,
                        buildId: feConfig.buildId || target.latestBuildId
                    })

                    $store.commit('pipelines/updatePipelineValueById', {
                        pipelineId,
                        obj: {
                            isRunning: false,
                            status: 'known_error'
                        }
                    })
                } catch (err) {
                    this.handleError(err, [{
                        actionId: this.$permissionActionMap.execute,
                        resourceId: this.$permissionResourceMap.pipeline,
                        instanceId: [{
                            id: target.pipelineId,
                            name: target.pipelineName
                        }],
                        projectId
                    }], this.getPermUrlByRole(projectId, target.pipelineId, this.roleMap.executor))
                } finally {
                    feConfig.buttonAllow.terminatePipeline = true
                }
            },
            /**
             *  继续任务
             */
            async resumePipeline (pipelineId) {
                const { $store, projectId } = this
                const target = this.pipelineList.find(item => item.pipelineId === pipelineId)
                const {
                    feConfig
                } = target

                if (!feConfig.buttonAllow.continuePipeline) return

                feConfig.buttonAllow.continuePipeline = false

                try {
                    await $store.dispatch('pipeline/requestResumePipeline', {
                        projectId,
                        pipelineId: target.id
                    })

                    $store.commit('pipelines/updatePipelineValueById', {
                        pipelineId,
                        obj: {
                            isRunning: true,
                            status: 'running'
                        }
                    })
                } catch (err) {
                    this.$showTips({
                        message: err.data ? err.data.message : err,
                        theme: 'error'
                    })
                } finally {
                    feConfig.buttonAllow.continuePipeline = true
                }
            },
            /**
             *  点击卡片的title跳转详情
             */
            titleClickHandler (pipelineId) {
                this.$router.push({
                    name: 'pipelinesHistory',
                    params: {
                        projectId: this.projectId,
                        pipelineId: pipelineId
                    }
                })
            },

            /**
             * 另存为模板
             */
            copyAsTemplate (pipelineId) {
                const curPipeline = this.pipelineList.find(item => item.pipelineId === pipelineId)
                this.saveAsTemp.templateName = `${curPipeline.pipelineName}_template`
                this.saveAsTemp.isShow = true
                this.saveAsTemp.pipelineId = pipelineId
            },

            async saveAsConfirmHandler () {
                if (this.errors.has('saveTemplateName')) return
                const projectId = this.projectId
                const postData = {
                    pipelineId: this.saveAsTemp.pipelineId,
                    templateName: this.saveAsTemp.templateName,
                    isCopySetting: this.saveAsTemp.isCopySetting
                }

                this.$store.dispatch('pipelines/saveAsTemplate', { projectId, postData }).then(({ id }) => {
                    this.saveAsCancelHandler()
                    this.$showTips({ message: this.$t('newlist.saveAsTempSuc'), theme: 'success' })
                    this.$router.push({
                        name: 'templateEdit',
                        params: { templateId: id }
                    })
                }).catch((err) => {
                    const message = err.message || this.$t('newlist.saveAsTempFail')
                    this.$showTips({ message, theme: 'error' })
                })
            },

            saveAsCancelHandler () {
                this.saveAsTemp.isShow = false
                this.saveAsTemp.templateName = ''
                this.saveAsTemp.pipelineId = ''
                this.saveAsTemp.nameHasError = false
            },

            /**
             *  复制流水线
             */
            copyPipeline (pipelineId) {
                const {
                    copyDialogConfig
                } = this
                const curPipeline = this.pipelineList.find(item => item.pipelineId === pipelineId)
                this.copyConfig.newPipelineName = `${curPipeline.pipelineName}_copy`
                copyDialogConfig.isShow = true
                copyDialogConfig.pipelineId = pipelineId
            },
            /**
             *  删除流水线
             */
            deletePipeline (pipelineId) {
                const {
                    $store
                } = this
                const curPipeline = this.pipelineList.find(item => item.pipelineId === pipelineId)
                const content = `${this.$t('newlist.deletePipeline')}: ${curPipeline.pipelineName}?`

                navConfirm({ type: 'warning', content })
                    .then(() => {
                        let message, theme
                        const {
                            loading
                        } = this

                        loading.title = this.$t('newlist.deleteTips')
                        this.togglePageLoading(true)
                        setTimeout(async () => {
                            try {
                                await $store.dispatch('pipelines/deletePipeline', {
                                    projectId: this.projectId,
                                    pipelineId: curPipeline.pipelineId
                                })

                                $store.commit('pipelines/removePipelineById', pipelineId)
                                message = this.$t('deleteSuc')
                                theme = 'success'
                            } catch (err) {
                                this.handleError(err, [{
                                    actionId: this.$permissionActionMap.delete,
                                    resourceId: this.$permissionResourceMap.pipeline,
                                    instanceId: [{
                                        id: curPipeline.pipelineId,
                                        name: curPipeline.pipelineName
                                    }],
                                    projectId: this.projectId
                                }], this.getPermUrlByRole(this.projectId, curPipeline.pipelineId, this.roleMap.manager))
                            } finally {
                                message && this.$showTips({
                                    message,
                                    theme
                                })
                                this.togglePageLoading(false)
                            }
                        }, 1000)
                    }).catch(() => {})
            },
            /**
             *  复制流水线弹窗的确认回调函数
             */
            async copyConfirmHandler () {
                const {
                    copyConfig: {
                        newPipelineName,
                        newPipelineDesc
                    },
                    copyConfig,
                    projectId,
                    $store
                } = this
                let message = ''
                let theme = ''

                if (!newPipelineName) {
                    copyConfig.nameHasError = true
                    return false
                }
                const prePipeline = this.pipelineList.find(item => item.pipelineId === this.copyDialogConfig.pipelineId)

                try {
                    copyConfig.loading = true

                    const res = await $store.dispatch('pipelines/copyPipeline', {
                        projectId,
                        pipelineId: this.copyDialogConfig.pipelineId,
                        args: {
                            name: newPipelineName,
                            desc: newPipelineDesc,
                            group: prePipeline.group,
                            hasCollect: false
                        }
                    })

                    message = this.$t('copySuc')
                    theme = 'success'
                    setTimeout(() => {
                        this.copyDialogConfig.isShow = false
                    }, 500)

                    const copyItem = JSON.parse(JSON.stringify(prePipeline))
                    const extMenu = [
                        {
                            text: this.$t('edit'),
                            handler: this.goEditPipeline
                        },
                        {
                            text: this.$t('collect'),
                            handler: this.togglePipelineCollect
                        },
                        {
                            text: this.$t('newlist.copyAs'),
                            handler: this.copyPipeline
                        },
                        {
                            text: this.$t('newlist.saveAsTemp'),
                            handler: this.copyAsTemplate
                        },
                        {
                            text: this.$t('delete'),
                            handler: this.deletePipeline
                        }
                    ]
                    const footer = [
                        {
                            upperText: copyItem.feConfig.footer[0].upperText,
                            lowerText: this.$t('newlist.totalAtomNums'),
                            handler: this.goEditPipeline
                        },
                        {
                            upperText: '0',
                            lowerText: this.$t('newlist.execTimes'),
                            handler: this.goHistory
                        }
                    ]
                    copyItem.pipelineId = res.id
                    copyItem.hasCollect = false
                    copyItem.pipelineName = newPipelineName
                    copyItem.pipelineDesc = newPipelineDesc
                    copyItem.latestBuildStatus = ''
                    copyItem.feConfig.name = newPipelineName
                    copyItem.feConfig.desc = newPipelineDesc
                    copyItem.feConfig.pipelineId = res.id
                    copyItem.feConfig.content[1].value = this.$t('newlist.noExecution')
                    copyItem.feConfig.status = 'not_built'
                    // copyItem.feConfig.footer[1].upperText = 0
                    copyItem.feConfig.extMenu.splice(0, copyItem.feConfig.extMenu.length, ...extMenu)
                    copyItem.feConfig.footer.splice(0, copyItem.feConfig.footer.length, ...footer)
                    copyItem.feConfig.isInstanceTemplate = false

                    $store.commit('pipelines/addPipeline', {
                        item: copyItem
                    })
                } catch (err) {
                    if (err.code === 403) { // 没有权限复制
                        this.copyDialogConfig.isShow = false
                        this.handleError(err, [{
                            actionId: this.$permissionActionMap.create,
                            resourceId: this.$permissionResourceMap.pipeline,
                            instanceId: [{
                                id: prePipeline.pipelineId,
                                name: prePipeline.pipelineName
                            }],
                            projectId
                        }, {
                            actionId: this.$permissionActionMap.edit,
                            resourceId: this.$permissionResourceMap.pipeline,
                            instanceId: [{
                                id: prePipeline.pipelineId,
                                name: prePipeline.pipelineName
                            }],
                            projectId
                        }], this.getPermUrlByRole(projectId, prePipeline.pipelineId, this.roleMap.manager))
                    } else {
                        message = err.message || err
                        theme = 'error'
                    }
                } finally {
                    setTimeout(() => {
                        copyConfig.loading = false
                        message && this.$showTips({
                            message,
                            theme
                        })
                    }, 300)
                }
            },
            /**
             *  复制流水线弹窗的取消回调
             */
            copyCancelHandler () {
                const {
                    copyConfig
                } = this

                copyConfig.newPipelineName = ''
                copyConfig.newPipelineDesc = ''
            }
        }
    }
</script>

<style lang='scss'>
    @import './../../scss/conf';

    .pipeline-list {
        height: calc(100% - 60px);
        padding-top: 2px;
        .devops-empty-tips {
            .bk-button {
                width: 147px;
                height: 42px;
                line-height: 40px;
            }
        }
        &-wrapper {
            padding-top: 20px;
            margin: 0 auto;
            @media only screen and (max-width: 1359px) {
                width: 973px;
                .task-card:nth-child(3n) {
                    margin-right: 0;
                }

            }
            @media only screen and (min-width: 1360px) and (max-width: 1694px) {
                width: 1304px;
                .task-card:nth-child(4n) {
                    margin-right: 0;
                }
            }
            @media only screen and (min-width: 1695px) and (max-width: 2025px) {
                width: 1635px;
                .task-card:nth-child(5n) {
                    margin-right: 0;
                }
            }
            @media only screen and (min-width: 2026px) {
                width: 1966px;
                .task-card:nth-child(6n) {
                    margin-right: 0;
                }
            }
        }
        &-content {
            padding-top: 20px;
        }
        &-cards {
            .task-card {
                float: left;
                width: 311px;
                margin: 0 20px 20px 0;
            }
        }
        .loading-wrapper {
            min-height: calc(100% - 60px);
            height: 100%;
            overflow: auto;
        }
        .toggle-layout {
            display: inline-block;
            margin-top: 8px;
            font-size: 24px;
            color: #c3cdd7;
            cursor: pointer;
        }
        .table-list-name {
            max-width: 300px;
            padding-left: 30px;
        }
        .table-list-progress {
            display: inline-block;
            width: 50%;
            & + .row-item-desc {
                vertical-align: bottom;
            }
        }
        .inline-component {
            display: inline-block;
        }
        .row-item-desc {
            display: inline-block;
            width: 80px;
            line-height: 32px;
            vertical-align: text-top;
        }
        .item-text-btn {
            display: inline-block;
            height: 32px;
            line-height: 32px;
            vertical-align: text-bottom;
            &.noticed {
                display: inline-block;
                height: 24px;
                line-height: 22px;
                border: 1px solid $primaryColor;
                padding: 0 10px;
                vertical-align: top;
                color: $primaryColor;
                transition: all .2s linear;
                &:hover {
                    box-shadow: 0 2px 2px 2px #f2f2f2;
                }
            }
        }
        .row-task-count,
        .row-build-count {
            display: block;
            width: 30px;
            height: 30px;
            line-height: 30px;
            font-size: 14px;
        }
        .row-status {
            &.success {
                color: $successColor;
            }
            &.error,
            &.known_error {
                color: $dangerColor;
            }
        }
        .table-list-name {
            position: relative;
            &:before {
                position: absolute;
                left: 18px;
                top: 50%;
                transform: translateY(-50%);
                content: '';
                width: 5px;
                height: 15px;
                border-radius: 3px;
                border: 1px solid transparent;
            }
            &.success {
                &:before {
                    border-color: $successColor;
                    background-color: #cdffe2;
                }
            }
            &.error,
            &.known_error {
                &:before {
                    border-color: $dangerColor;
                    background-color: #febcbb;
                }
            }
            .text-link {
                font-size: 14px;
            }
        }
    }
    .copy-pipeline {
        .bk-label {
            width: 100px;
        }
        .bk-form-content {
            margin-left: 110px;
        }
        .tip-bottom {
            padding: 0;
            margin: 10px 20px 10px 10px;
            width: auto;
        }
        .auth-radio {
            margin: 15px 30px 0 0;
        }
        .err-name {
            text-align: left;
            margin-left: 100px;
            margin-bottom: -21px;
        }
        .form-field-icon {
            position: relative;
            left: -16px;
            top: 14px;
        }
    }
</style>
