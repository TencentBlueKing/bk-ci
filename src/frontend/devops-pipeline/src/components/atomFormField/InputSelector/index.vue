
<template>
    <div class="bk-selector">
        <input type="text"
            class="bk-form-input"
            autocomplete="off"
            :name="name"
            :value="value"
            :placeholder="placeholder"
            @input="input"
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
            datalist: {
                type: Array,
                default: []
            }
        },
        data () {
            return {
                showList: false,
                focusList: '',
                list: []
            }
        },
        watch: {
            'datalist' (val) {
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
                this.$emit('onChange', this.name, value)
                // 触发调用getData方法
                // this.filterData(value)
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
                    this.list.map((item, index) => {
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
                this.$emit('onChange', this.name, data)
            },
            // 获取数据
            getData () {
                this.list = this.datalist
                this.list = ['android', 'ios', 'test', 'other']
            }
        }
    }
</script>
