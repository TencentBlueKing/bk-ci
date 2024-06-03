<template>
    <span :class="{
        'pipeline-status-icon': true,
        'spin-icon': isRunning
    }">
        <logo :name="logoName" />
    </span>
</template>

<script>
    import { statusIconMap, statusAlias } from '@/utils/pipelineStatus'
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
        computed: {
            logoName () {
                return statusIconMap[this.status || statusAlias.UNEXEC]
            },
            isRunning () {
                return this.logoName === 'circle-2-1'
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
