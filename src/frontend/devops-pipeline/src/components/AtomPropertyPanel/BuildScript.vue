<template>
    <section>
        <div v-bkloading="{ isLoading }" class="bk-form bk-form-vertical">
            <template v-for="(obj, key) in atomPropsModel">
                <form-field v-if="!isHidden(obj, element)" :key="key" :desc="obj.desc" :desc-link="obj.descLink" :desc-link-text="obj.descLinkText" :required="obj.required" :label="obj.label" :is-error="errors.has(key)" :error-msg="errors.first(key)">
                    <component :is="obj.component" v-if="key === 'scriptTurbo' && baseOSType === 'LINUX'"
                        v-validate.initial="Object.assign(obj.rule, { required: !!obj.required })"
                        :lang="lang"
                        :name="key"
                        :task="task"
                        :task-id="taskId"
                        :task-name="taskName"
                        :turbo-value="banAllBooster"
                        :project-id="projectId"
                        :element-id="elementId"
                        :turbo-disabled="inputDisabled"
                        @handleChange="handleUpdateTurbo"
                        v-bind="obj">
                    </component>
                    <component :is="obj.component" v-else-if="key !== 'scriptTurbo'" :disabled="disabled" v-validate.initial="Object.assign(obj.rule, { required: !!obj.required })" :lang="lang" :name="key" :handle-change="handleUpdateElement" :value="element[key]" v-bind="obj"></component>
                </form-field>
            </template>
        </div>
    </section>
</template>

