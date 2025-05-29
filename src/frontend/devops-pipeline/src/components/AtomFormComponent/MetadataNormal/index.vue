<template>
    <div class="sub-parameter">
        <div
            class="bk-form-content"
        >
            <ul v-bkloading="{ isLoading }">
                <p class="param-label">
                    <span class="is-required">{{ $t('attributeName') }}</span>
                    <span class="is-required">{{ $t('attributeValue') }}</span>
                </p>
                <li
                    class="param-input"
                    v-for="(parameter, index) in parameters"
                    :key="index"
                >
                    <form-field
                        class="from-input"
                        :required="true"
                        :is-error="errors.has(`parameter-key-${index}`)"
                        :error-msg="errors.first(`parameter-key-${index}`)"
                    >
                        <select-input
                            v-validate.initial="'required'"
                            :class="{ 'is-error': errors.has(`parameter-key-${index}`) }"
                            v-model="parameter.key"
                            :name="`parameter-key-${index}`"
                            :placeholder="$t('metadataAttributeName')"
                            :disabled="disabled"
                            type="text"
                            :options="keyList"
                            :handle-change="(name,value) => handleChangeKey(value,index)"
                        >
                        </select-input>
                        <span
                            v-if="errors.has(`parameter-key-${index}`)"
                            class="error"
                        >
                            {{ errors.first(`parameter-key-${index}`) }}
                        </span>
                    </form-field>
                    <span class="input-seg">=</span>
                    <form-field
                        class="from-input"
                        :required="true"
                        :is-error="errors.has(`parameter-value-${index}`)"
                        :error-msg="errors.first(`parameter-value-${index}`)"
                    >
                        <select-input
                            v-validate.initial="'required'"
                            :class="{ 'is-error': errors.has(`parameter-value-${index}`) }"
                            :value="parameter.value"
                            :name="`parameter-value-${index}`"
                            :placeholder="$t('metadataAttributeValue')"
                            :disabled="disabled"
                            type="text"
                            :options="valueList"
                            :handle-change="(name,value) => handleChangeValue(value,index)"
                        >
                        </select-input>
                        <span
                            v-if="errors.has(`parameter-value-${index}`)"
                            class="error"
                        >
                            {{ errors.first(`parameter-value-${index}`) }}
                        </span>
                    </form-field>
                    <i
                        v-if="!disabled"
                        class="bk-icon icon-minus-circle minus-btn"
                        @click="cutParam(index)"
                    />
                </li>
            </ul>
            <span
                v-if="!disabled"
                class="add-params-btn"
                @click="addParam"
            >
                <i class="devops-icon icon-plus-circle"></i>
                {{ $t('editPage.append') }}
            </span>
        </div>
    </div>
</template>

<script>
    import SelectInput from '@/components/AtomFormComponent/SelectInput'
    import { isObject } from '@/utils/util'

    export default {
        name: 'metadata-normal',
        components: {
            SelectInput
        },
        props: {
            value: String,
            name: String,
            disabled: Boolean,
            handleChange: {
                type: Function,
                default: () => () => {}
            },
            options: Array
        },
        data () {
            return {
                isLoading: false,
                parameters: [],
                valueList: []
            }
        },
        computed: {
            keyList () {
                return this.options
            }
        },

        watch: {
            value: {
                handler (value) {
                    if (value) {
                        this.parameters = JSON.parse(value)
                    } else {
                        this.addParam()
                    }
                },
                immediate: true
            }
        },
        methods: {
            addParam () {
                this.parameters.push({
                    key: '',
                    value: ''
                })
            },
            handleChangeKey (value, index) {
                this.parameters[index].key = value
                this.updateParameters()
            },

            handleChangeValue (value, index) {
                this.parameters[index].value = value
                this.updateParameters()
            },
            updateParameters () {
                const res = this.parameters.map((parameter) => {
                    const key = parameter.key
                    const value = isObject(parameter.value) ? JSON.stringify(parameter.value) : parameter.value
                    return { key, value }
                })
                this.handleChange(this.name, String(JSON.stringify(res)))
            },
            cutParam (index) {
                this.parameters.splice(index, 1)
                this.updateParameters()
            }
        }
    }
</script>

<style lang="scss" scoped>
    .sub-parameter {
        display: grid;
        .bk-form-content {
            padding: 12px 16px;
            background: #F5F7FA;
            border-radius: 2px;
            margin-bottom: 10px;
        }
        .from-input {
            width: 256px;
        }
        .add-params-btn {
            color: #3A84FF;
            cursor: pointer;
        }
        .param-label {
            display: flex;
            align-items: center;
            margin-bottom: 6px;
            .is-required {
                flex: 1;
            }
            .is-required:after {
                height: 8px;
                line-height: 1;
                content: "*";
                color: #ea3636;
                font-size: 12px;
                position: relative;
                left: 4px;
                top: 0;
                display: inline-block;
            }
        }
        .param-input {
            margin-bottom: 10px;
            display: flex;
            .input-seg {
                flex-basis: 20px;
                text-align: center;
                height: 42px;
            }
            .minus-btn {
                font-size: 14px;
                margin-left: 5px;
                padding-top: 10px;
                cursor: pointer;
            }
        }
        .error {
            font-size: 12px;
            color: #ff5656;
        }
    }
</style>
<style lang="scss">
    .sub-parameter .is-error {
        input {
            border-color: #ff5656;
        }
    }
</style>
