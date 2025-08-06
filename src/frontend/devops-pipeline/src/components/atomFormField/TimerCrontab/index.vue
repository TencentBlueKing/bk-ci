<template>
    <div
        class="cron-ci"
        :class="[
            { 'is-error': isError },
            `error-${errorField}`,
            `select-${selectIndex}`
        ]"
    >
        <div class="time-describe">
            <span
                v-for="item in timeMap"
                :key="item.key"
                :class="['time-text', item.key]"
                @click="handleTimeTextChange(item.key)"
            >
                {{ item.label }}
            </span>
        </div>
        <div class="time-input">
            <input
                ref="input"
                class="input"
                type="text"
                :value="nativeValue"
                @blur="handleBlur"
                @input="handleInput"
                @keyup.left="handleSelectText"
                @keyup.right="handleSelectText"
                @mousedown="handleSelectText"
            >
        </div>
        <component
            :is="renderText"
            v-if="parseValue.length > 1"
            :data="parseValue"
        />
        <div
            v-if="nextTime.length > 0"
            class="time-next"
            :class="{ active: isTimeMore }"
        >
            <div class="label">
                {{ $t('cron.下次：') }}
            </div>
            <div class="value">
                <div
                    v-for="(time, index) in nextTime"
                    :key="`${time}_${index}`"
                >
                    {{ time }}
                </div>
            </div>
            <div
                class="arrow"
                @click="handleShowMore"
            >
                <i
                    class="devops-icon icon-angle-down arrow-button"
                />
            </div>
        </div>
    </div>
</template>
<script>
    import CronExpression from 'cron-parser-custom'
    import { prettyDateTimeFormat } from '@/utils/util'
    import Translate from '@/utils/cron/translate'
    import renderTextCn from './components/render-text-cn.vue'
    import renderTextEn from './components/render-text-en.vue'

    const labelIndexMap = {
        minute: 0,
        hour: 1,
        dayOfMonth: 2,
        month: 3,
        dayOfWeek: 4,
        0: 'minute',
        1: 'hour',
        2: 'dayOfMonth',
        3: 'month',
        4: 'dayOfWeek'
    }

    export default {
        name: '',
        props: {
            value: {
                type: String,
                default: ['* * * * *']
            },
            name: {
                type: String,
                default: ''
            },
            handleChange: {
                type: Function,
                default: () => {}
            }
        },
        data () {
            return {
                selectIndex: '',
                nativeValue: this.value.join(' '),
                nextTime: [],
                parseValue: [],
                errorField: '',
                isError: false,
                isTimeMore: false
            }
        },
        computed: {
            curLocale () {
                return this.$i18n.locale || 'zh-CN'
            },
            renderText () {
                return this.curLocale === 'zh-CN' ? renderTextCn : renderTextEn
            },
            timeMap () {
                return [
                    {
                        label: this.$t('cron.分'),
                        key: 'minute'
                    },
                    {
                        label: this.$t('cron.时'),
                        key: 'hour'
                    },
                    {
                        label: this.$t('cron.日'),
                        key: 'dayOfMonth'
                    },
                    {
                        label: this.$t('cron.月'),
                        key: 'month'
                    },
                    {
                        label: this.$t('cron.周'),
                        key: 'dayOfWeek'
                    }
                ]
            }
        },
        mounted () {
            if (!this.nativeValue) {
                return
            }
            this.checkAndTranslate(this.nativeValue)
        },
        methods: {
            /**
             * @desc 检测crontab格式和翻译
             */
            checkAndTranslate (value) {
                const interval = CronExpression.parse(`0 ${value.trim()}`, {
                    currentDate: new Date()
                })

                let i = 5
                this.nextTime = []
                while (i > 0) {
                    this.nextTime.push(prettyDateTimeFormat(interval.next().toString()))
                    i -= 1
                }

                this.errorField = ''
                this.isError = false
                this.parseValue = Translate(value, this.curLocale)
            },
            /**
             * @desc 选中crontab字段
             * @param {String} lable 选中的字段名
             */
            handleTimeTextChange (label) {
                if (!this.nativeValue) {
                    return
                }
                const timeItem = this.nativeValue
                const index = labelIndexMap[label]
                if (timeItem.length < index) {
                    return
                }
                const preStrLength = timeItem.slice(0, index).length + index
                const endPosition = preStrLength + timeItem[index].length
                setTimeout(() => {
                    this.selectIndex = label
                    this.$refs.input.focus()
                    this.$refs.input.selectionStart = preStrLength
                    this.$refs.input.selectionEnd = endPosition
                })
            },
            /**
             * @desc 输入框失去焦点
             */
            handleBlur () {
                this.selectIndex = ''
            },
            /**
             * @desc 选中输入框文本
             * @param {Object} event 文本选择事件
             */
            handleSelectText (event) {
                const $target = event.target
                const value = $target.value?.trim()
                this.nativeValue = value
                if (!value) return
                setTimeout(() => {
                    const cursorStart = $target.selectionStart
                    const cursorStr = value.slice(0, cursorStart)
                    const checkBackspce = cursorStr.match(/ /g)
                    if (checkBackspce) {
                        this.selectIndex = labelIndexMap[checkBackspce.length]
                    } else {
                        this.selectIndex = labelIndexMap['0']
                    }
                })
            },
            /**
             * @desc 输入框输入
             * @param {Object} event 输入框input事件
             */
            handleInput (event) {
                const $target = event.target
                const value = $target.value?.trim()
                this.nativeValue = value

                try {
                    this.checkAndTranslate(value)
                    this.handleChange(this.name, [value])
                } catch (error) {
                    this.parseValue = []
                    this.nextTime = []
                    const all = [
                        'minute',
                        'hour',
                        'dayOfMonth',
                        'month',
                        'dayOfWeek'
                    ]
                    if (all.includes(error.message)) {
                        this.errorField = error.message
                    }
                    this.isError = true
                    this.handleChange(this.name, [])
                }
            },
            /**
             * @desc 展示下次执行时间列表
             */
            handleShowMore () {
                this.isTimeMore = !this.isTimeMore
            }
        }
    }
