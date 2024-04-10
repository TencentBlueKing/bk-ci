<template>
    <bk-dialog v-model="versionSelectConf.isShow"
        width="800"
        ext-cls="version-package-wrapper"
        header-position="left"
        title="请选择ipa或apk文件"
        :has-header="versionSelectConf.hasHeader"
        :close-icon="versionSelectConf.closeIcon"
        :quick-close="versionSelectConf.quickClose"
        :ok-text="versionSelectConf.confirmText"
        @confirm="confirmFn"
        @cancel="cancelFn"
    >
        <div class="artifactory-container" v-bkloading="{ isLoading: loading.isLoading, title: loading.title }">
            <bk-form :model="selectInfo" form-type="inline" class="select-nav">
                <bk-form-item label="流水线">
                    <bk-select
                        enable-scroll-load
                        searchable
                        ref="pipeline"
                        v-model="selectInfo.pipelineId"
                        :remote-method="handleSearch"
                        :scroll-loading="bottomLoadingOptions"
                        @scroll-end="handleScrollToBottom"
                        :clearable="false"
                    >
                        <bk-option v-for="item in pipelineList" :key="item.pipelineId" class="artifactory-option"
                            :id="item.pipelineId"
                            :name="item.pipelineName"
                        >
                        </bk-option>
                    </bk-select>
                </bk-form-item>
                <bk-form-item label="构建号">
                    <bk-select ref="build" searchable :loading="isConstructLoading" :disabled="!selectInfo.pipelineId" v-model="selectInfo.constructId" @toggle="refreshConstructList" @selected="changeBuildNo" :clearable="false">
                        <bk-option v-for="item in constructList" :key="item.id" class="artifactory-option"
                            :id="item.id"
                            :name="item.buildNum"
                        >
                        </bk-option>
                    </bk-select>
                </bk-form-item>
            </bk-form>
            <bk-table v-bkloading="{ isLoading: listLoading.isLoading, title: listLoading.title }"
                :max-height="400"
                :data="fileList"
                empty-text="暂无数据"
                
            >
                <bk-table-column label="名称" prop="name" width="250">
                    <template slot-scope="props">
                        <label class="bk-form-radio">
                            <input type="radio" name="selectedFile" :checked="selectedFile.file.path === props.row.path" @change="handleFileSelect(props.row)" />
                            <i v-if="selectedFile.file.path === props.row.path" class="icon-check"></i>
                            <span class="bk-radio-text file-name" :title="props.row.name">{{props.row.buildNo}}{{ props.row.name }}</span>
                        </label>
                    </template>
                </bk-table-column>
                <bk-table-column label="路径" prop="fullName"></bk-table-column>
                <bk-table-column label="文件大小" prop="size" width="100"></bk-table-column>
                <bk-table-column label="仓库类型" prop="artifactoryTypeDesc"></bk-table-column>
            </bk-table>
        </div>
    </bk-dialog>
</template>

