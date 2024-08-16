<template>
    <div class="pull-code-panel bk-form bk-form-vertical distribution-panel">
        <section>
            <form-field :inline="obj.inline" :class="obj.class" v-if="!obj.hidden" v-for="(obj, key) of newModel" :key="key" :desc="obj.desc" :required="obj.required" :label="obj.label" :is-error="errors.has(key)" :error-msg="errors.first(key)">
                <component :is="obj.component" :name="key" v-validate.initial="Object.assign({}, obj.rule, { required: !!obj.required })" :handle-change="handleMethods" :value="element[key]" v-bind="obj"></component>
                <route-tips :visible="true" :src-tips="srcTips" :path-tips="''" v-if="key === 'srcPath' && element['srcPath']"></route-tips>
            </form-field>
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
                newModel: {}
            }
        },
        computed: {
            projectId () {
                return this.$route.params.projectId
            },
            srcTips () {
                let prefix = ''
                if (this.element.srcType === 'PIPELINE') {
                    prefix = this.$t('details.artifactory') + '/${pipeline.name}/${pipeline.build.id}/'
                } else if (this.element.srcType === 'CUSTOMIZE') {
                    prefix = `${this.$t('details.artifactory')}/`
                } else {
                    return ''
                }
                if (this.element.srcPath === '' || this.element.srcPath === './') {
                    return prefix
                } else {
                    let path = this.element.srcPath
                    if (path.startsWith('./')) {
                        path = path.slice(2)
                    }
                    return prefix + path
                }
            }
        },
        async mounted () {
            this.newModel = this.atomPropsModel
            this.handleChooseSrcType('srcType', this.element.srcType)
            this.handleChooseEnvType('targetEnvType', this.element.targetEnvType)
            if (this.element.targetEnvType === 'NODE' && this.element.targetNodeId.length > 0) {
                // this.element.targetNodeId.push('aaa')
                await this.filterNotExistData('targetNodeId')
            }

            // 把targetEnvName转换成字符串
            const envName = this.element.targetEnvName ? this.element.targetEnvName.join(',') : ''
            this.handleUpdateElement('targetEnvName', envName)
        },
        destroyed () {
            let arr = []
            if (this.element.targetEnvName) {
                arr = this.element.targetEnvName.split(',')
            }
            this.handleUpdateElement('targetEnvName', arr)
            console.log(this.element)
        },
        methods: {
            handleMethods (name, value) {
                switch (name) {
                    case 'srcType':
                        this.handleChooseSrcType(name, value)
                        break
                    case 'targetEnvType':
                        this.handleChooseEnvType(name, value)
                        break
                    default:
                        this.handleUpdateElement(name, value)
                }
            },
            handleChooseSrcType (name, value) {
                if (value === 'PIPELINE') {
                    this.newModel.srcNodeId.hidden = true
                    this.newModel.srcAccount.hidden = true
                    this.newModel.srcPath.label = ''
                    this.newModel.srcPath.placeholder = this.$t('editPage.atomForm.pipelinePathTips')
                    this.newModel.srcPath.class = ''
                    this.handleUpdateElement('srcNodeId', '')
                    this.handleUpdateElement('srcAccount', '')
                } else if (value === 'CUSTOMIZE') {
                    this.newModel.srcNodeId.hidden = true
                    this.newModel.srcAccount.hidden = true
                    this.newModel.srcPath.label = ''
                    this.newModel.srcPath.placeholder = this.$t('editPage.atomForm.customPathTips')
                    this.newModel.srcPath.class = ''
                    this.handleUpdateElement('srcNodeId', '')
                    this.handleUpdateElement('srcAccount', '')
                } else {
                    this.newModel.srcNodeId.hidden = false
                    this.newModel.srcAccount.hidden = false
                    this.newModel.srcPath.label = this.$t('editPage.atomForm.slaveAbsPath')
                    this.newModel.srcPath.placeholder = this.$t('editPage.atomForm.slaveAbsPath')
                    this.newModel.srcPath.class = 'extra-margin-top'
                }
                this.handleUpdateElement(name, value)
            },
            handleChooseEnvType (name, value) {
                if (value === 'ENV') {
                    this.newModel.targetEnvId.hidden = false
                    this.newModel.targetEnvName.hidden = true
                    this.newModel.targetNodeId.hidden = true
                    this.handleUpdateElement('targetEnvName', [])
                    this.handleUpdateElement('targetNodeId', [])
                } else if (value === 'ENV_NAME') {
                    this.newModel.targetEnvId.hidden = true
                    this.newModel.targetEnvName.hidden = false
                    this.newModel.targetNodeId.hidden = true
                    this.handleUpdateElement('targetEnvId', [])
                    this.handleUpdateElement('targetNodeId', [])
                } else {
                    this.newModel.targetEnvId.hidden = true
                    this.newModel.targetEnvName.hidden = true
                    this.newModel.targetNodeId.hidden = false
                    this.handleUpdateElement('targetEnvId', [])
                    this.handleUpdateElement('targetEnvName', [])
                }
                this.handleUpdateElement(name, value)
            },
            async filterNotExistData (type) {
                try {
                    const url = `/environment/api/user/envnode/${this.projectId}/?page=1&pageSize=300`
                    const res = await this.$ajax.get(url)
                    const list = (res.data.records || res.data || []).map(item => ({
                        ...item
                    }))
                    if (this.element[type].length > 0) {
                        this.element[type].forEach(typeItem => {
                            if (typeItem !== '' && list.filter(item => item.nodeHashId === typeItem).length === 0) {
                                // 删除原数组中的当前项
                                this.element[type].splice(this.element[type].findIndex(iitem => iitem.id === typeItem), 1)
                            }
                        })
                    }
                } catch (e) {
                    console.log(e.message || 'request Error')
                }
            }
        }
    }
</script>
