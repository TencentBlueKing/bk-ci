<template>
    <article class="pipelines-home">
        <aside class="aside-nav section-box" v-bkloading="{ isLoading }">
            <h3 class="nav-title">
                {{$t('pipelines')}}
                <div
                    v-bk-tooltips="{ content: $t('exception.permissionDeny'), disabled: permission }"
                    class="nav-button"
                >
                    <bk-button
                        size="small"
                        theme="primary"
                        :disabled="!permission"
                        @click="showAddYml"
                    >{{$t('new')}}</bk-button>
                </div>
            </h3>

            <ul v-if="!isLoading" @scroll.passive="scrollLoadMore">
                <li
                    :class="{
                        'nav-item': true,
                        active: curPipeline.pipelineId === undefined
                    }"
                    @click="choosePipeline(allPipeline)"
                >
                    <icon name="all" size="24"></icon>
                    <span class="text-ellipsis item-text">{{$t('pipeline.allPipelines')}}</span>
                </li>
                <li
                    v-for="(dir, index) in dirList"
                    v-bkloading="{ isLoading: dir.isInitPipeline }"
                    :key="index"
                >
                    <section v-if="dir.isShow" @click="chooseDir(dir)" class="nav-item">
                        <icon :name="activeDirList.includes(dir) ? 'folder-open-shape' : 'folder-shape'" size="16"></icon>
                        <span
                            class="text-ellipsis item-text"
                            v-bk-overflow-tips="{
                                content: dir.name,
                                placement: 'right'
                            }"
                        >{{ dir.name }}</span>
                    </section>
                    <section
                        v-for="(pipeline) in dir.children"
                        :key="pipeline.pipelineId"
                        :class="{
                            'nav-item': true,
                            'dir-pipeline': dir.isShow,
                            active: curPipeline.pipelineId === pipeline.pipelineId,
                            disabled: !pipeline.enabled
                        }"
                        @click="choosePipeline(pipeline)"
                    >
                        <icon name="pipeline" size="24"></icon>
                        <span
                            class="text-ellipsis item-text"
                            v-bk-overflow-tips="{
                                content: pipeline.displayName,
                                placement: 'right'
                            }"
                        >{{ pipeline.displayName }}</span>
                    </section>
                </li>
            </ul>
        </aside>

        <router-view class="pipelines-main" v-if="!isLoading"></router-view>

        <bk-sideslider :is-show.sync="isShowAddYml" :quick-close="true" :width="622" :title="$t('pipeline.newPipeline')" :before-close="hidden">
            <bk-form :model="yamlData" ref="yamlForm" slot="content" class="yaml-form" form-type="vertical">
                <bk-form-item :label="$t('pipeline.yamlName')" :rules="[requireRule($t('pipeline.yamlName')), nameRule]" :required="true" property="file_name" error-display-type="normal">
                    <bk-compose-form-item class="yaml-name-container">
                        <bk-input value=".ci / " disabled class="yaml-path"></bk-input>
                        <bk-input v-model="yamlData.file_name" @change="handleChange" class="yaml-name" :placeholder="$t('pipeline.ymlNamePlaceholder')"></bk-input>
                    </bk-compose-form-item>
                </bk-form-item>
                <bk-form-item label="YAML" :rules="[requireRule('Yaml'), checkYaml]" ref="codeSection" :required="true" property="content" error-display-type="normal">
                    <bk-link theme="primary" :href="LINK_CONFIG.YAML_EXAMPLE" target="_blank" class="yaml-examples">{{$t('pipeline.yamlExample')}}</bk-link>
                    <code-section @blur="$refs.codeSection.validate('blur')"
                        @focus="$refs.codeSection.clearValidator()"
                        @change="handleChange"
                        :code.sync="yamlData.content"
                        :read-only="false"
                        :cursor-blink-rate="530"
                    ></code-section>
                </bk-form-item>
                <bk-form-item :label="$t('pipeline.branch')" :rules="[requireRule($t('pipeline.branch'))]" :required="true" property="branch_name" error-display-type="normal">
                    <bk-select v-model="yamlData.branch_name"
                        :remote-method="remoteGetBranchList"
                        :loading="isLoadingBranches"
                        :clearable="false"
                        searchable
                        @change="handleChange"
                        @toggle="toggleFilterBranch"
                        :placeholder="$t('pipeline.branchPlaceholder')"
                    >
                        <bk-option v-for="option in branchList"
                            :key="option"
                            :id="option"
                            :name="option">
                        </bk-option>
                    </bk-select>
                </bk-form-item>
                <bk-form-item :label="$t('pipeline.commitMsg')" :rules="[requireRule($t('pipeline.commitMsg'))]" :required="true" property="commit_message" error-display-type="normal">
                    <bk-input v-model="yamlData.commit_message" @change="handleChange" :placeholder="$t('pipeline.commitMsgPlaceholder')"></bk-input>
                </bk-form-item>
                <bk-form-item>
                    <bk-button ext-cls="mr5" theme="primary" title="Submit" @click.stop.prevent="submitData" :loading="isSaving">{{$t('submit')}}</bk-button>
                    <bk-button ext-cls="mr5" title="Cancel" @click="hidden" :disabled="isSaving">{{$t('cancel')}}</bk-button>
                </bk-form-item>
            </bk-form>
        </bk-sideslider>
    </article>
