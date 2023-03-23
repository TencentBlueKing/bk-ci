<template>
    <section class="param-input-home">
        <span v-if="hyphen && hyphen.trim()" class="param-hyphen">{{hyphen}}</span>
        <section class="parameter-input">
            <p v-if="label && label.trim()" class="input-label" :title="label">{{ label }}：</p>
            <bk-input class="input-main" :clearable="!disabled" :value="value" @change="(newValue) => $emit('update-value', newValue)" v-if="type === 'input'" :disabled="disabled"></bk-input>
            <vuex-textarea class="textarea-main" v-else-if="type === 'textarea'" :value="value" :handle-change="(name, value) => $emit('update-value', value)" :disabled="disabled" click-unfold hover-unfold></vuex-textarea>
            <section v-else class="parameter-select input-main" v-bk-clickoutside="toggleShowList">
                <bk-input ref="inputItem"
                    :clearable="!disabled"
                    :value="displayValue"
                    :disabled="disabled"
                    @clear="$emit('update-value', '')"
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
    </section>
</template>

<script>
    import mixins from '../mixins'
    import VuexTextarea from '@/components/atomFormField/VuexTextarea'

    export default {
        components: {
            VuexTextarea
        },
        mixins: [mixins],

        props: {
            label: {
                type: String
            },
            type: {
                type: String,
                default: 'input'
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
            },
            paramId: {
                type: String,
                default: 'id'
            },
            paramName: {
                type: String,
                default: 'name'
            },
            dataPath: {
                type: String,
                default: 'data.records'
            },
            hyphen: {
                type: String
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
                        this.$emit('update-value', '')
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
                    } else if (typeof id === 'string' && id.isBkVar()) {
                        tempName = id
                    }
                    return tempName
                }

                const res = []
                if (this.isMultiple) {
                    const valArr = String(this.value || '').split(',')
                    valArr.forEach((val) => {
                        const name = findItemName(val)
                        if (name !== undefined) res.push(name)
                    })
                } else {
                    res.push(findItemName(this.value))
                }
                this.displayValue = res.join(',')
            },

            chooseOption (option) {
                if (!this.isMultiple) {
                    this.$emit('update-value', option.id)
                    this.$refs.inputItem.$refs.input.blur()
                    this.toggleShowList()
                } else {
                    const valArr = String(this.value || '').split(',').filter(x => x !== '')
                    const index = valArr.findIndex(x => x === option.id)
                    if (index > -1) valArr.splice(index, 1)
                    else valArr.push(option.id)
                    this.$emit('update-value', valArr.join(','))
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
                        if (name.isBkVar()) item = name
                        else item = ''
                    } else {
                        item = item.id
                    }
                    return item
                }

                const res = []
                if (this.isMultiple) {
                    (String(value || '').split(',') || []).forEach((val) => {
                        const tempId = findItemId(val)
                        if (tempId !== '') res.push(tempId)
                    })
                } else {
                    res.push(findItemId(value))
                }
                this.$emit('update-value', res.join(','))
            },

            isActive (id) {
                const valArr = String(this.value || '').split(',')
                return valArr.includes(id)
            },

            initList () {
                if (this.listType === 'list') {
                    this.paramList = (JSON.parse(JSON.stringify(this.list)) || []).map((item) => ({ id: item[this.paramId], name: item[this.paramName] }))
                    this.calcDisplayVal()
                    return
                }

                if (typeof this.url === 'string' && this.url !== '') { // 只有存在url字段时才去请求
                    const [url, queryKey] = this.generateReqUrl(this.url, this.paramValues)
                    this.queryKey = queryKey

                    if (!url) return
                    this.loading = true
                    this.$ajax.get(url).then((res) => {
                        const list = this.getResponseData(res, this.dataPath)
                        const data = (list || []).map((item) => ({ id: item[this.paramId], name: item[this.paramName] }))
                        this.paramList.splice(0, this.paramList.length, ...data)
                        this.calcDisplayVal()
                    }).catch(e => this.$showTips({ message: e.message, theme: 'error' })).finally(() => (this.loading = false))
                }
            }
        }
    }
</script>

<style lang="scss" scoped>
    .param-input-home {
        display: flex;
        align-items: flex-end;
        flex: 1;
        .param-hyphen {
            margin-right: 11px;
        }
    }
    .parameter-input {
        flex: 1;
        .input-label {
            max-width: 100%;
            overflow: hidden;
            text-overflow: ellipsis;
            white-space: nowrap;
        }
        .input-main {
            flex: 1;
        }
        .textarea-main {
            position: relative;
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
