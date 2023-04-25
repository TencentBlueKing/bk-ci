<template>
    <div v-bkloading="{ isLoading }" class="xcode-panel bk-form bk-form-vertical">
        <section>
            <form-field v-if="!obj.hidden" v-for="(obj, key) in atomPropsModel" :key="key" :desc="obj.desc" :required="obj.required" :label="obj.label" :is-error="errors.has(key)" :error-msg="errors.first(key)">
                <component :is="obj.component" :name="key" v-validate.initial="Object.assign({}, obj.rule, { required: !!obj.required })" :handle-change="(key === 'taskId') ? handleSelect : handleUpdateElement" :value="element[key]" v-bind="obj"></component>
            </form-field>

            <form-field v-if="!obj.hidden" v-for="(obj, key) in newModel" :key="key" :required="obj.required" :label="obj.label" :is-error="errors.has(key)" :error-msg="errors.first(key)">
                <component :is="obj.component" :name="key" v-validate.initial="Object.assign({}, obj.rule, { required: !!obj.required })" :handle-change="handleUpdateNewElement" :value="element[key]" v-bind="obj"></component>
            </form-field>
        </section>
    </div>
</template>

<script>
    import atomMixin from './atomMixin'
    import validMixins from '../validMixins'

    export default {
        name: 'job-devops-executeTaskExt',
        mixins: [atomMixin, validMixins],
        data () {
            return {
                newModel: {},
                requestVar: [],
                isLoading: false
            }
        },
        computed: {
            projectId () {
                return this.$route.params.projectId
            },
            settingKeys () {
                return [
                    'name',
                    '@type',
                    'taskId',
                    'timeout',
                    'globalVar'
                ]
            }
        },
        watch: {

        },
        async mounted () {
            if (this.element.taskId) {
                this.handleSelect('taskId', this.element.taskId)
            }
        },
        methods: {
            async handleSelect (name, value) {
                // this.newModel = {}
                const newElement = {}

                if (value) {
                    await this.getRequestVar(value)
                } else {
                    return
                }

                this.handleUpdateElement(name, value)

                const CurTaskParams = {}
                // 保留select前的值
                const preTask = this.element.globalVar || ''
                // // 处理几个hidden的字段
                Object.assign(newElement, { globalVar: {} })
                // // 每次切换task则清除动态加入的element
                this.deleteNewElement()
                // // 页面渲染新加的动态参数
                this.handleNewModel()
                // // 把新加的动态参数添加到element
                if (this.requestVar.length > 0) {
                    for (let i = 0; i < this.requestVar.length; i++) {
                        const curParam = this.requestVar[i]
                        let curValue = curParam.value
                        // 处理编辑时情况
                        if (preTask && preTask[curParam.id] !== undefined) {
                            curValue = preTask[curParam.id]
                        }
                        Object.assign(newElement, { [curParam.id]: curValue })
                        Object.assign(CurTaskParams, { [curParam.id]: curValue })
                    }
                    Object.assign(newElement, { globalVar: CurTaskParams })
                }
                this.updateAtom({
                    element: this.element,
                    newParam: Object.assign(newElement, { [name]: value })
                })
            },
            async getRequestVar (taskId) {
                if (taskId) {
                    this.isLoading = true
                    try {
                        const url = `/job/api/user/task/${this.projectId}/${taskId}/detail/`
                        const res = await this.$ajax.get(url)
                        this.requestVar = res.data.globalVars.filter(item => item.type === 1)
                    } catch (e) {
                        this.$showTips({
                            message: e.message || e,
                            theme: 'error'
                        })
                    } finally {
                        this.isLoading = false
                    }
                }
            },
            handleUpdateNewElement (name, value) {
                const newElement = {}
                const task = Object.keys(this.element.globalVar)
                const taskParams = {}
                task.forEach((itemValue, itemIndex, array) => {
                    if (itemValue.toString() === name.toString()) {
                        Object.assign(taskParams, { [name]: value })
                    } else {
                        Object.assign(taskParams, { [itemValue]: this.element[itemValue] })
                    }
                })
                Object.assign(newElement, { globalVar: taskParams })

                this.updateAtom({
                    element: this.element,
                    newParam: Object.assign(newElement, { [name]: value })
                })
            },
            handleNewModel () {
                const newModel = {}
                if (this.requestVar && this.requestVar.length > 0) {
                    for (let i = 0; i < this.requestVar.length; i++) {
                        const curParam = this.requestVar[i]
                        const item = {
                            [curParam.id]: {
                                type: 'string',
                                component: 'vuex-input',
                                label: curParam.name,
                                placeholder: curParam.description,
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
            }
        }
    }
</script>
