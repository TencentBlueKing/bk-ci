<template>
    <div
        class="pipeline-template-list"
        v-bkloading="{ isLoading }"
    >
        <pipeline-header :title="$t('templateManage')"></pipeline-header>
        <div class="template-list-content">
            <template v-if="isEnabledPermission">
                <bk-button
                    v-if="!showSelfEmpty"
                    theme="primary"
                    icon="devops-icon icon-plus"
                    class="add-template"
                    @click="showSetting"
                    v-perm="{
                        hasPermission: hasCreatePermission,
                        disablePermissionApi: true,
                        permissionData: {
                            projectId: projectId,
                            resourceType: 'pipeline_template',
                            resourceCode: projectId,
                            action: TEMPLATE_RESOURCE_ACTION.CREATE
                        }
                    }"
                >
                    {{ $t('template.addTemplate') }}
                </bk-button>
            </template>
            <template v-else>
                <bk-button
                    v-if="!showSelfEmpty"
                    theme="primary"
                    icon="devops-icon icon-plus"
                    class="add-template"
                    @click="showSetting"
                    v-perm="{
                        hasPermission: isManagerUser,
                        disablePermissionApi: true,
                        permissionData: {
                            projectId: projectId,
                            resourceType: 'project',
                            resourceCode: projectId,
                            action: PROJECT_RESOURCE_ACTION.MANAGE
                        }
                    }"
                >
                    {{ $t('template.addTemplate') }}
                </bk-button>
            </template>
            <div class="devops-template-table">
                <template-table
                    ref="selfTemp"
                    :has-create-permission="hasCreatePermission"
                    @getApiData="getTempFromSelf"
                >
                </template-table>
            </div>
            <empty-tips
                v-if="showSelfEmpty"
                :title="$t('template.noTemplate')"
                :desc="emptyTipsConfig.desc"
                :btn-disabled="emptyTipsConfig.btnDisabled"
                :btns="emptyTipsConfig.btns"
                :has-permission="emptyTipsConfig.hasPermission"
                :disable-permission-api="emptyTipsConfig.disablePermissionApi"
                :permission-data="emptyTipsConfig.permissionData"
            >
            </empty-tips>
        </div>

        <bk-dialog
            v-model="setting.isShow"
            :close-icon="false"
            header-position="left"
            :title="$t('template.addTemplate')"
            :cancel-text="$t('cancel')"
            width="480"
            @confirm="createTemplate"
        >
            <div>
                <form-field
                    :required="false"
                    :label="$t('template.name')"
                    :is-error="errors.has('templateName')"
                    :error-msg="errors.first('templateName')"
                >
                    <input
                        class="bk-form-input"
                        maxlength="30"
                        :placeholder="$t('template.nameInputTips')"
                        v-focus="isFocus()"
                        v-model="setting.value"
                        id="templateName"
                        name="templateName"
                        v-validate="'required|max:30'"
                    />
                </form-field>
            </div>
        </bk-dialog>
    </div>
</template>

