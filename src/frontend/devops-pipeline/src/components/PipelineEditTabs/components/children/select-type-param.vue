<template>
    <div class="selector-type-param">
        <div class="option-type">
            <div
                class="type-select"
                :class="{ 'is-active': payloadValue.type === 'options' }"
                @click="handleParamTypeChange('type', 'options')"
            >
                {{ $t('newui.pipelineParam.optionsList') }}
            </div>
            <div
                class="type-select"
                :class="{ 'is-active': payloadValue.type === 'remote' }"
                @click="handleParamTypeChange('type', 'remote')"
            >
                {{ $t('newui.pipelineParam.optionsFromApi') }}
            </div>
        </div>
        <div class="option-items">
            <section v-if="payloadValue.type !== 'remote'">
                <key-options
                    :options="param.options"
                    :handle-change-options="updateOptions"
                />
            </section>
            <section v-else>
                <bk-form
                    form-type="vertical"
                    class="new-ui-form"
                    :label-width="300"
                >
                    <template v-for="obj in remoteTypeOptions">
                        <form-field
                            :hide-colon="true"
                            :key="obj.key"
                            :desc="obj.tips"
                            :required="obj.required"
                            :label="obj.label"
                            :is-error="errors.has(key)"
                            :error-msg="errors.first(key)"
                        >
                            <component
                                :is="'vuex-input'"
                                :disabled="disabled"
                                :name="obj.key"
                                v-validate.initial="Object.assign({}, { required: !!obj.required })"
                                :handle-change="handleRemoteParamChange"
                                :value="payloadValue[obj.key]"
                                v-bind="obj"
                                :placeholder="obj.placeholder"
                            ></component>
                        </form-field>
                    </template>
                </bk-form>
            </section>
        </div>
    </div>
</template>

<script>
    import KeyOptions from './key-options'
    import FormField from '@/components/AtomPropertyPanel/FormField'
    import VuexInput from '@/components/atomFormField/VuexInput'
    export default {
        components: {
            KeyOptions,
            FormField,
            VuexInput
        },
        props: {
            disabled: {
                type: Boolean,
                default: false
            },
            param: {
                type: Object,
                required: true
            },
            handleUpdateOptions: {
                type: Function,
                default: () => {}
            },
            handleUpdatePayload: {
                type: Function,
                default: () => {}
            },
            resetDefaultVal: {
                type: Function,
                default: () => {}
            }
        },
        data () {
            return {
                payloadValue: {},
                remoteTypeOptions: [
                    {
                        key: 'url',
                        label: this.$t('newui.pipelineParam.apiUrl'),
                        placeholder: this.$t('editPage.atomForm.inputTips'),
                        tips: this.$t('editPage.atomForm.URLTips', [`{${this.$t('editPage.atomForm.var')}}`])
                    },
                    {
                        key: 'dataPath',
                        label: this.$t('newui.pipelineParam.dataPath'),
                        placeholder: this.$t('newui.pipelineParam.dataPathTips')
                    },
                    {
                        key: 'paramId',
                        label: this.$t('newui.pipelineParam.paramId'),
                        placeholder: this.$t('newui.pipelineParam.paramIdTips')
                    },
                    {
                        key: 'paramName',
                        label: this.$t('newui.pipelineParam.paramName'),
                        placeholder: this.$t('newui.pipelineParam.paramNameTips')
                    }
                ]
            }
        },
        created () {
            this.payloadValue = Object.assign({}, { type: 'options' }, this.param?.payload || {})
        },
        methods: {
            handleParamTypeChange (name, value) {
                // 类型重置， 清空defaultvalue
                if (value !== this.payloadValue.type) {
                    this.resetDefaultVal()
                }
                this.handleRemoteParamChange(name, value)
            },
            handleRemoteParamChange (name, value) {
                Object.assign(this.payloadValue, { [name]: value })
                this.updatePayload('payload', this.payloadValue)
            },
            updateOptions (name, value) {
                this.handleUpdateOptions(name, value)
            },
            updatePayload (name, value) {
                this.handleUpdatePayload(name, value)
            }
        }
    }
</script>

<style lang="scss" scoped>
    .selector-type-param {
        background-color: #FAFBFD;
        margin-bottom: 24px;
        .option-type {
            height: 40px;
            display: flex;
            align-items: center;
            cursor: pointer;
            border-bottom: 1px solid #DCDEE5;
            .type-select {
                flex: 1;
                font-size: 14px;
                height: 42px;
                line-height: 42px;
                color: #63656E;
                text-align: center;
            }
            .is-active {
                color: #3A84FF;
                border-bottom: 2px solid #3A84FF;
            }
        }
        .option-items {
            padding: 16px 24px;
        }
    }
</style>
