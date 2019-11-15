<template>
    <div class="cron-trigger-week">
        <div class="cron-build-week">
            <span>每周</span>
            <ul>
                <li v-for="(week, index) of weeks"
                    :key="index"
                    class="week-item"
                    :class="{ 'cur-item': isSelectedWeek(index + 1) }"
                    @click="toggleWeek(index + 1)">
                    <label>{{ week }}</label>
                </li>
            </ul>
        </div>
        <div class="cron-build-time">
            <span>触发于</span>
            <input :disabled="disabled" type="text" :value="normalTime" class="bk-form-input" @change="changeTimes" placeholder="多个触发时间可以用英文逗号分隔，比如09:00,15:30，为空的时候默认为00:00" />
            <div class="bk-form-help is-danger" v-if="error">触发时间格式有误，请用英文逗号分隔，如09:00,15:30</div>
        </div>
    </div>
</template>

<script>
    import atomFieldMixin from '../atomFieldMixin'

    export default {
        mixins: [atomFieldMixin],
        props: {
            name: {
                type: String,
                required: true
            },
            value: {
                type: Array,
                required: true,
                default: ''
            }
        },
        data () {
            return {
                weeks: [
                    '日',
                    '一',
                    '二',
                    '三',
                    '四',
                    '五',
                    '六'
                ],
                normalTime: '',
                error: false,
                selectedWeek: []
            }
        },
        created () {
            if (this.value) {
                this.setExp(this.value)
            }
        },
        methods: {
            prezero (num) {
                num = Number(num)
                if (num < 10) {
                    return '0' + num
                }
                return num
            },
            setExp (lines) {
                if (lines.length) {
                    const times = []
                    lines && lines.map(item => {
                        const li = item.split(' ')
                        times.push(`${this.prezero(li[2])}:${this.prezero(li[1])}`)
                        this.selectedWeek = li[5].split(',').map(i => Number(i))
                    })
                    times && (this.normalTime = times.join(','))
                }
            },
            isSelectedWeek (index) {
                return this.selectedWeek.indexOf(index) > -1
            },
            getExp () {
                this.error = false
                const val = []
                const weekStr = this.selectedWeek.join(',')
                if (weekStr) {
                    let times = []
                    const normalTime = this.normalTime || '00:00'
                    if (normalTime) {
                        if (!normalTime.match(/\d{1,2}:\d{1,2}(,\d{1,2}:\d{1,2})*$/)) {
                            this.error = true
                            return
                        }
                        times = normalTime.split(',')
                        times && times.map(time => {
                            const mul = time.split(':')
                            if (mul.length === 2) {
                                val.push(`0 ${Number(mul[1])} ${Number(mul[0])} ? * ${weekStr}`)
                            } else {
                                this.error = true
                            }
                        })
                    }
                }
                !this.error && this.handleUpdateElement(this.name, val)
            },
            toggleWeek (index) {
                if (this.isSelectedWeek(index)) {
                    this.selectedWeek = this.selectedWeek.filter(item => item !== index)
                } else {
                    this.selectedWeek.push(index)
                }
                this.getExp()
            },
            resetSelectedWeek () {
                this.selectedWeek = []
                this.normalTime = ''
                this.getExp()
            },
            changeTimes (e) {
                const { value } = e.target
                const trimVal = value.trim()
                this.$emit('input', trimVal)
                this.normalTime = trimVal
                this.getExp()
            },
            handleUpdateElement (name, value) {
                this.handleChange(name, value)
            }
        }
    }
</script>

<style lang="scss">
    .cron-trigger-week {
        .cron-build-week, .cron-build-time {
            font-size: 12px;
            padding: 6px 0;
            span {
                width: 65px;
                float: left;
                padding: 7px 15px 7px 0;
                display: block;
            }
            .bk-form-input {
                width: 510px;
            }
        }
        .week-item {
            cursor: pointer;
            min-width: 36px;
            height: 30px;
            line-height: 30px;
            text-align: center;
            display: inline-block;
            vertical-align: middle;
            font-size: 14px;
            border: 1px solid #c3cdd7;
            box-sizing: border-box;
            overflow: hidden;
            margin-right: -1px;
            border-radius: 0;
            &.cur-item {
                border-color: #3c96ff;
                background: #3c96ff;
                color: #fff;
            }
        }
    }
</style>
