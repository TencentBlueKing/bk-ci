<template>
    <div v-bkloading="{ isLoading }" class="pull-code-panel bk-form bk-form-vertical">
        <section v-if="hasAppId && isOpenBcs">
            <form-field v-if="!obj.hidden" v-for="(obj, key) of newModel" :key="key" :desc="obj.desc" :required="obj.required" :label="obj.label" :is-error="errors.has(key)" :error-msg="errors.first(key)">
                <component :is="obj.component" :name="key" v-validate.initial="Object.assign({}, obj.rule, { required: !!obj.required })" :handle-change="handleChange" :value="element[key]" v-bind="obj"></component>
            </form-field>
        </section>
        <section v-if="!hasAppId || !isOpenBcs">
            <div class="empty-tips">{{ $t('editPage.atomForm.noAppidTips') }}</div>
        </section>
    </div>
</template>

<script>
    import atomMixin from './atomMixin'
    import validMixins from '../validMixins'
    import newBcsMixin from './newBcsMixin'

    export default {
        name: 'bcs-container-op',
        mixins: [atomMixin, validMixins, newBcsMixin],
        data () {
            return {
                newModel: {},
                curProject: {},
                hasAppId: true,
                isOpenBcs: false,
                isLoading: false
            }
        },
        computed: {
            projectId () {
                return this.$route.params.projectId
            },
            routeName () {
                return this.$route.name
            }
        },
        async mounted () {
            try {
                this.isLoading = true
                const res = await this.$store.dispatch('common/requestProjectDetail', {
                    projectId: this.projectId
                })
                this.curProject = res
                this.handleUpdateElement('ccAppId', this.curProject.cc_app_id)
                this.hasAppId = this.curProject.cc_app_id
                this.isOpenBcs = this.curProject.kind > 0
                if (this.hasAppId && this.isOpenBcs) {
                    this.newModel = this.atomPropsModel
                    this.initData()
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
            handleChange (name, value, isUpdate) {
                switch (name) {
                    case 'opType':
                        this.handleChooseOpType(name, value)
                        break
                    case 'category':
                        this.handleChooseCategory(name, value)
                        break
                    case 'namespace':
                        this.handleUpdateNamespace(name, value, isUpdate)
                        break
                    case 'bcsAppInstName':
                        this.handleUpdateBcsAppInstName(name, value, isUpdate)
                        break
                    case 'instVersionName':
                        this.handleUpdateInstVersionName(name, value, isUpdate)
                        break
                    case 'instVar':
                        this.handleUpdateInstVar(name, value)
                        break
                    case 'clusterId':
                        this.handleUpdateElement(name, value)
                        break
                    case 'musterName':
                        this.handleUpdateMusterName(name, value, isUpdate)
                        break
                    case 'showVersionName':
                        this.handleUpdateVersionName(name, value, isUpdate)
                        break
                    default:
                        this.handleUpdateElement(name, value)
                }
            },
            initData () {
                const mesosOperation = [
                    {
                        id: 'signal',
                        name: 'signal'
                    },
                    {
                        id: 'command',
                        name: 'command'
                    }
                ]
                if (this.curProject.kind === 2 && !this.newModel.opType.list.filter(item => item.id === 'signal').length) {
                    this.newModel.opType.list = this.newModel.opType.list.concat(mesosOperation)
                }
                this.handleChooseOpType('opType', this.element.opType)
                this.getCategoryList()
                this.getNamespace()
                this.getMusterVersion(this.element.musterName)
                this.getInstAppName(this.element.namespace)
                this.getInstVersion(this.element.bcsAppInstId)
            }
        }
    }
</script>
