<template>
    <div class="bk-form bk-form-vertical">
        <route-tips :visible="githubAppUrl" :github-app-url="githubAppUrl" v-if="githubAppUrl"></route-tips>
        <template v-for="(obj, key) in atomPropsModel">
            <form-field v-if="!isHidden(obj, element)" :key="key" :desc="obj.desc" :desc-link="obj.descLink" :desc-link-text="obj.descLinkText" :required="obj.required" :label="obj.label" :is-error="errors.has(key)" :error-msg="errors.first(key)">
                <component
                    :is="obj.component"
                    :name="key"
                    v-validate.initial="Object.assign({}, obj.rule, { required: !!obj.required })"
                    :handle-change="handleMethods"
                    :value="element[key]"
                    v-bind="obj"
                    @change="listChange">
                </component>
            </form-field>
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
                const res = await this.$store.dispatch('soda/getGithubAppUrl')
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
                if (value === 'ID') {
                    this.atomPropsModel.repositoryHashId.hidden = false
                    this.atomPropsModel.repositoryName.hidden = true
                } else if (value === 'NAME') {
                    this.atomPropsModel.repositoryHashId.hidden = true
                    this.atomPropsModel.repositoryName.hidden = false
                }
                this.handleUpdateElement(name, value)
            }
        }
    }
</script>
