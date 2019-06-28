
<template>
    <div class="bk-tag-selector">
        <input type="text"
            class="bk-form-input"
            autocomplete="off"
            :name="name"
            :value="selectedText"
            :placeholder="placeholder"
            @input="input"
            @mousedown="focus"
            @blur="hideAll" />

        <div class="bk-selector-list" v-show="(showList || openList) && localList.length">
            <ul>
                <li
                    v-for="(data, index) in list"
                    class="bk-selector-list-item"
                    :class="activeClass(index)"
                    :key="index"
                    @click.stop="selectList(data)">
                    <div class="bk-selector-node">
                        <div class="text">{{ data[displayKey] }}</div>
                    </div>
                </li>
            </ul>
        </div>
    </div>
</template>

<script>
    import atomFieldMixin from '../atomFieldMixin'

    export default {
        name: 'atom-complete',
        mixins: [atomFieldMixin],
        props: {
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
            list: {
                type: Array,
                default: []
            },
            displayKey: {
                type: String,
                default: ''
            },
            settingKey: {
                type: String,
                default: ''
            },
            openList: {
                type: Boolean,
                default: false
            }
        },
        data () {
            return {
                showList: false,
                focusList: '',
                localValue: '',
                localList: []
            }
        },
        computed: {
            selectedText () {
                const textObj = this.list.find(item => {
                    if (item[this.settingKey] === this.value) {
                        return item
                    }
                })
                return textObj ? textObj[this.displayKey] : this.value
            }
        },
        watch: {
            list (val) {
                this.getData()
            }
        },
        created () {
            this.getData()
        },
        methods: {
            input (e) {
                this.showList = true
                const { value } = e.target
                this.handleChange(this.name, value, false)
                // 触发调用getData方法
                this.filterData(value)
            },
            // 隐藏补全列表
            hideAll (e) {
                // 为了让blur方法延迟执行，以便能够成功执行click方法
                setTimeout(() => {
                    this.showList = false
                    const { value } = e.target
                    if (this.localValue && this.localValue !== value) {
                        this.handleChange(this.name, value, true)
                    } else {
                        this.handleChange(this.name, value, false)
                    }
                }, 250)
            },
            focus (e) {
                this.showList = true
                this.focusList = -1
                this.localValue = this.value
                if (this.value) {
                    this.filterData(this.value)
                    this.localList.map((item, index) => {
                        if (item === this.value) {
                            this.focusList = index
                        }
                    })
                }
            },
            mousemove (i) {
                this.focusList = i
            },
            // 更新样式
            activeClass (i) {
                return {
                    'bk-selector-selected': i === this.focusList
                }
            },
            // 选中列表中的哪一项
            selectList (data) {
                this.handleChange(this.name, data[this.settingKey], this.localValue !== data[this.settingKey])
            },
            // 过滤数据
            filterData (val) {
                this.localList = this.list.filter(item => item[this.displayKey].indexOf(val) > -1)
            },
            // 获取数据
            getData () {
                this.localList = this.list
            }
        }
    }
</script>
