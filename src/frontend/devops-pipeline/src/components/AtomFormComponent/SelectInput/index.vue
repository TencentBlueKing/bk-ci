<template>
    <div
        class="select-input"
        v-bk-clickoutside="handleBlur"
    >
        <input
            class="bk-form-input"
            v-bind="restProps"
            v-model="displayName"
            :disabled="disabled || loading"
            ref="inputArea"
            :title="value"
            :placeholder="placeholder"
            autocomplete="off"
            @input="handleInput"
            @focus="handleFocus"
            @keypress.enter.prevent="handleEnterOption"
            @keydown.up.prevent="handleKeyup"
            @keydown.down.prevent="handleKeydown"
            @keydown.tab.prevent="handleBlur"
        />
        <i
            v-if="loading"
            class="devops-icon icon-circle-2-1 option-fetching-icon spin-icon"
        />
        <i
            v-else-if="!disabled && value"
            class="devops-icon icon-close-circle-shape option-fetching-icon"
            @click.stop="clearValue"
        />
        <div
            class="dropbox-container"
            v-show="hasOption && optionListVisible && !loading"
            ref="dropMenu"
        >
            <ul>
                <template v-if="hasGroup">
                    <li
                        v-for="(item, index) in filteredList"
                        :key="item.id + index"
                        :disabled="item.disabled"
                    >
                        <div class="option-group-name">{{ item.name }}</div>
                        <div
                            class="option-group-item"
                            v-for="(child, childIndex) in item.children"
                            :key="child.id"
                            :class="{ active: child.id === value, selected: selectedPointer === childIndex && selectedGroupPointer === index }"
                            :disabled="child.disabled"
                            @click.stop="selectOption(child)"
                            @mouseover="setSelectGroupPointer(index, childIndex)"
                            :title="item.name"
                        >
                            {{ child.name }}
                        </div>
                    </li>
                </template>
                <template v-else>
                    <li
                        v-for="(item, index) in filteredList"
                        class="option-item"
                        :key="item.id + index"
                        :class="{ active: item.id === value, selected: selectedPointer === index }"
                        :disabled="item.disabled"
                        @click.stop="selectOption(item)"
                        @mouseover="setSelectPointer(index)"
                        :title="item.name"
                    >
                        {{ item.name }}
                    </li>
                </template>
                <template v-if="mergedOptionsConf.hasAddItem">
                    <div class="bk-select-extension">
                        <a
                            :href="addItemUrl"
                            target="_blank"
                        >
                            <i class="bk-icon icon-plus-circle" />
                            {{ mergedOptionsConf.itemText }}
                        </a>
                    </div>
                </template>
            </ul>
        </div>
    </div>
</template>

