<template>
    <section class="parameter-input">
        <span v-if="label" class="input-label" :title="label">{{ label }}：</span>
        <bk-input class="input-main" :clearable="!disabled" :value="value" @change="(newValue) => $emit('updateValue', newValue)" v-if="type === 'input'" :disabled="disabled"></bk-input>
        <section v-else class="parameter-select input-main" v-bk-clickoutside="toggleShowList">
            <bk-input ref="inputItem"
                :clearable="!disabled"
                :value="displayValue"
                :disabled="disabled"
                @blur="handleBlur"
                @change="handleInput"
                @focus="toggleShowList(true)">
            </bk-input>
            <ul v-if="showList && paramList.length" class="parameter-list">
                <li v-for="(option, index) in paramList"
                    :key="index"
                    @click="chooseOption(option)"
                    :class="{ 'is-active': isActive(option.id) }"
                >{{option.name}}</li>
            </ul>
        </section>
    </section>
</template>

<script>
    export default {
        props: {
            label: {
                type: String
            },
            type: {
                type: String
            },
            value: {
                type: [String, Array]
            },
            disabled: {
                type: Boolean,
                default: false
            },
            listType: {
                type: String
            },
            list: {
                type: Array,
                default: () => ([])
            },
            url: {
                type: String
            },
            isMultiple: {
                type: Boolean,
                default: false
            },
            paramValues: {
                type: Object,
                default: () => ({})
            }
        },

        data () {
            return {
                showList: false,
                paramList: [],
                loading: false,
                queryKey: [],
                displayValue: ''
            }
        },

        watch: {
            value (newValue) {
                this.calcDisplayVal()
            },

            paramValues: {
                handler (value, oldValue) {
                    const index = this.queryKey.findIndex((key) => value[key] !== oldValue[key])
                    if (index > -1) {
                        const defaultValue = this.isMultiple ? [] : ''
                        this.$emit('updateValue', defaultValue)
                        this.initList()
                    }
                },
                deep: true
            }
        },

        created () {
            this.initList()
        },

        methods: {
            calcDisplayVal () {
                const findItemName = (id) => {
                    let tempName
                    const item = this.paramList.find(x => x.id === id)
                    if (item) {
                        tempName = item.name
                    } else if (/^\$\{.+\}$/.test(id)) {
                        tempName = id
                    }
                    return tempName
                }

                let res
                if (this.isMultiple) {
                    const tempArr = [];
                    (this.value || []).forEach((val) => {
                        const name = findItemName(val)
                        if (name !== undefined) tempArr.push(name)
                    })
                    res = tempArr.join(',')
                } else {
                    res = findItemName(this.value)
                }
                this.displayValue = res
            },

            chooseOption (option) {
                if (!this.isMultiple) {
                    this.$emit('updateValue', option.id)
                    this.$refs.inputItem.$refs.input.blur()
                    this.toggleShowList()
                } else {
                    const cloneValue = JSON.parse(JSON.stringify(this.value))
                    if (Array.isArray(cloneValue)) {
                        const index = cloneValue.findIndex(x => x === option.id)
                        if (index > -1) cloneValue.splice(index, 1)
                        else cloneValue.push(option.id)
                        this.$emit('updateValue', cloneValue)
                    } else {
                        this.$emit('updateValue', [option.id])
                    }
                }
            },

            toggleShowList (value = false) {
                value = value === true ? value : false
                this.showList = value
            },

            handleInput (value) {
                this.displayValue = value
            },

            handleBlur (value) {
                const findItemId = (name) => {
                    let item = this.paramList.find(x => x.name === name)
                    if (!item) {
                        if (/^\$\{.+\}$/.test(name)) item = name
                        else item = undefined
                    } else {
                        item = item.id
                    }
                    return item
                }

                let res = ''
                let tempId = ''
                if (this.isMultiple) {
                    res = [];
                    (value.split(',') || []).forEach((val) => {
                        tempId = findItemId(val)
                        if (tempId !== undefined) res.push(tempId)
                    })
                } else {
                    tempId = findItemId(value)
                    res = tempId === undefined ? '' : tempId
                }
                this.$emit('updateValue', res)
            },

            isActive (id) {
                if (Array.isArray(this.value)) {
                    return this.value.includes(id)
                } else {
                    return this.value === id
                }
            },

            initList () {
                if (this.listType === 'list') {
                    this.paramList = JSON.parse(JSON.stringify(this.list))
                    this.calcDisplayVal()
                    return
                }

                if (typeof this.url === 'string' && this.url !== '') { // 只有存在url字段时才去请求
                    let url = this.url
                    let isErrorParam = false
                    this.queryKey = []
                    url = url.replace(/{([^\{\}]+)}/g, (str, key) => {
                        this.queryKey.push(key)
                        const value = this.paramValues[key]
                        if (typeof value === 'undefined') isErrorParam = true
                        return value
                    })

                    if (isErrorParam) return
                    this.loading = true
                    this.$ajax.get(url).then((res) => {
                        const data = res.data || []
                        this.paramList.splice(0, this.paramList.length, ...data)
                        this.calcDisplayVal()
                    }).catch(e => this.$showTips({ message: e.message, theme: 'error' })).finally(() => (this.loading = false))
                }
            }
        }
    }
</script>

<style lang="scss" scoped>
    .parameter-input {
        display: flex;
        align-items: center;
        .input-label {
            max-width: 30%;
            overflow: hidden;
            text-overflow: ellipsis;
            white-space: nowrap;
        }
        .input-main {
            flex: 1;
        }
    }
    .parameter-select {
        position: relative;
        .parameter-list {
            position: absolute;
            top: 32px;
            left: 0;
            right: 0;
            padding: 6px 0;
            list-style: none;
            border: 1px solid #dcdee5;
            border-radius: 2px;
            line-height: 32px;
            background: #fff;
            color: #63656e;
            overflow: auto;
            max-height: 216px;
            z-index: 2;
            li {
                padding: 0 16px;
                position: relative;
                &:hover {
                    color: #3a84ff;
                    background-color: #eaf3ff;
                }
            }
            .is-active {
                color: #3a84ff;
                background-color: #f4f6fa;
                &:after {
                    content: '';
                    position: absolute;
                    right: 16px;
                    top: 11px;
                    height: 7px;
                    width: 3px;
                    transform: rotate(45deg);
                    border-bottom: 1px solid #3a84ff;
                    border-right: 1px solid #3a84ff;
                }
            }
        }
    }
</style>
