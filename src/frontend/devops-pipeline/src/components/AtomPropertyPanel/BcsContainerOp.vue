<template>
    <div v-bkloading="{ isLoading }" class="pull-code-panel bk-form bk-form-vertical">
        <section v-if="appId && isOpenBcs">
            <form-field v-if="!obj.hidden" v-for="(obj, key) of newModel" :key="key" :desc="obj.desc" :required="obj.required" :label="obj.label" :is-error="errors.has(key)" :error-msg="errors.first(key)">
                <component :is="obj.component" :name="key" v-validate.initial="Object.assign({}, obj.rule, { required: !!obj.required })" :handle-change="handleChange" :value="element[key]" v-bind="obj"></component>
            </form-field>
        </section>
        <section v-if="!appId || !isOpenBcs">
            <div class="empty-tips">{{ $t('editPage.atomForm.noAppidTips') }}</div>
        </section>
    </div>
</template>

<script>
    import atomMixin from './atomMixin'
    import validMixins from '../validMixins'
    import bcsMixin from './bcsMixin'

    export default {
        name: 'bcs-container-op',
        mixins: [atomMixin, validMixins, bcsMixin],
        data () {
            return {
                newModel: {},
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
            },
            appId () {
                return this.$store.state.curProject.ccAppId || ''
            },
            curProject () {
                return this.$store.state.curProject
            }
        },
        async mounted () {
            try {
                this.isLoading = true
                this.handleUpdateElement('ccAppId', this.appId)
                this.isOpenBcs = this.curProject.kind === 1 || this.curProject.kind === 2
                if (this.appId && this.isOpenBcs) {
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
        created () {
            // 处理模板详情字段
            if (this.element.instanceEntity) {
                let data = []
                const list = JSON.parse(this.element.instanceEntity)
                if (list && Object.keys(list).length > 0) {
                    for (const obj in list) {
                        const ids = list[obj].map(item => item.id)
                        data = data.concat(ids)
                    }
                }
                this.handleUpdateElement('showInstanceEntity', data)
            }
        },
        destroyed () {
            // 处理模板详情字段
            if (this.element.opType === 'create' && this.element.showInstanceEntity.length > 0) {
                const data = this.element.showInstanceEntity
                const list = this.newModel.showInstanceEntity.tmpData
                const finalData = {}
                for (const obj in list) {
                    const selectItem = list[obj].filter(item => data.find(iitem => iitem === item.id))
                    if (selectItem && selectItem.length) {
                        Object.assign(finalData, { [obj]: selectItem })
                    }
                }
                this.handleUpdateElement('instanceEntity', JSON.stringify(finalData))
            }
        },
        methods: {
            handleChange (name, value) {
                switch (name) {
                    case 'opType':
                        this.handleChooseOpType(name, value)
                        break
                    case 'category':
                        this.handleChooseCategory(name, value)
                        break
                    case 'bcsAppInstId':
                        this.handleUpdateBcsAppInstId(name, value)
                        break
                    case 'instVersionId':
                        this.handleUpdateInstVersionId(name, value)
                        break
                    case 'instVar':
                        this.handleUpdateInstVar(name, value)
                        break
                    case 'clusterId':
                        this.handleUpdateElement(name, value)
                        break
                    case 'musterId':
                        this.handleUpdateMusterId(name, value)
                        break
                    case 'versionId':
                        this.handleUpdateVersionId(name, value)
                        break
                    case 'showInstanceEntity':
                        this.handleUpdateInstanceEntity(name, value)
                        break
                    default:
                        this.handleUpdateElement(name, value)
                }
            },
            initData () {
                if (this.element.musterId) {
                    this.handleUpdateElement('musterId', parseInt(this.element.musterId))
                }
                if (this.element.versionId) {
                    this.handleUpdateElement('versionId', parseInt(this.element.versionId))
                }
                if (this.element.instVersionId) {
                    this.handleUpdateElement('instVersionId', parseInt(this.element.instVersionId))
                }
                if (this.element.bcsAppInstId) {
                    this.handleUpdateElement('bcsAppInstId', parseInt(this.element.bcsAppInstId))
                }
                this.handleChooseOpType('opType', this.element.opType)
                this.getCategoryList()
                if (this.routeName !== 'pipelinesCreate') {
                    if (this.element.opType === 'create') {
                        this.getMusterVersion(this.element.musterId)
                        this.getInstanceEntity(this.element.versionId)
                    } else {
                        this.getInstAppId(this.element.category)
                        this.getInstVersion(this.element.bcsAppInstId)
                    }
                }
            }
        }
    }
</script>