<script>
    import atomMixin from './atomMixin'
    import validMixins from '../validMixins'
    import { mapActions, mapGetters, mapState } from 'vuex'
    export default {
        name: 'build-script',
        mixins: [atomMixin, validMixins],
        data () {
            return {
                task: {},
                elementId: '',
                banAllBooster: false,
                taskId: '',
                taskName: '',
                isShow: false,
                isLoading: false,
                // btnLoading: true,
                inputDisabled: false,
                btnDisabled: false,
                baseOSType: ''
            }
        },
        computed: {
            ...mapGetters('atom', [
                'checkPipelineInvalid',
                'getEditingElementPos'
            ]),
            ...mapState('atom', [
                'pipeline'
            ]),
            projectId () {
                return this.$route.params.projectId
            },
            pipelineId () {
                return this.$route.params.pipelineId
            },
            langList () {
                return this.atomPropsModel.scriptType.list
            },
            lang () {
                const lang = this.langList.find(stype => stype.value === this.element.scriptType)
                return lang ? lang.id : ''
            }
        },
        created () {
            this.elementId = this.element.id || false
            this.baseOSType = this.container.baseOS
            if (this.baseOSType === 'LINUX') {
                this.initData()
            }
            if (this.atomPropsModel.archiveFile !== undefined) {
                this.atomPropsModel.archiveFile.hidden = !this.element.enableArchiveFile
            }
        },
        methods: {
            ...mapActions('pipelines', [
                'requestTurboIofo',
                'setTurboSwitch',
                'updateToTurbo'
            ]),
            ...mapActions('soda', [
                'requestInterceptAtom'
            ]),
            ...mapActions('atom', [
                'setPipeline'
            ]),
            async handleUpdateTurbo (name, value) {
                if (this.elementId && this.taskId) {
                    const { checkPipelineInvalid, $route: { params }, pipeline } = this
                    const { inValid, message } = checkPipelineInvalid(pipeline.stages)
                    try {
                        if (inValid) {
                            throw new Error(message)
                        }
                        const { data } = await this.$ajax.put(`/process/api/user/pipelines/${params.projectId}/${params.pipelineId}`, pipeline)
                        if (data) {
                            this.updatePipelineToTurbo(pipeline)
                            this.inputDisabled = true
                            try {
                                const { taskId } = this
                                const res = await this.setTurboSwitch({
                                    banAllBooster: !value,
                                    taskId
                                })
                                if (res) {
                                    this.banAllBooster = value
                                }
                            } catch (err) {
                                this.$bkMessage({
                                    message: err.message ? err.message : err,
                                    theme: 'error'
                                })
                            } finally {
                                this.inputDisabled = false
                            }
                        } else {
                            this.$showTips({
                                message: `${pipeline.name}${this.$t('updateFail')}`,
                                theme: 'error'
                            })
                        }
                    } catch (e) {
                        if (e.code === 403) { // 没有权限编辑
                            this.setPermissionConfig(`${this.$t('pipeline')}：${this.pipeline.name}`, this.$t('edit'))
                        } else {
                            this.$showTips({
                                message: e.message,
                                theme: 'error'
                            })
                        }
                    }
                } else if (this.elementId && !this.taskId) {
                    this.isLoading = true
                    try {
                        const res = await this.requestTurboIofo({
                            bsPipelineId: this.$route.params.pipelineId,
                            bsElementId: this.elementId
                        })
                        if (res) {
                            res.data.banAllBooster === 'false' ? (this.banAllBooster = true) : (this.banAllBooster = false)
                            this.task = Object.assign({}, res.data)
                            this.taskId = res.data.taskId
                            this.taskName = res.data.taskName
                            if (!this.taskId) {
                                this.banAllBooster = value
                                this.$bkInfo({
                                    subTitle: this.$t('editPage.atomForm.createTurbo'),
                                    closeIcon: false,
                                    confirmFn: this.goRegist,
                                    cancelFn: () => {
                                        this.banAllBooster = false
                                    }
                                })
                            }
                        }
                    } catch (err) {
                        this.$bkMessage({
                            message: err.message ? err.message : err,
                            theme: 'error'
                        })
                    } finally {
                        this.isLoading = false
                    }
                } else {
                    this.isShow = true
                }
            },
            async initData () {
                try {
                    const res = await this.requestTurboIofo({
                        bsPipelineId: this.$route.params.pipelineId,
                        bsElementId: this.elementId
                    })
                    if (res) {
                        res.data.banAllBooster === 'false' ? (this.banAllBooster = true) : (this.banAllBooster = false)
                        this.task = Object.assign({}, res.data)
                        this.taskId = res.data.taskId
                        this.taskName = res.data.taskName
                    }
                } catch (err) {
                    this.$bkMessage({
                        message: err.message ? err.message : err,
                        theme: 'error'
                    })
                }
            },

            // 新建任务跳转
            async goRegist () {
                if (!this.elementId) {
                    const { checkPipelineInvalid, $route: { params }, pipeline } = this
                    const { inValid, message } = checkPipelineInvalid(pipeline.stages)
                    this.btnDisabled = true
                    const tab = window.open('about:blank')
                    try {
                        if (inValid) {
                            throw new Error(message)
                        }
                        const { data } = await this.$ajax.put(`/process/api/user/pipelines/${params.projectId}/${params.pipelineId}`, pipeline)
                        if (data) {
                            // this.requestPipeline(this.$route.params)
                            const response = await this.$ajax.get(`/process/api/user/pipelines/${params.projectId}/${params.pipelineId}`)
                            this.setPipeline(response.data)
                            this.updatePipelineToTurbo(response.data)
                            const { containerIndex, elementIndex, stageIndex } = this.getEditingElementPos
                            const container = response.data.stages[stageIndex]
                            this.elementId = container.containers[containerIndex].elements[elementIndex].id

                            tab.location = `${WEB_URL_PIRFIX}/turbo/${this.projectId}/registration#${this.$route.params.pipelineId}&${this.elementId}`
                        } else {
                            this.$showTips({
                                message: `${pipeline.name}${this.$t('updateFail')}`,
                                theme: 'error'
                            })
                            tab.close()
                        }
                    } catch (e) {
                        if (e.code === 403) { // 没有权限编辑
                            this.setPermissionConfig(`${this.$t('pipeline')}：${this.pipeline.name}`, this.$t('edit'))
                        } else {
                            this.$showTips({
                                message: e.message,
                                theme: 'error'
                            })
                        }
                        tab.close()
                    } finally {
                        this.btnDisabled = false
                    }
                } else {
                    setTimeout(() => {
                        window.open(`${WEB_URL_PIRFIX}/turbo/${this.projectId}/registration#${this.$route.params.pipelineId}&${this.elementId}`, '_blank')
                    }, 200)
                }
            },
            setPermissionConfig (resource, option) {
                this.$showAskPermissionDialog({
                    noPermissionList: [{
                        resource,
                        option
                    }],
                    applyPermissionUrl: `${PERM_URL_PIRFIX}/backend/api/perm/apply/subsystem/?client_id=pipeline&project_code=${this.projectId}&service_code=pipeline&${option === '执行' ? 'role_executor' : 'role_manager'}=pipeline:${this.pipelineId}`
                })
            },
            updatePipelineToTurbo (pipeline) {
                try {
                    this.updateToTurbo({
                        pipelineId: this.pipelineId,
                        params: pipeline
                    })
                } catch (e) {
                    this.$showTips({
                        message: e.message || e,
                        theme: 'error'
                    })
                }
            }
        }
    }
</script>

<style lang="scss" scoped>
    .dialog-regist {
        position: relative;
        .regist-content {
            padding: 45px 65px 15px 65px;
        }
        .regist-footer {
            text-align: center;
            padding: 20px 65px 40px;
            font-size: 0;
            .bk-button {
                width: 110px;
                height: 36px;
                font-size: 14px;
                border: 1px solid #c3cdd7;
                border-radius: 2px;
                box-shadow: none;
                outline: none;
                background-color: #fff;
                text-overflow: ellipsis;
                overflow: hidden;
                white-space: nowrap;
                cursor: pointer;
                &.bk-primary {
                    margin-right: 20px;
                    color: #fff;
                    background-color: #3c96ff;
                    border-color: #3c96ff;
                }
            }
        }
    }
</style>