<script>

    export default {
        props: {
            versionSelectConf: Object,
            loading: Object,
            confirmFn: Function,
            cancelFn: Function
        },
        data () {
            return {
                isConstructLoading: false,
                fileList: [],
                pipelineList: [],
                constructList: [],
                selectInfo: {
                    pipelineId: '',
                    constructId: ''
                },
                listLoading: {
                    isLoading: false,
                    title: ''
                },
                selectedFile: {
                    file: {}
                },
                filterByPipelineName: '',
                bottomLoadingOptions: {
                    size: 'mini',
                    isLoading: true
                },
                pagination: {
                    page: 0,
                    pageSize: 10,
                    total: 0
                }
            }
        },
        computed: {
            projectId () {
                return this.$route.params.projectId
            },
            artifactoryTypeMap () {
                return {
                    CUSTOM_DIR: '自定义仓库',
                    PIPELINE: '流水线仓库'
                }
            }
        },
        watch: {
            'versionSelectConf.isShow' (val) {
            },
            'selectInfo.pipelineId' (val) {
                this.selectInfo.constructId = ''
                this.$store.dispatch('experience/updateCurSelectedFile', {
                    selectFile: {}
                })
                this.getDefaultFile()
            },
            'selectedFile.file' (val) {
                this.$store.dispatch('experience/updateCurSelectedFile', {
                    selectFile: val
                })
            }
        },
        mounted () {
            this.handleScrollToBottom()
        },
        methods: {
            async handleScrollToBottom () {
                if (this.pipelineList.length > 0 && this.pipelineList.length >= this.pagination.total) {
                    return
                }
                try {
                    this.bottomLoadingOptions.isLoading = true

                    const res = await this.fetchPipelineList(this.pagination.page + 1, this.filterByPipelineName)
                    if (res.records.length === 0) {
                        this.bottomLoadingOptions.isLoading = false
                        Object.assign(this.pagination, {
                            total: this.pipelineList.length
                        })
                        return
                    }
                    this.pipelineList = [
                        ...this.pipelineList,
                        ...res.records
                    ]
                    Object.assign(this.pagination, {
                        page: res.page,
                        total: res.count
                    })
                } catch (err) {
                    this.$bkMessage({
                        message: err.message || err,
                        theme: 'error'
                    })
                } finally {
                    this.bottomLoadingOptions.isLoading = false
                }
            },
            async fetchPipelineList (page, filterByPipelineName) {
                const res = await this.$store.dispatch('experience/requestPipelineList', {
                    projectId: this.projectId,
                    params: {
                        page,
                        pageSize: this.pagination.pageSize,
                        filterByPipelineName
                    }
                })
                return res
            },
            async handleSearch (keyword) {
                try {
                    this.filterByPipelineName = keyword
                    const res = await this.fetchPipelineList(1, keyword)
                    Object.assign(this.pagination, {
                        page: res.page,
                        total: res.count
                    })
                    this.pipelineList = res.records
                } catch (error) {
                    console.error(error)
                }
            },
            handleFileSelect (file) {
                this.selectedFile.file = file
            },
            changeBuildNo (id) {
                if (id) {
                    this.requestList(id)
                }
            },
            /**
             获取文件列表
             *
             */
            async requestList (buildId) {
                this.listLoading.isLoading = true

                const params = {
                    fileNames: ['*.ipa', '*.apk'],
                    props: {
                        pipelineId: this.selectInfo.pipelineId,
                        buildId: buildId === 'all' ? undefined : buildId
                    }
                }

                try {
                    const res = await this.$store.dispatch('experience/requestFileList', {
                        projectId: this.projectId,
                        params: params
                    })
                    this.selectedFile.file = ''
                    this.$store.dispatch('experience/updateCurSelectedFile', {
                        selectFile: {}
                    })

                    this.fileList = res.records.map(item => {
                        item.properties = item.properties.map(property => {
                            if (property.key === 'buildNo') item.buildNo = `(#${property.value})`
                            return property
                        })
                        return {
                            ...item,
                            size: `${((item.size / 1024) / 1024).toFixed(2)}MB`,
                            artifactoryTypeDesc: this.artifactoryTypeMap[item.artifactoryType]
                        }
                    })
                } catch (err) {
                    this.$bkMessage({
                        message: err.message || err,
                        theme: 'error'
                    })
                } finally {
                    this.listLoading.isLoading = false
                }
            },
            async refreshConstructList (val) {
                this.$refs.build.condition = ''
                
                if (val && this.selectInfo.pipelineId) {
                    this.isConstructLoading = true

                    try {
                        const res = await this.$store.dispatch('experience/requestBuildList', {
                            projectId: this.projectId,
                            pipelineId: this.selectInfo.pipelineId,
                            params: { pageSize: -1, checkPermission: false }
                        })

                        this.constructList.splice(0, this.constructList.length, ...res.records)
                        this.constructList.unshift({ id: 'all', buildNum: '全部' })
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
            async getDefaultFile () {
                if (this.selectInfo.pipelineId) {
                    this.listLoading.isLoading = true
                    this.isConstructLoading = true

                    try {
                        const res = await this.$store.dispatch('experience/requestBuildList', {
                            projectId: this.projectId,
                            pipelineId: this.selectInfo.pipelineId,
                            params: { pageSize: -1, checkPermission: false }
                        })

                        this.constructList.splice(0, this.constructList.length, ...res.records)
                        this.constructList.unshift({ id: 'all', buildNum: '全部' })
                        if (this.constructList.length) {
                            this.selectInfo.constructId = this.constructList[0].id
                            this.changeBuildNo(this.constructList[0].id)
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
            }
        }
    }
</script>

<style lang="scss">
    @import './../../scss/conf';
    @import './../../scss/mixins/ellipsis';

    %flex {
        display: flex;
        align-items: center;
    }

    .version-package-wrapper {
        .version-package-header {
            padding-left: 20px;
            height: 54px;
            line-height: 54px;
            border-bottom: 1px solid $borderWeightColor;
            font-weight: bold;
        }

        .select-nav {
            margin-bottom: 20px;
            width: 100%;
            display: flex;
            justify-content: center;
            .bk-select {
                width: 300px;
            }
        }
        .file-name {
            font-size: 12px;
            width: 190px;
            @include ellipsis();
        }
    }
    .artifactory-option {
        width: 295px;
        >.bk-option-content {
            .bk-option-name {
                @include ellipsis();
                display: block;
            }
        }
    }
</style>
