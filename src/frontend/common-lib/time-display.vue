<template>
    <span
        v-if="hasValue"
        v-bk-tooltips="tooltipConfig"
        class="bk-time-display"
    >{{ displayTime }}</span>
    <span
        v-else
        class="bk-time-display"
    >--</span>
</template>

<script>
    import {
        formatByUserTz,
        formatTimezoneTooltip,
        getUserTimeZone,
        toEpochMilli
    } from './time'

    export default {
        name: 'BkTimeDisplay',
        props: {
            /** Unix epoch millis (preferred), seconds, or parseable date string */
            value: {
                type: [Number, String],
                default: null
            },
            /** IANA timezone; default getUserTimeZone() */
            timeZone: {
                type: String,
                default: ''
            },
            /** Display / tooltip time pattern */
            format: {
                type: String,
                default: 'YYYY-MM-DD HH:mm:ss'
            },
            placements: {
                type: Array,
                default: () => ['top']
            }
        },
        computed: {
            resolvedTz () {
                return this.timeZone || getUserTimeZone()
            },
            hasValue () {
                return toEpochMilli(this.value) !== null
            },
            displayTime () {
                if (!this.hasValue) return '--'
                return formatByUserTz(this.value, this.resolvedTz, this.format)
            },
            tooltipContent () {
                return formatTimezoneTooltip(this.value, this.resolvedTz, this.format)
            },
            tooltipConfig () {
                return {
                    content: this.tooltipContent,
                    placements: this.placements,
                    allowHTML: false
                }
            }
        }
    }
</script>

<style scoped>
    .bk-time-display {
        display: inline-block;
        white-space: nowrap;
    }
</style>
