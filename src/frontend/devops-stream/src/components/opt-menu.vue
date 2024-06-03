<template>
    <bk-popover trigger="click" placement="bottom-end" theme="light" ext-cls="stream-dropdown" @click.native.stop>
        <i class="header-opt bk-icon icon-more"></i>
        <ul slot="content">
            <slot></slot>
        </ul>
    </bk-popover>
</template>

<style lang="postcss">
    .header-opt {
        font-size: 24px;
        padding: 3px;
        color: #979ba5;
        cursor: pointer;
        &:hover {
            border-radius: 100px;
            background: #f0f1f5;
        }
    }
    .stream-dropdown {
        .tippy-tooltip {
            padding: 6px 0;
            border-radius: 0;
            background: #ffffff;
            border: 1px solid #dcdee5;
            box-shadow: 0 3px 6px 0 rgba(0,0,0,0.10);
            border-radius: 2px;
            .tippy-arrow {
                display: none;
            }
            li {
                line-height: 32px;
                font-size: 12px;
                padding: 0 15px;
                cursor: pointer;
                color: #7b7d8a;
                min-width: 65px;
                &:hover {
                    background: #e1ecff;
                    color: #3a84ff;
                }
                &.disabled {
                    cursor: not-allowed;
                    background: #f5f7fa;
                    &:hover {
                        color: #7b7d8a;
                        background: #f5f7fa;
                    }
                }
            }
        }
    }
</style>
