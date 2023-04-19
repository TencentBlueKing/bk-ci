<template>
    <div class="devops-staff-selector" @click="toEdit">
        <div :class="['devops-staff-input', { 'active': isEdit, 'disabled': disabled }]">
            <span class="placeholder" v-if="!isEdit && !value.length && !curInsertVal.length">{{placeholder}}</span>
            <div class="tag-list" :class="!value.length ? 'no-item' : ''">
                <div class="select-editor" ref="selectEditor">
                    <span class="tag-info"
                        v-for="(entry, index) in value"
                        :key="index"
                        @click="selectInfo($event, entry)"
                    >{{entry}}</span>
                    <input type="text" ref="staffInput" id="staffInput"
                        class="form-input"
                        autocomplete="off"
                        :name="name"
                        :value="curInsertVal"
                        @input="input"
                        @keydown="keydown"
                        @paste="paste"
                        @blur="hideAll" />
                </div>
            </div>
        </div>
    </div>
</template>

<script>
    import atomFieldMixin from '../atomFieldMixin'

    export default {
        name: 'user-input',
        mixins: [atomFieldMixin],
        props: {
            name: {
                type: String,
                default: ''
            },
            value: {
                type: String,
                default: ''
            },
            placeholder: {
                type: String,
                default: ''
            },
            disabled: {
                type: Boolean,
                default: false
            }
        },
        data () {
            return {
                isEdit: false,
                curInsertVal: ''
            }
        },
        watch: {
            curInsertVal (val) {
                if (val.length) this.$refs.staffInput.style.width = (val.length * 20) + 'px'
            }
        },
        methods: {
            changeInputValue (name, value) {
                this.curInsertVal = value
            },
            input (e) {
                e.preventDefault()

                const { value } = e.target
                this.changeInputValue(this.name, value)
                this.isEdit = true
            },
            paste (e) {
                e.preventDefault()

                const value = e.clipboardData.getData('text')

                try {
                    const tagList = []
                    let resList = value.split(';')

                    resList = resList.filter(item => item && !this.value.includes(item))

                    resList.forEach(item => {
                        tagList.push(item)
                    })

                    if (tagList.length) this.selectList(tagList, 'batch')
                } catch (err) {
                    this.$showTips({
                        message: err.message || err,
                        theme: 'error'
                    })
                }
            },
            /**
             *  隐藏补全列表
             */
            hideAll (e) {
                e.preventDefault()
                this.handleValue(e.target.value)
                this.isEdit = false
            },
            handleValue (val) {
                setTimeout(() => {
                    let temp = []
                    const value = val.trim()
                    let resList = value.split(',')

                    resList = resList.filter(item => {
                        return item && item.trim()
                    })
                    resList.forEach(item => {
                        temp.push(item)
                    })
                    temp = temp.filter(item => this.value.indexOf(item) === -1)
                    if (temp.length) this.selectList(temp, 'batch')
                    this.changeInputValue(this.name, '')

                    if (this.$refs.staffInput) this.$refs.staffInput.style.width = 10 + 'px'
                }, 100)
            },
            /**
             *  键盘移动
             */
            keydown (e) {
                let target
                const key = e.keyCode
                const nodes = this.$refs.selectEditor.childNodes
                const result = this.getSiteInfo(nodes)

                switch (key) {
                    // 删除
                    case 8:
                    case 46:
                        this.isEdit = true
                        if (parseInt(result.index) !== 0) {
                            target = result.temp[result.index - 1].innerText

                            if (!this.curInsertVal.length) {
                                this.deleteItem(this.name, result.index, target)

                                const site = nodes[nodes.length - 1]
                                this.insertAfter(this.$refs.staffInput, site)
                                this.$refs.staffInput.focus()
                            }
                        }
                        break
                    // 向左
                    case 37:
                        this.isEdit = true
                        if (!this.curInsertVal.length) {
                            if (parseInt(result.index) > 1) {
                                const leftsite = nodes[parseInt(result.index) - 2]
                                this.insertAfter(this.$refs.staffInput, leftsite)
                                this.$refs.staffInput.value = ''
                                this.$refs.staffInput.style.width = 10 + 'px'
                            } else {
                                const tags = this.$refs.selectEditor.getElementsByTagName('span')
                                this.$refs.selectEditor.insertBefore(this.$refs.staffInput, tags[0])
                            }
                            this.$refs.staffInput.focus()
                        }
                        break
                    // 向右
                    case 39:
                        this.isEdit = true
                        if (!this.curInsertVal.length) {
                            const rightsite = nodes[parseInt(result.index) + 1]
                            this.insertAfter(this.$refs.staffInput, rightsite)
                            this.$refs.staffInput.focus()
                        }
                        break
                    // 确认
                    case 13:
                        this.isEdit = true
                        this.handleValue(this.curInsertVal)
                        break
                    // 退出
                    case 27:
                        this.showList = false
                        break
                }
            },
            /**
             *  选中列表中的一项
             */
            selectList (data, type) {
                this.changeInputValue(this.name, data)

                const nodes = this.$refs.selectEditor.childNodes
                const result = this.getSiteInfo(nodes)

                this.setValue(this.name, result.index, data, type)
                const site = nodes[nodes.length - 1]
                this.insertAfter(this.$refs.staffInput, site)
                this.$refs.staffInput.focus()
            },
            setValue (name, index, item, type) {
                let newVal = []
                if (type) {
                    newVal = [...this.value.slice(0, index), ...item, ...this.value.slice(index, this.value.length)]
                } else {
                    newVal = [...this.value.slice(0, index), item, ...this.value.slice(index, this.value.length)]
                }
                this.handleChange(this.name, newVal)
                this.changeInputValue(this.name, '')
            },
            deleteItem (name, index, item) {
                const updateVal = [...this.value.slice(0, index - 1), ...this.value.slice(index, this.value.length)]
                this.handleChange(this.name, updateVal)
            },
            // 重置input
            resetInput () {
                const nodes = this.$refs.selectEditor.childNodes
                const result = this.getSiteInfo(nodes)

                if (result.index !== result.temp.length) {
                    const site = nodes[nodes.length - 1]
                    this.insertAfter(this.$refs.staffInput, site)
                }
            },
            toEdit (event) {
                this.isEdit = true
                this.$refs.staffInput.focus()
            },
            selectInfo (event, val) {
                this.insertAfter(this.$refs.staffInput, event.target)
            },
            getSiteInfo (nodes) {
                const res = {
                    index: 0,
                    temp: []
                }

                for (let i = 0; i < nodes.length; i++) {
                    const node = nodes[i]
                    if (!(node.nodeType === 3 && !/\S/.test(node.nodeValue))) {
                        res.temp.push(node)
                    }
                }

                Object.keys(res.temp).forEach(key => {
                    if (res.temp[key].id === 'staffInput') res.index = key
                })

                return res
            },
            insertAfter (newElement, targetElement) {
                const parent = targetElement.parentNode
                if (parent.lastChild === targetElement) {
                    parent.appendChild(newElement)
                } else {
                    parent.insertBefore(newElement, targetElement.nextSibling)
                }
            }
        }
    }
