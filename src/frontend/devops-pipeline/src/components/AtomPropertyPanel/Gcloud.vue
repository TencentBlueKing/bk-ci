<template>
    <div v-bkloading="{ isLoading }" class="xcode-panel bk-form bk-form-vertical">
        <section v-if="hasTemplate && appId">
            <form-field :desc="$t('editPage.atomForm.appidDesc')" :label="$t('editPage.atomForm.appidLabel')">
                <vuex-input readonly :value="appName" disabled />
            </form-field>
            <form-field v-if="!obj.hidden" v-for="(obj, key) in atomPropsModel" :key="key" :desc="obj.desc" :required="obj.required" :label="obj.label" :is-error="errors.has(key)" :error-msg="errors.first(key)">
                <component :is="obj.component" :name="key" v-validate.initial="Object.assign({}, obj.rule, { required: !!obj.required })" :handle-change="(key === 'templateId') ? handleSelect : handleUpdateElement" :value="element[key]" v-bind="obj"></component>
            </form-field>

            <form-field v-if="!obj.hidden" v-for="(obj, key) in newModel" :key="key" :required="obj.required" :label="obj.label" :is-error="errors.has(key)" :error-msg="errors.first(key)">
                <component :is="obj.component" :name="key" v-validate.initial="Object.assign({}, obj.rule, { required: !!obj.required })" :handle-change="handleUpdateNewElement" :value="element[key]" v-bind="obj"></component>
            </form-field>
        </section>
        <section v-if="appId && !hasTemplate">
            <div class="empty-tips"><a target="_blank" :href="gcloudUrl">{{ $t('editPage.atomForm.gcloudSettingTips') }}</a></div>
        </section>
        <section v-if="!appId">
            <div class="empty-tips">{{ $t('editPage.atomForm.noAppidTips') }}</div>
        </section>
    </div>
</template>

<script>
    import { mapActions, mapState } from 'vuex'
    import atomMixin from './atomMixin'
    import { bus } from '../../utils/bus'
    import validMixins from '../validMixins'

    export default {
        name: 'gcloud',
        mixins: [atomMixin, validMixins],
        data () {
            return {
                newModel: {},
                hasTemplate: true,
                isLoading: false,
                gcloudUrl: `${OPEN_URL}/s/gcloud/biz_home/${this.appId}`
            }
        },
        computed: {
            ...mapState('common', [
                'gcloudTempList'
            ]),
            projectId () {
                return this.$route.params.projectId
            },
            appId () {
                return this.$store.state.curProject.ccAppId || ''
            },
            appName () {
                return this.$store.state.curProject.ccAppName || ''
            },
            settingKeys () {
                return [
                    'name',
                    '@type',
                    'templateId',
                    'timeoutInSeconds',
                    'appId',
                    'appName',
                    'taskParameters',
                    'apiAuthCode',
                    'additionalOptions'
                ]
            }
        },

        watch: {
            gcloudTempList: function () {
                this.updateTemplateList()
                if (this.gcloudTempList.length > 0 && this.element.templateId) {
                    this.handleSelect('templateId', this.element.templateId)
                }
            }
        },

        async mounted () {
            this.handleUpdateElement('appId', this.appId)
            this.element.templateId && this.handleUpdateElement('templateId', this.element.templateId.toString())
            const { requestGcloudTempList } = this
            const { projectId } = this.$route.params
            this.isLoading = true
            await requestGcloudTempList({
                projectId
            })
            this.isLoading = false
        },

        methods: {
            ...mapActions('common', [
                'requestGcloudTempList'
            ]),
            updateTemplateList () {
                const { atomPropsModel, gcloudTempList } = this
                if (gcloudTempList.length === 0) {
                    this.hasTemplate = false
                } else {
                    this.hasTemplate = true
                }
                if (gcloudTempList !== atomPropsModel.templateId.list) {
                    atomPropsModel.templateId.list = gcloudTempList
                }
            },
            handleSelect (name, value) {
                // 如果是点击选择了模板，把参数显示
                const newElement = {}

                this.newModel = {}
                let curParam = {}
                const CurTaskParams = {}
                const curTemplate = this.getCurrentTemp(value)
                const preTask = this.element.taskParameters || ''
                // 处理几个hidden的字段
                Object.assign(newElement, { apiAuthCode: curTemplate.api_authorization_code })
                Object.assign(newElement, { taskParameters: {} })
                // 每次切换task则清楚动态加入的element
                this.deleteNewElement()
                // 页面渲染新加的动态参数
                this.handleNewModel(value)
                // 把新加的动态参数添加到element
                if (curTemplate.param.length > 0) {
                    for (let i = 0; i < curTemplate.param.length; i++) {
                        curParam = curTemplate.param[i]
                        let curValue = ''
                        // 处理编辑时情况
                        if (preTask && preTask[curParam.key] !== undefined) {
                            curValue = preTask[curParam.key]
                        }
                        Object.assign(newElement, { [curParam.key]: curValue })
                        Object.assign(CurTaskParams, { [curParam.key]: curValue })
                    }
                    Object.assign(newElement, { taskParameters: CurTaskParams })
                }
                this.updateAtom({
                    element: this.element,
                    newParam: Object.assign(newElement, { [name]: value })
                })
            },
            handleUpdateNewElement (name, value) {
                const newElement = {}
                const task = Object.keys(this.element.taskParameters)
                const taskParams = {}
                task.forEach((itemValue, itemIndex, array) => {
                    if (itemValue === name) {
                        Object.assign(taskParams, { [name]: value })
                    } else {
                        Object.assign(taskParams, { [itemValue]: this.element[itemValue] })
                    }
                })
                Object.assign(newElement, { taskParameters: taskParams })

                this.updateAtom({
                    element: this.element,
                    newParam: Object.assign(newElement, { [name]: value })
                })
            },
            handleNewModel (value) {
                let item, curParam
                const newModel = {}
                const curTemp = this.getCurrentTemp(value)
                if (curTemp && curTemp.param.length > 0) {
                    for (let i = 0; i < curTemp.param.length; i++) {
                        curParam = curTemp.param[i]
                        item = {
                            [curParam.key]: {
                                type: 'string',
                                component: 'vuex-input',
                                label: curParam.name,
                                placeholder: '请输入' + [curParam.name],
                                default: ''
                            }
                        }
                        Object.assign(newModel, item)
                    }
                    this.newModel = newModel
                }
            },
            deleteNewElement () {
                // 把动态增加的元素从element中移除
                Object.keys(this.element).forEach(key => {
                    const notRemove = this.settingKeys.find(item => item === key)
                    if (!notRemove) {
                        this.deletePropKey({
                            element: this.element,
                            propKey: key
                        })
                    }
                })
            },
            getCurrentTemp (tempId) {
                return this.gcloudTempList.find(item => +item.id === +tempId)
            },
            updateProject () {
                const data = this.getCurProject
                bus.$emit('update-project', data)
            }
        }
    }
</script>
