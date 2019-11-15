<template>
    <bk-date-picker
        class="atom-date-picker"
        :type="dateType"
        :value="date"
        :disabled="disabled"
        :start-date="start"
        @change="handleDateChange">
    </bk-date-picker>
</template>

<script>
    import atomFieldMixin from '../atomFieldMixin'
    import { convertTime } from '@/utils/util'
    export default {
        name: 'atom-date-picker',
        mixins: [atomFieldMixin],
        props: {
            timer: {
                type: Boolean,
                default: false
            },
            startDate: {
                type: String,
                default: '2019-08-01'
            }
        },
        computed: {
            msValue () {
                return this.value ? this.value * 1000 : null
            },
            date () {
                return this.msValue ? convertTime(this.msValue).split(' ')[0] : ''
            },
            dateType () {
                return this.timer ? 'datetime' : 'date'
            }
        },
        methods: {
            handleDateChange (date) {
                const timeStamp = +new Date(date)
                if (timeStamp === this.msValue) {
                    return
                }
                this.handleChange(this.name, Math.floor(timeStamp / 1000))
            }
        }
    }
</script>

<style lang="scss">
    
</style>
