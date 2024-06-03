<template>
    <div class="cron-trigger">
        <accordion show-checkbox :show-content="isShowBasicRule" :after-toggle="toggleBasicRule">
            <header class="var-header" slot="header">
                <span>{{ $t('editPage.baseRule') }}</span>
                <input class="accordion-checkbox" type="checkbox" :checked="isShowBasicRule" style="margin-left: auto;" />
            </header>
            <div slot="content">
                <form-field :required="true" :label="$t('editPage.baseRule')" :is-error="errors.has('newExpression')">
                    <cron-timer :name="'newExpression'" ref="newExpression" :value="element['newExpression']" :handle-change="handleUpdateElement" v-validate.initial="{ 'required': isShowBasicRule }" />
                </form-field>
            </div>
        </accordion>

        <accordion show-checkbox :show-content="advance" :after-toggle="toggleAdvance">
            <header class="var-header" slot="header">
                <span>{{ $t('editPage.crontabTitle') }}</span>
                <input class="accordion-checkbox" type="checkbox" :checked="advance" style="margin-left: auto;" />
            </header>
            <div slot="content" class="cron-build-tab">
                <form-field :required="false" :label="$t('editPage.planRule')" :is-error="errors.has('advanceExpression')" :error-msg="errors.first('advanceExpression')">
                    <vuex-textarea name="advanceExpression" :handle-change="handleUpdateElement" :value="advanceValue" :placeholder="$t('editPage.crontabExpression')" v-validate.initial="{ 'required': advance }"></vuex-textarea>
                </form-field>
            </div>
        </accordion>
        <p class="empty-trigger-tips" v-if="!isShowBasicRule && !advance">{{ $t('editPage.triggerEmptyTips') }}</p>
        <accordion show-checkbox :show-content="isShowCodelibConfig" :after-toggle="toggleCodelibConfig">
            <header class="var-header" slot="header">
                <span>{{ $t('editPage.codelibConfigs') }}</span>
                <input class="accordion-checkbox" type="checkbox" :checked="isShowCodelibConfig" style="margin-left: auto;" />
            </header>
            <div slot="content" class="cron-build-tab">
                <form-field class="cron-build-tab" :desc="$t('editPage.timerTriggerCodelibTips')" :required="false" :label="$t('editPage.codelib')">
                    <div class="conditional-input-selector">
                        <bk-select
                            v-model="repositoryType"
                            ext-cls="group-box"
                            :clearable="false"
                            :disabled="disabled"
                            @change="(val) => handleUpdateElement('repositoryType', val)"
                        >
                            <bk-option
                                v-for="item in codelibConfigList"
                                :key="item.value"
                                :id="item.value"
                                :name="item.label"
                            >
                                <slot name="option-item" v-bind="item"></slot>
                            </bk-option>
                        </bk-select>
                        <request-selector
                            v-if="repositoryType === 'ID'"
                            class="input-selector"
                            v-bind="codelibOption"
                            :popover-min-width="250"
                            :disabled="disabled"
                            :url="getCodeUrl"
                            name="repoHashId"
                            :value="element['repoHashId']"
                            :handle-change="handleUpdateElement"
                        >
                        </request-selector>
                        <vuex-input
                            v-else
                            disabled
                            placeholder="将自动监听所属PAC代码库，无需设置"
                            class="input-selector"
                        >

                        </vuex-input>
                    </div>
                </form-field>
    
                <form-field v-if="repositoryType === 'ID'" class="cron-build-tab" :label="$t('editPage.branches')">
                    <BranchParameterArray
                        name="branches"
                        :disabled="disabled"
                        :repo-hash-id="element['repoHashId']"
                        :value="element['branches']"
                        :handle-change="handleUpdateElement"
                        :key="element['repoHashId']"
                    >
                    </BranchParameterArray>
                </form-field>
                <form-field class="bk-form-checkbox">
                    <atom-checkbox :disabled="disabled" :text="$t('editPage.noScm')" :name="'noScm'" :value="element['noScm']" :handle-change="handleUpdateElement" />
                </form-field>
            </div>
        </accordion>

    </div>
</template>

