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
        height: calc(94.04vh - 50px);
        max-width: 13.1rem;
        overflow: auto;
    }
    .home-nav {
        display: flex;
        align-items: center;
        justify-content: center;
        .home-nav-tab {
            cursor: pointer;
            padding: 0 18px;
        }
        ::v-deep .bk-tab-header {
            background-color: #fff;
            height: 5.96vh !important;
            line-height: 5.96vh !important;
            background-image: none !important;
            .bk-tab-label-wrapper .bk-tab-label-list {
                height: 5.96vh !important;
                .bk-tab-label-item {
                    line-height: 5.96vh !important;
                    height: 5.96vh !important;
                    color: #63656e;
                    padding: 0;
                    &::after {
                        height: 3px;
                    }
                    &.active {
                        color: #3a84ff;
                    }
                    .bk-tab-label {
                        font-size: 16px;
                        width: 100%;
                    }
                }
            }
            .bk-tab-header-setting {
                height: 5.96vh !important;
                line-height: 5.96vh !important;
            }
        }
        ::v-deep .bk-tab-section {
            padding: 0;
        }
    }
</style>


