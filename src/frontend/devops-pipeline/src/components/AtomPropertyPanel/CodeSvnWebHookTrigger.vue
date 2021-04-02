<template>
    <div class="bk-form bk-form-vertical">
        <form-field v-for="(obj, key) in atomPropsModel" v-if="!obj.hidden" :key="key" :desc="obj.desc" :desc-link="obj.descLink" :desc-link-text="obj.descLinkText" :required="obj.required" :label="obj.label" :is-error="errors.has(key)" :error-msg="errors.first(key)">
            <component
                :is="obj.component"
                :name="key"
                v-validate.initial="Object.assign({}, obj.rule, { required: !!obj.required })"
                :handle-change="handleMethods"
                :value="element[key]"
                v-bind="obj">
            </component>
        </form-field>
    </div>
</template>

<script>
    import atomMixin from './atomMixin'
    import validMixins from '../validMixins'
    export default {
        name: 'code-svn-web-hook-trigger',
        mixins: [atomMixin, validMixins],
        created () {
            if (!this.element.repositoryType) {
                this.handleUpdateElement('repositoryType', 'ID')
            }
            this.handleChooseCodelibType('repositoryType', this.element.repositoryType)
        },
        methods: {
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
