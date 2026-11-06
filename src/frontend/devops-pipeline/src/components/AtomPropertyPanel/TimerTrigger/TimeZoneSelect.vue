<template>
    <bk-select
        :value="value"
        :disabled="disabled"
        :clearable="false"
        :searchable="true"
        :placeholder="placeholder"
        @selected="handleSelected"
    >
        <bk-option
            v-for="tz in timeZoneOptions"
            :key="tz"
            :id="tz"
            :name="tz"
        />
    </bk-select>
</template>

<script>
    import { DEFAULT_USER_TIME_ZONE, getUserTimeZone } from '../../../../../common-lib/time'

    function listIanaTimeZones () {
        try {
            if (typeof Intl !== 'undefined' && typeof Intl.supportedValuesOf === 'function') {
                return Intl.supportedValuesOf('timeZone')
            }
        } catch (e) {
            // ignore
        }
        return [
            'Asia/Shanghai',
            'Asia/Hong_Kong',
            'Asia/Tokyo',
            'Asia/Singapore',
            'Asia/Seoul',
            'UTC',
            'Europe/London',
            'Europe/Berlin',
            'Europe/Moscow',
            'America/New_York',
            'America/Los_Angeles',
            'America/Chicago',
            'Australia/Sydney'
        ]
    }

    export default {
        name: 'timer-time-zone-select',
        props: {
            name: {
                type: String,
                default: 'timeZone'
            },
            value: {
                type: String,
                default: ''
            },
            disabled: {
                type: Boolean,
                default: false
            },
            handleChange: {
                type: Function,
                required: true
            },
            placeholder: {
                type: String,
                default: ''
            }
        },
        data () {
            return {
                timeZoneOptions: listIanaTimeZones()
            }
        },
        created () {
            if (!this.value) {
                const defaultTz = getUserTimeZone() || DEFAULT_USER_TIME_ZONE
                this.handleChange(this.name, defaultTz)
            }
        },
        methods: {
            handleSelected (val) {
                this.handleChange(this.name, val)
            }
        }
    }
</script>
