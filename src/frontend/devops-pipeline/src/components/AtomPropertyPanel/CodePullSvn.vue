<template>
    <form class="bk-form bk-form-vertical">
        <template v-for="(obj, key) in newModel">
            <form-field v-if="!obj.hidden" :key="key" :desc="obj.desc" :required="obj.required" :label="obj.label" :is-error="errors.has(key)" :error-msg="errors.first(key)">
                <component v-if="key === 'svnPath'" :is="obj.component" :name="key" v-validate.initial="Object.assign({}, obj.rule, { required: !!obj.required })" :handle-change="handleUpdate" :value="element[key]" v-bind="obj" :repository-hash-id="element[&quot;repositoryHashId&quot;]" :list="list"></component>
                <component v-else-if="key === 'repositoryHashId'" :is="obj.component" :name="key" v-validate.initial="Object.assign({}, obj.rule, { required: !!obj.required })" :handle-change="handleUpdate" :value="element[key]" v-bind="obj" @change="listChange"></component>
                <component v-else :is="obj.component" :name="key" v-validate.initial="Object.assign({}, obj.rule, { required: !!obj.required })" :handle-change="handleUpdate" :value="element[key]" v-bind="obj"></component>
                <p class="bk-form-help is-warning" v-if="key === 'path' && obj.warn">{{ obj.warn }}</p>
                <route-tips :visible="true" :src-tips="srcTips" :path-tips="pathTips" v-if="key === 'path' && element['repositoryHashId']"></route-tips>
            </form-field>
        </template>
    </form>
</template>

<script>
    import atomMixin from './atomMixin'
    import validMixins from '../validMixins'
    import { mapGetters, mapState } from 'vuex'

    export default {
        name: 'code-pull-svn-atom',
        mixins: [atomMixin, validMixins],
        data () {
            return {
                newModel: {},
                list: [],
                tmpRevision: '',
                tips: '当前流水线存在多个代码拉取插件，你需设置此字段以解决冲突问题'
            }
        },
        computed: {
            ...mapState('atom', [
                'pipeline'
            ]),
            ...mapGetters('atom', [
                'isTriggerContainer',
                'getAllContainers',
                'isCodePullAtom'
            ]),
            isThirdDocker () {
                return this.container && this.container.dispatchType && this.container.dispatchType.buildType === 'DOCKER' && ['tlinux1.2', 'tlinux2.2'].indexOf(this.container.dispatchType.value) === -1
            },
            pipelineStages () {
                const { getAllContainers } = this
                return getAllContainers(this.pipeline.stages || []) || []
            },
            triggerContainer () {
                let trigger = []
                if (this.pipelineStages.length) {
                    this.pipelineStages.map(stage => {
                        if (this.isTriggerContainer(stage)) {
                            trigger = stage
                        }
                    })
                }
                return trigger
            },
            srcTips () {
                const srcItem = this.list.find(item => item.repositoryHashId === this.element.repositoryHashId)
                let srcPrefix = (srcItem && srcItem.url) ? srcItem.url : '${CODE_ROOT_URL}'
                if (srcPrefix.endsWith('/')) {
                    srcPrefix = srcPrefix.substring(0, srcPrefix.length - 1)
                }
                if (!this.element.svnPath || this.element.svnPath === '' || this.element.svnPath === '.' || this.element.svnPath === './' || this.element.svnPath === './*') {
                    return ` ${srcPrefix}/*`
                } else {
                    let path = this.element.svnPath
                    path = this.handlePath(path)
                    return ` ${srcPrefix}/${path}/*`
                }
            },
            pathTips () {
                if (this.element.path === '' || this.element.path === './') {
                    return ' ${WORKSPACE}/*'
                } else {
                    let path = this.element.path
                    path = this.handlePath(path)
                    return ' ${WORKSPACE}/' + path + '/*'
                }
            }
        },
        watch: {
            'element.path' (val) {
                this.setWarn(val)
            }
        },
        created () {
            this.newModel = JSON.parse(JSON.stringify(this.atomPropsModel))
            this.setWarn(this.element.path)
            this.setSvnVersionState()

            if (!this.element.repositoryType) {
                this.handleUpdateElement('repositoryType', 'ID')
            }
            this.handleChooseCodelibType('repositoryType', this.element.repositoryType)
            if (this.element.specifyRevision) {
                this.newModel.revision.hidden = false
                if (this.element.revision && this.element.revision.startsWith('${') && this.element.revision.endsWith('}')) {
                    this.tmpRevision = this.element.revision.substring(2)
                    this.tmpRevision = this.tmpRevision.substring(0, (this.tmpRevision.length - 1))
                }
            }
        },
        methods: {
            setWarn (path) {
                const paths = this.detectCodeContainer()
                if (paths.length && paths.filter(p => p === path).length > 1) { // 包含本身数据
                    this.newModel.path.warn = this.tips
                } else {
                    this.newModel.path.warn = ''
                }
            },
            detectCodeContainer () {
                const path = []
                if (this.container.elements.length) {
                    this.container.elements.forEach(e => {
                        if (this.isCodePullAtom(e)) {
                            path.push(e.path)
                        }
                    })
                }
                return path
            },
            listChange (list) {
                this.list = list || []
            },
            handleUpdate (name, value) {
                this.handleUpdateElement(name, value)
                if (name === 'specifyRevision' && this.triggerContainer) {
                    const params = this.triggerContainer.params
                    if (value) {
                        this.newModel.revision.hidden = false
                        this.handleNewRevision()
                    } else {
                        this.newModel.revision.hidden = true
                        this.handleUpdateElement('revision', '')
                        this.triggerContainer.params = params.filter(item => item.id !== this.tmpRevision)
                    }
                }
                if (name === 'repositoryType') {
                    this.handleChooseCodelibType(name, value)
                }
            },
            handleChooseCodelibType (name, value) {
                if (value === 'ID') {
                    this.newModel.repositoryHashId.hidden = false
                    this.newModel.repositoryName.hidden = true
                } else if (value === 'NAME') {
                    this.newModel.repositoryHashId.hidden = true
                    this.newModel.repositoryName.hidden = false
                }
                this.handleUpdateElement(name, value)
                if (name === 'repositoryHashId' && this.element.specifyRevision && this.triggerContainer) {
                    this.triggerContainer.params = this.triggerContainer.params.filter(item => item.id !== this.tmpRevision)
                    this.handleNewRevision()
                }
            },
            getRevisionParam () {
                const selectItem = this.list.find(item => item.repositoryHashId === this.element.repositoryHashId)
                let name = (selectItem && selectItem.aliasName) || ''
                name = name.replace(/\//g, '_')
                return 'svn.revision_' + name
            },
            handleNewRevision () {
                const paramId = this.getRevisionParam()
                this.handleUpdateElement('revision', '${' + paramId + '}')
                this.tmpRevision = paramId
                this.triggerContainer.params.push({
                    id: paramId,
                    type: 'STRING',
                    defaultValue: 'HEAD',
                    desc: 'Svn拉取代码插件指定的版本号',
                    required: true
                })
            },
            setSvnVersionState () {
                if (this.isThirdParty || this.isThirdDocker) {
                    this.newModel.svnVersion && this.newModel.svnVersion.list.map(item => {
                        item.disabled = false
                    })
                } else {
                    this.handleUpdateElement('svnVersion', 'V_1_8')
                    this.newModel.svnVersion && this.newModel.svnVersion.list.map(item => {
                        item.disabled = true
                    })
                }
            }
        }
    }
</script>
