<template>
    <section>
        <div class="bk-form bk-form-vertical">
            <template v-for="(obj, key) in atomPropsModel">
                <form-field
                    v-if="!isHidden(obj, element)"
                    :key="key"
                    :desc="obj.desc"
                    :desc-link="obj.descLink"
                    :desc-link-text="obj.descLinkText"
                    :required="obj.required"
                    :label="obj.label"
                    :is-error="errors.has(key)"
                    :error-msg="errors.first(key)"
                >
                    <component
                        :is="obj.component"
                        :disabled="disabled"
                        v-validate.initial="Object.assign({}, { max: getMaxLengthByType(obj.component) }, obj.rule, { required: !!obj.required })"
                        :lang="lang"
                        :name="key"
                        :handle-change="handleUpdateElement"
                        :value="element[key]"
                        v-bind="obj"
                    ></component>
                </form-field>
            </template>
        </div>
    </section>
</template>

<script>
    import atomMixin from './atomMixin'
    import validMixins from '../validMixins'
    export default {
        name: 'build-script',
        mixins: [atomMixin, validMixins],
        computed: {
            langList () {
                return this.atomPropsModel.scriptType.list
            },
            lang () {
                const lang = this.langList.find(stype => stype.value === this.element.scriptType)
                return lang ? lang.id : ''
            }
        },
        created () {
            if (this.atomPropsModel.archiveFile !== undefined) {
                this.atomPropsModel.archiveFile.hidden = !this.element.enableArchiveFile
            }
        }
    }
</script>
