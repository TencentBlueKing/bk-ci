<template>
    <div class="cron-trigger">
        <accordion show-checkbox :show-content="isShowBasicRule" :after-toggle="toggleBasicRule">
            <header class="var-header" slot="header">
                <span>基础规则</span>
                <input class="accordion-checkbox" type="checkbox" :checked="showBasicRule" style="margin-left: auto;" />
            </header>
            <div slot="content">
                <form-field :required="true" label="基础规则" :is-error="errors.has(&quot;newExpression&quot;)" :error-msg="errors.first(&quot;newExpression&quot;)">
                    <cron-timer :name="'newExpression'" ref="newExpression" :value="element['newExpression']" :handle-change="handleUpdateElement" v-validate.initial="{ &quot;required&quot;: showBasicRule }" />
                </form-field>
            </div>
        </accordion>

        <accordion show-checkbox :show-content="showAdvance" :after-toggle="toggleAdvance">
            <header class="var-header" slot="header">
                <span>高级（自定义crontab表达式）</span>
                <input class="accordion-checkbox" type="checkbox" :checked="advance" style="margin-left: auto;" />
            </header>
            <div slot="content" class="cron-build-tab">
                <form-field :required="false" label="计划任务规则" :is-error="errors.has(&quot;advanceExpression&quot;)" :error-msg="errors.first(&quot;advanceExpression&quot;)">
                    <vuex-textarea name="advanceExpression" :handle-change="handleUpdateElement" :value="advanceValue" placeholder="请填写语法正确的crontab表达式，多条表达式请换行输入" v-validate.initial="{ &quot;required&quot;: advance }"></vuex-textarea>
                </form-field>
            </div>
        </accordion>

        <p class="empty-trigger-tips" v-if="!showBasicRule && !advance">基础规则和自定义表达式不能同时为空</p>

        <form-field class="bk-form-checkbox">
            <atom-checkbox :disabled="disabled" text="源代码未更新时不触发" :name="'noScm'" :value="element['noScm']" :handle-change="handleUpdateElement" />
        </form-field>
    </div>
</template>

<script>
    import atomMixin from './atomMixin'
    import validMixins from '../validMixins'

    export default {
        name: 'timer-trigger',
        mixins: [atomMixin, validMixins],
        data () {
            return {
                isShowBasicRule: true,
                showBasicRule: false,
                advance: false,
                advanceValue: (this.element.advanceExpression && this.element.advanceExpression.join('\n')) || []
            }
        },
        computed: {
            showAdvance () {
                return this.element.advanceExpression !== undefined && !!this.element.advanceExpression.length
            }
            // advanceValue () {
            //     return (this.showAdvance && this.element.advanceExpression.join('\n')) || []
            // }
        },
        watch: {
            showBasicRule (newVal) {
                if (!newVal && !this.advance) {
                    setTimeout(() => {
                        this.handleUpdateElement('isError', true)
                    }, 200)
                }
            },
            advance (newVal) {
                if (!newVal && !this.showBasicRule) {
                    setTimeout(() => {
                        this.handleUpdateElement('isError', true)
                    }, 200)
                }
            }
        },
        created () {
            if (!this.showAdvance && this.element.expression !== undefined && this.element.expression !== '') { // 原始定时数据改为高级
                this.handleUpdateElement('advanceExpression', this.element.expression)
                this.deletePropKey({
                    element: this.element,
                    propKey: 'expression'
                })
            }
            if (this.advanceValue && this.advanceValue.length && !this.element['newExpression'].length) {
                this.isShowBasicRule = false
            }
            this.$nextTick(() => {
                this.toggleBasicRule(this.$el, this.isShowBasicRule)
                this.toggleAdvance(this.$el, this.showAdvance)
            })
        },
        methods: {
            updateProps (newParam) {
                this.updateAtom({
                    element: this.element,
                    newParam
                })
            },
            toggleBasicRule (element, show) {
                if (show) {
                    this.showBasicRule = true
                } else {
                    const emptyArr = []
                    this.showBasicRule = false
                    this.handleUpdateElement('newExpression', emptyArr)
                    this.$refs.newExpression && this.$refs.newExpression.resetSelectedWeek()
                }
            },
            toggleAdvance (element, show) {
                if (show) {
                    this.advance = true
                } else {
                    const emptyArr = ''
                    this.advance = false
                    this.advanceValue = []
                    this.handleUpdateElement('advanceExpression', emptyArr)
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
