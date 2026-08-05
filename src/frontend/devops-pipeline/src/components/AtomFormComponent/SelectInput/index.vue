<template>
    <div class="select-input">
        <span
            @click="handleChangeType"
            :class="['change-type', !isVarInputMode ? 'open-var' : 'close-var', { disabled: disabled }]"
            v-bk-tooltips="{ content: !isVarInputMode ? $t('switchToVarMode') : $t('closeVarMode') }"
        >
            <Logo
                size="18"
                name="isSetAsVariable"
            />
        </span>
        <div class="select-input-content">
            <bk-select
                v-if="!isVarInputMode"
                v-bind="dropdownConf"
                :name="name"
                :loading="loading"
                :value="value"
                :disabled="disabled || loading"
                @selected="handleSelect"
                @toggle="toggleVisible"
                @clear="handleClear"
                :popover-options="popoverOptions"
            >
                <template v-if="hasGroup">
                    <bk-option-group
                        v-for="(group, index) in processedOptionList"
                        :key="group.id || index"
                        :name="group.name"
                    >
                        <bk-option
                            v-for="child in group.children"
                            :key="child.id"
                            :id="child.id"
                            :name="child.name"
                            :disabled="child.disabled"
                        />
                    </bk-option-group>
                </template>
                <template v-else>
                    <bk-option
                        v-for="item in processedOptionList"
                        :key="item.id"
                        :id="item.id"
                        :name="item.name"
                        :disabled="item.disabled"
                    />
                </template>
                <template v-if="mergedOptionsConf.hasAddItem">
                    <div
                        slot="extension"
                        class="bk-select-extension"
                    >
                        <a
                            :href="addItemUrl"
                            target="_blank"
                        >
                            <i class="bk-icon icon-plus-circle" />
                            {{ mergedOptionsConf.itemText }}
                        </a>
                    </div>
                </template>
            </bk-select>
            <bk-input
                v-else
                class="var-input"
                :clearable="!disabled"
                v-model="displayValue"
                @blur="handleVarBlur"
                @clear="handleVarClear"
                :disabled="disabled"
            />
        </div>
    </div>
</template>

