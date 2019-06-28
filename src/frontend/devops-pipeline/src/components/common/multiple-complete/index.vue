
<template>
    <div class="bk-selector bk-tag-selector" :class="{ 'is-required': required }">
        <input type="text"
            class="bk-form-input"
            autocomplete="off"
            :name="name"
            :value="value"
            :placeholder="placeholder"
            :disabled="disabled"
            @input="input"
            @keydown="keydown"
            @mousedown="focus"
            @blur="hideAll" />

        <div class="bk-selector-list" v-show="showList && list.length">
            <ul>
                <li
                    v-for="(data, index) in list"
                    class="bk-selector-list-item"
                    :class="activeClass(index)"
                    :key="index"
                    @click.stop="selectList(data)">
                    <div class="bk-selector-node">
                        <div class="text">{{ data }}</div>
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
                type: String
            },
            placeholder: {
                type: String,
                default: '请输入'
            },
            disabled: {
                type: Boolean,
                default: false
            }
        },
        data () {
            return {
                showList: false,
                focusList: '',
                list: [],
                groupIdStr: this.value
            }
        },
        watch: {
            'config.data' (val) {
                this.getData()
            }
        },
        created () {
            this.getData()
        },
        methods: {
            input (e) {
                this.showList = true
                this.focusList = -1
                let val
                if (e) {
                    let value = e.target.value
                    val = e.target.value
                    value = value.replace(new RegExp(';+', 'gm'), ';')
                    const lastIdx = val.lastIndexOf(';')
                    if (lastIdx > -1) {
                        this.groupIdStr = val.substring(0, lastIdx + 1) || ''
                        val = val.substring(lastIdx + 1)
                    } else {
                        // 未匹配到‘;’时
                        this.groupIdStr = ''
                    }
                    this.config.onChange(this.name, value)
                } else {
                    val = ''
                }
                // 触发调用getData方法
                this.filterData(val)
            },
            // 隐藏补全列表
            hideAll (e) {
                // 为了让blur方法延迟执行，以便能够成功执行click方法
                setTimeout(() => {
                    this.showList = false
                }, 250)
            },
            focus (e) {
                this.showList = true
                this.focusList = -1
                if (this.value) {
                    const lastIndex = this.value.lastIndexOf(';')
                    const value = lastIndex > -1 ? this.value.substring(lastIndex + 1) : this.value
                    this.filterData(value)
                    this.list.map((item, index) => {
                        if (item === value) {
                            this.focusList = index
                        }
                    })
                }
            },
            mousemove (i) {
                this.focusList = i
            },
            // 键盘移动
            keydown (e) {
                const key = e.keyCode

                // 如果没有展示的list，则直接返回
                if (!this.showList) return

                switch (key) {
                    case 40: // 向上,阻止默认事件
                        e.preventDefault()
                        this.focusList++
                        break
                    case 38: // 向下,阻止默认事件
                        e.preventDefault()
                        this.focusList--
                        break
                    case 13: // 确认
                        if (this.focusList < 0) {
                            return false
                        }
                        this.selectList(this.list[this.focusList])
                        // 重置下拉列表
                        this.showList = false
                        this.input()
                        break
                    case 27: // 退出
                        this.showList = false
                        break
                }

                // 点中的序号超过数组的长度时，循环到第一个
                const listLength = this.list.length - 1
                if (key === 13 || (key === 8 && !this.groupIdStr)) {
                    this.focusList = -1
                } else {
                    this.focusList = this.focusList > listLength ? 0 : this.focusList < 0 ? listLength : this.focusList
                }
            },
            // 更新样式
            activeClass (i) {
                return {
                    'bk-selector-selected': i === this.focusList
                }
            },
            // 选中列表中的哪一项
            selectList (data) {
                if (data) {
                    this.groupIdStr += data + ';'
                }
                this.config.onChange(this.name, this.groupIdStr)
            },
            // 过滤数据
            filterData (val) {
                this.list = this.config.data.filter(item => item.indexOf(val) > -1)
                if (!this.list.length) {
                    this.showList = false
                }
            },
            // 获取数据
            getData () {
                this.list = this.config.data
            }
        }
    }
</script>
