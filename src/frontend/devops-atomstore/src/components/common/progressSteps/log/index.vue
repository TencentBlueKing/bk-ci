<template>
    <ul class="progress-log">
        <li class="log-item" v-for="(log, index) in computedLogs" :key="index">
            [{{index + 1}}] - <span v-if="log.time">{{log.time|timeFilter}}</span>
            <span :style="`color: ${log.color}`"> {{log.message}}</span>
        </li>
    </ul>
</template>

<script>
    import ansiParse from './ansiParse.js'

    function prezero (num) {
        num = Number(num)
        if (num < 10) return '0' + num
        return num
    }

    function millisecond (num) {
        num = Number(num)
        if (num < 10) return '00' + num
        else if (num < 100) return '0' + num
        return num
    }

    export default {
        filters: {
            timeFilter (val) {
                if (!val) return ''
                const time = new Date(val)
                return `${time.getFullYear()}-${prezero(time.getMonth() + 1)}-${prezero(time.getDate())} ${prezero(time.getHours())}:${prezero(time.getMinutes())}:${prezero(time.getSeconds())}:${millisecond(time.getMilliseconds())}`
            }
        },

        props: {
            logs: {
                type: Array
            }
        },

        computed: {
            computedLogs () {
                return this.logs.map((log) => {
                    const val = log.message
                    const parseRes = ansiParse(val) || [{ message: '', hasHandle: false }]
                    const res = { message: '', time: log.timestamp }
                    parseRes.forEach((item) => {
                        res.message += item.message
                        if (!res.color && item.color) res.color = item.color
                    })
                    this.$nextTick(() => {
                        const ele = document.querySelector('.progress-log')
                        ele.scrollTop = ele.scrollHeight
                    })
                    return res
                })
            }
        }
    }
</script>

<style lang="scss" scoped>
    @import '@/assets/scss/conf.scss';

    .progress-log {
        background: #f5f5f5;
        border: 1px solid #d9d9d9;
        height: 100%;
        width: 100%;
        overflow: auto;
        padding: 16px;
        .log-item {
            font-family: Consolas, "Courier New", monospace;
            width: 100%;
            word-break: break-all;
            font-size: 12px;
            line-height: 22px;
            color: $grayBlack;
            &:hover {
                background: #3330303d;
            }
        }
    }
</style>
