<template>
    <resize-layout :container-width="containerWidth">
        <template #aside>
            <group-aside />
        </template>
        <template #content>
            <router-view />
        </template>
    </resize-layout>
</template>

<script>
    import GroupAside from './group_aside.vue'
    import ResizeLayout from '@/components/ResizeLayout.vue'

    export default {
        name: 'EnvIndex',
        components: {
            GroupAside,
            ResizeLayout
        },
        props: {
            containerWidth: {
                type: Number
            }
        }
    }
</script>

<style lang="scss" scoped>

</style>
