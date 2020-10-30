<template>
    <div class="select-input" v-bk-clickoutside="handleBlur">
        <input class="bk-form-input" v-bind="restProps" v-model="displayName" :disabled="disabled || loading" ref="inputArea" :title="value" autocomplete="off" @focus="handleFocus" @keypress.enter.prevent="handleEnterOption" @keydown.up.prevent="handleKeyup" @keydown.down.prevent="handleKeydown" @keydown.tab.prevent="handleBlur" />
        <i v-if="loading" class="bk-icon icon-circle-2-1 option-fetching-icon spin-icon" />
        <i v-else-if="!disabled && value" class="bk-icon icon-close-circle-shape option-fetching-icon" @click.stop="clearValue" />
        <div class="dropbox-container" v-show="hasOption && optionListVisible && !loading" ref="dropMenu">
            <ul>
                <template v-if="hasGroup">
                    <li v-for="(item, index) in filteredList"
                        :key="item.id"
                        :disabled="item.disabled">
                        <div class="option-group-name">{{ item.name }}</div>
                        <div class="option-group-item"
                            v-for="(child, childIndex) in item.children"
                            :key="child.id"
                            :class="{ active: child.id === value, selected: selectedPointer === childIndex && selectedGroupPointer === index }"
                            :disabled="child.disabled"
                            @click.stop="selectOption(child)"
                            @mouseover="setSelectGroupPointer(index, childIndex)"
                            :title="item.name"
                        >{{ child.name }}</div>
                    </li>
                </template>
                <template v-else>
                    <li class="option-item" v-for="(item, index) in filteredList" :key="item.id" :class="{ active: item.id === value, selected: selectedPointer === index }" :disabled="item.disabled" @click.stop="selectOption(item)" @mouseover="setSelectPointer(index)" :title="item.name">
                        {{ item.name }}
                    </li>
                </template>
            </ul>
        </div>
    </div>
</template>

<script>
    import mixins from '../mixins'
    import scrollMixins from '../SelectInput/scrollMixins'
    import selectorMixins from '../selectorMixins'
    import { debounce, isObject } from '@/utils/util'

    export default {
        name: 'devops-select',
        mixins: [mixins, scrollMixins, selectorMixins],
        props: {
            isLoading: Boolean
        },
        data () {
            return {
                optionList: Array.isArray(this.options) ? this.options : [],
                optionListVisible: false,
                isFocused: false,
                loading: this.isLoading,
                selectedPointer: 0,
                selectedGroupPointer: 0,
                displayName: ''
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
                const { displayName, optionList } = this
                const strVal = String(displayName).toLowerCase()
                return this.formatList(optionList, strVal)
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
                    this.displayName = ''
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
            } else {
                this.displayName = this.getDisplayName(this.value)
            }
        },
        beforeDestroy () {
            this.handleBlur()
        },
        methods: {
            handleInput (e) {
                // const { name, value } = e.target
                this.optionListVisible = true
                // if (!this.hasGroup ) this.handleChange(name, value.trim())
            },
            formatList (list = [], keyword = '', paramId = 'id', paramName = 'name') {
                let noSensiveKeyword = ''
                if (keyword && typeof keyword === 'string') {
                    noSensiveKeyword = keyword.toLowerCase()
                }
                if (this.hasGroup) {
                    return list.reduce((result, item) => {
                        if (isObject(item) && Array.isArray(item.children) && item.children.length > 0) {
                            const children = item.children.map(child => {
                                return {
                                    ...child,
                                    id: child[paramId],
                                    name: child[paramName]
                                }
                            })
                            result.push({
                                ...item,
                                children: noSensiveKeyword ? children.filter(child => child.name.toLowerCase().indexOf(noSensiveKeyword) > -1) : children
                            })
                        }
                        return result
                    }, [])
                }
                const resultList = list.map(item => {
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

                return noSensiveKeyword ? resultList.filter(item => item.name.toLowerCase().indexOf(noSensiveKeyword) > -1) : resultList
            },
            selectOption ({ id, name, disabled = false }) {
                if (disabled) return
                this.handleChange(this.name, id)
                this.$nextTick(() => {
                    this.handleBlur()
                })
            },

            clearValue () {
                this.handleChange(this.name, '')
                this.displayName = ''
                this.$refs.inputArea.focus()
            },

            isEnvVar (str) {
                return /^\$(\{\w+\}|\w+)$/.test(str)
            },

            handleBlur () {
                this.optionListVisible = false
                this.selectedPointer = 0
                this.selectedGroupPointer = 0
                this.isFocused = false
                this.$refs.inputArea && this.$refs.inputArea.blur()
                this.$emit('blur', null)
                if (this.isEnvVar(this.displayName)) {
                    this.handleChange(this.name, this.displayName.trim())
                } else if (this.isEnvVar(this.value)) {
                    this.displayName = this.value
                } else {
                    this.displayName = this.getDisplayName(this.value)
                }
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

            setSelectGroupPointer (index, childIndex) {
                this.selectedGroupPointer = index
                this.selectedPointer = childIndex
                this.adjustViewPort()
            },
            getDisplayName (val) {
                if (this.isEnvVar(val)) {
                    return val
                }
                if (this.hasGroup) {
                    for (let i = 0; i < this.optionList.length; i++) {
                        const option = this.optionList[i]
                        const matchVal = option.children.find(child => child.id === val)
                        console.log(matchVal)
                        if (matchVal) {
                            return matchVal.name
                        }
                    }
                } else {
                    const option = this.optionList.find(option => option.id === val)
                    if (option) {
                        return option.name
                    }
                }
                return ''
            },
            async getOptionList () {
                if (this.isLackParam) { // 缺少参数时，选择列表置空
                    if (this.value !== '') this.displayName = this.getDisplayName(this.value)
                    this.optionList = []
                    return
                }
                try {
                    this.loading = true
                    const { mergedOptionsConf: { url, paramId, paramName, dataPath }, queryParams, urlParse, getResponseData } = this
                    const reqUrl = urlParse(url, queryParams)
                    const res = await this.$ajax.get(reqUrl)
                    const options = getResponseData(res, dataPath)

                    this.optionList = this.formatList(options, '', paramId, paramName)
                    if (this.value) {
                        this.displayName = this.getDisplayName(this.value)
                    }
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
                //         color: $fontLigtherColor;
                //     }
                // }
                li:first-child {
                    .option-group-name {
                        border-top: 0
                    }
                }
                .option-group-name {
                    padding: 0 12px;
                    line-height: 36px;
                    font-size: 12px;
                    border-bottom: 1px solid #dcdee5;
                    border-top: 1px solid #dcdee5;
                    color: #999;

                }
                .option-item,
                .option-group-item {
                    line-height: 36px;
                    padding: 0 18px;
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
