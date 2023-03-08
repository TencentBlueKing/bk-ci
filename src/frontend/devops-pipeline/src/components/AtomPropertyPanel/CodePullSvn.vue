<template>
    <form class="bk-form bk-form-vertical">
        <template v-for="(obj, key) in newModel">
            <form-field v-if="!obj.hidden" :key="key" :desc="obj.desc" :required="obj.required" :label="obj.label" :is-error="errors.has(key)" :error-msg="errors.first(key)">
                <component v-if="key === 'svnPath'" :is="obj.component" :name="key" v-validate.initial="Object.assign({}, obj.rule, { required: !!obj.required })" :handle-change="handleUpdate" :value="element[key]" v-bind="obj" :repository-hash-id="element[&quot;repositoryHashId&quot;]" :list="list"></component>
                <component v-else-if="key === 'repositoryHashId'" :is="obj.component" :name="key" v-validate.initial="Object.assign({}, obj.rule, { required: !!obj.required })" :handle-change="handleUpdate" :value="element[key]" v-bind="obj" @change="listChange"></component>
                <component v-else :is="obj.component" :name="key" v-validate.initial="Object.assign({}, { max: getMaxLengthByType(obj.component) }, obj.rule, { required: !!obj.required })" :handle-change="handleUpdate" :value="element[key]" v-bind="obj"></component>
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
                tips: this.$t('editPage.repoConflict')
            }
        },
        computed: {
            ...mapState('atom', [
                'pipeline'
            ]),
            ...mapGetters('atom', [
                'getAllContainers',
                'isCodePullAtom'
            ]),
            pipelineStages () {
                const { getAllContainers } = this
                return getAllContainers(this.pipeline.stages || []) || []
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
                if (name === 'specifyRevision') {
                    if (value) {
                        this.newModel.revision.hidden = false
                    } else {
                        this.newModel.revision.hidden = true
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
            },
            setSvnVersionState () {
                if (this.isThirdParty) {
                    this.newModel.svnVersion && this.newModel.svnVersion.list.forEach(item => {
                        item.disabled = false
                    })
                } else {
                    this.handleUpdateElement('svnVersion', 'V_1_8')
                    this.newModel.svnVersion && this.newModel.svnVersion.list.forEach(item => {
                        item.disabled = true
                    })
                }
            }
        }
    }
</script>
