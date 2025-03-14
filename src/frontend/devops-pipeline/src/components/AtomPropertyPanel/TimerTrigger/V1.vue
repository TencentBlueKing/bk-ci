<template>
    <div class="bk-form bk-form-vertical">
        <accordion
            show-checkbox
            :show-content="isShowBasicRule"
            :disabled="disabled"
            :after-toggle="toggleBasicRule"
        >
            <header
                class="var-header"
                slot="header"
            >
                <span>{{ $t('editPage.baseRule') }}</span>
                <input
                    :class="{ 'accordion-checkbox': true, 'disabled': disabled }"
                    type="checkbox"
                    :disabled="disabled"
                    :checked="isShowBasicRule"
                    style="margin-left:auto;"
                />
            </header>
            <div slot="content">
                <form-field
                    :required="true"
                    :label="$t('editPage.baseRule')"
                    :is-error="errors.has('newExpression')"
                >
                    <cron-timer
                        :name="'newExpression'"
                        ref="newExpression"
                        :value="element['newExpression']"
                        :handle-change="handleUpdateElement"
                        v-validate.initial="{ 'required': isShowBasicRule }"
                    />
                </form-field>
            </div>
        </accordion>

        <accordion
            show-checkbox
            :show-content="advance"
            :disabled="disabled"
            :after-toggle="toggleAdvance"
        >
            <header
                class="var-header"
                slot="header"
            >
                <span>{{ $t('editPage.crontabTitle') }}</span>
                <input
                    class="accordion-checkbox"
                    type="checkbox"
                    :checked="advance"
                    :disabled="disabled"
                    style="margin-left: auto;"
                />
            </header>
            <div
                slot="content"
                class="cron-build-tab"
            >
                <form-field
                    :required="false"
                    :label="$t('editPage.planRule')"
                    :is-error="errors.has('advanceExpression')"
                    :error-msg="errors.first('advanceExpression')"
                >
                    <vuex-textarea
                        name="advanceExpression"
                        :handle-change="handleUpdateElement"
                        :value="advanceValue"
                        :placeholder="$t('editPage.crontabExpression')"
                        v-validate.initial="{ 'required': advance }"
                        :disabled="disabled"
                    />
                </form-field>
            </div>
        </accordion>
        <p
            class="empty-trigger-tips"
            v-if="!isShowBasicRule && !advance"
        >
            {{ $t('editPage.triggerEmptyTips') }}
        </p>
        <accordion
            show-checkbox
            :show-content="isShowCodelibConfig"
            :disabled="disabled"
            :after-toggle="toggleCodelibConfig"
        >
            <header
                class="var-header"
                slot="header"
            >
                <span>{{ $t('editPage.codelibConfigs') }}</span>
                <input
                    class="accordion-checkbox"
                    type="checkbox"
                    :checked="isShowCodelibConfig"
                    :disabled="disabled"
                    style="margin-left: auto;"
                />
            </header>
            <div
                slot="content"
                class="cron-build-tab"
            >
                <form-field
                    class="cron-build-tab"
                    :desc="$t('editPage.timerTriggerCodelibTips')"
                    :required="false"
                    :label="$t('editPage.codelib')"
                >
                    <CodelibSelector
                        :disabled="disabled"
                        :element="element"
                        :handle-change="handleUpdateElement"
                    />
                </form-field>
    
                <form-field
                    class="cron-build-tab"
                    :label="$t('editPage.branches')"
                    :desc="$t('editPage.timerTriggerBranchTips')"
                >
                    <BranchParameterArray
                        name="branches"
                        :element="element"
                        :disabled="disabled"
                        :value="element['branches']"
                        :handle-change="handleUpdateElement"
                    >
                    </BranchParameterArray>
                </form-field>
                <form-field class="bk-form-checkbox">
                    <atom-checkbox
                        :disabled="disabled"
                        :text="$t('editPage.noScm')"
                        :name="'noScm'"
                        :value="element['noScm']"
                        :handle-change="handleUpdateElement"
                    />
                </form-field>
            </div>
        </accordion>
    </div>
</template>

<script>
    import BranchParameterArray from '../../AtomFormComponent/BranchParameterArray/index'
    import validMixins from '../../validMixins'
    import atomMixin from '../atomMixin'
    import CodelibSelector from './CodelibSelector'

    export default {
        name: 'timer-trigger',
        components: {
            BranchParameterArray,
            CodelibSelector
        },
        mixins: [atomMixin, validMixins],
        data () {
            return {
                isShowBasicRule: false,
                advance: false,
                isShowCodelibConfig: this.element?.repoHashId || this.element?.noScm || this.element?.repoName || this.element?.branches?.length || this.element?.repositoryType === 'SELF',
                advanceValue: (this.element.advanceExpression && this.element.advanceExpression.join('\n')) || ''
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
            this.isShowBasicRule = this.notEmptyArray('newExpression')
            this.advance = this.notEmptyArray('advanceExpression')
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
    @import '../../../scss/conf';
    .timer-trigger {
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