<script>
    import mixins from '../mixins'
    import scrollMixins from './scrollMixins'
    import selectorMixins from '../selectorMixins'
    import Logo from '@/components/Logo'
    import { debounce, isObject } from '@/utils/util'

    export default {
        name: 'select-input',
        components: {
            Logo
        },
        mixins: [mixins, scrollMixins, selectorMixins],
        props: {
            isLoading: Boolean,
            placeholder: String,
            type: String,
            preFilter: {
                type: Object,
                default: () => ({})
            }
        },
        data () {
            return {
                optionList: Array.isArray(this.options) ? this.options : [],
                loading: this.isLoading,
                isVarInputMode: false,
                displayValue: '',
                // url 接口拉取的 options 是否已就绪
                listFetched: false
            }
        },
        computed: {
            hasGroup () {
                return this.mergedOptionsConf && this.mergedOptionsConf.hasGroup
            },
            popoverOptions () {
                return {
                    popperOptions: {
                        modifiers: {
                            preventOverflow: {
                                boundariesElement: 'window'
                            }
                        }
                    }
                }
            },
            dropdownConf () {
                const { searchable, multiple, clearable, searchPlaceholder } = this.mergedOptionsConf
                const confPlaceholder = this.mergedOptionsConf.placeholder
                return {
                    searchable: searchable ?? true,
                    multiple,
                    clearable,
                    placeholder: this.loading ? this.$t('editPage.loadingData') : (this.placeholder || confPlaceholder),
                    searchPlaceholder: searchPlaceholder ?? (this.placeholder || confPlaceholder)
                }
            },
            processedOptionList () {
                const { optionList, preFilter } = this
                if (this.hasGroup) {
                    return optionList.map(option => {
                        if (!option.children || !option.children.length) return option
                        const filteredChildren = option.children.filter(child => {
                            if (isObject(preFilter) && preFilter.key) {
                                if (Array.isArray(preFilter.value) && Array.isArray(child[preFilter.key])) {
                                    const intersection = preFilter.value.filter(val => child[preFilter.key].indexOf(val) > -1)
                                    return intersection.length
                                } else {
                                    return child[preFilter.key] === preFilter.value
                                }
                            }
                            return true
                        })
                        return {
                            ...option,
                            children: filteredChildren
                        }
                    }).filter(group => group.children && group.children.length)
                } else {
                    if (isObject(preFilter) && preFilter.key) {
                        return optionList.filter(option => {
                            if (Array.isArray(preFilter.value) && Array.isArray(option[preFilter.key])) {
                                const intersection = preFilter.value.filter(val => option[preFilter.key].indexOf(val) > -1)
                                return intersection.length
                            } else {
                                return option[preFilter.key] === preFilter.value
                            }
                        })
                    }
                    return optionList
                }
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
                this.listFetched = true
                this.resolveInputMode()
                this.addNoPermItems()
            },
            isLoading (isLoading) {
                this.loading = isLoading
            },
            value () {
                // 存量数据回显：能匹配 options 则用下拉模式，否则用变量模式
                this.resolveInputMode()
            }
        },
        created () {
            if (this.hasUrl) {
                this.getOptionList()
                this.debounceGetOptionList = debounce(this.getOptionList)
            } else {
                this.listFetched = true
                this.resolveInputMode()
            }
        },
        methods: {
            getOptionIds () {
                if (this.hasGroup) {
                    const ids = []
                    this.optionList.forEach(group => {
                        (group.children || []).forEach(child => {
                            ids.push(child.id)
                        })
                    })
                    return ids
                }
                return this.optionList.map(item => item.id)
            },

            isValueInOptions (val) {
                const ids = this.getOptionIds()
                if (!ids.length) return false
                if (Array.isArray(val)) {
                    return val.length > 0 && val.every(v => ids.some(id => id == v))
                }
                return ids.some(id => id == val)
            },

            resolveInputMode () {
                const val = this.value
                const isEmpty = val === '' || val === null || val === undefined || (Array.isArray(val) && !val.length)
                if (isEmpty) return
                // url 配置的 options 尚未拉取完成时，暂不判定，避免空列表误判为变量模式
                if (this.hasUrl && !this.listFetched) return

                if (this.isValueInOptions(val)) {
                    this.isVarInputMode = false
                } else {
                    this.isVarInputMode = true
                    this.displayValue = Array.isArray(val) ? val.join(',') : val
                }
            },

            handleChangeType () {
                if (this.disabled) return
                this.isVarInputMode = !this.isVarInputMode
                this.displayValue = ''
                this.handleChange(this.name, '')
            },

            handleVarBlur () {
                this.handleChange(this.name, this.displayValue)
            },

            handleVarClear () {
                this.handleChange(this.name, '')
            },

            handleSelect (selected) {
                this.handleChange(this.name, selected)
            },

            handleClear () {
                const val = this.mergedOptionsConf.multiple ? [] : ''
                this.handleChange(this.name, val)
            },

            toggleVisible (open) {
                if (open) {
                    this.hasUrl && this.getOptionList()
                    this.$emit('focus')
                }
            },

            addNoPermItems () {
                if (!this.value || this.isVarInputMode) return
                if (this.hasGroup) {
                    const allChildren = []
                    this.optionList.forEach(group => {
                        if (group.children) {
                            allChildren.push(...group.children)
                        }
                    })
                    const childMap = allChildren.reduce((map, child) => {
                        map[child.id] = child
                        return map
                    }, {})
                    if (!childMap[this.value]) {
                        this.optionList.unshift({
                            id: `__no_perm_${this.value}`,
                            name: '',
                            children: [{
                                id: this.value,
                                name: `******（${this.$t('editPage.noPermToView')}）`
                            }]
                        })
                    }
                } else {
                    const listMap = this.optionList.reduce((map, item) => {
                        map[item.id] = item
                        return map
                    }, {})
                    if (!listMap[this.value]) {
                        this.optionList.splice(0, 0, {
                            id: this.value,
                            name: `******（${this.$t('editPage.noPermToView')}）`
                        })
                    }
                }
            },

            async getOptionList () {
                if (this.isLackParam) {
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
                        options = options.filter(item => item.children && item.children.length)
                        this.optionList = options.map(item => {
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
                            return item
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

                    this.listFetched = true
                    // 接口 options 就绪后再做存量回显模式识别
                    this.resolveInputMode()
                    this.addNoPermItems()
                } catch (e) {
                    console.error(e)
                    // 接口失败也标记已尝试拉取，避免一直阻塞模式判定
                    this.listFetched = true
                    this.resolveInputMode()
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
        display: flex;
        align-items: flex-start;
        position: relative;
        .change-type {
            display: flex;
            align-items: center;
            justify-content: space-around;
            flex-shrink: 0;
            margin-right: 4px;
            width: 32px;
            height: 32px;
            line-height: 34px;
            text-align: center;
            border-radius: 2px;
            cursor: pointer;
            &.disabled {
                cursor: not-allowed;
                opacity: 0.6;
            }
        }
        .open-var {
            background: #EAEBF0;
            svg {
                color: #979BA5;
            }
            &:not(.disabled):hover {
                background-color: #DCDEE5;
            }
            &:not(.disabled):hover svg {
                color: #4D4F56;
            }
        }
        .close-var {
            background: #E1ECFF;
            svg {
                color: #3A84FF;
            }
            &:not(.disabled):hover {
                background-color: #CDDFFE;
            }
            &:not(.disabled):hover svg {
                color: #1768EF;
            }
        }
        .select-input-content {
            flex: 1;
            min-width: 0;
        }
        .bk-select,
        .var-input {
            width: 100%;
        }
        .bk-select-extension a {
            color: #63656e;
        }
    }
</style>
