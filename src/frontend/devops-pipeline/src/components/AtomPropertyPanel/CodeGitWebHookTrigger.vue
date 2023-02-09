<template>
    <div class="bk-form bk-form-vertical">
        <template v-for="(obj, key) in atomPropsModel">
            <form-field v-if="!obj.hidden && rely(obj, element)" :key="key" :desc="obj.desc" :desc-link="obj.descLink" :desc-link-text="obj.descLinkText" :required="obj.required" :label="obj.label" :is-error="errors.has(key)" :error-msg="errors.first(key)">
                <component
                    :is="obj.component"
                    :name="key"
                    v-validate.initial="Object.assign({}, { max: getMaxLengthByType(obj.component) }, obj.rule, { required: !!obj.required })"
                    :handle-change="key === 'eventType' ? handleBlockEnable : handleMethods"
                    :value="element[key]"
                    v-bind="obj">
                </component>
            </form-field>
        </template>
        <form-field v-if="Object.keys(customTriggerControlModel).length">
            <accordion show-checkbox :show-content="enableThirdFilter" key="customTriggerControl" :is-version="true">
                <header class="var-header" style="height: 16px;" slot="header">
                    <span>
                        {{ $t('editPage.customTriggerControl') }}
                        <i class="bk-icon icon-info-circle ml5" v-bk-tooltips="$t('editPage.customTriggerControlTips')"></i>
                        <a class="title-link" target="blink" :href="customTriggerDocsLink">{{ $t('editPage.customTriggerLinkDesc') }}</a>
                    </span>
                    <input class="accordion-checkbox" :disabled="disabled" :checked="enableThirdFilter" type="checkbox" @click.stop @change="toggleEnableThirdFilter" />
                </header>
                <div slot="content" class="bk-form bk-form-vertical" v-if="enableThirdFilter">
                    <template v-for="(obj, key) in customTriggerControlModel">
                        <form-field :key="key" :desc="obj.desc" :desc-link="obj.descLink" :desc-link-text="obj.descLinkText" :required="obj.required" :label="obj.label" :is-error="errors.has(key)" :error-msg="errors.first(key)">
                            <component
                                :is="obj.component"
                                :name="key"
                                v-validate.initial="Object.assign({}, { max: getMaxLengthByType(obj.component) }, obj.rule, { required: !!obj.required })"
                                :handle-change="key === 'eventType' ? handleBlockEnable : handleMethods"
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
    import validMixins from '../validMixins'
    export default {
        name: 'code-git-web-hook-trigger',
        mixins: [atomMixin, validMixins],
        data () {
            return {
                customTriggerControlModel: {},
                enableThirdFilter: false,
                customTriggerDocsLink: `${IWIKI_DOCS_URL}/pages/viewpage.action?pageId=4007038192`
            }
        },
        watch: {
            'element.enableCheck': {
                // git事件触发选中commit check ， 同时锁定提交才显示
                handler (newVal) {
                    if (newVal) {
                        this.atomPropsModel.block.hidden = false
                    } else {
                        this.atomPropsModel.block.hidden = true
                        this.handleUpdateElement('block', false)
                    }
                },
                immediate: true,
                deep: true
            }
        },
        created () {
            this.enableThirdFilter = this.element.enableThirdFilter || false
            this.customTriggerControlModel = {}
            const { thirdUrl, thirdSecretToken } = this.atomPropsModel
            if (thirdUrl && thirdSecretToken) {
                this.customTriggerControlModel.thirdUrl = thirdUrl
                this.customTriggerControlModel.thirdSecretToken = thirdSecretToken
                this.atomPropsModel.thirdUrl.hidden = true
                this.atomPropsModel.thirdSecretToken.hidden = true
            }
            if (this.element.eventType === 'MERGE_REQUEST') {
                this.atomPropsModel.webhookQueue.hidden = false
            } else {
                this.atomPropsModel.block.hidden = true
                this.atomPropsModel.webhookQueue.hidden = true
            }
            if (!this.element.repositoryType) {
                this.handleUpdateElement('repositoryType', 'ID')
            }
            this.handleChooseCodelibType('repositoryType', this.element.repositoryType)
        },
        methods: {
            handleBlockEnable (name, value) {
                if (value === 'MERGE_REQUEST') {
                    this.atomPropsModel.block.hidden = false
                    this.atomPropsModel.webhookQueue.hidden = false
                } else {
                    this.atomPropsModel.block.hidden = true
                    this.atomPropsModel.webhookQueue.hidden = true
                }
                this.handleUpdateElement(name, value)
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
            },
            toggleEnableThirdFilter () {
                this.enableThirdFilter = !this.enableThirdFilter
                this.handleUpdateElement('enableThirdFilter', this.enableThirdFilter)
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