</script>
<style lang='scss'>
.cron-ci {
    background: #f5f7fa;
    padding: 16px 20px;
    position: relative;
    &.is-error {
        .time-input {
            .input {
                border-color: #ff5656;
            }
        }
    }

    /* stylelint-disable selector-class-pattern */
    &.error-month .month,
    &.error-dayOfMonth .dayOfMonth,
    &.error-dayOfWeek .dayOfWeek,
    &.error-hour .hour,
    &.error-minute .minute {
        color: #ff5656 !important;
    }

    &.select-month .month,
    &.select-dayOfMonth .dayOfMonth,
    &.select-dayOfWeek .dayOfWeek,
    &.select-hour .hour,
    &.select-minute .minute {
        color: #3a84ff;
    }

    .time-describe {
        display: flex;
        justify-content: center;
    }

    .time-text {
        padding: 0 19px;
        font-size: 12px;
        line-height: 22px;
        color: #c4c6cc;
        cursor: pointer;
        transition: all 0.1s;

        &.active {
            color: #3a84ff;
        }

        &.field-error {
            color: #ff5656;
        }
    }

    .time-input {
        .input {
            width: 100%;
            height: 48px;
            padding: 0 30px;
            font-size: 24px;
            line-height: 48px;
            word-spacing: 30px;
            color: #63656e;
            text-align: center;
            border: 1px solid #3a84ff;
            border-radius: 2px;
            outline: none;

            &::selection {
                color: #3a84ff;
                background: transparent;
            }
        }
    }

    .time-parse {
        padding: 10px 0;
        margin-top: 8px;
        font-size: 0;
        line-height: 18px;
        color: #63656e;
        text-align: center;

        span {
            font-size: 12px;
            white-space: break-spaces;
        }
    }

    .time-next {
        display: flex;
        height: 18px;
        overflow: hidden;
        font-size: 12px;
        line-height: 18px;
        color: #979ba5;
        text-align: center;
        transition: height 0.2s linear;
        align-content: center;
        justify-content: center;
        &.active {
            height: 90px;

            .arrow {
                align-items: flex-end;

                .arrow-button {
                    transform: rotateZ(-180deg);
                }
            }
        }
        .arrow-button {
            display: block;
            margin-left: 2px;
        }
        .value {
            text-align: left;
        }

        .arrow {
            display: flex;
            padding-top: 2px;
            padding-bottom: 2px;
            padding-left: 2px;
            font-size: 12px;
            cursor: pointer;
        }
    }
}
</style>
