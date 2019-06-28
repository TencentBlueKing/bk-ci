<template>
    <bk-date-picker
        class="plugin-date-picker"
        v-bind="config"
        :value="date"
        :disabled="disabled"
        @change="handleDateChange">
    </bk-date-picker>
</template>

<script>
    import mixins from '../mixins'
    import { convertTime } from '@/utils/util'
    export default {
        name: 'date-picker',
        mixins: [mixins],
        props: {
            datePickerConf: {
                type: Object
            }
        },
        computed: {
            config () {
                return {
                    format: 'yyyy-MM-dd',
                    type: 'date',
                    startDate: new Date(),
                    readonly: false,
                    ...this.datePickerConf
                }
            },
            date () {
                return this.value ? convertTime(this.value) : ''
            }
        },
        methods: {
            handleDateChange (date) {
                const timeStamp = +new Date(date)
                if (timeStamp === this.value) {
                    return
                }
                this.handleChange(this.name, timeStamp)
            }
        }
    }
</script>
