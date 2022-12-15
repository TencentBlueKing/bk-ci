<template>
    <div class="select-input" v-bk-clickoutside="handleBlur">
        <input
            ref="inputArea"
            class="bk-form-input"
            type="text"
            v-bind="restProps"
            v-model="displayName"
            :disabled="disabled || loading"
            :title="displayName"
            autocomplete="off"
            @focus="handleFocus"
            @keypress.enter.prevent="handleEnterOption"
            @keypress="handleKeyPress"
            @keydown.up.prevent="handleKeyup"
            @keydown.down.prevent="handleKeydown"
            @keydown.tab.prevent="handleBlur" />
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
                            :class="{
                                active: child.active,
                                selected: child.selected
                            }"
                            :disabled="child.disabled"
                            @click.stop="selectOption(child)"
                            @mouseover="setSelectGroupPointer(index, childIndex)"
                            :title="item.name"
                        >{{ child.name }}</div>
                    </li>
                </template>
                <template v-else>
                    <li
                        v-for="(item, index) in filteredList"
                        :key="item.id"
                        :class="{
                            'option-item': true,
                            active: item.active,
                            selected: item.selected
                        }"
                        :title="item.name"
                        :disabled="item.disabled"
                        @click.stop="selectOption(item)"
                        @mouseover="setSelectPointer(index)"
                    >
                        {{ item.name }}
                        <i v-if="isMultiple && item.active" class="devops-icon icon-check-1"></i>
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
            isLoading: Boolean,
            label: String
        },
        data () {
            return {
                optionList: Array.isArray(this.options) ? this.options : [],
                optionListVisible: false,
                isFocused: false,
                loading: this.isLoading,
                selectedPointer: -1,
                selectedGroupPointer: -1,
                displayName: '',
                selectedMap: {}
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
            isMultiple () {
                return this.mergedOptionsConf && this.mergedOptionsConf.multiple
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
                    this.clearValue(false)
                }
            },
            options (newOptions) {
                this.optionList = newOptions
                this.$nextTick(() => {
                    if (this.isFocused && !this.disabled) {
                        this.$refs.inputArea.focus()
                    }

                    if (this.isMultiple) {
                        this.getMultipleDisplayName(this.value)
                    } else {
                        this.displayName = this.getDisplayName(this.value)
                    }
                })
            },
            isLoading (isLoading) {
                this.loading = isLoading
            },
            selectedMap (obj) {
                const keyArr = []
                const params = []
                for (const key in obj) {
                    keyArr.push(obj[key])
                    params.push(key)
                }
                this.displayName = keyArr.join(',')
                this.handleChange(this.name, params)
            }
        },
        created () {
            if (this.hasUrl) {
                this.getOptionList()
                this.debounceGetOptionList = debounce(this.getOptionList)
            } else {
                if (this.isMultiple) {
                    this.getMultipleDisplayName(this.value)
                } else {
                    this.displayName = this.getDisplayName(this.value)
                }
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
                    return list.reduce((result, item, index) => {
                        if (isObject(item) && Array.isArray(item.children) && item.children.length > 0) {
                            const children = item.children.map((child, childIndex) => {
                                return {
                                    ...child,
                                    id: child[paramId],
                                    name: child[paramName],
                                    active: child.id === this.value,
                                    selected: this.selectedPointer === childIndex && this.selectedGroupPointer === index && !this.isMultiple
                                }
                            })
                            result.push({
                                ...item,
                                children: (noSensiveKeyword && !this.isMultiple) ? children.filter(child => child.name.toLowerCase().indexOf(noSensiveKeyword) > -1) : children
                            })
                        }
                        return result
                    }, [])
                }
                const resultList = list.map((item, index) => {
                    if (isObject(item)) {
                        return {
                            ...item,
                            id: item[paramId],
                            name: item[paramName],
                            active: !this.isMultiple ? item.id === this.value : this.value.includes(item.id),
                            selected: this.selectedPointer === index
                        }
                    }

                    return {
                        id: item,
                        name: item
                    }
                })

                return (noSensiveKeyword && !this.isMultiple) ? resultList.filter(item => item.name.toLowerCase().indexOf(noSensiveKeyword) > -1) : resultList
            },

            selectOption ({ id, name, disabled = false }) {
                if (disabled) return
                if (!this.isMultiple) {
                    this.handleChange(this.name, id)
                    this.$nextTick(() => {
                        this.handleBlur()
                    })
                } else {
                    if (id in this.selectedMap) {
                        this.$delete(this.selectedMap, id)
                    } else {
                        this.$set(this.selectedMap, id, name)
                    }
                }
            },

            clearValue (focus = true) {
                this.displayName = ''
                if (this.isMultiple) {
                    this.selectedMap = {}
                } else {
                    this.handleChange(this.name, '')
                }
                if (focus) {
                    this.$refs.inputArea.focus()
                }
            },

            isEnvVar (str) {
                return typeof str === 'string' && str.isBkVar()
            },

            handleBlur () {
                this.optionListVisible = false
                this.resetSelectPointer()
                this.isFocused = false
                this.$refs.inputArea && this.$refs.inputArea.blur()
                this.$emit('blur', null)

                if (this.isMultiple) {
                    if (this.displayName) {
                        this.getMultipleDisplayName(this.displayName, 'name')
                    } else {
                        this.getMultipleDisplayName(this.value)
                    }
                } else {
                    if (this.isEnvVar(this.displayName)) {
                        this.handleChange(this.name, this.displayName.trim())
                    } else if (this.isEnvVar(this.value)) {
                        this.displayName = this.value
                    } else {
                        this.displayName = this.getDisplayName(this.value ?? this.displayName)
                    }
                }
            },

            handleFocus (e) {
                this.isFocused = true
                if (!this.optionListVisible) {
                    this.optionListVisible = true
                    this.$emit('focus', e)
                }
            },

            handleKeyPress (e) {
                if (e.key === ',') {
                    this.resetSelectPointer()
                }
            },

            resetSelectPointer () {
                this.selectedPointer = -1
                this.selectedGroupPointer = -1
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
            getMultipleDisplayName (val, type = 'id') {
                if (typeof val === 'string') {
                    val = val === '' ? [] : val.split(',')
                }
                const valSet = new Set(val)
                let opts = this.optionList
                if (this.hasGroup) {
                    opts = this.optionList.reduce((cur, option) => {
                        cur = [...cur, ...option.children]
                        return cur
                    }, [])
                }
                const typeMap = opts.reduce((cur, opt) => {
                    const key = type === 'name' ? opt.name : opt.id
                    cur[key] = opt
                    return cur
                }, {})
                const resultMap = {}
                const invalidVal = []
                valSet.forEach(v => {
                    if (this.isEnvVar(v)) {
                        resultMap[v] = v
                    } else if (Object.prototype.hasOwnProperty.call(typeMap, v)) {
                        const selectOpt = typeMap[v]
                        resultMap[selectOpt.id] = selectOpt.name
                    } else {
                        invalidVal.push(v)
                    }
                })
                this.selectedMap = resultMap
                if (!this.loading && invalidVal.length > 0) {
                    this.showValValidTips(invalidVal.join(','))
                }
            },
            getDisplayName (val) {
                const defaultVal = Array.isArray(val) ? val.join(',') : val
                if (this.isEnvVar(defaultVal)) {
                    return defaultVal
                }
                if (this.hasGroup) {
                    for (let i = 0; i < this.optionList.length; i++) {
                        const option = this.optionList[i]
                        const matchVal = option.children.find(child => child.id === defaultVal)
                        if (matchVal) {
                            return matchVal.name
                        }
                    }
                } else {
                    const option = this.optionList.find(option => option.id === defaultVal)
                    if (option) {
                        return option.name
                    }
                }
                if (defaultVal && !this.loading) {
                    this.showValValidTips(defaultVal)
                    this.handleChange(this.name, '')
                }
                return ''
            },
            showValValidTips (val) {
                this.$bkMessage({
                    theme: 'error',
                    message: `${this.$t('editPage.invalidValue', [this.label])}: ${val}`
                })
            },
            async getOptionList () {
                if (this.isLackParam) { // 缺少参数时，选择列表置空
                    if (this.value.length) {
                        if (this.isMultiple) {
                            this.getMultipleDisplayName(this.value)
                        } else {
                            this.displayName = this.getDisplayName(this.value)
                        }
                    }
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
                } catch (e) {
                    console.error(e)
                } finally {
                    if (this.isMultiple) {
                        this.getMultipleDisplayName(this.value)
                    } else {
                        this.displayName = this.getDisplayName(this.value)
                    }

                    this.loading = false
                }
            },
            handleEnterOption () {
                let option

                if (this.hasGroup && this.selectedGroupPointer >= 0 && this.selectedPointer >= 0) {
                    option = this.filteredList[this.selectedGroupPointer].children[this.selectedPointer]
                } else if (this.selectedPointer >= 0) {
                    option = this.filteredList[this.selectedPointer]
                }

                if (option) {
                    this.selectOption(option)
                } else {
                    this.handleBlur()
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
                .option-item {
                    display: flex;
                    justify-content: space-between;
                    align-items: center;
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
            }
        }
    }
</style>
