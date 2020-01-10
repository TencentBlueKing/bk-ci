<template>
    <div class="bk-tag-selector staff-selector" :class="{ 'disable-status': disabled }">
        <span class="staff-placeholder" v-if="!value.length && !curInsertVal.length">{{ placeholder }}</span>
        <div class="select-tags" @click="toEdit">
            <div class="select-editor" ref="selectEditor">
                <span class="tag-info" @click="selectInfo($event, entry)" v-for="(entry, index) in value" :key="index">{{ entry }}</span>
                <input type="text" ref="staffInput" id="staffInput"
                    class="bk-tag-input form-input"
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
                        if (!this.curInsertVal.length) {
                            const rightsite = nodes[parseInt(result.index) + 1]
                            this.insertAfter(this.$refs.staffInput, rightsite)
                            this.$refs.staffInput.focus()
                        }
                        break
                    // 确认
                    case 13:
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
    .staff-selector {
        border: 1px solid $fontLigtherColor;
        .select-tags {
            position: relative;
            z-index: 99;
            margin: 4px 10px 0px 4px;
        }
        .tag-info {
            display: inline-block;
            padding: 2px 5px;
            margin: 1px 5px 4px 5px;
            background-color: #fafafa;
            border: 1px solid #d9d9d9;
            border-radius: 2px;
            font-size: 14px;
            color: #2b2b2b;
        }
        .form-input {
            padding: 6px 0 8px 4px;
            width: 10px;
            max-width: 160px;
            border: none;
            background-color: transparent;
            outline: 0;
        }
        .staff-placeholder {
            position: absolute;
            z-index: 10;
            line-height: 36px;
            padding-left: 8px;
            color: $fontLigtherColor;
        }
        .select-editor {
            cursor: text;
        }
        .bk-selector-list {
            position: absolute;
            top: calc(100% + 5px);
        }
        .bk-selector-list > ul {
            max-height: 212px;
        }
        .bk-selector-list-item {
            // background-color: pink;
            .bk-selector-node {
                display: flex;
                align-items: center;
            }
            .text {
                display: inline;
            }
        }
        .bk-data-avatar {
            margin-left: 16px;
            margin-right: 0;
            width: 28px;
            height: 28px;
            border-radius: 50%;
        }
        .bk-selector-selected {
            background-color: #eef6fe;
            color: #3c96ff;
        }
    }
    .disable-status {
        background-color: #fafafa;
        pointer-events: none;
    }
</style>