<script>
    import FormField from '@/components/AtomPropertyPanel/FormField'
    import pipelineHeader from '@/components/devops/pipeline-header.vue'
    import emptyTips from '@/components/pipelineList/imgEmptyTips'
    import templateTable from '@/components/template/templateTable'

    import {
        PROJECT_RESOURCE_ACTION,
        TEMPLATE_RESOURCE_ACTION
    } from '@/utils/permission'

    export default {
        components: {
            pipelineHeader,
            emptyTips,
            FormField,
            templateTable
        },

        directives: {
            focus: {
                inserted: function (el) {
                    el.focus()
                }
            }
        },

        data () {
            return {
                isLoading: false,
                isManagerUser: false,
                setting: {
                    value: '',
                    isShow: false
                },
                showSelfEmpty: false,
                showStoreEmpty: false,
                emptyTipsConfig: {
                    desc: this.$t('template.emptyDesc'),
                    disablePermissionApi: true,
                    btns: [
                        {
                            theme: 'primary',
                            size: 'normal',
                            handler: () => {
                                this.setting.isShow = true
                            },
                            text: this.$t('template.addTemplate')
                        }
                    ]
                },
                emptyParams: {
                    name: '',
                    desc: '',
                    labels: [],
                    stages: [{
                        containers: [{
                            '@type': 'trigger',
                            name: this.$t('buildTrigger'),
                            elements: [{
                                '@type': 'manualTrigger',
                                name: this.$t('manualTrigger'),
                                id: 'T-1-1-1',
                                canElementSkip: true,
                                useLatestParameters: false,
                                executeCount: 1,
                                canRetry: false,
                                classType: 'manualTrigger',
                                taskAtom: ''
                            }],
                            params: [],
                            canRetry: false,
                            classType: 'trigger'
                        }],
                        id: 'stage-1'
                    }]
                },
                hasCreatePermission: false,
                isEnabledPermission: false
            }
        },

        computed: {
            projectId () {
                return this.$route.params.projectId
            },
            TEMPLATE_RESOURCE_ACTION () {
                return TEMPLATE_RESOURCE_ACTION
            },
            PROJECT_RESOURCE_ACTION () {
                return PROJECT_RESOURCE_ACTION
            }
        },

        methods: {
            showSetting () {
                this.setting.isShow = true
            },
            getTempFromSelf (params, success) {
                const { pagingConfig: { current, limit } } = this.$refs.selfTemp
                this.getApiData(current, limit, params).then((res) => {
                    success(res)
                    const list = res.models || []
                    this.showSelfEmpty = list.length <= 0
                })
            },

            /**
             * 获取流水线模板列表
             */
            async getApiData (pageIndex, pageSize, params = {}) {
                this.isLoading = true
                try {
                    const res = await this.$store.dispatch('pipelines/requestTemplateList', {
                        projectId: this.projectId,
                        pageIndex,
                        pageSize,
                        params
                    })
                    this.isManagerUser = res.hasPermission
                    this.hasCreatePermission = res.hasCreatePermission
                    this.isEnabledPermission = res.enableTemplatePermissionManage
                    if (!this.isEnabledPermission) {
                        Object.assign(this.emptyTipsConfig, {
                            btnDisabled: !this.isManagerUser,
                            hasPermission: this.isManagerUser,
                            disablePermissionApi: true,
                            permissionData: {
                                projectId: this.projectId,
                                resourceType: 'project',
                                resourceCode: this.projectId,
                                action: PROJECT_RESOURCE_ACTION.MANAGE
                            }
                        })
                    } else {
                        Object.assign(this.emptyTipsConfig, {
                            btnDisabled: !this.hasCreatePermission,
                            hasPermission: this.hasCreatePermission,
                            disablePermissionApi: true,
                            permissionData: {
                                projectId: this.projectId,
                                resourceType: 'pipeline_template',
                                resourceCode: this.projectId,
                                action: TEMPLATE_RESOURCE_ACTION.CREATE
                            }
                        })
                    }
                    console.log()
                    return res
                } catch (err) {
                    this.$showTips({
                        message: err.message || err,
                        theme: 'error'
                    })
                } finally {
                    this.isLoading = false
                }
            },

            async createTemplate () {
                const valid = await this.$validator.validate()
                if (valid) {
                    try {
                        const params = Object.assign(this.emptyParams, { name: this.setting.value.trim() })
                        const res = await this.$store.dispatch('pipelines/createTemplate', {
                            projectId: this.projectId,
                            params
                        })
                        this.$router.push({
                            name: 'templateEdit',
                            params: { templateId: res.id }
                        })
                    } catch (err) {
                        this.$showTips({
                            theme: 'error',
                            message: err.message || err
                        })
                    }
                    this.setting.value = ''
                    this.setting.isShow = false
                }
            },

            isFocus () {
                return this.setting.isShow
            },

            goToStore () {
                window.open(`${WEB_URL_PREFIX}/store/market/home?pipeType=template`, '_blank')
            }
        }
    }
</script>

<style lang='scss'>

    .pipeline-template-list {
        height: 100%;
        flex: 1;
        display: flex;
        flex-direction: column;
        overflow: hidden;
        .template-list-content {
            display: flex;
            padding: 20px 100px;
            flex: 1;
            overflow: hidden;
            flex-direction: column;
            align-items: flex-start;
            .devops-template-table {
                flex: 1;
                align-self: stretch;
                margin-top: 20px;
                overflow: hidden;
            }
        }

        .template-empty-placeholder {
            height: 100%;
            display: flex;
            align-items: center;
            justify-content: center;
        }

        #templateName {
            margin-top: 3px;
        }
    }
</style>
