<template>
    <div class="codelib-detail">
        <section
            v-if="curRepo && !errorCode"
            class="content-wrapper"
            v-bkloading="{ isLoading }">
            <div class="detail-header">
                <div
                    v-if="!isEditing"
                    class="codelib-name"
                >
                    <span v-bk-overflow-tips class="name mr5">{{ repoInfo.aliasName }}</span>
                    <span
                        v-perm="{
                            hasPermission: curRepo.canEdit,
                            disablePermissionApi: true,
                            permissionData: {
                                projectId: projectId,
                                resourceType: RESOURCE_TYPE,
                                resourceCode: curRepo.repositoryHashId,
                                action: RESOURCE_ACTION.EDIT
                            }
                        }"
                        v-bk-tooltips="{
                            content: $t('codelib.PAC 模式下不允许修改别名'),
                            disabled: !curRepo.enablePac
                        }"
                        @click="handleEditName"
                    >
                        <Icon
                            name="edit-line"
                            size="16"
                            class="edit-icon"
                            :class="{
                                'disable-delete-icon': curRepo.enablePac
                            }"
                        />
                    </span>
                    <span
                        v-perm="{
                            hasPermission: curRepo.canDelete,
                            disablePermissionApi: true,
                            permissionData: {
                                projectId: projectId,
                                resourceType: RESOURCE_TYPE,
                                resourceCode: curRepo.repositoryHashId,
                                action: RESOURCE_ACTION.DELETE
                            }
                        }"
                        v-bk-tooltips="{
                            content: $t('codelib.请先关闭 PAC 模式，再删除代码库'),
                            disabled: !curRepo.enablePac
                        }"
                        @click="handleDeleteCodeLib"
                    >
                        <Icon
                            name="delete"
                            size="14"
                            class="delete-icon"
                            :class="{
                                'disable-delete-icon': curRepo.enablePac
                            }"
                        />
                    </span>
                </div>
                <div
                    v-else
                    class="edit-input"
                >
                    <bk-input
                        class="aliasName-input"
                        ref="aliasNameInput"
                        :maxlength="60"
                        v-model="repoInfo.aliasName"
                        @enter="checkPipelines"
                    >
                    </bk-input>
                    <bk-button
                        class="ml5 mr5"
                        text
                        @click="checkPipelines"
                    >
                        {{ $t('codelib.save') }}
                    </bk-button>
                    <bk-button
                        text
                        @click="handleCancelEdit"
                    >
                        {{ $t('codelib.cancel') }}
                    </bk-button>
                </div>
                <div class="address-content">
                    <Icon
                        class="codelib-type-icon"
                        :name="codelibIconMap[curRepo.type]"
                        size="16"
                    />
                    <!-- <a
                        v-if="repoInfo.url && repoInfo.url.startsWith('http')"
                        class="codelib-address"
                        v-bk-overflow-tips
                        @click="handleToRepo(repoInfo.url)"
                    >
                        {{ repoInfo.url }}
                    </a> -->
                    <p
                        class="codelib-address"
                        v-bk-overflow-tips
                    >
                        {{ repoInfo.url }}
                    </p>
                    <span @click="handleCopy">
                        <Icon
                            name="copy2"
                            size="16"
                            class="copy-icon"
                        />
                    </span>
                </div>
            </div>
            <bk-tab ext-cls="detail-tab" :active.sync="active" type="unborder-card">
                <bk-tab-panel
                    v-for="(panel, index) in panels"
                    v-bind="panel"
                    :key="index">
                    <component
                        ref="tabCom"
                        v-if="panel.name === active"
                        :is="componentName"
                        :repo-info="repoInfo"
                        :cur-repo="curRepo"
                        :type="repoInfo['@type']"
                        :pac-project-name="pacProjectName"
                        :fetch-repo-detail="fetchRepoDetail"
                        :event-type-list="eventTypeList"
                        :trigger-type-list="triggerTypeList"
                        :refresh-codelib-list="refreshCodelibList"
                        @updateList="updateList"
                    >
                    </component>
                </bk-tab-panel>
            </bk-tab>
            <UsingPipelinesDialog
                :is-show.sync="pipelinesDialogPayload.isShow"
                :pipelines-list="pipelinesList"
                :fetch-pipelines-list="fetchPipelinesList"
                :is-loading-more="pipelinesDialogPayload.isLoadingMore"
                :has-load-end="pipelinesDialogPayload.hasLoadEnd"
                :task-repo-type="pipelinesDialogPayload.taskRepoType"
                @confirm="handleSave"
            />
        </section>
        <empty-tips
            v-if="errorCode === 403"
            :title="$t('codelib.无该代码库权限')"
            :desc="$t('codelib.你没有该代码库的查看权限，请申请权限')"
        >
            <bk-button
                theme="primary"
                @click="handleApply"
            >
                {{ $t('codelib.申请权限') }}
            </bk-button>
        </empty-tips>
        <empty-tips
            v-else-if="errorCode === 404"
            :title="$t('codelib.代码库不存在')"
            :desc="$t('codelib.该代码库不存在，请切换代码库')"
        >
        </empty-tips>
    </div>
