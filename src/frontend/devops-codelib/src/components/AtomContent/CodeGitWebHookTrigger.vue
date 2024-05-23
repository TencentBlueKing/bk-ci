<template>
    <div class="bk-form bk-form-vertical">
        <template v-for="(obj, key) in atomPropsModel">
            <form-field v-if="!obj.hidden && rely(obj, element)" :key="key" :desc="obj.desc" :desc-link="obj.descLink" :desc-link-text="obj.descLinkText" :required="obj.required" :label="obj.label" :is-error="errors.has(key)" :error-msg="errors.first(key)">
                <component
                    :is="obj.component"
                    :name="key"
                    :value="element[key]"
                    disabled
                    v-bind="obj">
                </component>
            </form-field>
        </template>
        <form-field v-if="Object.keys(customTriggerControlModel).length">
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
                enableThirdFilter: false,
                customTriggerDocsLink: 'https://github.com/Tencent/bk-ci/issues/7743#issue-1391717634'
            }
        },
        watch: {
            element: {
                handler (val) {
                    const showName = this.element.repositoryType === 'NAME'
                    this.atomPropsModel.repositoryName.hidden = !showName
                    this.atomPropsModel.repositoryHashId.hidden = showName
                },
                deep: true,
                immediate: true
            }
        },
        created () {
            const { thirdUrl, thirdSecretToken } = this.atomPropsModel
            if (thirdUrl && thirdSecretToken) {
                this.customTriggerControlModel.thirdUrl = thirdUrl
                this.customTriggerControlModel.thirdSecretToken = thirdSecretToken
                this.atomPropsModel.thirdUrl.hidden = true
                this.atomPropsModel.thirdSecretToken.hidden = true
            }
        },
        methods: {
            toggleEnableThirdFilter () {
                this.enableThirdFilter = !this.enableThirdFilter
            }
        }
    }
</script>

<style lang="scss" scoped>
    .title-link {
        cursor: pointer;
        margin-left: 10px;
        color: #3c96ff;
    }
    ::v-deep .bk-label,
    ::v-deep .bk-form-content,
    ::v-deep .bk-form-radio {
        font-size: 12px !important;
    }
</style>
