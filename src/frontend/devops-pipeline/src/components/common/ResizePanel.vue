<template>
    <transition name="resize-panel-slide">
        <div
            v-if="visible"
            :class="['resize-panel', `resize-panel--${placement}`, extCls]"
            :style="panelStyle"
        >
            <div
                class="resize-panel__drag-handle"
                @mousedown.prevent="handleDragStart"
            >
                <i class="devops-icon icon-drag resize-panel__drag-icon" />
            </div>
            <div class="resize-panel__inner">
                <div
                    v-if="$slots.header"
                    class="resize-panel__header"
                >
                    <slot name="header" />
                </div>
                <div class="resize-panel__body">
                    <div class="resize-panel__content">
                        <slot name="content" />
                    </div>
                    <div
                        v-if="$slots.footer"
                        class="resize-panel__footer"
                    >
                        <slot name="footer" />
                    </div>
                </div>
            </div>
        </div>
    </transition>
</template>

<script>
    import { computed, ref, watch } from 'vue'

    export default {
        name: 'ResizePanel',
        props: {
            visible: {
                type: Boolean,
                default: false
            },
            width: {
                type: Number,
                default: 480
            },
            minWidth: {
                type: Number,
                default: 320
            },
            maxWidth: {
                type: Number,
                default: 800
            },
            placement: {
                type: String,
                default: 'right',
                validator: val => ['right', 'left'].includes(val)
            },
            extCls: {
                type: String,
                default: ''
            }
        },
        emits: ['update:visible'],
        setup (props, { emit }) {
            const currentWidth = ref(props.width)

            watch(() => props.width, (val) => {
                currentWidth.value = val
            })

            const panelStyle = computed(() => ({
                width: `${currentWidth.value}px`
            }))

            const isDragging = ref(false)
            let startX = 0
            let startWidth = 0

            function handleDragStart (e) {
                isDragging.value = true
                startX = e.clientX
                startWidth = currentWidth.value
                document.addEventListener('mousemove', handleDragMove)
                document.addEventListener('mouseup', handleDragEnd)
                document.body.style.cursor = 'col-resize'
                document.body.style.userSelect = 'none'
            }

            function handleDragMove (e) {
                if (!isDragging.value) return
                const diff = props.placement === 'right'
                    ? startX - e.clientX
                    : e.clientX - startX
                const newWidth = Math.min(
                    props.maxWidth,
                    Math.max(props.minWidth, startWidth + diff)
                )
                currentWidth.value = newWidth
            }

            function handleDragEnd () {
                isDragging.value = false
                document.removeEventListener('mousemove', handleDragMove)
                document.removeEventListener('mouseup', handleDragEnd)
                document.body.style.cursor = ''
                document.body.style.userSelect = ''
            }

            return {
                currentWidth,
                panelStyle,
                handleDragStart
            }
        }
    }
</script>

<style lang="scss" scoped>
    @import "@/scss/conf";

    .resize-panel {
        position: absolute;
        top: 0;
        bottom: 0;
        z-index: 10;
        background: #FAFBFD;
        box-shadow: -2px 0 6px rgba(0, 0, 0, 0.12);
        display: flex;

        &--right {
            right: 0;
        }

        &--left {
            left: 0;
        }
    }

    .resize-panel__drag-handle {
        width: 3px;
        cursor: col-resize;
        position: relative;
        z-index: 1;
        flex-shrink: 0;
        display: flex;
        align-items: center;
        transition: background-color 0.2s;

        &:hover,
        &:active {
            background-color: $primaryColor;
            .resize-panel__drag-icon {
                color: $primaryColor;
            }
        }
    }

    .resize-panel__drag-icon {
        font-size: 16px;
        color: #C4C6CC;
        transition: color 0.2s;

    }

    .resize-panel__inner {
        flex: 1;
        display: flex;
        flex-direction: column;
        min-width: 0;
        overflow: hidden;
    }

    .resize-panel__header {
        display: flex;
        align-items: center;
        padding: 0 24px;
        height: 48px;
        line-height: 48px;
        color: #313238;
        background: #FAFBFD;
        border-bottom: 1px solid $borderColor;
        flex-shrink: 0;
        
    }

    .resize-panel__body {
        flex: 1;
        overflow-y: auto;
        display: flex;
        flex-direction: column;
    }

    .resize-panel__footer {
        display: flex;
        align-items: center;
        padding: 12px 24px;
        gap: 8px;
        border-top: 1px solid $borderColor;
        background: #FAFBFD;
        flex-shrink: 0;
        position: sticky;
        bottom: 0;
    }

    .resize-panel-slide-enter-active,
    .resize-panel-slide-leave-active {
        transition: transform 0.25s ease;
    }

    .resize-panel-slide-enter-from,
    .resize-panel-slide-leave-to {
        transform: translateX(100%);
    }
</style>
