<template>
    <accordion
        show-checkbox
        show-content
        key="otherChoice"
        v-if="showPanelType !== 'PAUSE'"
    >
        <header
            class="var-header"
            slot="header"
        >
            <span>{{ $t('editPage.atomOption') }}</span>
            <i
                class="devops-icon icon-angle-down"
                style="display:block"
            ></i>
        </header>
        <div
            slot="content"
            class="bk-form bk-form-vertical atom-control-option"
        >
            <template v-for="(obj, key) in optionModel">
                <form-field
                    :key="key"
                    v-if="(!isHidden(obj, element) && container['@type'] !== 'trigger') || key === 'enable'"
                    :desc="obj.desc"
                    :required="obj.required"
                    :label="obj.label"
                    :docs-link="obj.docsLink"
                    :is-error="errors.has(key)"
                    :error-msg="errors.first(key)"
                    :class="obj.extCls"
                >
                    <component
                        :disabled="disabled"
                        :is="obj.component"
                        :container="container"
                        :element="element"
                        :name="key"
                        v-validate.initial="Object.assign({}, obj.rule, { required: !!obj.required })"
                        :handle-change="handleUpdateElementOption"
                        :value="atomOption[key]"
                        v-bind="getBindObj(obj)"
                    ></component>
                </form-field>
            </template>
        </div>
    </accordion>
</template>

<script>
    import optionConfigMixin from '@/store/modules/common/optionConfigMixin'
    import Vue from 'vue'
    import { mapActions, mapState } from 'vuex'
    import validMixins from '../validMixins'
    import atomMixin from './atomMixin'
    export default {
        name: 'atom-config',
        mixins: [atomMixin, validMixins, optionConfigMixin],
        computed: {
            ...mapState('atom', [
                'showPanelType'
            ]),
            atomOption () {
                return this.element.additionalOptions || {}
            },
            atomCode () {
                return this.element.atomCode
            },
            atomVersion () {
                return this.element.version
            },
            atomOptionConfig () {
                return this.atomPropsModel.config || {}
            },
            optionModel () {
                const model = { ...this.ATOM_OPTION }
                const failControlManualRetryOption = {
                    id: 'MANUAL_RETRY',
                    name: this.$t('storeMap.manualRetry')
                }
                model.failControl.list = [
                    {
                        id: 'continueWhenFailed',
                        name: this.$t('storeMap.continueWhenFailed')
                    },
                    {
                        id: 'retryWhenFailed',
                        name: this.$t('storeMap.automaticRetry')
                    }
                ]

                if (!(this.atomOption.manualSkip === false && this.atomOption.failControl && this.atomOption.failControl.includes('continueWhenFailed'))) {
                    model.failControl.list.push(failControlManualRetryOption)
                }

                // 编辑状态
                if (!this.disabled) {
                    model.runCondition.list = model.runCondition.list.filter(item => {
                        return item.id !== 'PARENT_TASK_CANCELED_OR_TIMEOUT'
                    })
                }

                return model
            }
        },
        watch: {
            atomCode () {
                this.initOptionConfig()
            },
            atomVersion () {
                this.initOptionConfig()
            }
        },
        created () {
            this.initOptionConfig()
        },
        methods: {
            ...mapActions('atom', [
                'setPipelineEditing'
            ]),
            getBindObj (obj) {
                const { isHidden, extCls, desc, ...rest } = obj
                return rest
            },
            handleUpdateElementOption (name, value) {
                if (this.element.additionalOptions && this.element.additionalOptions[name] === undefined) {
                    Vue.set(this.element.additionalOptions, name, value)
                }
                let clearFields = {}
                if (
                    value === this.ATOM_OPTION[name]?.clearValue
                    && Array.isArray(this.ATOM_OPTION[name]?.clearFields)
                ) {
                    // 重置关联的值，可配置相关的联动值
                    clearFields = this.ATOM_OPTION[name].clearFields.reduce((acc, key) => {
                        acc[key] = this.getFieldDefault(key, this.ATOM_OPTION)
                        return acc
                    }, {})
                }

                const currentfailControl = [...new Set(name === 'failControl' ? value : this.atomOption.failControl)] // 去重

                const includeManualRetry = currentfailControl.includes('MANUAL_RETRY')
                const continueable = currentfailControl.includes('continueWhenFailed')
                const isAutoSkip = continueable && (this.atomOption.manualSkip === false || (name === 'manualSkip' && value === false))
                const retryable = currentfailControl.includes('retryWhenFailed')
                const manualRetry = !isAutoSkip && includeManualRetry

                const failControl = isAutoSkip ? currentfailControl.filter(item => item !== 'MANUAL_RETRY') : [...currentfailControl]

                this.handleUpdateElement('additionalOptions', {
                    ...this.atomOption,
                    manualRetry,
                    [name]: value,
                    ...clearFields,
                    continueWhenFailed: continueable,
                    retryWhenFailed: retryable,
                    failControl
                })
            },
            initOptionConfig (isInit = false) {
                this.handleUpdateElement('additionalOptions', this.getAtomOptionDefault(this.atomOption), isInit)
            }
        }
    }
</script>

<style lang="scss">
    .header {
        pointer-events: auto;
    }
    .atom-control-option {
        position: relative;
        .atom-checkbox-list-item {
            display: block;
            margin: 12px 0 12px 10px;
        }
        .pause-conf-options .bk-form-checkbox,
        .pause-conf-user {
            margin-left: 10px;
        }

        .form-field.bk-form-item {
            &.manual-skip-options,
            &.retry-count-input {
                position: absolute;
                margin-top: 0;
                left: 180px;
                top:77px;
                &.retry-count-input {
                    top: 117px;
                    width: 260px;
                    .bk-form-content {
                        display: inline-block;
                        width: 150px;
                    };
                }
            }

        }
    }
</style>