</template>
<script>
    import { RESOURCE_ACTION, RESOURCE_TYPE } from '@/utils/permission'
    import {
        mapActions
    } from 'vuex'
    import {
        REPOSITORY_API_URL_PREFIX
    } from '../../store/constants'
    import UsingPipelinesDialog from '../UsingPipelinesDialog.vue'
    import BasicSetting from './basic-setting.vue'
    import TriggerEvent from './trigger-event.vue'
    import Trigger from './trigger.vue'
    export default {
        name: 'CodeLibDetail',
        components: {
            Trigger,
            BasicSetting,
            TriggerEvent,
            UsingPipelinesDialog
        },
        props: {
            curRepoId: {
                type: String,
                default: ''
            },
            curRepo: {
                type: Object,
                default: () => {}
            },
            codelibList: {
                type: Array,
                default: () => []
            },
            refreshCodelibList: {
                type: Function
            },
            switchPage: {
                type: Function,
                required: true
            }
        },
        data () {
            return {
                RESOURCE_ACTION,
                RESOURCE_TYPE,
                isEditing: false,
                isLoading: false,
                oldAliasName: '',
                panels: [
                    { name: 'basic', label: this.$t('codelib.basicSetting') },
                    { name: 'trigger', label: this.$t('codelib.trigger') },
                    { name: 'triggerEvent', label: this.$t('codelib.triggerEvent') }
                ],
                active: '',
                repoInfo: {},
                pipelinesList: [],
                pipelinesDialogPayload: {
                    isShow: false,
                    isLoadingMore: false,
                    hasLoadEnd: false,
                    page: 1,
                    pageSize: 20,
                    repositoryHashId: '',
                    taskRepoType: ''
                },
                codelibIconMap: {
                    CODE_SVN: 'code-SVN',
                    CODE_GIT: 'code-Git',
                    CODE_GITLAB: 'code-Gitlab',
                    GITHUB: 'code-Github',
                    CODE_TGIT: 'code-TGit',
                    CODE_P4: 'code-P4'
                },
                pacProjectName: '',
                eventTypeList: [],
                triggerTypeList: [],
                errorCode: 0
            }
        },
        computed: {
            componentName () {
                const comMap = {
                    trigger: 'Trigger',
                    basic: 'BasicSetting',
                    triggerEvent: 'TriggerEvent'
                }
                return comMap[this.active]
            },
            projectId () {
                return this.$route.params.projectId
            },
            eventId () {
                return this.$route.query.eventId || ''
            },
            userId () {
                return this.$route.query.userId || ''
            },
            resetType () {
                return this.$route.query.resetType || ''
            },
            scmType () {
                return this.$route.query.scmType || ''
            },
            urlRepoId () {
                return this.$route.query.id
            }
        },
        watch: {
            curRepoId: {
                handler (val) {
                    this.errorCode = 0
                    this.pacProjectName = ''
                    this.fetchRepoDetail(val)
                },
                immediate: true
            },
            active: {
                handler (val) {
                    this.$router.push({
                        query: {
                            ...this.$route.query,
                            tab: val
                        }
                    })
                }
            },
            'pipelinesDialogPayload.isShow' (val) {
                if (!val) {
                    this.pipelinesList = []
                    this.pipelinesDialogPayload.taskRepoType = ''
                }
            },
            scmType: {
                handler (val) {
                    if (!val) return
                    this.getEventTypeList()
                    this.getTriggerTypeList()
                },
                immediate: true
            }
        },
        mounted () {
            const tab = this.$route.query.tab || (this.eventId ? 'triggerEvent' : 'basic')
            this.active = tab
        },
        methods: {
            ...mapActions('codelib', [
                'deleteRepo',
                'checkPacProject',
                'renameAliasName',
                'fetchUsingPipelinesList',
                'fetchEventType',
                'fetchTriggerType'
            ]),
         
            getEventTypeList () {
                this.fetchEventType({
                    scmType: this.scmType
                }).then(res => {
                    this.eventTypeList = res.map(i => {
                        return {
                            ...i,
                            name: i.value
                        }
                    })
                })
            },
            getTriggerTypeList () {
                this.fetchTriggerType({
                    scmType: this.scmType
                }).then(res => {
                    this.triggerTypeList = res.map(i => {
                        return {
                            ...i,
                            name: i.value
                        }
                    })
                })
            },

            /**
             * 获取仓库详情
             * @params {String} id 仓库id
             */
            async fetchRepoDetail (id, loading = true) {
                this.isLoading = true
                await this.$ajax.get(`${REPOSITORY_API_URL_PREFIX}/user/repositories/${this.projectId}/${id}?repositoryType=ID`)
                    .then(async (res) => {
                        this.repoInfo = res
                        await this.handleCheckPacProject()
                        this.$router.push({
                            query: {
                                ...this.$route.query,
                                scmType: this.repoInfo.scmType
                            }
                        })
                    }).catch(e => {
                        this.errorCode = e.httpStatus || 404
                    }).finally(() => {
                        if (this.userId) {
                            this.isLoading = this.resetType ? loading : false
                        } else {
                            this.isLoading = false
                        }
                    })
            },

            /**
             * 开启代码库别名编辑状态
             */
            handleEditName () {
                if (this.curRepo.enablePac) return
                this.isEditing = true
                this.oldAliasName = this.repoInfo.aliasName
                setTimeout(() => {
                    this.$refs.aliasNameInput.focus()
                })
            },

            async checkPipelines () {
                if (this.repoInfo.aliasName === this.oldAliasName) {
                    this.isEditing = false
                    return
                }
                if (this.curRepo.repositoryHashId !== this.pipelinesDialogPayload.repositoryHashId) {
                    this.pipelinesDialogPayload.repositoryHashId = this.curRepo.repositoryHashId
                    this.pipelinesList = []
                }
                this.pipelinesDialogPayload.taskRepoType = 'NAME'
                this.pipelinesDialogPayload.page = 1
                await this.fetchPipelinesList()

                if (this.pipelinesList.length) return
                this.handleSave()
            },

            /**
             * 保存代码库别名
             */
            handleSave () {
                this.renameAliasName({
                    projectId: this.projectId,
                    repositoryHashId: this.repoInfo.repoHashId,
                    params: {
                        name: this.repoInfo.aliasName,
                        oldName: this.oldAliasName
                    }
                }).then(() => {
                    this.$bkMessage({
                        message: this.$t('codelib.保存成功'),
                        theme: 'success'
                    })
                    this.updateList()
                }).catch(e => {
                    this.$bkMessage({
                        message: e.message || e,
                        theme: 'error'
                    })
                    this.repoInfo.aliasName = this.oldAliasName
                    console.error(e)
                }).finally(() => {
                    this.pipelinesDialogPayload.isShow = false
                    this.isEditing = false
                })
            },

            updateList () {
                this.switchPage(1, this.$route.query.limit)
            },

            /**
             * 取消编辑 关闭代码库别名编辑状态
             */
            handleCancelEdit () {
                this.repoInfo.aliasName = this.oldAliasName
                this.isEditing = false
            },

            /**
             * 复制代码库地址
             */
            handleCopy () {
                const textarea = document.createElement('textarea')
                document.body.appendChild(textarea)
                textarea.value = this.repoInfo.url
                textarea.select()
                if (document.execCommand('copy')) {
                    document.execCommand('copy')
                    this.$bkMessage({
                        theme: 'success',
                        message: this.$t('codelib.copySuccess')
                    })
                }
                document.body.removeChild(textarea)
            },

            /**
             * 新窗口打开代码库地址
             */
            handleToRepo (url) {
                window.open(url, '__blank')
            },

            /**
             * 删除代码库
             */
            async handleDeleteCodeLib () {
                if (this.curRepo.enablePac) return
                if (this.curRepo.repositoryHashId !== this.pipelinesDialogPayload.repositoryHashId) {
                    this.pipelinesDialogPayload.repositoryHashId = this.curRepo.repositoryHashId
                    this.pipelinesList = []
                }
                this.pipelinesDialogPayload.page = 1
                
                await this.fetchPipelinesList()

                if (!this.pipelinesList.length) {
                    this.$bkInfo({
                        title: this.$t('codelib.是否删除该代码库？'),
                        confirmFn: () => {
                            this.deleteRepo({
                                projectId: this.projectId,
                                repositoryHashId: this.curRepo.repositoryHashId
                            }).then(async () => {
                                await this.refreshCodelibList()
                                await this.$emit('update:curRepoId', this.codelibList[0].repositoryHashId)
                                this.$router.push({
                                    query: {
                                        ...this.$route.query,
                                        id: this.codelibList[0].repositoryHashId
                                    }
                                })
                                this.$bkMessage({
                                    message: this.$t('codelib.successfullyDeleted'),
                                    theme: 'success'
                                })
                            }).catch((e) => {
                                this.$bkMessage({
                                    message: e.message || e,
                                    theme: 'error'
                                })
                            })
                        }
                    })
                }
            },

            /**
             * 获取关联的流水线列表
             */
            async fetchPipelinesList () {
                if (this.pipelinesDialogPayload.isLoadingMore) return
                this.pipelinesDialogPayload.isLoadingMore = true
                await this.fetchUsingPipelinesList({
                    projectId: this.projectId,
                    repositoryHashId: this.pipelinesDialogPayload.repositoryHashId,
                    taskRepoType: this.pipelinesDialogPayload.taskRepoType,
                    page: this.pipelinesDialogPayload.page,
                    pageSize: this.pipelinesDialogPayload.pageSize
                }).then(res => {
                    this.pipelinesList = [...this.pipelinesList, ...res.records]
                    if (this.pipelinesDialogPayload.page === 1 && this.pipelinesList.length) {
                        this.pipelinesDialogPayload.isShow = true
                    }
                    this.pipelinesDialogPayload.hasLoadEnd = res.count === this.pipelinesList.length
                    this.pipelinesDialogPayload.page += 1
                }).finally(() => {
                    this.pipelinesDialogPayload.isLoadingMore = false
                })
            },

            /**
             * 校验仓库是否已经在其他项目开启了PAC
             */
            handleCheckPacProject () {
                if (this.repoInfo.scmType === 'CODE_GIT') {
                    this.checkPacProject({
                        repoUrl: this.repoInfo.url,
                        repositoryType: this.repoInfo.scmType
                    }).then((res) => {
                        this.pacProjectName = res
                    })
                }
            },
            handleApply () {
                this.handleNoPermission({
                    projectId: this.projectId,
                    resourceType: RESOURCE_TYPE,
                    resourceCode: this.urlRepoId,
                    action: RESOURCE_ACTION.VIEW
                })
            }
        }
    }
