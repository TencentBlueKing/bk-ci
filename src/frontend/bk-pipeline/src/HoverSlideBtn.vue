<template>
    <span
        class="hover-slide-btn"
        :style="btnStyle"
        v-on="$listeners"
    >
        <span class="hover-slide-btn__badge">
            <slot name="badge">
                <Logo
                    v-if="icon"
                    :name="icon"
                    :size="iconSize"
                />
            </slot>
        </span>
        <span class="hover-slide-btn__text">
            <slot />
        </span>
    </span>
</template>

<script>
    import Logo from './Logo'

    export default {
        name: 'HoverSlideBtn',
        components: { Logo },
        props: {
            color: {
                type: String,
                default: '#e1ecff'
            },
            textColor: {
                type: String,
                default: '#3c96ff'
            },
            icon: {
                type: String,
                default: ''
            },
            iconSize: {
                type: [String, Number],
                default: 10
            },
            width: {
                type: [String, Number],
                default: 48
            },
            height: {
                type: [String, Number],
                default: 40
            },
            badgeSize: {
                type: [String, Number],
                default: 16
            }
        },
        computed: {
            btnStyle () {
                const w = typeof this.width === 'number' ? `${this.width}px` : this.width
                const h = typeof this.height === 'number' ? `${this.height}px` : this.height
                const bs = typeof this.badgeSize === 'number' ? `${this.badgeSize}px` : this.badgeSize
                return {
                    '--slide-btn-color': this.color,
                    '--slide-btn-text-color': this.textColor,
                    '--slide-btn-width': w,
                    '--slide-btn-height': h,
                    '--slide-btn-badge-size': bs
                }
            }
        }
    }
</script>

<style lang="scss">
.hover-slide-btn {
    position: absolute;
    display: inline-flex;
    overflow: hidden;
    width: var(--slide-btn-width);
    height: var(--slide-btn-height);
    right: 0;
    cursor: pointer;

    .hover-slide-btn__badge {
        position: absolute;
        right: 0;
        bottom: 0;
        display: inline-flex;
        align-items: center;
        justify-content: center;
        width: var(--slide-btn-badge-size);
        height: var(--slide-btn-badge-size);
        border-radius: 6px 0 0 0;
        background-color: var(--slide-btn-color);
        color: var(--slide-btn-text-color);
        z-index: 2;
        transition: opacity 0.25s ease;
    }

    .hover-slide-btn__text {
        display: inline-flex;
        align-items: center;
        justify-content: center;
        width: 100%;
        height: 100%;
        background-color: var(--slide-btn-color);
        color: var(--slide-btn-text-color);
        white-space: nowrap;
        font-size: 12px;
        z-index: 1;
        transform: translateX(100%);
        transition: transform 0.3s cubic-bezier(0.4, 0, 0.2, 1);
    }

    &:hover {
        .hover-slide-btn__badge {
            opacity: 0;
        }

        .hover-slide-btn__text {
            transform: translateX(0);
        }
    }
}
</style>