</template>

<script>
    import { mapState, mapActions } from 'vuex'
    import codeSection from '@/components/code-section'
    import { pipelines } from '@/http'
    import { debounce } from '@/utils'
    import register from '@/utils/websocket-register'
    import validateRule from '@/utils/validate-rule'
    import LINK_CONFIG from '@/conf/link-config.js'

    const getDefaultDir = ({ path, name }, isShow = true) => ({
        path,
        name,
        children: [],
        isShow,
        isLoadEnd: false,
        isLoadingMore: false,
        isInitPipeline: false,
        page: 1,
        pageSize: 100
    })

    export default {
        components: {
            codeSection
        },

        data () {
            return {
                allPipeline: { displayName: this.$t('pipeline.allPipelines'), enabled: true },
                dirList: [],
                branchList: [],
                yamlData: {
                    file_name: '',
                    content: '',
                    branch_name: '',
                    commit_message: ''
                },
                isShowAddYml: this.$route.query.isNew,
                isLoading: false,
                isLoadingBranches: false,
                isSaving: false,
                nameRule: {
                    validator (val) {
                        return /^[a-zA-Z0-9_\-\.]+(\/)?[a-zA-Z0-9_\-\.]+$/.test(val)
                    },
                    message: this.$t('pipeline.nameRule'),
                    trigger: 'blur'
                },
                checkYaml: validateRule.checkYaml,
                activeDirList: [],
                LINK_CONFIG
            }
        },

        computed: {
            ...mapState([
                'projectId',
                'curPipeline',
                'menuPipelineId',
                'permission'
            ])
        },

        watch: {
            '$route.name' (val) {
                if (val === 'buildList' && this.$route.params.pipelineId !== this.menuPipelineId) {
                    this.setMenuPipelineId(this.$route.params.pipelineId)
                }
                if (val === 'pipeline') {
                    this.isLoading = true
                    this.getPipelineDirList().finally(() => {
                        this.isLoading = false
                    })
                }
            }
        },

        created () {
            this.initStatus()
        },

        beforeDestroy () {
            register.unInstallWsMessage('pipelineList')
        },

        methods: {
            ...mapActions([
                'setCurPipeline',
                'setMenuPipelineId'
            ]),

            initStatus () {
                register.installWsMessage(this.getPipelineDirList, 'IFRAMEstream', 'pipelineList')
                this.isLoading = true
                this.getPipelineDirList().finally(() => {
                    this.isLoading = false
                })
            },

            getPipelineDirList () {
                return pipelines.getPipelineDirList(this.projectId, {
                    pipelineId: this.$route.params.pipelineId
                }).then((data) => {
                    const allDirs = (data.allPath || []).map((dir) => getDefaultDir(dir))
                    const ciDir = getDefaultDir({ name: '.ci/', path: '.ci/' }, false)
                    this.dirList = [...allDirs, ciDir]

                    // 展开最外层和当前流水线目录
                    const currentDir = allDirs.find((dir) => (dir.path === data.currentPath))
                    Promise.all([
                        this.chooseDir(ciDir),
                        this.chooseDir(currentDir)
                    ]).then(() => {
                        this.setDefaultPipeline()
                    })
                }).catch((err) => {
                    this.messageError(err.message || err)
                })
            },

            chooseDir (dir) {
                return new Promise((resolve) => {
                    if (typeof dir !== 'object') {
                        resolve()
                        return
                    }

                    const dirIndex = this.activeDirList.findIndex((activeDir) => activeDir === dir)
                    if (dirIndex < 0) {
                        this.activeDirList.push(dir)
                        dir.isInitPipeline = true
                        this.getPipelineList(dir).finally(() => {
                            dir.isInitPipeline = false
                            resolve()
                        })
                    } else {
                        dir.isLoadEnd = false
                        dir.isLoadingMore = false
                        dir.page = 1
                        dir.children = []
                        this.activeDirList.splice(dirIndex, 1)
                        resolve()
                    }
                })
            },

            scrollLoadMore (event) {
                const target = event.target
                const bottomDis = target.scrollHeight - target.clientHeight - target.scrollTop
                if (bottomDis <= 200) {
                    this.activeDirList.forEach((dir) => {
                        if (!dir.isLoadEnd && !dir.isLoadingMore) {
                            this.getPipelineList(dir)
                        }
                    })
                }
            },

            getPipelineList (dir) {
                dir.isLoadingMore = true
                const params = {
                    filePath: dir.path,
                    projectId: this.projectId,
                    page: dir.page,
                    pageSize: dir.pageSize
                }
                return pipelines.getPipelineList(params).then((res = {}) => {
                    const pipelines = (res.records || []).map((pipeline) => ({
                        displayName: pipeline.displayName,
                        enabled: pipeline.enabled,
                        pipelineId: pipeline.pipelineId,
                        filePath: pipeline.filePath,
                        branch: pipeline.latestBuildBranch
                    }))
                    dir.page++
                    this.$set(dir, 'children', [...dir.children, ...pipelines])
                    dir.isLoadEnd = dir.children.length > res.count
                }).catch((err) => {
                    this.messageError(err.message || err)
                }).finally(() => {
                    dir.isLoadingMore = false
                })
            },

            setDefaultPipeline () {
                const pipelineId = this.$route.params.pipelineId
                let curPipeline = this.allPipeline
                this.dirList.forEach((dir) => {
                    const pipelineList = dir.children || []
                    const tempPipeline = pipelineList.find((pipeline) => (pipeline.pipelineId === pipelineId))
                    if (tempPipeline) {
                        curPipeline = tempPipeline
                    }
                })
                this.setMenuPipelineId(curPipeline.pipelineId)
                this.setCurPipeline(curPipeline)
                if (this.$route.name === 'pipeline') {
                    this.$router.push({
                        name: 'buildList',
                        params: {
                            pipelineId: curPipeline.pipelineId
                        }
                    })
                }
            },

            choosePipeline (pipeline) {
                this.setCurPipeline(pipeline)
                this.setMenuPipelineId(pipeline.pipelineId)
                this.$router.push({
                    name: 'buildList',
                    params: {
                        pipelineId: pipeline.pipelineId
                    }
                })
            },

            showAddYml () {
                window.changeFlag = false
                this.isShowAddYml = true
            },

            hiddenFn () {
                this.isShowAddYml = false
                this.yamlData = {
                    file_name: '',
                    content: '',
                    branch_name: '',
                    commit_message: ''
                }
            },

            hidden () {
                if (window.changeFlag) {
                    this.$bkInfo({
                        title: this.$t('确认离开当前页？'),
                        subHeader: this.$createElement('p', {
                            style: {
                                color: '#63656e',
                                fontSize: '14px',
                                textAlign: 'center'
                            }
                        }, this.$t('离开将会导致未保存信息丢失')),
                        okText: this.$t('离开'),
                        confirmFn: () => {
                            window.changeFlag = false
                            this.hiddenFn()
                        }
                    })
                } else {
                    this.hiddenFn()
                }
            },

            toggleFilterBranch (isOpen) {
                if (isOpen) {
                    this.isLoadingBranches = true
                    this.getPipelineBranches().then(() => {
                        this.isLoadingBranches = false
                    })
                }
            },

            remoteGetBranchList (search) {
                return new Promise((resolve) => {
                    debounce(() => {
                        this.getPipelineBranches({ search }).then(() => {
                            resolve()
                        })
                    })
                })
            },

            getPipelineBranches (query = {}) {
                const params = {
                    projectId: this.projectId,
                    page: 1,
                    pageSize: 100,
                    ...query
                }
                return new Promise((resolve) => {
                    pipelines.getPipelineBranches(params).then((res) => {
                        this.branchList = res || []
                    }).catch((err) => {
                        this.messageError(err.message || err)
                    }).finally(() => {
                        resolve()
                    })
                })
            },

            submitData () {
                this.$refs.yamlForm.validate().then(() => {
                    const postData = {
                        ...this.yamlData,
                        file_path: `.ci/${this.yamlData.file_name}`
                    }
                    this.isSaving = true
                    pipelines.addPipelineYamlFile(this.projectId, postData).then(() => {
                        this.hiddenFn()
                    }).catch((err) => {
                        this.messageError(err.message || err)
                    }).finally(() => {
                        this.isSaving = false
                    })
                }, (err) => {
                    this.messageError(err.content || err)
                })
            },

            requireRule (name) {
                return {
                    required: true,
                    message: name + this.$t('isRequired'),
                    trigger: 'blur'
                }
            },

            handleChange () {
                window.changeFlag = true
            }
        }
    }
</script>

<style lang="postcss" scoped>
    .pipelines-home {
        display: flex;
        padding: 25px;
    }
    .nav-title {
        justify-content: space-between;
        margin: 0 12px 10px 20px;
        padding: 10px 0;
        font-size: 16px;
        .nav-button {
            height: 26px;
        }
        button {
            width: 56px;
            min-width: 56px;
            padding: 0;
            vertical-align: top;
        }
    }
    .pipelines-main {
        width: calc(100vw - 290px);
        height: 100%;
        background: #f5f6fa;
    }
    .dir-pipeline {
        padding-left: 20px;
    }
    .yaml-form {
        padding: 20px 30px;
        height: calc(100vh - 61px);
        .yaml-path {
            width: 50px;
        }
        .yaml-name-container {
            width: 100%;
            .yaml-name {
                width: calc(100% - 50px);
            }
        }
        .yaml-examples {
            position: absolute;
            top: -26px;
            left: 70px;
            /deep/ .bk-link-text {
                font-size: 12px;
            }
        }
        /deep/ button {
            margin: 8px 10px 0 0;
        }
    }
</style>
