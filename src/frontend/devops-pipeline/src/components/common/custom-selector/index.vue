<template>
    <div class="bk-tag-selector staff-selector" :class="{ 'disable-status': disabled }">
        <span class="staff-placeholder" v-if="!selectedList.length && !value.length">{{ placeholder }}</span>
        <div class="select-tags" @click="toEdit">
            <div class="select-editor" ref="selectEditor">
                <span class="tag-info" @click="selectInfo($event, entry)" v-for="(entry, index) in selectedList" :key="index">{{ entry }}</span>
                <input type="text" ref="staffInput" id="staffInput"
                    class="bk-tag-input form-input"
                    autocomplete="off"
                    :name="name"
                    :value="value"
                    @input="input"
                    @keydown="keydown"
                    @mousedown="mousedown"
                    @paste="paste"
                    @blur="hideAll" />
            </div>
        </div>

        <div class="bk-selector-list" v-show="showList && list.length">
            <ul class="outside-ul" ref="selectorList">
                <li v-for="(data, index) in list"
                    class="bk-selector-list-item"
                    :key="index"
                    @click.stop="selectList(data)">
                    <div class="bk-selector-node" :class="activeClass(index)">
                        <!--<img :src="localCoverAvatar(data)" class="bk-data-avatar">-->
                        <span class="text">{{ data }}</span>
                    </div>
                </li>
            </ul>
        </div>
    </div>
</template>

