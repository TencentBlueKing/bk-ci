<template>
    <div class="sub-parameter">
        <div
            class="bk-form-content"
        >
            <ul v-bkloading="{ isLoading }">
                <p class="param-label">
                    <span :class="attributeClass">{{ $t('attributeName') }}</span>
                    <span :class="attributeClass">{{ $t('attributeValue') }}</span>
                </p>
                <li
                    class="param-input"
                    v-for="(parameter, index) in parameters"
                    :key="index"
                >
                    <form-field
                        class="from-input"
                        :required="required"
                        :is-error="errors.has(`parameter-key-${index}`)"
                        :error-msg="errors.first(`parameter-key-${index}`)"
                    >
                        <select-input
                            v-validate.initial="required ? 'required' : ''"
                            :class="{ 'is-error': errors.has(`parameter-key-${index}`) }"
                            v-model="parameter.key"
                            :name="`parameter-key-${index}`"
                            :placeholder="$t('attributeName')"
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
                        :required="required"
                        :is-error="errors.has(`parameter-value-${index}`)"
                        :error-msg="errors.first(`parameter-value-${index}`)"
                    >
                        <select-input
                            v-validate.initial="required ? 'required' : ''"
                            :class="{ 'is-error': errors.has(`parameter-value-${index}`) }"
                            :value="parameter.value"
                            :name="`parameter-value-${index}`"
                            :placeholder="$t('attributeValue')"
                            :disabled="disabled"
                            type="text"
                            :options="valueList"
                            :handle-change="(name,value) => handleChangeValue(value,index)"
                            @focus="(e) => getValueIndex(parameter.key)"
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
    import mixins from '../mixins'
    import selectorMixins from '../selectorMixins'
    import { debounce, isObject } from '@/utils/util'

    export default {
        name: 'metadata-normal',
        components: {
            SelectInput
        },
        mixins: [mixins, selectorMixins],
        props: {
            value: Array,
            name: String,
            disabled: Boolean,
            required: Boolean,
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
                optionList: [],
                currentKey: 0
            }
        },
        computed: {
            attributeClass () {
                return ['label-tit', this.required ? 'is-required' : '']
            },
            keyList () {
                const listSource = this.hasUrl ? this.optionList : this.options
                return listSource.map(item => ({
                    id: item.key,
                    name: item.key
                }))
            },
            valueList () {
                const listSource = this.hasUrl ? this.optionList : this.options
                const keyValueMap = listSource.reduce((map, item) => {
                    const values = Array.isArray(item.values) ? item.values : []
                    map[item.key] = map[item.key] ? [...map[item.key], ...values] : values
                    return map
                }, {})

                const valuesArray = keyValueMap[this.currentKey] || []

                return valuesArray.map(item => ({
                    id: item,
                    name: item
                }))
            }
        },

        watch: {
            queryParams (newQueryParams, oldQueryParams) {
                if (this.isParamsChanged(newQueryParams, oldQueryParams)) {
                    this.debounceGetOptionList()
                    this.handleChange(this.name, '')
                }
            },
            value: {
                handler (newVal) {
                    if (newVal) {
                        this.parameters = typeof newVal === 'string' ? JSON.parse(newVal) : newVal
                    } else {
                        this.$nextTick(() => {
                            this.addParam()
                        })
                    }
                },
                immediate: true,
                deep: true
            }
        },
        created () {
            if (this.hasUrl) {
                this.getOptionList()
                this.debounceGetOptionList = debounce(this.getOptionList)
            } else {
                const value = typeof this.value === 'string' ? JSON.parse(this.value) : this.value
                this.parameters = value || []
            }
        },
        methods: {
            getValueIndex (key) {
                this.currentKey = key
            },
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
                this.handleChange(this.name, res)
            },
            cutParam (index) {
                this.parameters.splice(index, 1)
                this.updateParameters()
            },
            async getOptionList () {
                if (this.isLackParam) {
                    if (this.value) this.parameters = this.value
                    this.optionList = []
                    return
                }
                try {
                    this.loading = true
                    const { mergedOptionsConf: { url, paramId, paramName, dataPath }, queryParams, urlParse, getResponseData } = this
                    const reqUrl = urlParse(url, queryParams)
                    const res = await this.$ajax.get(reqUrl)
                    const options = getResponseData(res, dataPath)
                    this.optionList = options.map(item => {
                        if (isObject(item)) {
                            return {
                                ...item,
                                key: item[paramId],
                                values: item[paramName]
                            }
                        }

                        return {
                            key: item,
                            values: []
                        }
                    })
                } catch (e) {
                    console.error(e)
                    this.parameters = this.value
                } finally {
                    this.loading = false
                }
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
          .label-tit {
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
