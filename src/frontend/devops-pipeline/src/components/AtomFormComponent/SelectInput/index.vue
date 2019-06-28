<template>
    <div class="select-input" v-bk-clickoutside="handleBlur">
        <input class="bk-form-input" v-bind="restProps" :disabled="disabled || loading" ref="inputArea" :title="value" autocomplete="off" @input="handleInput" @focus="handleFocus" @keypress.enter.prevent="handleEnterOption" @keydown.up.prevent="handleKeyup" @keydown.down.prevent="handleKeydown" @keydown.tab.prevent="handleBlur" />
        <i v-if="loading" class="bk-icon icon-circle-2-1 option-fetching-icon spin-icon" />
        <i v-else-if="!disabled && value" class="bk-icon icon-close-circle-shape option-fetching-icon" @click.stop="clearValue" />
        <div class="dropbox-container" v-show="hasOption && optionListVisible && !loading" ref="dropMenu">
            <ul>
                <li v-for="(item, index) in filteredList" :key="item.id" :class="{ active: item.id === value, selected: selectedPointer === index }" :disabled="item.disabled" @click.stop="selectOption(item)" @mouseover="setSelectPointer(index)" :title="item.name">
                    {{ item.name }}
                </li>
            </ul>
        </div>
    </div>
</template>

<script>
    import mixins from '../mixins'
    import scrollMixins from './scrollMixins'
    import { debounce, isObject } from '@/utils/util'

    export default {
        name: 'select-input',
        mixins: [mixins, scrollMixins],
        props: {
            isLoading: Boolean,
            options: {
                type: Array,
                default: []
            },
            optionsConf: {
                type: Object,
                default: () => ({})
            }
        },
        data () {
            return {
                optionList: Array.isArray(this.options) ? this.options : [],
                optionListVisible: false,
                isFocused: false,
                loading: this.isLoading,
                selectedPointer: 0
            }
        },
        computed: {
            mergedOptionsConf () {
                return Object.assign({}, {
                    url: '',
                    paramId: 'id',
                    paramName: 'name'
                }, this.optionsConf)
            },
            restProps () {
                const { options, optionsConf, atomvalue, container, ...restProps } = this.$props
                return restProps
            },
            hasUrl () {
                return this.mergedOptionsConf && this.mergedOptionsConf.url && typeof this.mergedOptionsConf.url === 'string'
            },
            urlParamKeys () {
                if (this.hasUrl) {
                    const paramKey = this.mergedOptionsConf.url.match(/\{(.*?)\}/g)
                    return paramKey ? paramKey.map(key => key.replace(/\{(.*?)\}/, '$1')) : []
                }
                return []
            },
            queryParams () {
                const { atomValue = {}, $route: { params = {} } } = this
                return {
                    ...params,
                    ...atomValue
                }
            },
            isLackParam () {
                return this.urlParamKeys.some(key => {
                    return this.queryParams.hasOwnProperty(key) && (typeof this.queryParams[key] === 'undefined' || this.queryParams[key] === null || this.queryParams[key] === '')
                })
            },
            filteredList () {
                const { value, optionList } = this
                const strVal = value + ''
                return optionList.filter(option => {
                    if (typeof option.name === 'string' && option.name.toLowerCase().indexOf(strVal.toLowerCase()) > -1) {
                        return option
                    }
                    return false
                })
            },
            hasOption () {
                return Array.isArray(this.filteredList) && this.filteredList.length > 0
            }
        },
        watch: {
            atomValue: {
                handler: function (newAtomValues, oldAtomValues) {
                    if (this.urlParamKeys.some(key => newAtomValues[key] !== oldAtomValues[key])) {
                        this.debounceGetOptionList()
                    }
                }
            },
            options (newOptions) {
                this.optionList = newOptions
                this.isFocused && !this.disabled && this.$nextTick(() => {
                    this.$refs.inputArea.focus()
                })
            },
            isLoading (isLoading) {
                this.loading = isLoading
            }
        },
        created () {
            if (this.hasUrl) {
                this.getOptionList()
                this.debounceGetOptionList = debounce(this.getOptionList)
            }
        },
        methods: {
            urlParse (originUrl, query) {
                /* eslint-disable */
                return new Function('ctx', `return '${originUrl.replace(/\{(.*?)\}/g, '\'\+ ctx.$1 \+\'')}'`)(query)
                /* eslint-enable */
            },
            handleInput (e) {
                const { name, value } = e.target
                this.optionListVisible = true
                this.handleChange(name, value.trim())
            },
            selectOption ({ id, name, disabled = false }) {
                if (disabled) return
                this.handleBlur()
                this.handleChange(this.name, id)
            },

            clearValue () {
                this.handleChange(this.name, '')
                this.$refs.inputArea.focus()
            },

            handleBlur () {
                this.optionListVisible = false
                this.selectedPointer = 0
                this.isFocused = false
                this.$refs.inputArea && this.$refs.inputArea.blur()
                this.$emit('blur', null)
            },
            
            handleFocus (e) {
                this.isFocused = true
                if (!this.optionListVisible) {
                    this.optionListVisible = true
                    this.$emit('focus', e)
                }
            },

            setSelectPointer (index) {
                this.selectedPointer = index
                this.adjustViewPort()
            },
            
            getResponseData (res) {
                try {
                    if (Array.isArray(res.data)) {
                        return res.data
                    } else if (Array.isArray(res.data.record)) {
                        return res.data.record
                    }
                } catch (e) {
                    console.error('获取列表数据失败', e)
                    return []
                }
            },

            async getOptionList () {
                if (this.isLackParam) { // 缺少参数时，选择列表置空
                    this.optionList = []
                    return
                }
                try {
                    this.loading = true
                    const { mergedOptionsConf: { url, paramId, paramName }, queryParams, urlParse, getResponseData } = this
                    const reqUrl = urlParse(url, queryParams)
                    const res = await this.$ajax.get(reqUrl)
                    const options = getResponseData(res)
                    this.optionList = options.map(item => {
                        if (isObject(item)) {
                            return {
                                ...item,
                                id: item[paramId],
                                name: item[paramName]
                            }
                        }

                        return {
                            id: item,
                            name: item
                        }
                    })
                } catch (e) {
                    console.error(e)
                } finally {
                    this.loading = false
                }
            }
        }
    }
</script>

<style lang="scss" scoped>
    @import "../../../scss/conf";
    .select-input {
        position: relative;
        .option-fetching-icon {
            position: absolute;
            right: 20px;
            top: 10px;
            color: $fontLigtherColor;
            &.icon-close-circle-shape {
                cursor: pointer;
            }
        }
        > input {
            padding-right: 50px;
            text-overflow: ellipsis;
        }
        > .dropbox-container {
            position: absolute;
            max-height: 222px;
            width: 100%;
            border: 1px solid $borderLightColor;
            border-radius: 2px;
            box-shadow: 0 0 8px 1px rgba(0, 0, 0, 0.1);
            margin-top: 4px;
            z-index: 1111;
            overflow: auto;
            background: white;
            > ul {
                float:left;
                transition: all 0.3s ease;
                min-width: 100%;
                > li {
                    line-height: 36px;
                    padding-left: 10px;
                    white-space: nowrap;
                    cursor: pointer;
                    font-size: 12px;
                    &.selected,
                    &.active,
                    &:hover {
                        background-color: $primaryLightColor;
                        color: $primaryColor;
                    }

                    &[disabled] {
                        color: $fontLigtherColor;
                    }
                }
            }
        }
    }
</style>
