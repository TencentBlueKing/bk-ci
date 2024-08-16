<template>
    <div v-bkloading="{ isLoading }" class="xcode-panel bk-form bk-form-vertical">
        <section v-if="hasTask && appId">
            <form-field :desc="$t('editPage.atomForm.appidDesc')" :label="$t('editPage.atomForm.appidLabel')">
                <vuex-input readonly :value="appName" disabled />
            </form-field>
            <form-field v-if="!obj.hidden" v-for="(obj, key) in atomPropsModel" :key="key" :desc="obj.desc" :required="obj.required" :label="obj.label" :is-error="errors.has(key)" :error-msg="errors.first(key)">
                <component :is="obj.component" :name="key" v-validate.initial="Object.assign({}, obj.rule, { required: !!obj.required })" :handle-change="(key === 'taskId') ? handleSelect : handleUpdateElement" :value="element[key]" v-bind="obj"></component>
            </form-field>
            <form-field v-if="!obj.hidden" v-for="(obj, key) in newModel" :key="key" :required="obj.required" :label="obj.label" :is-error="errors.has(key)" :error-msg="errors.first(key)">
                <component :is="obj.component" :name="key" v-validate.initial="Object.assign({}, obj.rule, { required: !!obj.required })" :handle-change="handleUpdateNewElement" :value="element[key]" v-bind="obj"></component>
            </form-field>
        </section>
        <section v-if="appId && !hasTask">
            <div class="empty-tips"><a target="_blank" :href="jobUrl">{{ $t('editPage.atomForm.jobSettingTips') }}</a></div>
        </section>
        <section v-if="!appId">
            <div class="empty-tips">{{ $t('editPage.atomForm.noAppidTips') }}</div>
        </section>
    </div>
</template>

<script>
    import atomMixin from './atomMixin'
    import validMixins from '../validMixins'
    export default {
        name: 'job-task',
        mixins: [atomMixin, validMixins],
        data () {
            return {
                jobUrl: `${JOB_URL}/?main&appId=${this.getAppId}`,
                newModel: {},
                hasTask: true,
                isLoading: false,
                curTask: {
                    param: []
                }
            }
        },
        computed: {
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
                    'taskId',
                    'timeout',
                    'appId',
                    'globalVar',
                    'additionalOptions'
                ]
            }
        },
        async mounted () {
            try {
                this.isLoading = true
                this.handleUpdateElement('appId', this.appId)
                if (this.appId && this.element.taskId) {
                    this.handleSelect('taskId', this.element.taskId)
                }
            } catch (err) {
                this.$showTips({
                    message: err.message || err,
                    theme: 'error'
                })
            }
            this.isLoading = false
        },
        methods: {
            async handleSelect (name, value) {
                if (!value) {
                    console.log(value)
                    return
                }
                this.handleUpdateElement('taskId', value)
                this.newModel = {}
                const newElement = {}
                let curParam = {}
                let CurTaskParams = {}
                const curTask = await this.getCurrentTask(value)
                const globalVarArr = []
                const preTask = this.element.globalVar || '[]'
                const preTaskObj = {}
                const preTaskArr = JSON.parse(preTask)
                if (preTaskArr) {
                    for (let i = 0; i < preTaskArr.length; i++) {
                        let value = ''
                        if (preTaskArr[i].ipList !== undefined) {
                            value = preTaskArr[i].ipList
                        } else {
                            value = preTaskArr[i].value
                        }
                        Object.assign(preTaskObj, { [preTaskArr[i].id]: value })
                    }
                }
                // 处理几个hidden的字段
                Object.assign(newElement, { globalVar: '' })
                // 每次切换task则清楚动态加入的element
                this.deleteNewElement()
                // 页面渲染新加的动态参数
                this.handleNewModel(value)
                // 把新加的动态参数添加到element
                if (curTask.param.length > 0) {
                    for (let i = 0; i < curTask.param.length; i++) {
                        CurTaskParams = {}
                        curParam = curTask.param[i]

                        const valueName = curParam.type === 1 ? 'value' : 'ipList'
                        let curValue = curParam.defaultValue
                        if (preTaskObj && preTaskObj[curParam.id] !== undefined) {
                            curValue = preTaskObj[curParam.id]
                        }
                        Object.assign(newElement, { [curParam.id]: curValue })
                        Object.assign(CurTaskParams, { id: curParam.id }, { [valueName]: curValue })
                        globalVarArr.push(CurTaskParams)
                    }
                    Object.assign(newElement, { globalVar: JSON.stringify(globalVarArr) })
                }
                this.updateAtom({
                    element: this.element,
                    newParam: Object.assign(newElement, { [name]: value })
                })
            },
            handleUpdateNewElement (name, value) {
                const globalVarArr = []
                const globalVar = JSON.parse(this.element.globalVar)
                const task = globalVar.map((item) => {
                    return item.id
                })
                const newElement = {}
                let valueName = 'value'
                task.forEach((itemValue, itemIndex, array) => {
                    const taskParams = {}
                    valueName = this.getValueType(itemValue)
                    if (+itemValue === +name) {
                        Object.assign(taskParams, { id: name }, { [valueName]: value })
                    } else {
                        Object.assign(taskParams, { id: itemValue }, { [valueName]: this.element[itemValue] })
                    }
                    globalVarArr.push(taskParams)
                })
                Object.assign(newElement, { globalVar: JSON.stringify(globalVarArr) })
                this.updateAtom({
                    element: this.element,
                    newParam: Object.assign(newElement, { [name]: value })
                })
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
            handleNewModel (value) {
                let item, curParam
                const newModel = {}
                const curTask = this.curTask
                if (curTask.param.length > 0) {
                    for (let i = 0; i < curTask.param.length; i++) {
                        curParam = curTask.param[i]
                        item = {
                            [curParam.id]: {
                                type: 'string',
                                component: 'vuex-input',
                                label: curParam.name,
                                placeholder: this.$t('editPage.atomForm.inputTips') + [curParam.name],
                                default: ''
                            }
                        }
                        Object.assign(newModel, item)
                    }
                    this.newModel = newModel
                }
            },
            async getCurrentTask (taskId) {
                this.isLoading = true
                try {
                    const res = await this.$store.dispatch('common/requestJobTaskParam', {
                        projectId: this.projectId,
                        taskId
                    })
                    Object.assign(this.curTask, { param: res })
                    this.isLoading = false
                } catch (err) {
                    this.$showTips({
                        theme: 'error',
                        message: err.message || err
                    })
                }
                return this.curTask
            },
            getValueType (id) {
                const curTask = this.curTask
                const params = curTask.param
                for (let i = 0; i < params.length; i++) {
                    if (parseInt(params[i].id) === parseInt(id)) {
                        return +params[i].type === 1 ? 'value' : 'ipList'
                    }
                }
                return 'value'
            }
        }
    }
</script>
