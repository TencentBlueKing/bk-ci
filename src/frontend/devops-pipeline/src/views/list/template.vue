<template>
    <div class="pipeline-template-list" v-bkloading="{ isLoading }">
        <div class="template-list-content">
            <div class="view-table-wrapper">
                <bk-button theme="primary" icon="devops-icon icon-plus" class="add-template" @click="showSetting" :disabled="!isManagerUser" v-if="!showSelfEmpty">
                    {{ $t('template.addTemplate') }}
                </bk-button>
                <template-table @getApiData="getTempFromSelf" ref="selfTemp"></template-table>
                <empty-tips v-if="showSelfEmpty"
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
        </div>

        <bk-dialog
            v-model="setting.isShow"
            :close-icon="false"
            header-position="left"
            :title="$t('template.addTemplate')"
            width="480"
            @confirm="createTemplate">
            <div>
                <form-field :required="false" :label="$t('template.name')" :is-error="errors.has(&quot;templateName&quot;)" :error-msg="errors.first(&quot;templateName&quot;)">
                    <input class="bk-form-input"
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
    import emptyTips from '@/components/pipelineList/imgEmptyTips'
    import FormField from '@/components/AtomPropertyPanel/FormField'
    import templateTable from '@/components/template/templateTable'

    import {
        PROJECT_RESOURCE_ACTION
    } from '@/utils/permission'

    export default {
        components: {
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
                    btnDisabled: true,
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
                    stages: [{ containers: [{ '@type': 'trigger', name: this.$t('buildTrigger'), elements: [{ '@type': 'manualTrigger', name: this.$t('manualTrigger'), id: 'T-1-1-1', canElementSkip: true, useLatestParameters: false, executeCount: 1, canRetry: false, classType: 'manualTrigger', taskAtom: '' }], params: [], canRetry: false, classType: 'trigger' }], id: 'stage-1' }]
                }
            }
        },

        computed: {
            projectId () {
                return this.$route.params.projectId
            }
        },

        methods: {
            showSetting () {
                this.setting.isShow = true
            },
            getTempFromSelf (success) {
                const { pagingConfig: { current, limit } } = this.$refs.selfTemp
                this.getApiData(current, limit).then((res) => {
                    success(res)
                    const list = res.models || []
                    this.showSelfEmpty = list.length <= 0
                })
            },

            /**
             * 获取流水线模板列表
             */
            async getApiData (pageIndex, pageSize) {
                this.isLoading = true
                try {
                    const res = await this.$store.dispatch('pipelines/requestTemplateList', {
                        projectId: this.projectId,
                        pageIndex,
                        pageSize
                    })
                    this.isManagerUser = res.hasPermission
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
    @import './../../scss/conf';

    .pipeline-template-list {
        padding-bottom: 20px;
        .template-list-content {
            margin: 0 auto;
            padding-top: 16px;
            width: 1200px;
            h3 {
                font-weight: normal;
                font-size: 16px;
                .small-title {
                    font-size: 14px;
                    vertical-align: middle;
                    border-bottom: 1px dotted #63656E;
                    margin-left: 4px;
                    cursor: pointer;
                }
            }
            .add-template {
                height: 33px;
                line-height: 31px;
                padding: 0 13px;
            }
        }
        #templateName {
            margin-top: 3px;
        }
    }
</style>
