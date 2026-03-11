<template>
    <div
        class="bk-select"
        :class="{
            'is-focus': focus,
            'is-disabled': disabled,
            'is-readonly': readonly,
            'is-loading': loading,
            'is-unselected': isUnselected,
            'is-default-trigger': !$scopedSlots.trigger
        }"
        :data-placeholder="localPlaceholder"
    >
        <template v-if="!$scopedSlots.trigger">
            <template v-if="loading">
                <icon
                    class="bk-select-loading spin-icon"
                    name="circle-2-1"
                />
            </template>
            <template v-else>
                <i
                    v-if="clearable && !isUnselected && !disabled && !readonly"
                    class="bk-select-clear bk-icon icon-close-circle-shape"
                    @click.prevent.stop="reset"
                />
                <i class="bk-select-angle bk-icon icon-angle-down" />
            </template>
        </template>
        <bk-popover
            ref="selectDropdown"
            class="bk-select-dropdown"
            trigger="click"
            placement="bottom-start"
            theme="light bk-select-dropdown"
            animation="slide-toggle"
            :offset="-1"
            :distance="16"
            :on-show="handleDropdownShow"
            :on-hide="handleDropdownHide"
            :tippy-options="popoverOptions"
        >
            <slot
                name="trigger"
                v-bind="$props"
            >
                <div
                    class="bk-select-name"
                    :title="selectedName"
                >
                    {{ selectedName }}
                </div>
            </slot>
            <div
                slot="content"
                v-bkloading="{ isLoading: remoteSearchLoading }"
                class="bk-select-dropdown-content"
                :style="popoverStyle"
            >
                <input
                    v-if="searchable"
                    ref="searchInput"
                    v-model="searchValue"
                    class="bk-select-search-input"
                    type="text"
                    :placeholder="localPlaceholder"
                    @keydown="handleKeydown($event)"
                >
                <ul
                    ref="optionList"
                    class="bk-options"
                    :class="{
                        'bk-options-single': !multiple
                    }"
                    :style="{
                        maxHeight: scrollHeight + 'px'
                    }"
                >
                    <li
                        v-for="(option, index) in filterOptions"
                        :key="option.id"
                        class="bk-option"
                        :class="{
                            'is-selected': isSelect(option),
                            'is-disabled': option.disabled,
                            'is-highlight': index === highlightIndex
                        }"
                        @click.stop="handleOptionClick(option)"
                    >
                        <slot
                            name="option"
                            v-bind="slotOption(option)"
                        >
                            <devops-option
                                v-bind="option"
                                :is-selected="isSelect(option)"
                                :multiple="multiple"
                            />
                        </slot>
                    </li>
                </ul>
                <div
                    v-if="!options.length"
                    class="bk-select-empty"
                >
                    {{ emptyText }}
                </div>
                <div
                    v-else-if="searchable && unmatchedCount === options.length"
                    class="bk-select-empty"
                >
                    {{ emptyText }}
                </div>
                <div
                    v-if="$slots.extension"
                    class="bk-select-extension"
                >
                    <slot name="extension" />
                </div>
            </div>
        </bk-popover>
    </div>
</template>

