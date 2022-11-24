<template>
    <section class="pipeline-group-section">
        <pipeline-group-aside />
        <router-view />
    </section>
</template>

<script>
    import PipelineGroupAside from './PipelineGroupAside'

    export default {
        components: {
            PipelineGroupAside
        }
    }
</script>

<style lang="scss">
    @import '@/scss/mixins/ellipsis';
    @import '@/scss/conf';

    .pipeline-group-section {
        flex: 1;
        display: flex;
        overflow: hidden;

        .flex-row {
            display: flex;
            align-items: center;
        }
        .pipeline-list-main {
            display: flex;
            flex-direction: column;
            flex: 1;
            padding: 24px;
            overflow: hidden;

            .current-pipeline-group-name {
                display: flex;
                align-items: center;
                font-size: 14px;
                line-height: 26px;
                margin: 0 0 16px 0;
                color: #313238;
                font-weight: normal;
                > :first-child {
                    margin: 0 8px 0 0;
                }
                > span {
                    font-weight: bold;

                }
            }
            .pipeline-list-main-header {
                display: flex;
                justify-content: space-between;
                margin-bottom: 16px;
                .pipeline-list-main-header-right-area {
                    flex: 1;
                    display: flex;
                    margin-left: 100px;
                    .search-pipeline-input {
                        flex: 1;
                        background: white;
                    }
                }
            }
            .pipeline-latest-exec-cell {
                display: flex;
                align-items: center;
                .pipeline-exec-status-icon {
                    display: inline-flex;
                    font-size: 22px;
                    margin-right: 10px;

                }
                .pipeline-exec-msg {
                    display: flex;
                    flex-direction: column;
                    font-size: 12px;
                    line-height: 20px;
                    margin-left: 12px;
                    overflow: hidden;
                    .desc {
                        color: #979BA5;
                    }
                    .pipeline-exec-msg-title {
                        @include ellipsis();
                        flex: 1;
                    }
                    .pipeline-exec-msg-desc {
                        display: grid;
                        column-gap: 16px;
                        grid-template-columns: auto auto auto;
                        > span {
                            display: flex;
                            align-items: center;
                            overflow: hidden;
                            > span {
                                display: flex;
                                @include ellipsis();
                                min-width: 0;
                            }
                        }
                    }
                }
            }
            .hidden {
                visibility: hidden;
            }
            .pipeline-operation-cell {
                display: flex;
                align-items: center;
                .pipeline-exec-btn {
                    min-width: 60px;
                    text-align: left;
                }
                .more-action-menu {
                    font-size: 0;
                    cursor: pointer;
                    .more-action-menu-trigger {
                        font-size: 18px;
                        padding: 0 6px;
                    }
                }
            }
            .pipeline-cell-link {
                color: $primaryColor;
                cursor: pointer;
                &:hover {
                    color: #699df4;
                }
                &[disabled] {
                    cursor: not-allowed;
                    color: $fontColor
                }
            }
        }
    }
</style>