<script>
    export default {
        props: {
            config: {
                type: Object,
                default: {
                    initData: [],
                    data: [],
                    onChange: () => {}
                },
                required: true
            },
            name: {
                type: String,
                required: true
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
                default: true
            },
            selectedList: {
                type: Array,
                default: []
            },
            setValue: Function,
            deleteItem: Function
        },
        data () {
            return {
                showList: false,
                isSelected: false,
                focusList: '',
                minscroll: 0,
                maxscroll: 4,
                list: []
            }
        },
        watch: {
            'config.data' (val) {
                this.getData()
            },
            value (val) {
                if (this.list.length && val.length) {
                    this.showList = true
                    this.$refs.selectorList.scrollTop = 0
                } else {
                    setTimeout(() => {
                        this.showList = false
                    }, 250)
                }

                if (val.length) this.$refs.staffInput.style.width = (val.length * 20) + 'px'
            }
        },
        created () {
            this.getData()
        },
        methods: {
            input (e) {
                e.preventDefault()

                // 重置下拉菜单选中信息
                this.focusList = 0
                this.minscroll = 0
                this.maxscroll = 4
                this.$refs.selectorList.scrollTop = 0

                const { value } = e.target
                this.config.onChange(this.name, value)

                if (value.length) this.filterData(value)
            },
            paste (e) {
                e.preventDefault()

                const value = e.clipboardData.getData('text')
                const validateRes = this.validate(value)

                try {
                    if (value && validateRes.reg && validateRes.target.length > 3) {
                        if (validateRes.isEixt > -1) {
                            throw new Error(`${value}已存在`)
                        } else {
                            this.selectList(value)
                        }
                    } else {
                        const temp = []
                        const tagList = []
                        const errList = []
                        let resList = value.split(';')

                        resList = resList.filter(item => item)

                        resList.forEach(item => {
                            if (item.match(/^[a-zA-Z][a-zA-Z_]+/g)) {
                                temp.push(item.match(/^[a-zA-Z][a-zA-Z_]+/g).join(''))
                            } else {
                                errList.push(item)
                            }
                        })

                        temp.forEach(item => {
                            if (this.config.initData.indexOf(item) < 0) errList.push(item)
                            for (let i = this.config.data.length - 1; i >= 0; i--) {
                                if (this.config.data[i] === item) {
                                    tagList.push(item)
                                    this.config.data.splice(i, 1)
                                }
                            }
                        })

                        if (tagList.length) this.selectList(tagList, 'batch')
                        if (value && errList.length) {
                            errList.join(';')
                            throw new Error(`${errList}不是项目组成员`)
                        }
                    }
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
                // 为了让blur方法异步执行，以便能够成功执行click方法
                setTimeout(() => {
                    const errList = []
                    let temp = []
                    const value = val.trim()
                    let resList = value.split(',')

                    resList = resList.filter(item => {
                        return item && item.trim()
                    })

                    resList.forEach(item => {
                        if (this.checkVariable(item)) {
                            temp.push(item)
                        } else {
                            errList.push(item)
                        }
                    })

                    temp = temp.filter(item => this.selectedList.indexOf(item) === -1)

                    if (temp.length) this.selectList(temp, 'batch')

                    if (this.value && errList.length && !this.showList) {
                        errList.join(';')
                        const err = (`${errList}不是项目组成员`)
                        this.$showTips({
                            message: err,
                            theme: 'error'
                        })

                        this.config.onChange(this.name, '')
                        this.resetInput()
                    }
                    this.value = ''

                    if (this.$refs.staffInput) this.$refs.staffInput.style.width = 10 + 'px'
                }, 100)
            },
            mousedown (e) {
                this.focusList = 0
                this.minscroll = 0
                this.maxscroll = 4
                this.$refs.selectorList.scrollTop = 0

                if (this.value.length) {
                    this.filterData(this.value)
                    this.list.map((item, index) => {
                        if (item === this.value) this.focusList = index
                    })
                }
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

                            if (!this.value.length) {
                                this.deleteItem(this.name, result.index, target)

                                const site = nodes[nodes.length - 1]
                                this.insertAfter(this.$refs.staffInput, site)
                                this.$refs.staffInput.focus()
                                this.isFocus = true
                            }
                        }
                        break
                    // 向下
                    case 40:
                        this.focusList++
                        this.focusList = this.focusList > this.list.length - 1 ? this.list.length - 1 : this.focusList
                        if (this.focusList > this.maxscroll) {
                            this.$refs.selectorList.scrollTop += 42
                            this.minscroll++
                            this.maxscroll++
                        }
                        break
                    // 向上
                    case 38:
                        this.focusList--
                        if (this.focusList < 0) this.focusList = 0

                        this.focusList = this.focusList < 0 ? 0 : this.focusList
                        if (this.focusList < this.minscroll) {
                            this.$refs.selectorList.scrollTop -= 42
                            this.maxscroll--
                            this.minscroll--
                        }
                        break
                    // 向左
                    case 37:
                        if (!this.value.length) {
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
                        if (!this.value.length) {
                            const rightsite = nodes[parseInt(result.index) + 1]
                            this.insertAfter(this.$refs.staffInput, rightsite)
                            this.$refs.staffInput.focus()
                        }
                        break
                    // 确认
                    case 13:
                        if (this.showList) {
                            this.selectList(this.list[this.focusList])
                            this.showList = false
                        } else {
                            this.handleValue(this.value)
                        }
                        break
                    // 退出
                    case 27:
                        this.showList = false
                        break
                }
            },
            validate (value) {
                const target = value.replace(/\s/g, '')
                const resObj = {
                    target,
                    reg: this.checkVariable(target),
                    isEixt: this.selectedList.indexOf(target)
                }

                return resObj
            },
            /**
             *  更新样式
             */
            activeClass (i) {
                return {
                    'bk-selector-selected': i === this.focusList
                }
            },
            /**
             *  选中列表中的一项
             */
            selectList (data, type) {
                this.config.onChange(this.name, data)
                this.isSelected = true

                const nodes = this.$refs.selectEditor.childNodes
                const result = this.getSiteInfo(nodes)

                this.setValue(this.name, result.index, data, type)
                const site = nodes[nodes.length - 1]
                this.insertAfter(this.$refs.staffInput, site)
                this.$refs.staffInput.focus()
            },
            /**
             *  过滤数据
             */
            filterData (val) {
                this.list = this.config.data.filter(item => item.indexOf(val) > -1)
            },
            /**
             *  获取数据
             */
            getData () {
                this.list = this.config.data
            },
            /**
             *  头像格式
             */
            localCoverAvatar (data) {
                const member = /^\$\{(.*)\}$/.test(data) ? 'un_know' : data
                return `http://bking.com/avatars/${member}/avatar.jpg`
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
            // 检验变量
            checkVariable (val) {
                return /^\$\{(.*)\}$/.test(val)
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
            top: 2px;
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
