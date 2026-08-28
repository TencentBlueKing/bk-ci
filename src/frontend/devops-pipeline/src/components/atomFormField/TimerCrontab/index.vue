<template>
    <div class="timer-crontab">
        <bk-crontab
            v-model="cronValue"
            :local="curLocal"
            :language-package="languagePackage"
            :shortcuts="shortcuts"
            @change="handleChangeCron"
            @error="handleError"
        />
        <p
            v-if="showError && hasInternalError"
            class="error-msg"
        >
            {{ $t('cron.errorTips') }}
        </p>
        <div
            v-if="nextRuns.length"
            class="cron-next-runs"
        >
            <p class="cron-next-runs-title">
                {{ $t('cron.nextRuns', [resolvedTz]) }}
            </p>
            <ul>
                <li
                    v-for="(ts, idx) in nextRuns"
                    :key="idx"
                >
                    <time-display
                        :value="ts"
                        :time-zone="resolvedTz"
                    />
                </li>
            </ul>
        </div>
    </div>
</template>
<script>
    import CronExpression from 'cron-parser-custom'
    import BkCrontab from '@blueking/crontab/vue2'
    import '@blueking/crontab/vue2/vue2.css'
    import TimeDisplay from '../../../../../common-lib/time-display'
    import { DEFAULT_USER_TIME_ZONE, getUserTimeZone } from '../../../../../common-lib/time'
    export default {
        components: {
            BkCrontab,
            TimeDisplay
        },
        props: {
            value: {
                type: String,
                default: ['0 0 1 * *']
            },
            name: {
                type: String,
                default: ''
            },
            handleChange: {
                type: Function,
                default: () => {}
            },
            /** IANA 时区，用于下次执行预览；空则用户时区，再空则东八区。不传给 crontab@0.0.2-beta.7 */
            timeZone: {
                type: String,
                default: ''
            },
            keepError: {
                type: Boolean,
                default: true
            },
            showError: {
                type: Boolean,
                default: false
            }
        },
        data () {
            return {
                hasInternalError: false,
                internalValue: '' // 保存组件内部的值（包括错误的值）
            }
        },
        computed: {
            cronValue: {
                get () {
                    if (this.hasInternalError && this.keepError) {
                        return this.internalValue
                    }
                    return Array.isArray(this.value) ? this.value.join('') : ''
                },
                set (val) {
                    this.internalValue = val
                }
            },
            curLocal () {
                const localeAliasMap = {
                    'zh-CN': 'zh-CN',
                    'en-US': 'en',
                    'ja-jp': 'ja-jp'
                }
                return localeAliasMap[this.$i18n.locale] || 'zh-CN'
            },
            languagePackage () {
                return {
                    'ja-jp': {
                        '下次': '次回',
                        '分': '分',
                        '时': '時',
                        '日': '日',
                        '月': '月',
                        '周': '週',
                        '每分钟': '1 分ごと',
                        '每小时': '1 時間ごと',
                        '每天': '1 日ごと',
                        '每月': '1 か月ごと',
                        '每周': '1 週間ごと',
                        '快捷选项': 'クイックオプション',
                        '任意值': '任意の値',
                        '枚举值': '列挙値',
                        '范围值': '範囲値',
                        '每隔一段时间': '一定間隔で',
                        '允许值': '許可される値',
                        '每分钟执行': '1 分ごとに実行',
                        '每小时执行': '1 時間ごとに実行',
                        '每天执行': '1 日ごとに実行',
                        '每月执行': '1 か月ごとに実行',
                        '每周执行': '1 週間ごとに実行',
                        '5,10：5 号和 10 号执行': '5, 10：5 日と 10 日に実行',
                        '1-10：从 1 号到 10 号周期执行': '1-10：1 日から 10 日まで周期的に実行',
                        '5/10：从 5 号开始，每隔 10 天执行': '5/10：5 日から開始し、10 日ごとに実行',
                        'mon-sun': '月曜日-日曜日',
                        '5,7：周五和周日执行': '5, 7：金曜日と日曜日に実行',
                        '1-5：从周一到周五周期执行': '1-5：月曜日から金曜日まで周期的に実行',
                        '2/2：从周二开始，每隔 2 天执行': '2/2：火曜日から開始し、2 日ごとに実行',
                        '5,10：5 点和 10 点执行': '5, 10：5 時と 10 時に実行',
                        '0-10：从 0 点到 10 点周期执行': '0-10：0 時から 10 時まで周期的に実行',
                        '5/10：从 5 点开始，每隔 10 小时执行': '5/10：5 時から開始し、10 時間ごとに実行',
                        '5,10：第 5 分钟和第 10 分钟执行': '5, 10：5 分と 10 分に実行',
                        '0-10：从 0 分钟到 10 分钟周期执行': '0-10：0 分から 10 分まで周期的に実行',
                        '5/10：从第 5 分钟开始，每隔 10 分钟执行': '5/10：5 分から開始し、10 分ごとに実行',
                        '1-10：从 1 月到 10 月周期执行': '1-10：1 月から 10 月まで周期的に実行',
                        '5/10：从 5 月开始，每隔 10 个月执行': '5/10：5 月から開始し、10 か月ごとに実行',
                        '5,10：5 月和 10 月执行': '5, 10：5 月と 10 月に実行'
                    }
                }
            },
            resolvedTz () {
                return this.timeZone || getUserTimeZone() || DEFAULT_USER_TIME_ZONE
            },
            nextRuns () {
                const expr = this.cronValue
                if (!expr || expr === 'error' || this.hasInternalError) {
                    return []
                }
                try {
                    const sixField = expr.trim().split(/\s+/).length === 5 ? `0 ${expr.trim()}` : expr.trim()
                    const interval = CronExpression.parse(sixField, { tz: this.resolvedTz })
                    return Array.from({ length: 5 }, () => {
                        const next = interval.next()
                        return next && typeof next.getTime === 'function' ? next.getTime() : null
                    }).filter(Boolean)
                } catch (e) {
                    return []
                }
            },
            shortcuts () {
                return [
                    {
                        label: this.$t('cron.everyMinute'),
                        value: '* * * * *'
                    },
                    {
                        label: this.$t('cron.everyHour'),
                        value: '0 * * * *'
                    },
                    {
                        label: this.$t('cron.everyDay'),
                        value: '0 0 * * *'
                    },
                    {
                        label: this.$t('cron.everyMonth'),
                        value: '0 0 1 * *'
                    },
                    {
                        label: this.$t('cron.everyWeek'),
                        value: '0 0 * * 0'
                    }
                ]
            }
        },
        methods: {
            handleChangeCron (value) {
                if (!value) {
                    this.hasInternalError = true
                    this.handleChange(this.name, 'error')
                    return
                }
                this.hasInternalError = false
                this.handleChange(this.name, [value])
            },
            handleError () {
                if (!this.internalValue && Array.isArray(this.value) && this.value.length > 0) {
                    // 如果internalValue为空，尝试从value中获取
                    this.internalValue = this.value.join('')
                }
                // 标记组件有格式错误
                this.hasInternalError = true
                // 触发 crontabArrayRule 校验失败
                this.handleChange(this.name, 'error')
            }
        }
    }
</script>

<style lang="scss">
    .timer-crontab {
        .error-msg {
            color: #ff4d4f;
            font-size: 12px;
            margin-top: 4px;
        }
        .cron-next-runs {
            margin-top: 8px;
            font-size: 12px;
            color: #63656e;
            line-height: 20px;
        }
        .cron-next-runs-title {
            color: #979ba5;
            margin-bottom: 2px;
        }
    }
</style>