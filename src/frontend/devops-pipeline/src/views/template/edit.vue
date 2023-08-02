<template>
    <section class="pipeline-edit-wrapper template-edit" v-bkloading="{ isLoading }">
        <template v-if="template">
            <pipeline :pipeline="pipeline" :template-type="template.templateType" :is-saving="isSaving" :is-editing="isEditing">
                <div slot="pipeline-bar">
                    <bk-button @click="savePipeline()" theme="primary"
                        :disabled="isSaveDisable"
                    >{{ $t('save') }}</bk-button>
                    <bk-button @click="openVersionSideBar">{{ $t('template.versionList') }}</bk-button>
                    <bk-button @click="exit">{{ $t('cancel') }}</bk-button>
                </div>
            </pipeline>
            <bk-sideslider :title="$t('template.versionList')" class="bkci-property-panel" width="640" :is-show.sync="showVersionSideBar" :quick-close="true">
                <template slot="content">
                    <section class="version-list-wrapper">
                        <bk-table
                            :data="versionList"
                            size="small"
                        >
                            <bk-table-column :label="$t('version')" prop="name"></bk-table-column>
                            <bk-table-column :label="$t('lastUpdateTime')" prop="updateTime">
                                <template slot-scope="props">
                                    <span>{{ localConvertMStoString(props.row.updateTime) }}</span>
                                </template>
                            </bk-table-column>
                            <bk-table-column :label="$t('lastUpdater')" prop="creator"></bk-table-column>
                            <bk-table-column :label="$t('operate')" width="150">
                                <template slot-scope="props">
                                    <bk-button theme="primary" text @click.stop="requestTemplateByVersion(props.row.version)">{{ $t('load') }}</bk-button>
                                    <bk-button theme="primary" text :disabled="!template.hasPermission || currentVersionId === props.row.version || template.templateType === 'CONSTRAINT'" @click="deleteVersion(props.row)">{{ $t('delete') }}</bk-button>
                                </template>
                            </bk-table-column>
                        </bk-table>
                    </section>
                </template>
            </bk-sideslider>
            <mini-map :stages="pipeline.stages" scroll-class=".scroll-container" v-if="!isLoading"></mini-map>
        </template>

        <bk-dialog
            ext-cls="version-dialog"
            v-model="showVersionDialog"
            :close-icon="false"
            :auto-close="false"
            width="400"
            @confirm="saveTemplate">
            <div>
                <form-field v-if="showVersionDialog" required="true" :label="$t('template.saveAsVersion')" :is-error="errors.has(&quot;saveVersionName&quot;)" :error-msg="errors.first(&quot;saveVersionName&quot;)">
                    <auto-complete v-validate="Object.assign({}, { max: 64, required: true })" :list="versionList" name="saveVersionName" open-list="true" :placeholder="$t('template.versionInputTips')" :value="saveVersionName" display-key="name" setting-key="versionName" :handle-change="handleVersionChange"></auto-complete>
                </form-field>
            </div>
        </bk-dialog>
    </section>
</template>

