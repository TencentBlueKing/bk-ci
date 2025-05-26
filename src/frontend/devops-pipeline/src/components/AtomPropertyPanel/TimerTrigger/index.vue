<template>
    <div>
        <component
            :is="timerTriggerComponent"
            v-bind="$attrs"
        />
    </div>
</template>

<script>
    import TimerTriggerV1 from './V1'
    import TimerTriggerV2 from './V2'
    export default {
        name: 'timer-trigger',
        props: {
            atom: Object
        },
        computed: {
            isTimerTriggerV1 () {
                return this.atom.version.startsWith('1.')
            },
            timerTriggerComponent () {
                return this.isTimerTriggerV1 ? TimerTriggerV1 : TimerTriggerV2
            }
        }
    }
</script>
