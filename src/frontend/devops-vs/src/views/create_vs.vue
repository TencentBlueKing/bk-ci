<template>
    <div class="vs-create-wrapper">
        <content-header>
            <div slot="left">新增扫描</div>
            <p slot="right">本服务由金刚团队（企业微信：KingKong）提供后台支持</p>
        </content-header>

        <section class="sub-view-port" ref="scrollBox"
            v-bkloading="{
                isLoading: loading.isLoading,
                title: loading.title
            }">
            <template v-if="showContent">
                <div class="select-nav">
                    <div class="select-content pipeline-content">
                        <label class="select-label">流水线</label>
                        <bk-select v-model="selectInfo.pipelineId" style="width: 300px;" :clearable="false" searchable :loading="isPipelineLoading">
                            <bk-option v-for="(option, index) in pipelineList"
                                :key="index"
                                :id="option.pipelineId"
                                :name="option.pipelineName">
                            </bk-option>
                        </bk-select>
                    </div>
                    <div class="select-content construct-content">
                        <label class="select-label">构建号</label>
                        <bk-select v-model="selectInfo.constructId" style="width: 300px;" :clearable="false" searchable :loading="isConstructLoading" @change="changeBuildNo">
                            <bk-option v-for="(option, index) in constructList"
                                :key="index"
                                :id="option.id"
                                :name="option.buildNum">
                            </bk-option>
                        </bk-select>
                    </div>
                </div>
                <bk-table style="margin-top: 15px;" :data="fileList" empty-text="暂无数据">
                    <bk-table-column label="名称" class="name-item" width="300">
                        <template slot-scope="props">
                            <div :title="props.row.name">
                                <i class="devops-icon icon-file"></i>
                                <span><span class="buildno-item" v-if="selectInfo.constructId === 'all'">{{ props.row.buildDesc }}&nbsp;</span>{{ props.row.name }}</span>
                            </div>
                        </template>
                    </bk-table-column>
                    <bk-table-column label="路径" prop="source" class="path-item">
                        <template slot-scope="props">
                            <span :title="props.row.fullName">{{ props.row.fullName }}</span>
                        </template>
                    </bk-table-column>
                    <bk-table-column label="大小" prop="status" width="150">
                        <template slot-scope="props">
                            <span>{{ props.row.size }}&nbsp;MB</span>
                        </template>
                    </bk-table-column>
                    <bk-table-column label="仓库类型" prop="create_time" width="150">
                        <template slot-scope="props">
                            <span v-if="props.row.artifactoryType === 'CUSTOM_DIR'">自定义仓库</span>
                            <span v-if="props.row.artifactoryType === 'PIPELINE'">流水线仓库</span>
                        </template>
                    </bk-table-column>
                    <bk-table-column width="150" :render-header="renderHeader">
                        <template slot-scope="props">
                            <span @click="toScanFile(props.row, '3')" class="handler-item">静态扫描</span>
                            <span style="margin-left: 8px;" @click="toScanFile(props.row, '1')" class="handler-item">完整扫描</span>
                        </template>
                    </bk-table-column>
                </bk-table>
            </template>
        </section>
    </div>
</template>

