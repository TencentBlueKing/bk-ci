<template>
    <div class="devops-staff-selector" @click="toEdit">
        <div :class="['devops-staff-input', { 'active': isEdit, 'disabled': disabled }]">
            <span class="placeholder"
                v-if="!isEdit && !selectedList.length && !value.length"
            >{{ placeholder }}</span>
            <div class="tag-list" :class="!selectedList.length ? 'no-item' : ''">
                <div class="select-editor" ref="selectEditor">
                    <span class="tag-info"
                        v-for="(entry, index) in selectedList"
                        :key="index"
                        @click="selectInfo($event, entry)"
                    >{{ entry }}</span>
                    <input type="text" ref="staffInput" id="staffInput"
                        class="form-input"
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
        </div>

        <div class="staff-selector-list" v-show="showList && list.length">
            <ul class="outside-ul" ref="selectorList" @scroll.passive="scrollLoadMore">
                <li v-for="(data, index) in renderList"
                    class="staff-selector-list-item"
                    :key="data"
                    @click.stop="selectList(data)">
                    <div class="selector-node" :class="activeClass(index)">
                        <img :src="localCoverAvatar(data)" class="avatar">
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
                default: false
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
                isEdit: false,
                showList: false,
                isSelected: false,
                focusList: '',
                minscroll: 0,
                maxscroll: 6,
                list: [],
                renderList: [],
                loadIndex: 6,
                loadSize: 100,
                loadEnd: false
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
            scrollLoadMore (event) {
                const target = event.target
                const bottomDis = target.scrollHeight - target.clientHeight - target.scrollTop
                if (bottomDis <= 50 && !this.loadEnd) {
                    const newList = this.list.slice(this.loadIndex * this.loadSize, (++this.loadIndex) * this.loadSize)
                    this.loadEnd = this.loadIndex * this.loadSize >= this.list.length
                    this.renderList = this.renderList.concat(newList)
                }
            },

            input (e) {
                e.preventDefault()

                // 重置下拉菜单选中信息
                this.focusList = 0
                this.minscroll = 0
                this.maxscroll = 6
                this.$refs.selectorList.scrollTop = 0
                this.isEdit = true

                let { value } = e.target
                if (value.endsWith(',') || value.endsWith(';')) {
                    value = value.substr(0, value.length - 1)
                }
                this.config.onChange(this.name, value)

                if (value.length) this.filterData(value)
            },

            paste (e) {
                e.preventDefault()

                const clipText = e.clipboardData.getData('text')
                const { value, isExist, isVar } = this.validate(clipText)
                try {
                    if (value && isVar && value.length > 3) {
                        if (isExist > -1) {
                            throw new Error(`${value}${this.$t('editPage.atomForm.hasExisted')}`)
                        } else {
                            this.selectList(value)
                        }
                    } else {
                        const temp = []
                        const tagList = []
                        const errList = []
                        const resList = value.split(';').filter(item => item)

                        resList.forEach(item => {
                            const reg = /^[a-z][a-z0-9_-]+/ig
                            const matchResult = item.match(reg)
                            if (matchResult) {
                                temp.push(matchResult.join(''))
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
                            throw new Error(`${errList}${this.$t('editPage.atomForm.notFound')}`)
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
                this.handleValue(this.value)
            },
            handleValue (val) {
                // 为了让blur方法异步执行，以便能够成功执行click方法
                setTimeout(() => {
                    if (val.endsWith(',') || val.endsWith(';')) {
                        val = val.substr(0, val.length - 1)
                    }
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
                        const err = (`${errList}${this.$t('editPage.atomForm.notFound')}`)
                        this.$showTips({
                            message: err,
                            theme: 'error'
                        })

                        this.config.onChange(this.name, '')
                        this.resetInput()
                        this.isEdit = false
                    }
                    this.value = ''

                    if (this.$refs.staffInput) this.$refs.staffInput.style.width = 10 + 'px'
                }, 100)
            },
            mousedown (e) {
                this.focusList = 0
                this.minscroll = 0
                this.maxscroll = 6
                this.$refs.selectorList.scrollTop = 0

                if (this.value.length) {
                    this.filterData(this.value)
                    this.list.forEach((item, index) => {
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
                        this.isEdit = true
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
                            this.$refs.selectorList.scrollTop += 32
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
                            this.$refs.selectorList.scrollTop -= 32
                            this.maxscroll--
                            this.minscroll--
                        }
                        break
                    // 向左
                    case 37:
                        this.isEdit = true
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
                        this.isEdit = true
                        if (!this.value.length) {
                            const rightsite = nodes[parseInt(result.index) + 1]
                            this.insertAfter(this.$refs.staffInput, rightsite)
                            this.$refs.staffInput.focus()
                        }
                        break
                    // 确认
                    case 186:
                    case 188:
                    case 13:
                        e.preventDefault()
                        this.isEdit = true
                        if (this.showList) {
                            this.selectList(this.list[this.focusList])
                            if (key === 188 || key === 186) {
                                return
                            }
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
                const trimVal = value.replace(/[\s\r\n]/g, '')
                return {
                    value: trimVal,
                    isVar: this.checkVariable(trimVal),
                    isExist: this.selectedList.indexOf(trimVal)
                }
            },
            /**
             *  更新样式
             */
            activeClass (i) {
                return {
                    selected: i === this.focusList
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
                this.list = Array.from(new Set(this.config.data.filter(item => item.toLowerCase().indexOf(val.toLowerCase()) > -1)))
                this.resetLoadData()
            },
            /**
             *  获取数据
             */
            getData () {
                this.list = this.config.data
                this.resetLoadData()
            },
            resetLoadData () {
                this.loadIndex = 6
                this.loadEnd = false
                this.renderList = this.list.slice(0, 400)
            },
            /**
             *  头像格式
             */
            localCoverAvatar (data) {
                const member = data.isBkVar() ? 'un_know' : data
                return `${USER_IMG_URL}/avatars/${member}/avatar.jpg`
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
                return typeof val === 'string' && val.isBkVar()
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
    @import '../../../scss/mixins/scroller';

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
            margin: 4px 5px 4px 0;
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
        .staff-selector-list {
            position: absolute;
            top: calc(100% + 4px);
            width: 100%;
            box-shadow: 0 3px 9px 0 rgba(0,0,0,.1);
            background-color: #fff;
            border-radius: 4px;
            z-index: 2000;
        }
        .outside-ul {
            margin: 0;
            list-style: none;
            overflow-y: auto;
            padding: 6px 0;
            border-radius: 2px;
            background-color: #fff;
            border: 1px solid #dcdee5;
            @include scroller(#a5a5a5, 5px);
        }
        .staff-selector-list > ul {
            max-height: 238px;
        }
        .staff-selector-list-item {
            list-style: none;
            .selector-node {
                display: flex;
                align-items: center;
                padding: 0 10px;
                background: none;
                height: 32px;
                line-height: 30px;
            }
            .selected {
                background-color: #f4f6fa;
            }
            .text {
                display: inline;
                font-size: 12px;
                color: #63656e;
            }
        }
        .avatar {
            width: 22px;
            height: 22px;
            float: left;
            margin-right: 8px;
            border-radius: 50%;
            vertical-align: middle;
            border:1px solid #C4C6CC;
        }
    }
</style>
