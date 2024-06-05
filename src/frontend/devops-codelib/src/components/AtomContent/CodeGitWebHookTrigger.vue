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
        <form-field v-if="Object.keys(customTriggerControlModel).length && !atomPropsModel?.branchSettings">
            <accordion show-checkbox :show-content="enableThirdFilter" key="customTriggerControl" :is-version="true">
                <header class="var-header" style="height: 16px;" slot="header">
                    <span>
                        {{ $t('codelib.自定义触发控制') }}
                        <i class="bk-icon icon-info-circle ml5" v-bk-tooltips="$t('codelib.满足基础过滤条件后，根据第三方接口返回判断是否能够触发')"></i>
                        <a class="title-link" target="blink" :href="customTriggerDocsLink">{{ $t('codelib.查看使用指引和示例') }}</a>
                    </span>
                    <input class="accordion-checkbox" :checked="enableThirdFilter" type="checkbox" @change="toggleEnableThirdFilter" />
                </header>
                <div slot="content" class="bk-form bk-form-vertical">
                    <template v-for="(obj, key) in customTriggerControlModel">
                        <form-field :key="key" :desc="obj.desc" :desc-link="obj.descLink" :desc-link-text="obj.descLinkText" :required="obj.required" :label="obj.label" :is-error="errors.has(key)" :error-msg="errors.first(key)">
                            <component
                                :is="obj.component"
                                :name="key"
                                :value="element[key]"
                                v-bind="obj">
                            </component>
                        </form-field>
                    </template>
                </div>
            </accordion>
        </form-field>
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
                    if (!this.atomPropsModel?.branchSettings) {
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
            if (!this.atomPropsModel?.branchSettings) {
                const { thirdUrl, thirdSecretToken } = this.atomPropsModel
                if (thirdUrl && thirdSecretToken) {
                    this.customTriggerControlModel.thirdUrl = thirdUrl
                    this.customTriggerControlModel.thirdSecretToken = thirdSecretToken
                    this.atomPropsModel.thirdUrl.hidden = true
                    this.atomPropsModel.thirdSecretToken.hidden = true
                }
            }
        },
        methods: {
            toggleEnableThirdFilter () {
                this.enableThirdFilter = !this.enableThirdFilter
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
