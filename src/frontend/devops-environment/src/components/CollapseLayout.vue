<template>
    <div
        ref="layoutRef"
        class="collapse-page-layout"
        :class="{
            'is-flod': flod
        }"
    >
        <div class="layout-left">
            <div
                class="left-wraper"
                :style="leftStyles"
            >
                <slot />
            </div>
        </div>
        <div
            v-if="flod"
            class="layout-right"
            :style="rightStyles"
        >
            <div
                class="right-wraper"
                :class="{ active: flod }"
            >
                <slot
                    v-if="flod"
                    name="flod"
                />
            </div>
        </div>
    </div>
</template>

<script>
    import useCollapseLayout from '@/hooks/useCollapseLayout'

    export default {
        name: 'CollapseLayoutV2',
        props: {
            // 本地存储的唯一标识，用于记忆折叠状态
            storageKey: {
                type: String,
                required: true
            },
            // 默认是否折叠
            defaultFlod: {
                type: Boolean,
                default: false
            }
        },
        setup (props) {
            const {
                layoutRef,
                flod,
                leftStyles,
                rightStyles,
                toggleFlod,
                setFlod,
                expand,
                collapse
            } = useCollapseLayout(props.storageKey, props.defaultFlod)

            return {
                layoutRef,
                flod,
                leftStyles,
                rightStyles,
                toggleFlod,
                setFlod,
                expand,
                collapse
            }
        }
    }
</script>

<style lang="scss" scoped>
    .collapse-page-layout {
        display: flex;
        flex: 1;
        min-height: 0;

        &.is-flod {
            ::v-deep .devops-codelib-table {
                .bk-table-row {
                    cursor: pointer;
                    &.active {
                        background: #eff5ff;
                    }
                }
            }
        }

        .layout-left {
            position: relative;
            z-index: 9;
            transition: all 0.15s;
            flex: 0 0 auto;
            min-height: 0;
            
            .left-wraper {
                height: calc(-192px + 100vh);
                min-height: 0;
                display: flex;
                flex-direction: column;
                overflow: hidden;
                transition: all 0.1s;
            }
            
            ::v-deep .bk-table {
                background-color: #fff;
            }
        }

        &.is-flod .layout-left .left-wraper {
            box-shadow: 0 2px 2px 0 rgba(0, 0, 0, 0.15);
        }

        .layout-right {
            position: relative;
            overflow: hidden;
            border-radius: 2px;
            transition: all 0.15s;
            flex: 1;
            box-shadow: 0 2px 2px 0 rgba(0, 0, 0, 0.15);
            
            .right-wraper {
                opacity: 0;
                height: 100%;
                transition: all 0.5s;

                &.active {
                    opacity: 1;
                }
            }
        }
    }
</style>