<script lang="ts">
    import Vue from 'vue'
    import { Prop, Component, Watch } from 'vue-property-decorator'
    import DevopsOption from './Option.vue'

    @Component({
        name: 'big-select',
        components: {
            DevopsOption
        }
    })
    export default class DevopsSelect extends Vue {
        @Prop({ default: '' })
        value: any
        
        @Prop()
        multiple: boolean
        
        @Prop()
        showSelectAll: boolean
        
        @Prop({ default: 216 })
        scrollHeight: number

        @Prop()
        popoverMinWidth: number

        @Prop()
        popoverWidth: number

        @Prop({ default: {} })
        popoverOptions: object

        @Prop({ default: '' })
        placeholder: string

        @Prop({ default: true })
        clearable: boolean

        @Prop()
        disabled: boolean

        @Prop()
        readonly: boolean

        @Prop()
        loading: boolean

        @Prop({ default: true })
        searchable: boolean

        @Prop({ default: true })
        searchIgnoreCase: boolean
        
        @Prop()
        remoteMethod: Function
        
        @Prop({ default: '' })
        emptyText: string
        
        @Prop({ default: [] })
        options: []

        @Prop({ default: 'id' })
        settingKey: string

        @Prop({ default: 'name' })
        displayKey: string

        focus: boolean = false
        selected: any = ''
        defaultWidth: number = 0
        searchValue: string = ''
        highlightIndex: number | null = null
        isRemoteSearch: boolean = typeof this.remoteMethod === 'function'
        remoteSearchLoading: boolean = false

        get finalOptions () {
            return this.options.map((option: object) => ({
                ...option,
                id: option[this.settingKey],
                name: option[this.displayKey]
            }))
        }

        get filterOptions () {
            return this.searchValue
? this.finalOptions.filter((option: any) => {
                const name = this.searchIgnoreCase ? option.name.toLowerCase() : option.name
                const keyword = this.searchIgnoreCase ? this.searchValue.toLowerCase() : this.searchValue
                return name.indexOf(keyword) > -1
            })
: this.finalOptions
        }

        get optionMap () {
            return this.finalOptions.reduce((optionMap, option: any) => {
                optionMap[option.id] = option
                return optionMap
            }, {})
        }

        get enabledOptions () {
            return this.finalOptions.filter((option: any) => !option.disabled)
        }

        get highlightOption () {
            const filterOptions = this.filterOptions
            if (this.searchable && filterOptions.length) {
                return filterOptions[this.highlightIndex]
            }
            return null
        }

        get selectedOptions () {
            if (this.multiple) {
                return this.selected.map(id => this.optionMap[id])
            }
            return this.optionMap[this.selected] || {}
        }

        get selectedName () {
            if (this.multiple) {
                return this.selectedOptions.map(option => option.name).join(',')
            }
            return this.selectedOptions.name
        }

        get isUnselected () {
            if (this.multiple) {
                return !this.selected.length
            }
            return this.selected === ''
        }

        get unmatchedCount () {
            return this.finalOptions.filter((option: any) => option.unmatched).length
        }

        get localPlaceholder () {
            return this.placeholder || ''
        }

        get dropdownActive () {
            return !(this.disabled || this.loading || this.readonly)
        }

        get popoverStyle () {
            return {
                width: (this.popoverWidth ? this.popoverWidth : this.defaultWidth) + 'px',
                minWidth: (this.popoverMinWidth ? this.popoverMinWidth : this.defaultWidth) + 'px'
            }
        }

        get isAllSelected () {
            return this.selected.length > 0 && this.enabledOptions.length === this.selected.length
        }

        @Watch('value')
        handleWatchValue (value) {
            if (!this.isSame(value, this.selected)) {
                this.selected = value
            }
        }

        @Watch('selected')
        handleWatchSelected (value, oldValue) {
            this.$emit('input', value)
            this.$emit('change', value, oldValue)
        }

        @Watch('focus')
        handleWatchFocus (focus) {
            if (!focus) {
                this.resetHighlightIndex()
                this.resetSearchValue()
            }
            this.$emit('toggle', focus)
        }

        @Watch('dropdownActive')
        handleWatchDropDownActive () {
            this.setDropdownState()
        }

        @Watch('searchValue')
        handleWatchSearchValue (val) {
            this.resetHighlightIndex()
            if (!this.isRemoteSearch) {
                return
            }

            if (this.isRemoteSearch) {
                this.remoteSearchLoading = true
                new Promise((resolve, reject) => {
                    const func = this.remoteMethod(val)
                    if (func instanceof Promise) {
                        func.then(ret => {
                            resolve(ret)
                        }).catch(() => {
                            reject(func)
                        })
                    } else {
                        resolve(func)
                    }
                }).then(() => {
                    // console.log('then')
                }).catch(() => {
                    // console.error('catch')
                }).finally(() => {
                    this.remoteSearchLoading = false
                })
            }
        }

        @Watch('searchable')
        handleWatchSearchable () {
            this.resetHighlightIndex()
        }

        @Watch('highlightIndex')
        handleWatchHighlightIndex (newIndex, oldIndex) {
            this.calcListScrollPosition(newIndex, oldIndex)
        }

        created () {
            let selected = this.value
            if (this.multiple && !Array.isArray(selected)) {
                selected = []
            }
            this.selected = selected
            this.resetHighlightIndex()
        }
    
        mounted () {
            this.setDropdownState()
            this.setDropdownCallback()
        }

        slotOption (option) {
            return {
                ...option,
                isSelected: this.isSelect(option),
                multiple: this.multiple
            }
        }
        
        isSelect ({ id }) {
            if (this.multiple) {
                return this.selected.includes(id)
            }
            return id === this.selected
        }
        
        getPopoverInstance () {
            const selectDropdown: any = this.$refs.selectDropdown
            const ref: any = selectDropdown.$refs
            return ref.reference._tippy
        }
        
        close () {
            const popover = this.getPopoverInstance()
            popover.hide()
        }
        
        show () {
            const popover = this.getPopoverInstance()
            popover.show()
        }
        
        setDropdownState () {
            const popover = this.getPopoverInstance()
            if (this.dropdownActive) {
                popover.enable()
            } else {
                popover.disable()
            }
        }
        
        setDropdownCallback () {
            const popover = this.getPopoverInstance()
            popover.set({
                onShown: () => {
                    if (this.searchable) {
                        const inst: any = this.$refs.searchInput
                        inst.focus()
                    }
                }
            })
        }
        
        handleDropdownShow () {
            // @ts-ignore
            this.defaultWidth = this.$el.offsetWidth
            this.focus = true
        }
        
        handleDropdownHide () {
            this.focus = false
        }
        
        handleOptionClick (option) {
            return this.isSelect(option) && this.multiple ? this.unselectOption(option) : this.selectOption(option)
        }
        
        selectOption (option) {
            if (this.multiple) {
                this.selected = [...this.selected, option.id]
            } else {
                this.selected = option.id
                this.close()
            }
            this.$emit('selected', this.selected, this.selectedOptions)
        }
        
        unselectOption (option) {
            if (this.multiple) {
                this.selected = this.selected.filter(value => value !== option.id)
            } else {
                this.reset()
            }
            this.$emit('selected', this.selected, this.selectedOptions)
        }
        
        reset () {
            const prevSelected = this.multiple ? [...this.selected] : this.selected
            this.selected = this.multiple ? [] : ''
            this.$emit('clear', prevSelected)
        }
        
        selectAll () {
            if (this.isAllSelected) {
                this.reset()
            } else {
                this.selected = this.enabledOptions.map((option: any) => option.id)
            }
        }
        
        resetHighlightIndex () {
            this.highlightIndex = this.searchable ? -1 : null
        }
        
        resetSearchValue () {
            this.searchValue = ''
        }
        
        calcListScrollPosition (newIndex, oldIndex) {
            const optionList: any = this.$refs.optionList
            const option = optionList.children[newIndex]
            if (option && oldIndex !== null) {
                const optionElement = option
                const optionRect = optionElement.getBoundingClientRect()
                const listRect = optionList.getBoundingClientRect()
                const isInView = (optionRect.top > listRect.top) && (optionRect.bottom < listRect.bottom)
                if (!isInView) {
                    const isGoingDown = newIndex > oldIndex
                    let scrollTop
                    if (isGoingDown) {
                        const listHeight = listRect.bottom - listRect.top
                        scrollTop = optionElement.offsetTop - listHeight
                    } else {
                        scrollTop = optionElement.offsetTop - optionElement.offsetHeight
                    }
                    optionList.scrollTop = scrollTop
                }
            } else {
                optionList.scrollTop = 0
            }
        }
        
        handleKeydown (event) {
            const key = event.key
            const maxIndex = this.filterOptions.length - 1
            const currentIndex = this.highlightIndex
            if (key === 'ArrowUp') {
                event.preventDefault()
                this.highlightIndex = currentIndex > 0 ? currentIndex - 1 : maxIndex
            } else if (key === 'ArrowDown') {
                event.preventDefault()
                this.highlightIndex = currentIndex < maxIndex ? currentIndex + 1 : 0
            } else if (key === 'Enter') {
                event.preventDefault()
                this.handleEnter()
            }
        }
        
        handleEnter () {
            if (this.filterOptions.length) {
                const option = this.filterOptions[this.highlightIndex]
                if (option) {
                    this.selectOption(option)
                }
            } else {
                this.close()
            }
        }
        
        isSame (source, target) {
            const isArray = Array.isArray(source) && Array.isArray(target)
            if (isArray) {
                if (source.length !== target.length) {
                    return false
                }
                return !source.some((value, index) => value !== target[index])
            }
            return source === target
        }
    }
</script>
