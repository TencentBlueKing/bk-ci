<template>
    <div class="bk-form bk-form-vertical">
        <route-tips :visible="githubAppUrl" :github-app-url="githubAppUrl" v-if="githubAppUrl"></route-tips>
        <template v-for="(obj, key) in atomPropsModel">
            <template v-if="obj.type === 'group'">
                <form-field-group v-if="rely(obj, element)" :name="key" :value="element[key]" :handle-change="handleMethods" :key="key" v-bind="obj">
                    <template v-for="(i, index) in obj.children">
                        <form-field :key="i.key" v-if="rely(i, element)" v-bind="i" :is-error="errors.has(i.key)" :error-msg="errors.first(i.key)">
                            <component
                                :is="i.component"
                                :name="i.key"
                                v-validate.initial="Object.assign({}, { max: getMaxLengthByType(i.component) }, i.rule, { required: !!i.required })"
                                :handle-change="handleMethods"
                                :value="element[i.key] || atomPropsModel[key]?.children[index]?.default"
                                v-bind="i"
                                
                                @change="listChange">
                            </component>
                        </form-field>
                    </template>
                </form-field-group>
            </template>
            <template v-else>
                <form-field v-if="!obj.hidden && rely(obj, element)" :key="key" v-bind="obj" :is-error="errors.has(key)" :error-msg="errors.first(key)">
                    <component
                        :is="obj.component"
                        :name="key"
                        v-validate.initial="Object.assign({}, { max: getMaxLengthByType(obj.component) }, obj.rule, { required: !!obj.required })"
                        :handle-change="handleMethods"
                        :value="element[key]"
                        :element="element"
                        :disabled="disabled"
                        v-bind="obj">
                    </component>
                </form-field>
            </template>
        </template>
    </div>
</template>

<script>
    import atomMixin from './atomMixin'
    import validMixins from '../validMixins'
    export default {
        name: 'code-git-web-hook-trigger',
        mixins: [atomMixin, validMixins],
        data () {
            return {
                githubAppUrl: '',
                list: []
            }
        },
        async mounted () {
            try {
                const res = await this.$store.dispatch('common/getGithubAppUrl')
                this.githubAppUrl = res.url
            } catch (err) {
                this.$showTips({
                    theme: 'error',
                    message: err.message || err
                })
            }
            if (!this.element.repositoryType) {
                this.handleUpdateElement('repositoryType', 'ID')
            }
            this.handleChooseCodelibType('repositoryType', this.element.repositoryType)
        },
        methods: {
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
                if (!this.atomPropsModel?.branchSettings) {
                    if (value === 'ID') {
                        this.atomPropsModel.repositoryHashId.hidden = false
                        this.atomPropsModel.repositoryName.hidden = true
                    } else if (value === 'NAME') {
                        this.atomPropsModel.repositoryHashId.hidden = true
                        this.atomPropsModel.repositoryName.hidden = false
                    }
                }
                this.handleUpdateElement(name, value)
            }
        }
    }
</script>
