<template>
    <form class="bk-form bk-form-vertical">
        <template v-for="(obj, key) in newModel">
            <form-field v-if="!obj.hidden" :key="key" :desc="obj.desc" :required="obj.required" :label="obj.label" :is-error="errors.has(key)" :error-msg="errors.first(key)">
                <component :is="obj.component" :name="key" v-validate.initial="Object.assign({}, obj.rule, { required: !!obj.required })" :handle-change="handleMethods" :value="element[key]" v-bind="obj" @change="listChange" :repository-type="element[&quot;repositoryType&quot;]" :repository-hash-id="element[&quot;repositoryHashId&quot;]" :branch-name="element[&quot;branchName&quot;]" :element-type="element[&quot;@type&quot;]" :no-use-permission="noUsePermission" :set-no-use-permission="setNoUsePermission"></component>
                <p class="bk-form-help is-warning" v-if="key === 'path' && obj.warn">{{ obj.warn }}</p>
                <route-tips :visible="true" :src-tips="srcTips" :path-tips="pathTips" v-if="key === 'path' && element['repositoryHashId']"></route-tips>
            </form-field>
        </template>
    </form>
</template>

<script>
    import atomMixin from './atomMixin'
    import validMixins from '../validMixins'
    import { mapGetters } from 'vuex'
    export default {
        name: 'code-pull-atom',
        mixins: [atomMixin, validMixins],
        data () {
            return {
                tips: '当前流水线存在多个代码拉取插件，你需设置此字段以解决冲突问题',
                noUsePermission: true,
                list: [],
                newModel: {}
            }
        },
        computed: {
            ...mapGetters('atom', [
                'isCodePullAtom'
            ]),
            srcTips () {
                const srcItem = this.list.find(item => item.repositoryHashId === this.element.repositoryHashId)
                const srcPrefix = (srcItem && srcItem.url) ? srcItem.url : '${CODE_ROOT_URL}'
                return `${srcPrefix}/*`
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
            if (!this.element.repositoryType && this.element['@type'] === 'CODE_GIT') {
                this.handleUpdateElement('repositoryType', 'ID')
            }
            this.handleChooseCodelibType('repositoryType', this.element.repositoryType)
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
            setNoUsePermission (val) {
                this.noUsePermission = !!val
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
            handleMethods (name, value) {
                if (name === 'repositoryType') {
                    this.handleChooseCodelibType(name, value)
                } else {
                    this.handleUpdateElement(name, value)
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
            }
        }
    }
</script>
