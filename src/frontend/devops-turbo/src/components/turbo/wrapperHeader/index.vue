<template>
        <div class="wrapper-header">
            <div class="title">{{ title }}</div>
            <header-process></header-process>
        </div>
</template>

<script>
    import headerProcess from '@/components/turbo/headerProcess'

    export default {
        props: {
            title: {
                type: String,
                default: ''
            }
        },
        components: {
            headerProcess
        }
    }
</script>

<style lang="scss">

</style>