</script>

<style lang="scss" scoped>
    @import '../../../scss/conf';
    .devops-staff-selector {
        position: relative;
        min-height: 32px;
        .devops-staff-input {
            display: flex;
            align-items: center;
            justify-content: space-between;
            padding: 0 0 0 5px;
            border: 1px solid #c4c6cc;
            min-height: 32px;
            border-radius: 2px;
            font-size: 12px;
            position: relative;
            z-index: 1;
            background: #fff;
            cursor: pointer;
            overflow: hidden;
            .placeholder {
                margin: 0;
                padding: 0;
                font-size: 12px;
                position: absolute;
                line-height: 30px;
                top: 0;
                left: 8px;
                color: #c4c6cc;
            }
            &.disabled {
                background: #fafafa;
                cursor: not-allowed;
            }
            &.active {
                border-color: #3a84ff;
            }
        }

        .tag-list {
            z-index: 99;
            display: inline-block;
            max-height: 135px;
            overflow: auto;
            margin: 0;
            padding: 0;
            &.no-item {
                padding: 0 0 0 5px;
            }
        }
        .tag-item {
            display: inline-block;
            cursor: pointer;
            position: relative;
            margin: 4px 5px 4px 0;
            border-radius: 2px;
            height: 22px;
            overflow: hidden;
            font-size: 0;
            line-height: 0;
        }
        .tag-info {
            cursor: pointer;
            position: relative;
            margin: 2px 5px 4px 0;
            border-radius: 2px;
            height: 22px;
            line-height: 20px;
            overflow: hidden;
            display: inline-block;
            background-color: #F0F1F5;
            color: #63656e;
            font-size: 12px;
            border: none;
            vertical-align: middle;
            box-sizing: border-box;
            border-radius: 2px;
            padding: 0 5px;
            word-break: break-all;
            max-width: 190px;
            display: inline-block;
            text-overflow: ellipsis;
            white-space: nowrap;
        }
        .form-input {
            width: 10px;
            height: 22px;
            padding: 0;
            border: 0;
            box-sizing: border-box;
            outline: none;
            max-width: 295px;
            font-size: 12px;
            background-color: transparent;
        }
        .select-editor {
            cursor: text;
        }
    }
</style>