<script>
    import mixins from '../mixins'
    import scrollMixins from './scrollMixins'
    import selectorMixins from '../selectorMixins'
    import { debounce, isObject } from '@/utils/util'

    export default {
        name: 'select-input',
        mixins: [mixins, scrollMixins, selectorMixins],
        props: {
            isLoading: Boolean,
            placeholder: String,
            preFilter: {
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
                selectedPointer: 0,
                selectedGroupPointer: 0,
                displayName: '',
                timerId: null
            }
        },
        computed: {
            restProps () {
                const { options, optionsConf, atomvalue, container, ...restProps } = this.$props
                return restProps
            },
            hasGroup () {
                return this.mergedOptionsConf && this.mergedOptionsConf.hasGroup
            },
            filteredList () {
                const { displayName, value, optionList } = this
                const strVal = this.hasGroup ? displayName + '' : value + ''

                if (this.hasGroup) {
                    let target = []
                    optionList.map(option => {
                        if (option.children.length && option.children.some(child => child.name.toLowerCase().indexOf(strVal.toLowerCase()) > -1)) {
                            const temp = {
                                ...option,
                                children: option.children.filter(child => {
                                    if (isObject(this.preFilter) && this.preFilter.key) {
                                        if (Array.isArray(this.preFilter.value) && Array.isArray(child[this.preFilter.key])) {
                                            const intersection = this.preFilter.value.filter(val => child[this.preFilter.key].indexOf(val) > -1)
                                            return child.name.toLowerCase().indexOf(strVal.toLowerCase()) > -1 && intersection.length
                                        } else {
                                            return child.name.toLowerCase().indexOf(strVal.toLowerCase()) > -1 && child[this.preFilter.key] === this.preFilter.value
                                        }
                                    } else {
                                        return child.name.toLowerCase().indexOf(strVal.toLowerCase()) > -1
                                    }
                                })
                            }
                            target.push(temp)
                        }
                        return false
                    })
                    target = target.filter(tag => tag.children.length)
                    return target
                } else {
                    return optionList.filter(option => {
                        if (typeof option.name === 'string' && option.name.toLowerCase().indexOf(strVal.toLowerCase()) > -1) {
                            return option
                        }
                        return false
                    })
                }
            },
            hasOption () {
                return Array.isArray(this.filteredList) && this.filteredList.length > 0
            }
        },
        watch: {
            queryParams (newQueryParams, oldQueryParams) {
                if (this.isParamsChanged(newQueryParams, oldQueryParams)) {
                    this.debounceGetOptionList()
                    this.handleChange(this.name, '')
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
            },
            value (newVal) {
                if (newVal) {
                    if (this.hasGroup) {
                        this.optionList.forEach(option => {
                            const matchVal = option.children.find(child => child.id === newVal)
                            if (matchVal) this.displayName = matchVal.name
                        })
                    } else {
                        this.displayName = newVal
                    }
                } else this.displayName = ''
            }
        },
        created () {
            if (this.hasUrl) {
                this.getOptionList()
                this.debounceGetOptionList = debounce(this.getOptionList)
            } else {
                this.displayName = this.value
            }
        },
        beforeDestroy () {
            if (this.timerId) {
                clearTimeout(this.timerId)
                this.timerId = null
            }
        },
        methods: {
            handleInput (e) {
                const { name, value } = e.target
                this.optionListVisible = true
                if (!this.hasGroup) this.handleChange(name, value.trim())
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
                this.selectedGroupPointer = 0
                this.isFocused = false
                this.$refs.inputArea && this.$refs.inputArea.blur()
                this.$emit('blur', null)
                if (this.hasGroup && !this.filteredList.length) {
                    this.handleChange(this.name, '')
                    this.displayName = ''
                } else if (!this.hasGroup) {
                    this.displayName = this.value
                }
            },

            handleFocus (e) {
                this.isFocused = true
                if (!this.optionListVisible) {
                    this.timerId = setTimeout(() => {
                        this.optionListVisible = true
                        this.$emit('focus', e)
                    }, 300)
                }
            },

            setSelectPointer (index) {
                this.$refs.inputArea.focus()
                this.selectedPointer = index
                this.adjustViewPort()
            },

            setSelectGroupPointer (index, childIndex) {
                this.$refs.inputArea.focus()
                this.selectedGroupPointer = index
                this.selectedPointer = childIndex
                this.adjustViewPort()
            },

            async getOptionList () {
                if (this.isLackParam) { // 缺少参数时，选择列表置空
                    if (this.value !== '') this.displayName = this.value
                    this.optionList = []
                    return
                }
                try {
                    this.loading = true
                    const { mergedOptionsConf: { url, paramId, paramName, dataPath }, queryParams, urlParse, getResponseData } = this
                    const reqUrl = urlParse(url, queryParams)
                    const res = await this.$ajax.get(reqUrl)
                    let options = getResponseData(res, dataPath)

                    if (this.hasGroup) {
                        options = options.filter(item => item.children.length)
                        this.optionList = options.forEach(item => {
                            if (isObject(item)) {
                                return {
                                    ...item,
                                    children: item.children.map(child => ({
                                        ...child,
                                        id: child[paramId],
                                        name: child[paramName]
                                    }))
                                }
                            }
                        })
                    } else {
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
                    }

                    if (this.value) {
                        if (this.hasGroup) {
                            this.optionList.forEach(option => {
                                const matchVal = option.children.find(child => child.id === this.value)
                                if (matchVal) this.displayName = matchVal.name
                            })
                        } else {
                            this.displayName = this.value
                        }
                    }
                } catch (e) {
                    console.error(e)
                    this.displayName = this.value
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
            color: $fontLighterColor;
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
                // > li {
                //     line-height: 36px;
                //     padding-left: 10px;
                //     white-space: nowrap;
                //     cursor: pointer;
                //     font-size: 12px;
                //     &.selected,
                //     &.active,
                //     &:hover {
                //         background-color: $primaryLightColor;
                //         color: $primaryColor;
                //     }

                //     &[disabled] {
                //         color: $fontLighterColor;
                //     }
                // }
                .option-group-name {
                    padding: 0 12px;
                    line-height: 36px;
                    font-size: 14px;
                    font-weight: bold;
                    border-bottom: 1px solid #dcdee5;
                    color: #979ba5;
                }
                .option-item,
                .option-group-item {
                    line-height: 36px;
                    padding: 0 18px;
                    white-space: nowrap;
                    cursor: pointer;
                    font-size: 12px;
                    border: 1px solid transparent;
                    &.selected,
                    &.active,
                    &:hover {
                        background-color: $primaryLightColor;
                        color: $primaryColor;
                    }
                    &.selected {
                       border-color : $primaryColor;
                    }

                    &[disabled] {
                        color: $fontLighterColor;
                    }
                }
                .bk-select-extension a {
                    color: #63656e;
                }
            }
        }
    }
</style>
