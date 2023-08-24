<template>
    <section
        v-if="curRepo"
        class="codelib-detail"
        v-bkloading="{ isLoading }">
        <div class="detail-header">
            <div
                v-if="!isEditing"
                class="codelib-name"
            >
                <span class="name mr5">{{ repoInfo.aliasName }}</span>
                <span @click="handleEditName">
                    <Icon
                        name="edit-line"
                        size="14"
                        class="edit-icon"
                    />
                </span>
                <span @click="handleDeleteCodeLib">
                    <Icon
                        name="delete"
                        size="12"
                        class="delete-icon"
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
                >
                </bk-input>
                <bk-button
                    class="ml5 mr5"
                    text
                    @click="handleSave"
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
                <a
                    class="codelib-address"
                    v-bk-overflow-tips
                    @click="handleToRepo(repoInfo.url)"
                >
                    {{ repoInfo.url }}
                </a>
                <span @click="handleCopy">
                    <Icon
                        name="copy2"
                        size="16"
                        class="copy-icon"
                    />
                </span>
            </div>
        </div>
        <bk-tab :active.sync="active" type="unborder-card">
            <bk-tab-panel
                v-for="(panel, index) in panels"
                v-bind="panel"
                :key="index">
                <component
                    v-if="panel.name === active"
                    :is="componentName"
                    :repo-info="repoInfo"
                    :cur-repo="curRepo"
                    :type="repoInfo['@type']"
                    :fetch-repo-detail="fetchRepoDetail"
                    :refresh-codelib-list="refreshCodelibList"
                >
                </component>
            </bk-tab-panel>
        </bk-tab>
        <UsingPipelinesDialog
            :is-show.sync="pipelinesDialogPayload.isShow"
            :pipelines-list="pipelinesList"
            :fetch-pipelines-list="fetchPipelinesList"
            :is-loadig-more="pipelinesDialogPayload.isLoadingMore"
            :has-load-end="pipelinesDialogPayload.hasLoadEnd"
        />
    </section>
</template>
<script>
    import {
        mapActions
    } from 'vuex'
    import {
        REPOSITORY_API_URL_PREFIX
    } from '../../store/constants'
    import Trigger from './trigger.vue'
    import BasicSetting from './basic-setting.vue'
    import TriggerEvent from './trigger-event.vue'
    import UsingPipelinesDialog from '../UsingPipelinesDialog.vue'
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
            }
        },
        data () {
            return {
                isEditing: false,
                isLoading: false,
                oldAliasName: '',
                panels: [
                    { name: 'basic', label: this.$t('codelib.basicSetting') },
                    // { name: 'trigger', label: this.$t('codelib.trigger') },
                    { name: 'triggerEvent', label: this.$t('codelib.triggerEvent') }
                ],
                active: 'basic',
                repoInfo: {},
                pipelinesList: [],
                pipelinesDialogPayload: {
                    isShow: false,
                    isLoadingMore: false,
                    hasLoadEnd: false,
                    page: 1,
                    pageSize: 20,
                    repositoryHashId: ''
                },
                codelibIconMap: {
                    CODE_SVN: 'code-SVN',
                    CODE_GIT: 'code-Git',
                    CODE_GITLAB: 'code-Gitlab',
                    GITHUB: 'code-Github',
                    CODE_TGIT: 'code-TGit',
                    CODE_P4: 'code-P4'
                }
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
            }
        },
        watch: {
            curRepoId: {
                handler (val) {
                    this.fetchRepoDetail(val)
                },
                immediate: true
            }
        },
        methods: {
            ...mapActions('codelib', [
                'deleteRepo',
                'renameAliasName',
                'fetchUsingPipelinesList'
            ]),

            /**
             * 获取仓库详情
             * @params {String} id 仓库id
             */
            async fetchRepoDetail (id) {
                this.isLoading = true
                await this.$ajax.get(`${REPOSITORY_API_URL_PREFIX}/user/repositories/${this.projectId}/${id}?repositoryType=ID`)
                    .then((res) => {
                        this.repoInfo = res
                    }).finally(() => {
                        this.isLoading = false
                    })
            },

            /**
             * 开启代码库别名编辑状态
             */
            handleEditName () {
                this.isEditing = true
                this.oldAliasName = this.repoInfo.aliasName
                setTimeout(() => {
                    this.$refs.aliasNameInput.focus()
                })
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
                    this.$emit('updateList')
                }).catch(e => {
                    this.repoInfo.aliasName = this.oldAliasName
                    console.error(e)
                }).finally(() => {
                    this.isEditing = false
                })
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
                                this.$bkMessage({
                                    message: this.$t('codelib.successfullyDeleted'),
                                    theme: 'success'
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
                    page: this.pipelinesDialogPayload.page,
                    pageSize: this.pipelinesDialogPayload.pageSize
                }).then(res => {
                    this.pipelinesList = [...this.pipelinesList, ...res.records]
                    if (this.pipelinesDialogPayload.page === 1 && this.pipelinesList.length) {
                        this.pipelinesDialogPayload.isShow = true
                    }
                    this.pipelinesDialogPayload.hasLoadEnd = res.totalPages === this.pipelinesDialogPayload.page
                    this.pipelinesDialogPayload.page += 1
                }).finally(() => {
                    this.pipelinesDialogPayload.isLoadingMore = false
                })
            }
        }
    }
</script>
<style lang='scss' scoped>
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
    .codelib-detail {
        height: 100%;
        .detail-header {
            display: flex;
            justify-content: space-between;
            flex: 1;
            height: 48px;
            line-height: 48px;
            background: #FAFBFD;
            padding: 0 24px;
        }
        .codelib-name {
            font-size: 16px;
            color: #313238;
            margin-right: 30px;
            overflow: hidden;
            text-overflow: ellipsis;
            white-space: nowrap;
            &:hover {
                .edit-icon,
                .delete-icon {
                    display: inline;
                }
            }
        }
        .edit-input {
            display: flex;
            width: 100%;
        }

        .aliasName-input {
            flex: 1;
            max-width: 400px;
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
        
        .edit-icon {
            position: relative;
            top: 2px;
        }
        .address-content {
            white-space: nowrap;
        }
        .codelib-type-icon {
            position: relative;
            bottom: 16px;
        }
        .codelib-address {
            display: inline-block;
            max-width: 480px;
            overflow: hidden;
            white-space: nowrap;
            text-overflow: ellipsis;
        }
        .copy-icon {
            margin-left: 10px;
            position: relative;
            top: -16px;
            cursor: pointer;
        }
    }
</style>
