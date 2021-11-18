<template>
    <div class='turbo-main'>
        总览
    </div>
</template>
s

<script>
    import { mapState, mapActions } from 'vuex'
    
    export default {
        name: 'turbo-main',
        computed: {
            ...mapState('turbo', [
                'turbo'
            ])
        },
        methods: {
            ...mapActions('turbo', [
                'testAction'
            ])
        }
    }
</script>

<style lang="scss">
    .turbo-main {
        min-height: 100%;
        padding: 20px 30px 0;
    }
</style>