</script>
<style lang='scss' scoped>
    @media (max-width: 1400px) {
        .codelib-name {
            span {
                max-width: 300px;
            }
        }
        .codelib-address {
            max-width: 300px;
        }
    }
    @media (min-width: 1400px) {
        .codelib-name {
            span {
                max-width: 320px;
            }
        }
        .codelib-address {
            max-width: 380px;
        }
    }
    ::v-deep {
        .bk-tab {
            height: calc(100% - 48px);
            background-color: #fff;
        }
        .bk-tab-section {
            height: calc(100% - 50px);
        }
        .bk-tab-content {
            height: 100%;
        }
    }

    .codelib-detail,
    .content-wrapper {
        height: 100%;
        .detail-header {
            display: flex;
            justify-content: space-between;
            align-items: center;
            height: 48px;
            background: #FAFBFD;
            padding: 0 24px;
        }
        .codelib-name {
            width: 100%;
            height: 48px;
            line-height: 48px;
            font-size: 16px;
            color: #313238;
            margin-right: 30px;
            &:hover {
                .edit-icon,
                .delete-icon {
                    display: inline;
                }
            }
            span {
                display: inline-block;
                overflow: hidden;
                text-overflow: ellipsis;
                white-space: nowrap;
            }
        }
        .edit-input {
            display: flex;
            align-items: center;
            width: 100%;
        }

        .aliasName-input {
            flex: 1;
            max-width: 350px;
            min-width: 200px;
            line-height: 48px;
        }
        .edit-icon,
        .delete-icon {
            position: relative;
            top: 1px;
            cursor: pointer;
            margin-left: 5px;
            color: #979BA5;
            display: none;
        }
        .disable-delete-icon {
            cursor: not-allowed;
        }
        
        .edit-icon {
            position: relative;
            top: 2px;
        }
        .address-content {
            white-space: nowrap;
            &:hover {
                .copy-icon {
                    opacity: 1;
                }
            }
        }
        .codelib-address {
            display: inline-block;
            overflow: hidden;
            white-space: nowrap;
            text-overflow: ellipsis;
        }
        .copy-icon {
            opacity: 0;
            margin-left: 10px;
            cursor: pointer;
        }
    }
</style>
<style lang="scss">
    .detail-tab {
        .bk-tab-section {
            overflow: auto;
        }
    }
</style>
