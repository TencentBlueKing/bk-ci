<template>
    <section
        class="template-edit"
        v-bkloading="{ isLoading }"
    >
        <template>
            <header class="template-edit-header">
                <TemplateBreadCrumb
                    :template-name="pipeline?.name"
                    :is-loading="!pipeline"
                />
                <aside>
                    <bk-button
                        theme="primary"
                        @click="saveTemplateDraft"
                        :disabled="saveStatus"
                        outline
                    >
                        {{ $t('save') }}
                    </bk-button>
                    <ReleaseButton
                        :can-release="canRelease && !isEditing"
                        :project-id="projectId"
                        :id="templateId"
                    />
                </aside>
            </header>
            <Edit
                v-if="pipeline"
                class="template-edit-wrapper"
            />
        </template>
    </section>
</template>

<script>
    import TemplateBreadCrumb from '@/components/template/TemplateBreadCrumb'
    import {
        TEMPLATE_RESOURCE_ACTION
    } from '@/utils/permission'
    import {
        convertMStoStringByRule,
        showPipelineCheckMsg
    } from '@/utils/util'
    import Edit from '@/views/subpages/edit'
    import { mapActions, mapGetters, mapState } from 'vuex'
    import ReleaseButton from '../../components/PipelineHeader/ReleaseButton.vue'

    export default {
        components: {
            TemplateBreadCrumb,
            ReleaseButton,
            Edit
        },
        props: {
            isEnabledPermission: Boolean
        },
        data () {
            return {
                isLoading: true
            }
        },
        computed: {
            ...mapGetters('atom', [
                'checkPipelineInvalid',
                'isEditing'
            ]),
            ...mapState('atom', [
                'saveStatus',
                'pipeline',
                'pipelineInfo',
                'pipelineWithoutTrigger',
                'pipelineSetting'
            ]),
            ...mapState([
                'fetchError'
            ]),
            canRelease () {
                return (this.pipelineInfo?.canRelease ?? false) && !this.saveStatus
            },
            projectId () {
                return this.$route.params.projectId
            },
            templateId () {
                return this.$route.params.templateId
            },
            currentVersionId () {
                return this.$route.params?.version ?? this.pipelineInfo?.version
            },
            TEMPLATE_RESOURCE_ACTION () {
                return TEMPLATE_RESOURCE_ACTION
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
            this.setPipeline(null)
            this.setPipelineEditing(false)
            this.setAtomEditing(false)
            this.removeLeaveListenr()
            this.errors.clear()
        },
        methods: {
            // TODO: 优化
            ...mapActions('atom', [
                'setPipeline',
                'saveDraftTemplate',
                'setPipelineEditing',
                'setAtomEditing',
                'requestTemplateSummary',
                'requestPipeline',
                'updateContainer'
            ]),
            requestTemplateByVersion (version = this.currentVersionId) {
                try {
                    this.requestPipeline({
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
            },
            async saveTemplateDraft () {
                const valid = await this.$validator.validate()
                const pipeline = Object.assign({}, this.pipeline, {
                    name: this.pipelineSetting.pipelineName,
                    stages: [
                        this.pipeline.stages[0],
                        ...this.pipelineWithoutTrigger.stages
                    ]
                })
                if (!valid) {
                    this.$showTips({
                        theme: 'error',
                        message: this.$t('template.versionErrTips')
                    })
                    return
                }

                // 清除流水线参数渲染过程中添加的key
                this.formatParams(pipeline)
                let result
                try {
                    this.saveStatus = true

                    const { data } = await this.saveDraftTemplate({
                        projectId: this.projectId,
                        templateId: this.templateId,
                        model: pipeline,
                        templateSetting: this.pipelineSetting,
                        baseVersion: this.currentVersionId
                    })
                    if (data) {
                        this.$showTips({
                            message: `${this.pipeline.name}${' '}${this.$t('updateSuc')}`,
                            theme: 'success'
                        })
                        this.setPipelineEditing(false)

                        await this.requestTemplateSummary(this.$route.params)

                        this.$router.replace({
                            ...this.$route,
                            params: {
                                ...this.$route.params,
                                version: data.version
                            }
                        })

                        result = true
                    } else {
                        this.$showTips({
                            message: `${this.pipeline.name} ${this.$t('updateFail')}`,
                            theme: 'error'
                        })
                    }
                } catch (err) {
                    if (err.code === 2101244) {
                        showPipelineCheckMsg(this.$bkMessage, err.message, this.$createElement)
                    } else {
                        this.$showTips({
                            message: err.message || err,
                            theme: 'error'
                        })
                    }

                    result = false
                } finally {
                    this.saveStatus = false
                }
                return result
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
            formatParams (pipeline) {
                const params = pipeline && pipeline.stages[0].containers[0].params
                const paramList = params && this.getParams(params)
                this.updateContainer({
                    container: pipeline.stages[0].containers[0],
                    newParam: {
                        params: paramList
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

            localConvertMStoString (num) {
                return convertMStoStringByRule(new Date().getTime() - num)
            }
        }
    }
</script>

<style lang="scss">
    @import './../../scss/conf';

    .template-edit {
        .template-edit-header {
            height: 48px;
            display: flex;
            align-items: center;
            background-color: white;
            box-shadow: 0 2px 5px 0 #333c4808;
            border-bottom: 1px solid $borderLightColor;
            padding: 0 0 0 24px;
            > aside {
                height: 100%;
                margin-left: auto;
                display: flex;
                justify-self: flex-end;
                align-items: center;
                grid-gap: 10px;
            }
        }
        .template-edit-wrapper {
            overflow: hidden;
        }
    }

</style>
