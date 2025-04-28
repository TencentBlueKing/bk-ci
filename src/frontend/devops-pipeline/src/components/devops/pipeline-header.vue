<template id="subHeader">
    <section class="sub-header">
        <div class="sub-header-left">
            <slot name="left">
                <span
                    class="default-logo fl"
                >
                    <logo
                        size="24"
                        name="pipeline"
                        slot="logo"
                    />
                </span>

                <span class="default-title fl">
                    <bk-breadcrumb
                        separator-class="devops-icon icon-angle-right"
                    >
                        <bk-breadcrumb-item
                            class="pipeline-breadcrumb-item"
                            :to="pipelineListRoute"
                        >
                            {{ $t('pipeline') }}
                        </bk-breadcrumb-item>
                        <bk-breadcrumb-item
                            v-if="title"
                            class="pipeline-breadcrumb-item"
                        >
                            {{ title }}
                        </bk-breadcrumb-item>
                    </bk-breadcrumb>
                </span>
            </slot>
        </div>
        <div class="sub-header-right">
            <slot name="right">
                <more-route />
            </slot>
        </div>
    </section>
</template>

<script>
    import Logo from '@/components/Logo'
    import MoreRoute from '@/components/MoreRoute'
    import { getCacheViewId } from '@/utils/util'
    export default {
        components: {
            MoreRoute,
            Logo
        },
        props: {
            title: {
                type: String,
                default: ''
            }
        },
        computed: {
            viewId () {
                return getCacheViewId(this.$route.params.projectId)
            },
            pipelineListRoute () {
                return {
                    name: 'PipelineManageList',
                    params: {
                        viewId: this.viewId,
                        ...this.$route.params
                    }
                }
            }
        }
    }
</script>

<style lang="scss">
    @import './../../scss/conf';

    .sub-header {
        width: 100%;
        height: 48px;
        padding: 0 30px;
        box-shadow: 0 2px 5px rgba(0, 0, 0, .03);
        display: flex;
        justify-content: space-between;
        align-items: center;
        background: #fff;
        flex-shrink: 0;
        &-left {
            display: flex;
            flex: 1;
            align-items: center;
        }
        .fl {
            display: flex;
            align-items: center;
        }
        &-right {
            display: flex;
            flex: 1;
            max-width: fit-content;
            height: 100%;
            align-items: center;
        }
        .default-logo {
            margin-right: 10px;
        }
        .pipeline-breadcrumb-item {
            display: flex;
            align-items: center;
            .devops-icon.icon-angle-right {
                font-size: 12px;
                align-self: center;
            }
        }
        .default-title {
            flex: 1;
            letter-spacing: .5px;
            color: #333948;
        }
    }
</style>