<script>
    export default {
        data () {
            return {
                showContent: false,
                moreLoading: false,
                scrollDisable: false,
                isPipelineLoading: false,
                isConstructLoading: false,
                fileList: [],
                pipelineList: [],
                constructList: [],
                selectInfo: {
                    pipelineId: '',
                    constructId: ''
                },
                loading: {
                    isLoading: false,
                    title: ''
                },
                config: {
                    page: 1,
                    pageSize: 30
                }
            }
        },
        computed: {
            projectId () {
                return this.$route.params.projectId
            }
        },
        watch: {
            projectId (val) {
                this.scrollDisable = false
                this.$router.push({
                    name: 'vsList',
                    params: {
                        projectId: this.projectId
                    }
                })
            },
            'selectInfo.pipelineId' (val) {
                this.selectInfo.constructId = ''
                this.getDefaultFile()
            }
        },
        async mounted () {
            // this.$refs.scrollBox.addEventListener('scroll', this.handleScroll)
            await this.init()
        },
        methods: {
            renderHeader (h) {
                const head = h('div', [
                    '操作',
                    h('bk-popover', {
                        scopedSlots: {
                            content () {
                                return h('div', [
                                    h('p', '静态扫描：运行较快，预计10分钟之内出结果；'),
                                    h('p', '完整扫描：包括静态扫描和动态扫描，真机运行动态扫描，运行较慢，'),
                                    h('p', '占用系统资源消较大，预计2小时出结果。')
                                ])
                            }
                        }
                    }, [
                        h('i', { class: ['devops-icon', 'icon-question-circle'] })
                    ])
                ])

                return head
            },

            async init () {
                const {
                    loading
                } = this

                loading.isLoading = true
                loading.title = '数据加载中，请稍候'

                try {
                    await this.refreshPipelineList()
                } catch (err) {
                    this.$bkMessage({
                        message: err.message ? err.message : err,
                        theme: 'error'
                    })
                } finally {
                    setTimeout(() => {
                        this.loading.isLoading = false
                    }, 1000)
                }
            },
            async requestFileList () {
                let response = []
                const params = {
                    fileNames: ['*.ipa', '*.apk'],
                    props: {
                        pipelineId: this.selectInfo.pipelineId,
                        buildId: this.selectInfo.constructId === 'all' ? undefined : this.selectInfo.constructId
                    }
                }

                try {
                    const res = await this.$store.dispatch('vs/requestFileList', {
                        projectId: this.projectId,
                        page: this.config.page,
                        pageSize: this.config.pageSize,
                        params: params
                    })

                    res.records.forEach(item => {
                        item.size = ((item.size / 1024) / 1024).toFixed(2)
                        item.properties.forEach(kk => {
                            if (kk.key === 'buildNo') {
                                item.buildDesc = `(#${kk.value})`
                                item.buildNo = kk.value
                            }
                            if (kk.key === 'buildId') {
                                item.buildId = kk.value
                            }
                        })
                    })
                    response = res.records
                } catch (err) {
                    this.$bkMessage({
                        message: err.message || err,
                        theme: 'error'
                    })
                }
                return response
            },
            /**
             获取文件列表
             *
             */
            async requestList () {
                this.loading.isLoading = true

                const params = {
                    fileNames: ['*.ipa', '*.apk'],
                    props: {
                        pipelineId: this.selectInfo.pipelineId,
                        buildId: this.selectInfo.constructId === 'all' ? undefined : this.selectInfo.constructId
                    }
                }

                try {
                    const res = await this.$store.dispatch('vs/requestFileList', {
                        projectId: this.projectId,
                        params: params
                    })

                    this.fileList.splice(0, this.fileList.length)
                    res.records.forEach(item => {
                        item.size = ((item.size / 1024) / 1024).toFixed(2)
                        item.properties.forEach(kk => {
                            if (kk.key === 'buildNo') {
                                item.buildDesc = `(#${kk.value})`
                                item.buildNo = kk.value
                            }
                            if (kk.key === 'buildId') {
                                item.buildId = kk.value
                            }
                        })
                        this.fileList.push(item)
                    })
                } catch (err) {
                    this.$bkMessage({
                        message: err.message || err,
                        theme: 'error'
                    })
                } finally {
                    this.loading.isLoading = false
                }
            },

            async refreshPipelineList (val) {
                try {
                    const res = await this.$store.dispatch('vs/requestPipelineList', {
                        projectId: this.projectId,
                        params: { pageSize: -1 }
                    })

                    this.pipelineList.splice(0, this.pipelineList.length, ...res.records)
                    if (res.records.length) {
                        this.selectInfo.pipelineId = this.pipelineList[0].pipelineId
                    }
                } catch (err) {
                    this.$bkMessage({
                        message: err.message || err,
                        theme: 'error'
                    })
                } finally {
                    this.isPipelineLoading = false
                }
                this.showContent = true
            },
            async toScanFile (row, type) {
                const payload = {
                    path: row.path,
                    artifactoryType: row.artifactoryType
                }

                const result = await this.$store.dispatch('vs/requestHasPermission', {
                    projectId: this.projectId,
                    payload
                })

                if (result) {
                    this.confirmScan(row, type)
                } else {
                    this.$showAskPermissionDialog({
                        noPermissionList: [{
                            actionId: this.$permissionActionMap.execute,
                            resourceId: this.$permissionResourceMap.pipeline,
                            instanceId: [{
                                id: this.selectInfo.pipelineId,
                                name: this.selectInfo.pipelineId
                            }],
                            projectId: this.projectId
                        }],
                        applyPermissionUrl: `/backend/api/perm/apply/subsystem/?client_id=code&project_code=${this.projectId}&service_code=pipeline&role_executor=pipeline:${this.selectInfo.pipelineId}`
                    })
                }
            },
            confirmScan (row, type) {
                this.$bkInfo({
                    title: '确认',
                    subTitle: '确认扫描该文件吗',
                    confirmFn: async () => {
                        let message, theme
                        const params = {
                            buildNo: row.buildNo,
                            file: row.path,
                            isCustom: row.artifactoryType === 'CUSTOM_DIR',
                            runType: type
                        }

                        this.loading.isLoading = true

                        try {
                            await this.$store.dispatch('vs/toScanFile', {
                                projectId: this.projectId,
                                pipelineId: this.selectInfo.pipelineId,
                                buildId: row.buildId,
                                params
                            })

                            message = '操作成功'
                            theme = 'success'
                        } catch (err) {
                            message = err.data ? err.data.message : err
                            theme = 'error'
                        } finally {
                            this.$bkMessage({
                                message,
                                theme
                            })

                            if (theme === 'success') {
                                this.$router.push({
                                    name: 'vsList',
                                    params: {
                                        projectId: this.projectId
                                    }
                                })
                            }

                            this.loading.isLoading = false
                        }
                    }
                })
            },
            changeBuildNo () {
                if (this.selectInfo.constructId) {
                    this.requestList()
                }
            },

            async getDefaultFile () {
                if (this.selectInfo.pipelineId) {
                    this.loading.isLoading = true
                    this.isConstructLoading = true

                    try {
                        const res = await this.$store.dispatch('vs/requestBuildList', {
                            projectId: this.projectId,
                            pipelineId: this.selectInfo.pipelineId,
                            params: { pageSize: -1, checkPermission: false }
                        })

                        this.constructList.splice(0, this.constructList.length, ...res.records)
                        this.constructList.unshift({ id: 'all', buildNum: '全部' })
                        if (this.constructList.length) {
                            this.selectInfo.constructId = this.constructList[0].id
                            this.changeBuildNo()
                        }
                    } catch (err) {
                        this.$bkMessage({
                            message: err.message || err,
                            theme: 'error'
                        })
                    } finally {
                        this.isConstructLoading = false
                    }
                }
            },
            async handleScroll () {
                const node = this.$refs.scrollBox
                const scrollTop = node.scrollTop
                if (scrollTop + window.innerHeight >= node.scrollHeight) {
                    // 触发加载数据
                    if (!this.scrollDisable) {
                        this.scrollDisable = true
                        this.moreLoading = true
                        let res = []
                        res = await this.requestFileList()
                        if (res) {
                            if (res.records !== undefined && res.records.length) {
                                this.config.page = this.config.page + 1
                                this.fileList = this.fileList.concat(res.records)
                                this.scrollDisable = false
                            } else {
                                this.scrollDisable = true
                            }
                        }
                        this.moreLoading = false
                    }
                }
            }
        }
    }
</script>

<style lang="scss">
    @import './../scss/conf';

    .vs-create-wrapper {
        overflow: auto;
        .create-main-container {
            padding: 20px;
            min-width: 960px;
        }

        .select-nav {
            margin-bottom: 20px;
            width: 100%;
            display: flex;
            justify-content: flex-start;
        }

        .select-content {
            display: flex;
            justify-content: flex-start;

            .bk-selector {
                width: 320px;
            }
        }

        .select-label {
            margin-top: 6px;
            width: 80px;
            text-align: center;
            font-weight: bold;
        }

        .pipeline-content {
            margin-right: 30px;
        }

        .file-table-wrapper {
            margin-bottom: 20px;
            min-height: calc(100% - 56px);
            border: 1px solid $borderWeightColor;
            background-color: #fff;
        }

        .folder-loading {
            text-align: center;
            padding-top: 30px;
            padding-bottom: 30px;
            font-size: 24px;
            i {
                display: inline-block;
            }
        }
    }
</style>
