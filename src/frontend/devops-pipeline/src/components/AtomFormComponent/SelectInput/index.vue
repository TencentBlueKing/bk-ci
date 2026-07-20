<template>
    <div class="select-input">
        <span
            @click="handleChangeType"
            :class="['change-type', !isVarInputMode ? 'open-var' : 'close-var', { disabled: disabled }]"
            v-bk-tooltips="{ content: !isVarInputMode ? $t('switchToVarMode') : $t('closeVarMode') }"
        >
            <Logo
                size="14"
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
                :class="['var-input', isError ? 'error-input' : '']"
                :clearable="!disabled"
                v-model="displayValue"
                @blur="handleVarBlur"
                @clear="handleVarClear"
                :disabled="disabled"
                :placeholder="pipelineDialect === 'CLASSIC' ? $t('placeholderVar') : $t('placeholderConstraintVar')"
            />
            <span
                v-if="isError"
                class="error-text"
            >
                {{ $t('validVariableFormat') }}
            </span>
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
                isError: false
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
                this.addNoPermItems()
            },
            isLoading (isLoading) {
                this.loading = isLoading
            },
            value (newVal) {
                // 存量数据自动识别：如果值为变量格式，自动切换到输入变量模式
                if (newVal && this.getValidaVar(newVal)) {
                    this.isVarInputMode = true
                    this.displayValue = newVal
                }
            }
        },
        created () {
            // 存量数据自动识别
            if (this.value && this.getValidaVar(this.value)) {
                this.isVarInputMode = true
                this.displayValue = this.value
            }
            if (this.hasUrl) {
                this.getOptionList()
                this.debounceGetOptionList = debounce(this.getOptionList)
            }
        },
        methods: {
            handleChangeType () {
                if (this.disabled) return
                this.isVarInputMode = !this.isVarInputMode
                this.isError = false
                this.displayValue = ''
                this.handleChange(this.name, '')
            },

            handleVarBlur () {
                const newValue = this.displayValue
                if (newValue !== '' && !this.getValidaVar(newValue)) {
                    this.isError = true
                } else {
                    this.isError = false
                    this.handleChange(this.name, newValue)
                }
            },

            handleVarClear () {
                this.isError = false
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

                    // 添加无权限查看项
                    this.addNoPermItems()
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
        display: flex;
        align-items: flex-start;
        position: relative;
        .change-type {
            display: inline-block;
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
        .error-text {
            display: block;
            margin-top: 4px;
            color: #ff5656;
            line-height: 16px;
            font-size: 12px;
        }
        .bk-select-extension a {
            color: #63656e;
        }
    }
</style>
<style lang="scss">
    .select-input {
        .error-text {
            display: block;
            margin-top: 4px;
            color: #ff5656;
            line-height: 16px;
            font-size: 12px;
        }
        .error-input {
            .bk-form-input {
                border-color: #ff5656 !important;
            }
        }
    }
</style>
