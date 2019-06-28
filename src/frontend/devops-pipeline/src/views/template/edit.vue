<template>
    <section class="pipeline-edit-wrapper template-edit" v-bkloading="{ isLoading }">
        <template v-if="template">
            <pipeline :pipeline="pipeline" :template-type="template.templateType" :is-saving="isSaving" :is-editing="isEditing">
                <div slot="pipeline-bar">
                    <bk-button @click="savePipeline()" theme="primary"
                        :disabled="isSaveDisable"
                    >保存</bk-button>
                    <bk-button @click="openVersionSideBar">版本列表</bk-button>
                    <bk-button @click="exit">取消</bk-button>
                </div>
            </pipeline>
            <bk-sideslider title="版本列表" class="sodaci-property-panel" width="640" :is-show.sync="showVersionSideBar" :quick-close="true">
                <template slot="content">
                    <section class="version-list-wrapper">
                        <bk-table
                            :data="versionList"
                            size="small"
                        >
                            <bk-table-column label="版本号" prop="name"></bk-table-column>
                            <bk-table-column label="更新时间" prop="updateTime">
                                <template slot-scope="props">
                                    <span>{{ localConvertMStoString(props.row.updateTime) }}</span>
                                </template>
                            </bk-table-column>
                            <bk-table-column label="最后更新人" prop="creator"></bk-table-column>
                            <bk-table-column label="操作" width="150">
                                <template slot-scope="props">
                                    <bk-button theme="primary" text @click.stop="requestTemplateByVersion(props.row.version)">加载</bk-button>
                                    <bk-button theme="primary" text :disabled="!template.hasPermission || currentVersionId === props.row.version || template.templateType === 'CONSTRAINT'" @click="deleteVersion(props.row)">删除</bk-button>
                                </template>
                            </bk-table-column>
                        </bk-table>
                    </section>
                </template>
            </bk-sideslider>
        </template>

        <bk-dialog
            ext-cls="version-dialog"
            v-model="showVersionDialog"
            :close-icon="false"
            width="400"
            @confirm="saveTemplate">
            <div>
                <form-field required="true" label="保存到版本" :is-error="errors.has(&quot;versionName&quot;)" :error-msg="errors.first(&quot;versionName&quot;)">
                    <auto-complete v-validate="'required'" :list="versionList" name="versionName" open-list="true" placeholder="搜索或创建版本" :value="saveVersionName" display-key="name" setting-key="versionName" :handle-change="handleVersionChange"></auto-complete>
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
    import { CONFIRM_MSG, CONFIRM_TITLE } from '@/utils/pipelineConst'
    import {
        convertMStoStringByRule,
        navConfirm
    } from '@/utils/util'

    export default {
        components: {
            Pipeline,
            AutoComplete,
            FormField
        },
        data () {
            return {
                showVersionDialog: false,
                showVersionSideBar: false,
                isSaving: false,
                isLoading: true,
                saveVersionName: ''
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
                        name: item.version === this.currentVersionId ? item.versionName + '(当前)' : item.versionName
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
        },
        beforeDestroy () {
            this.setPipeline()
            this.removeLeaveListenr()
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
                'requestTemplate'
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
                    const { checkPipelineInvalid, pipeline } = this
                    const { inValid, message } = checkPipelineInvalid(pipeline.stages)
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
                        message: '版本名称不能为空'
                    })
                    return
                }
                if (this.saveVersionName.endsWith('(当前)')) {
                    this.saveVersionName = this.saveVersionName.substr(0, this.saveVersionName.length - 4)
                }
                let result
                try {
                    this.isSaving = true
                    const { data } = await this.$ajax.put(`/process/api/user/templates/projects/${this.projectId}/templates/${this.templateId}?versionName=${this.saveVersionName}`, this.pipeline)
                    if (data) {
                        this.$showTips({
                            message: `${this.pipeline.name}修改成功`,
                            theme: 'success'
                        })
                        this.setPipelineEditing(false)
                        this.requestTemplateByVersion()
                        result = true
                    } else {
                        this.$showTips({
                            message: `${this.pipeline.name}修改失败`,
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
                    await this.$store.dispatch('pipelines/deleteTemplateVersion', {
                        projectId: this.projectId,
                        templateId: this.templateId,
                        versionId: row.version
                    })

                    // this.requestTemplateList()
                    this.requestTemplateByVersion(this.currentVersionId)
                    this.$showTips({
                        message: '删除模板版本成功',
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
                    const content = `删除${row.versionName}`
                    navConfirm({ title: `确认`, content })
                        .then(() => {
                            this.confirmDeleteVersion(row)
                        }).catch(() => {})
                }
            },
            exit () {
                this.$router.push({
                    name: 'pipelinesTemplate'
                })
            },
            leaveConfirm (to, from, next) {
                if (this.isEditing) {
                    navConfirm({ content: CONFIRM_MSG, title: CONFIRM_TITLE })
                        .then(() => next())
                        .catch(() => next(false))
                } else {
                    next(true)
                }
            },
            addLeaveListenr () {
                window.addEventListener('beforeunload', this.leaveSure)
            },
            removeLeaveListenr () {
                window.removeEventListener('beforeunload', this.leaveSure)
            },
            leaveSure (e) {
                e.returnValue = CONFIRM_MSG
                return CONFIRM_MSG
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
            height: 270px;
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
    }

</style>
