<template>
    <div class="bk-form bk-form-vertical">
        <template v-for="(obj, key) in atomPropsModel">
            <template v-if="obj.type === 'group'">
                <form-field-group
                    v-if="rely(obj, element)"
                    :name="key"
                    :value="element[key]"
                    :key="key"
                    v-bind="obj"
                >
                    <template v-for="(i) in obj.children">
                        <form-field
                            :key="i.key"
                            v-if="rely(i, element)"
                            v-bind="i"
                            :is-error="errors.has(i.key)"
                            :error-msg="errors.first(i.key)"
                        >
                            <component
                                :is="i.component"
                                :name="i.key"
                                disabled
                                :value="element[i.key]"
                                v-bind="i"
                            >
                            </component>
                        </form-field>
                    </template>
                </form-field-group>
            </template>
            <template v-else>
                <form-field
                    v-if="!obj.hidden && rely(obj, element)"
                    :key="key"
                    :desc="obj.desc"
                    :desc-link="obj.descLink"
                    :desc-link-text="obj.descLinkText"
                    :required="obj.required"
                    :label="obj.label"
                >
                    <component
                        :disabled="true"
                        :is="obj.component"
                        :name="key"
                        :value="element[key]"
                        v-bind="obj"
                    ></component>
                </form-field>
            </template>
        </template>
    </div>
</template>

<script>
    import atomMixin from './atomMixin'
    export default {
        name: 'normal-atom',
        mixins: [atomMixin],
        props: {
            element: Object,
            atomPropsModel: Object
        },
        watch: {
            element: {
                handler () {
                    const showName = this.element.repositoryType === 'NAME'
                    this.atomPropsModel.repositoryName.hidden = !showName
                    this.atomPropsModel.repositoryHashId.hidden = showName
                },
                deep: true,
                immediate: true
            }
        }
    }
</script>

<style lang="scss" scoped>
    ::v-deep .bk-label,
    ::v-deep .bk-form-content,
    ::v-deep .bk-form-radio {
        font-size: 12px !important;
    }
</style>