<script>
    import { mapActions, mapState, mapGetters } from 'vuex'
    import Pipeline from '@/components/Pipeline'
    import AutoComplete from '@/components/atomFormField/AutoComplete'
    import FormField from '@/components/AtomPropertyPanel/FormField'
    import MiniMap from '@/components/MiniMap'
    import {
        convertMStoStringByRule,
        navConfirm
    } from '@/utils/util'

    export default {
        components: {
            Pipeline,
            AutoComplete,
            FormField,
            MiniMap
        },
        data () {
            return {
                showVersionDialog: false,
                showVersionSideBar: false,
                isSaving: false,
                isLoading: true,
                saveVersionName: '',
                confirmMsg: this.$t('editPage.confirmMsg'),
                confirmTitle: this.$t('editPage.confirmTitle')
            }
        },
        computed: {
            ...mapGetters('atom', [
                'checkPipelineInvalid',
                'isEditing'
            ]),
            ...mapState('atom', [
                'pipeline',
                'template'
            ]),
            ...mapState([
                'fetchError'
            ]),
            projectId () {
                return this.$route.params.projectId
            },
            templateId () {
                return this.$route.params.templateId
            },
            currentVersionId () {
                return this.template && this.template.currentVersion && this.template.currentVersion.version
            },
            versionList () {
                if (this.template && this.template.versions) {
                    return this.template.versions.map(item => ({
                        ...item,
                        name: item.version === this.currentVersionId ? item.versionName + `(${this.$t('template.current')})` : item.versionName
                    }))
                } else {
                    return []
                }
            },
            isSaveDisable () {
                return this.isSaving || !this.template.hasPermission || this.template.templateType === 'CONSTRAINT'
            }
        },
        watch: {
            fetchError (error) {
                if (error.code === 403) {
                    this.isLoading = false
                    this.removeLeaveListenr()
                }
            },
            pipeline: {
                deep: true,
                handler (newVal, oldVal) {
                    this.isLoading = false
                }
            }
        },
        created () {
            this.requestTemplateByVersion()
        },
        mounted () {
            this.addLeaveListenr()
            this.requestQualityAtom()
            this.requestMatchTemplateRules()
        },
        beforeDestroy () {
            this.setPipeline()
            this.removeLeaveListenr()
            this.errors.clear()
        },
        beforeRouteUpdate (to, from, next) {
            this.leaveConfirm(to, from, next)
        },
        beforeRouteLeave (to, from, next) {
            this.leaveConfirm(to, from, next)
        },
        methods: {
            ...mapActions('atom', [
                'setPipeline',
                'setPipelineEditing',
                'requestTemplate',
                'updateContainer'
            ]),
            handleVersionChange (name, value) {
                this.saveVersionName = value
            },
            openVersionSideBar () {
                this.showVersionSideBar = true
            },
            requestTemplateByVersion (version) {
                try {
                    this.requestTemplate({
                        projectId: this.projectId,
                        templateId: this.templateId,
                        version
                    })
                } catch (err) {
                    this.$showTips({
                        theme: 'error',
                        message: err.message || err
                    })
                }
                this.showVersionSideBar = false
            },
            savePipeline () {
                try {
                    const { checkPipelineInvalid, pipeline, pipelineSetting } = this
                    const { inValid, message } = checkPipelineInvalid(pipeline.stages, pipelineSetting)
                    if (inValid) {
                        throw new Error(message)
                    }
                    this.showVersionDialog = true
                } catch (err) {
                    this.$showTips({
                        theme: 'error',
                        message: err.message || err
                    })
                }
            },
            async saveTemplate () {
                const valid = await this.$validator.validate()
                if (!valid) {
                    this.$showTips({
                        theme: 'error',
                        message: this.$t('template.versionErrTips')
                    })
                    return
                }
                const suffix = `(${this.$t('template.current')})`
                if (this.saveVersionName.endsWith(suffix)) {
                    this.saveVersionName = this.saveVersionName.substr(0, this.saveVersionName.length - suffix.length)
                }
                // 清除流水线参数渲染过程中添加的key
                this.formatParams(this.pipeline)
                let result
                try {
                    this.isSaving = true
                    const { data } = await this.$ajax.put(`/process/api/user/templates/projects/${this.projectId}/templates/${this.templateId}?versionName=${this.saveVersionName}`, this.pipeline)
                    if (data) {
                        this.$showTips({
                            message: `${this.pipeline.name} ${this.$t('updateSuc')}`,
                            theme: 'success'
                        })
                        this.setPipelineEditing(false)
                        this.requestTemplateByVersion()
                        result = true
                    } else {
                        this.$showTips({
                            message: `${this.pipeline.name} ${this.$t('updateFail')}`,
                            theme: 'error'
                        })
                    }
                } catch (err) {
                    this.$showTips({
                        message: err.message || err,
                        theme: 'error'
                    })
                    result = false
                } finally {
                    this.isSaving = false
                    this.saveVersionName = ''
                    this.showVersionDialog = false
                }
                return result
            },
            /**
             *  确认删除模板版本
             */
            async confirmDeleteVersion (row) {
                this.isLoading = true
                try {
                    await this.$store.dispatch('pipelines/deleteTemplateVersionByName', {
                        projectId: this.projectId,
                        templateId: this.templateId,
                        versionName: row.versionName
                    })

                    // this.requestTemplateList()
                    this.requestTemplateByVersion(this.currentVersionId)
                    this.$showTips({
                        message: this.$t('deleteSuc'),
                        theme: 'success'
                    })
                } catch (err) {
                    this.$showTips({
                        message: err.message || err,
                        theme: 'error'
                    })
                } finally {
                    this.isLoading = false
                }
            },
            /**
             *  点击删除模板版本
             */
            deleteVersion (row) {
                if (this.template.hasPermission && this.currentVersionId !== row.version && this.template.templateType !== 'CONSTRAINT') {
                    const content = `${this.$t('delete')}${row.versionName}`
                    navConfirm({ type: 'warning', content })
                        .then(() => {
                            this.confirmDeleteVersion(row)
                        }).catch(() => {})
                }
            },
            requestQualityAtom () {
                this.$store.dispatch('common/requestQualityAtom', {
                    projectId: this.projectId
                })
            },
            requestMatchTemplateRules () {
                this.$store.dispatch('common/requestMatchTemplateRuleList', {
                    projectId: this.projectId,
                    templateId: this.templateId
                })
            },
            exit () {
                this.$router.push({
                    name: 'pipelinesTemplate'
                })
            },
            leaveConfirm (to, from, next) {
                if (this.isEditing) {
                    navConfirm({ content: this.confirmMsg, type: 'warning' })
                        .then(() => next())
                        .catch(() => next(false))
                } else {
                    next(true)
                }
            },
            formatParams (pipeline) {
                const params = this.pipeline && this.pipeline.stages[0].containers[0].params
                const templateParams = this.pipeline && this.pipeline.stages[0].containers[0].templateParams
                const paramList = params && this.getParams(params)
                const templateParamList = templateParams && this.getParams(templateParams)
                this.updateContainer({
                    container: this.pipeline.stages[0].containers[0],
                    newParam: {
                        params: paramList,
                        templateParams: templateParamList
                    }
                })
            },
            getParams (params) {
                const result = params.map(param => {
                    const { paramIdKey, ...temp } = param
                    return temp
                })
                return result
            },
            addLeaveListenr () {
                window.addEventListener('beforeunload', this.leaveSure)
            },
            removeLeaveListenr () {
                window.removeEventListener('beforeunload', this.leaveSure)
            },
            leaveSure (e) {
                e.returnValue = this.confirmMsg
                return this.confirmMsg
            },
            localConvertMStoString (num) {
                return convertMStoStringByRule(new Date().getTime() - num)
            }
        }
    }
</script>

<style lang="scss">
    @import './../../scss/conf';

    .version-dialog {
        .bk-dialog-tool {
            min-height: 20px;
        }
        .bk-dialog-body {
            height: 300px;
        }
        .bk-label {
            font-size: 14px;
            font-weight: bold;
            display: inline-block;
            margin-bottom: 8px;
        }
    }
    .template-edit {
        .version-list-wrapper {
            margin: 20px;
        }
        .scroll-wraper {
            overflow: initial;
        }
    }

</style>
