<template>
    <resize-layout :container-width="containerWidth">
        <GroupAside slot="aside" />
        <router-view slot="content" />
    </resize-layout>
</template>

<script>
    import GroupAside from './group_aside.vue'
    import ResizeLayout from '@/components/ResizeLayout.vue'

    export default {
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
