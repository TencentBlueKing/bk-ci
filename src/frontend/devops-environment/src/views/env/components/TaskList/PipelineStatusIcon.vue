<template>
    <span
        :class="{
            'pipeline-status-icon': true,
            'spin-icon': isRunning,
            'hourglass-queue': isEnqueue
        }"
    >
        <logo
            :name="logoName"
            :size="12"
        />
    </span>
</template>

<script>
    import { computed } from 'vue'
    import { statusIconMap } from '@/utils/pipelineStatus'
    import Logo from '@/components/Logo'
    export default {
        name: 'pipeline-status-icon',
        components: {
            Logo
        },
        props: {
            status: {
                type: String,
                default: ''
            }
        },
        setup (props) {
            const logoName = computed(() => statusIconMap[props.status])
            const isRunning = computed(() => logoName.value === 'circle-2-1')
            const isEnqueue = computed(() => logoName.value === 'hourglass')
            return {
                logoName,
                isRunning,
                isEnqueue
            }
        }
    }
</script>

<style lang="scss" scoped>
    .pipeline-status-icon {
        display: flex;
        font-size: 22px;
        width: 22px;
        height: 22px;
        align-items: center;
        justify-content: center;
        &.spin-icon {
            color: #3A84FF;
        }

    }
</style>
