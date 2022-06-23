<template>
    <div class="pull-code-panel bk-form bk-form-vertical">
        <section>
            <template v-for="(obj, key) of newModel">
                <form-field v-if="!obj.hidden" :key="key" :desc="obj.desc" :required="obj.required" :label="obj.label" :is-error="errors.has(key)" :error-msg="errors.first(key)">
                    <component
                        :class="[{ 'build-wetest-inline': key === 'taskId' || key === 'testAccountFile' || key === 'notifyType' }]"
                        :is="obj.component" :name="key"
                        v-validate.initial="Object.assign({}, obj.rule, { required: !!obj.required })"
                        :handle-change="key === 'testType' ? handleTestType : handleUpdateElement"
                        :value="element[key]"
                        v-bind="obj">
                    </component>
                    <bk-button v-if="key === 'taskId' || key === 'notifyType'" @click="edit(key)">{{ $t('edit') }}</bk-button>
                    <a class="build-wetest-link" href="https://cdn.wetest.qq.com/com/c/WeTestAccountTemplate.xls" target="_blank" v-if="key === 'testAccountFile'">{{ $t('editPage.atomForm.templateDownload') }}</a>
                </form-field>
            </template>
        </section>
    </div>
</template>

<script>
    import atomMixin from './atomMixin'
    import validMixins from '../validMixins'

    export default {
        name: 'com-distribution',
        mixins: [atomMixin, validMixins],
        data () {
            return {
                loading: {
                    testType: false
                },
                newModel: {},
                testTypeUrl: '',
                testTypeList: [],
                isInit: false
            }
        },
        computed: {
            projectId () {
                return this.$route.params.projectId
            }
        },
        async created () {
            await this.hasPermission()
            if (this.element.testType) {
                this.isInit = true
            }
            await this.loadTestTypeScriptType()
        },
        mounted () {
            this.newModel = JSON.parse(JSON.stringify(this.atomPropsModel))
        },
        methods: {
            handleTestType (name, value) {
                this.handleUpdateElement(name, value)
                if (!value || value === 'install') {
                    this.newModel.scriptSourceType.hidden = true
                    this.newModel.scriptPath.hidden = true
                    this.newModel.scriptType.hidden = true
                    this.handleUpdateElement('scriptPath', '')
                } else {
                    this.newModel.scriptSourceType.hidden = false
                    this.newModel.scriptPath.hidden = false
                    this.newModel.scriptType.hidden = false
                }
                const typeItem = this.testTypeList.find(item => item.testtype === value)
                const typeList = []
                if (typeItem) {
                    typeItem.frametype.forEach(item => {
                        typeList.push({
                            label: item,
                            value: item
                        })
                    })
                }
                this.newModel.scriptType.list = typeList
                if (!this.isInit) {
                    this.handleUpdateElement('scriptType', typeList[0] ? typeList[0].value : '')
                }
            },
            async loadTestTypeScriptType () {
                try {
                    this.testTypeUrl = `wetest/api/user/wetest/task/${this.projectId}/getTestTypeScriptType?createUser=${this.$store.state.atom.pipeline.pipelineCreator}`
                    const res = await this.$ajax.get(this.testTypeUrl)
                    if (res && res.data) {
                        this.newModel.testType.list = res.data
                        this.testTypeList = JSON.parse(JSON.stringify(res.data))
                        if (this.isInit) {
                            this.handleTestType('testType', this.element.testType)
                            this.isInit = false
                        } else {
                            this.handleTestType('testType', res.data[0] ? res.data[0].testtype : '')
                        }
                    }
                } catch (e) {
                    this.$showTips({
                        theme: 'error',
                        message: e.message || e
                    })
                    this.newModel.testType.list = []
                    this.testTypeList = []
                    this.handleTestType('testType', '')
                }
            },
            edit (keyStr) {
                if (keyStr === 'taskId') {
                    window.open(`${WEB_URL_PREFIX}/wetest/${this.projectId}/${this.element.taskId ? ('#' + this.element.taskId) : ''}`, '_blank')
                } else {
                    window.open(`${WEB_URL_PREFIX}/wetest/${this.projectId}/mail${this.element.notifyType ? ('#' + this.element.notifyType) : ''}`, '_blank')
                }
            },
            async hasPermission () {
                try {
                    await this.$ajax.get(`/wetest/api/user/wetest/task/${this.projectId}/list?page=1&pageSize=100`)
                } catch (e) {
                    if (e.code === 403) {
                        this.$showAskPermissionDialog({
                            noPermissionList: [{
                                actionId: this.$permissionActionMap.view,
                                resourceId: this.$permissionResourceMap.pipeline,
                                instanceId: [],
                                projectId: this.$route.params.projectId
                            }],
                            applyPermissionUrl: `/backend/api/perm/apply/subsystem/?client_id=wetest&project_code=${this.$route.params.projectId}&service_code=wetest&role_viewer=wetest`
                        })
                    } else {
                        this.$showTips({
                            message: e.message,
                            theme: 'error'
                        })
                    }
                }
            }
        }
    }
</script>

<style lang="scss">
    .build-wetest-inline {
        display: inline-block;
        width: 480px;
        & + span.is-danger {
            display: block;
        }
        & + button {
            margin-left: 10px;
            vertical-align: top;
        }
    }
    .build-wetest-link {
        margin-left: 10px;
        color: #3c96ff;
        &:visited {
            color: #3c96ff;
        }
    }
</style>
