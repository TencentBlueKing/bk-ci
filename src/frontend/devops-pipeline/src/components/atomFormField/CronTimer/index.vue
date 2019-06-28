<template>
    <div class='soda-cron-timer duration-exp'>
        <span>
            <span class='desc-divider'>每</span>
            <bk-select
                :searchable="false"
                :clearable="false"
                :disabled="disabled"
                :value="contabObj.peroid.index"
                @selected="onSelect">
                <bk-option v-for="item in periodArray" :key="item.index" :id="item.index" :name="item.label">

                </bk-option>
            </bk-select>
            <span class='desc-divider'>的</span>
        </span>
        <!-- 星期 -->
        <span v-if="contabObj.peroid.value === 2">
            <bk-select
                :searchable="false"
                :clearable="false"
                :disabled='disabled'
                :value='contabObj.week.index'
                @selected='onSelect'>
                <bk-option v-for="item in weekArray" :key="item.index" :id="item.index" :name="item.label">
                </bk-option>
            </bk-select>
            <span class='desc-divider'>的</span>
        </span>
        <!-- 日期 -->
        <span v-if="contabObj.peroid.value === 3">
            <bk-select
                :searchable="false"
                :clearable="false"
                :disabled='disabled'
                :value='contabObj.day.index'
                @selected='onSelect'>
                <bk-option v-for="item in dayArray" :key="item.index" :id="item.index" :name="item.label">
                </bk-option>
            </bk-select>
        </span>
        <!-- 小时 -->
        <span v-if="contabObj.peroid.value > 0">
            <bk-select
                :searchable="false"
                :clearable="false"
                :disabled='disabled'
                :value='contabObj.hour.index'
                @selected='onSelect'>
                <bk-option v-for="item in hourArray" :key="item.index" :id="item.index" :name="item.label">
                </bk-option>
            </bk-select>
            <span class='desc-divider'>时</span>
        </span>
        <!-- 分钟 -->
        <span>
            <bk-select
                :searchable="false"
                :clearable="false"
                :disabled='disabled'
                :list='minutesArray'
                :value='contabObj.minute.index'
                @selected='onSelect'>
                <bk-option v-for="item in minutesArray" :key="item.index" :id="item.index" :name="item.label">
                </bk-option>
            </bk-select>
            <span class='desc-divider'>分</span>
        </span>
    </div>
</template>

<script>
    import atomFieldMixin from '../atomFieldMixin'
    import { PERIOD_ARRAY, WEEK_ARRAY, MONTH_ARRAY, DAY_ARRAY, HOUR_ARRAY, MINUTES_ARRAY } from './cronUtils'

    export default {
        mixins: [ atomFieldMixin ],
        name: 'cron-timer',
        props: {
            value: {
                type: String,
                required: true,
                default: '0 10 * ? * *'
            }
        },
        data () {
            return {
                periodArray: PERIOD_ARRAY,
                weekArray: WEEK_ARRAY,
                dayArray: DAY_ARRAY,
                hourArray: HOUR_ARRAY,
                minutesArray: MINUTES_ARRAY,
                contabObj: this.parseExpression(this.value)
            }
        },
        methods: {
            parseExpression (expression) {
                const [second, minute, hour, day, month, week] = expression.split(' ')
                let peroid = PERIOD_ARRAY[0]
                switch (true) {
                    case /\s\*\s\?(\s\*){2}$/g.test(expression):
                        peroid = PERIOD_ARRAY[0]
                        break
                    case /\s\?(\s\*){2}$/g.test(expression):
                        peroid = PERIOD_ARRAY[1]
                        break
                    case /\s\?\s\*\s\d+$/g.test(expression):
                        peroid = PERIOD_ARRAY[2]
                        break
                    case /\s\*\s\?$/g.test(expression):
                        peroid = PERIOD_ARRAY[3]
                        break
                }
                return {
                    peroid,
                    minute: this.findOption(MINUTES_ARRAY, minute) || MINUTES_ARRAY[0],
                    hour: this.findOption(HOUR_ARRAY, hour) || HOUR_ARRAY[0],
                    day: this.findOption(DAY_ARRAY, day) || DAY_ARRAY[0],
                    month: this.findOption(MONTH_ARRAY, month) || MONTH_ARRAY[0],
                    week: this.findOption(WEEK_ARRAY, week) || WEEK_ARRAY[0]
                }
            },
            findOption (list, val) {
                return list.find(opt => opt.value === Number(val))
            },
            generatorExpression () {
                const { peroid, minute, hour, day, month, week } = this.contabObj
                switch (peroid.label) {
                    case '小时':
                        return `0 ${minute.value} * ? * *`
                    case '天':
                        return `0 ${minute.value} ${hour.value} ? * *`
                    case '周':
                        return `0 ${minute.value} ${hour.value} ? * ${week.value}`
                    case '月':
                        return `0 ${minute.value} ${hour.value} ${day.value} * ?`
                }
            },
            onSelect (index, option) {
                const { id } = option
                const type = this.getTypeById(id)
                this.contabObj[type] = option
                this.handleExpChange(this.generatorExpression())
            },
            getTypeById (id) {
                const [type] = id.split('_')
                return type
            },
            handleExpChange (exp) {
                const { name, handleChange } = this
                handleChange(name, exp)
            }
        }
    }
</script>

<style lang='scss'>
    .soda-cron-timer {
        &.duration-exp span {
            display: inline-flex;
            align-items: center;
            .bk-selector {
                margin: 10px;
                max-width: 98px;
            }
        }
    }
</style>