<script>
    import atomMixin from './atomMixin'
    import validMixins from '../validMixins'
    import BranchParameterArray from '../AtomFormComponent/BranchParameterArray/index'
    import { REPOSITORY_API_URL_PREFIX } from '@/store/constants'

    export default {
        name: 'timer-trigger',
        components: {
            BranchParameterArray
        },
        mixins: [atomMixin, validMixins],
        data () {
            return {
                isShowBasicRule: this.notEmptyArray('newExpression'),
                advance: this.notEmptyArray('advanceExpression'),
                isShowCodelibConfig: this.element.repoHashId || this.element.noScm,
                advanceValue: (this.element.advanceExpression && this.element.advanceExpression.join('\n')) || '',
                repositoryType: 'ID'
            }
        },
        computed: {
            getCodeUrl () {
                return `/${REPOSITORY_API_URL_PREFIX}/user/repositories/{projectId}/hasPermissionList?permission=USE&page=1&pageSize=1000`
            },
            codelibOption () {
                return {
                    paramId: 'repositoryHashId',
                    paramName: 'aliasName',
                    searchable: true
                }
            },
            curComponent () {
                return this.codelibConfigList.find(i => i.value === this.repositoryType) || {
                    type: 'request-selector',
                    key: 'repositoryHashId',
                    required: false
                }
            },
            codelibConfigList () {
                return [
                    {
                        value: 'ID',
                        label: '选择代码库',
                        key: 'repositoryHashId'
                    },
                    {
                        value: 'SELF',
                        label: '监听PAC',
                        key: ''
                    }
                ]
            }
        },
        watch: {
            isShowBasicRule (newVal) {
                if (!newVal && !this.advance) {
                    setTimeout(() => {
                        this.handleUpdateElement('isError', true)
                    }, 200)
                }
            },
            advance (newVal) {
                if (!newVal && !this.isShowBasicRule) {
                    setTimeout(() => {
                        this.handleUpdateElement('isError', true)
                    }, 200)
                }
            }
        },
        created () {
            if (!this.advance && this.element.expression !== undefined && this.element.expression !== '') { // 原始定时数据改为高级
                this.handleUpdateElement('advanceExpression', this.element.expression)
                this.deletePropKey({
                    element: this.element,
                    propKey: 'expression'
                })
            }
        },
        methods: {
            notEmptyArray (prop) {
                return Array.isArray(this.element[prop]) && this.element[prop].length > 0
            },
            updateProps (newParam) {
                this.updateAtom({
                    element: this.element,
                    newParam
                })
            },
            toggleBasicRule (element, show) {
                this.isShowBasicRule = show
                if (!show) {
                    const emptyArr = []
                    this.handleUpdateElement('newExpression', emptyArr)
                    this.$refs.newExpression && this.$refs.newExpression.resetSelectedWeek()
                }
            },
            toggleAdvance (element, show) {
                this.advance = show
                if (!show) {
                    this.advanceValue = ''
                    this.handleUpdateElement('advanceExpression', this.advanceValue)
                }
            },
            toggleCodelibConfig (element, show) {
                this.isShowCodelibConfig = show
                if (!show) {
                    this.handleUpdateElement('repoHashId', '')
                    this.handleUpdateElement('branches', [])
                    this.handleUpdateElement('noScm', false)
                }
            },
            handleUpdateElement (name, value) {
                if (name === 'advanceExpression') {
                    value = value.split('\n').filter(item => item !== '')
                }
                this.updateProps({
                    [name]: value
                })
            }
        }
    }
</script>

<style lang="scss">
    @import '../../scss/conf';
    .cron-trigger {
        .bk-form-checkbox {
            padding-right: 20px;
        }
        .cron-build-tab {
            label {
                font-weight: normal;
                font-size: 12px;
                margin: 5px 0;
                display: block;
            }
        }
        .bk-form-item.is-danger .cron-build-week {
            ul li {
                border-top: 1px solid $dangerColor;
                border-bottom: 1px solid $dangerColor;
                &:last-child {
                    border-right: 1px solid $dangerColor;
                }
                &:first-child {
                    border-left: 1px solid $dangerColor;
                }
            }
        }
        .bk-form-item.is-danger .cron-build-time input {
            border-color: #c3cdd7;
        }
        .empty-trigger-tips {
            color: $dangerColor;
            font-size: 12px;
        }
    }
</style>
