<template>
    <div class="bk-form bk-form-vertical">
        <template v-for="(obj, key) in atomPropsModel">
            <template v-if="obj.type === 'group'">
                <form-field-group v-if="rely(obj, element)" :name="key" :value="element[key]" :key="key" v-bind="obj">
                    <template v-for="(i) in obj.children">
                        <form-field :key="i.key" v-if="rely(i, element)" v-bind="i" :is-error="errors.has(i.key)" :error-msg="errors.first(i.key)">
                            <component
                                :is="i.component"
                                :name="i.key"
                                disabled
                                :value="element[i.key]"
                                v-bind="i">
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
                        disabled
                        :value="element[key]"
                        :element="element"
                        v-bind="obj">
                    </component>
                </form-field>
            </template>
        </template>
    </div>
</template>
<script>
    import atomMixin from './atomMixin'
    export default {
        name: 'code-git-web-hook-trigger',
        mixins: [atomMixin],
        props: {
            element: Object,
            atomPropsModel: Object
        },
        data () {
            return {
                customTriggerControlModel: {},
                customTriggerDocsLink: 'https://github.com/Tencent/bk-ci/issues/7743#issue-1391717634'
            }
        },
        watch: {
            element: {
                handler (val) {
                    if (!this.atomPropsModel?.repositoryType?.list) {
                        const showName = this.element.repositoryType === 'NAME'
                        this.atomPropsModel.repositoryName.hidden = !showName
                        this.atomPropsModel.repositoryHashId.hidden = showName
                    }
                },
                deep: true,
                immediate: true
            }
        },
        created () {
            if (!this.atomPropsModel?.repositoryType?.list) {
                const { thirdUrl, thirdSecretToken } = this.atomPropsModel
                if (thirdUrl && thirdSecretToken) {
                    this.customTriggerControlModel.thirdUrl = thirdUrl
                    this.customTriggerControlModel.thirdSecretToken = thirdSecretToken
                    this.atomPropsModel.thirdUrl.hidden = true
                    this.atomPropsModel.thirdSecretToken.hidden = true
                }
            }
        }
    }
</script>

<style lang="scss">
    .title-link {
        cursor: pointer;
        margin-left: 10px;
        color: #3c96ff;
    }
</style>
