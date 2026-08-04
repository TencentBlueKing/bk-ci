<template>
    <span
        class="hover-slide-btn"
        :class="{
            'hover-slide-btn--group': isGroup
        }"
        :style="btnStyle"
        v-on="$listeners"
    >
        <template v-if="isGroup">
            <span
                v-for="action in actions"
                :key="action.key"
                class="hover-slide-btn__action"
                :style="getActionStyle(action)"
                :title="action.title || action.label"
                @click.stop="handleActionClick(action)"
            >
                <Logo
                    v-if="action.icon"
                    :name="action.icon"
                    :size="action.iconSize || iconSize"
                    class="hover-slide-btn__action-icon"
                />
                <span class="hover-slide-btn__action-text">{{ action.label }}</span>
            </span>
        </template>
        <span
            v-else
            class="hover-slide-btn__badge"
        >
            <slot name="badge">
                <Logo
                    v-if="icon"
                    :name="icon"
                    :size="iconSize"
                />
            </slot>
        </span>
        <span
            v-if="!isGroup"
            class="hover-slide-btn__text"
        >
            <slot />
        </span>
    </span>
</template>

<script>
    import Logo from './Logo'

    const toCssSize = value => typeof value === 'number' ? `${value}px` : value

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
            },
            actions: {
                type: Array,
                default: () => []
            },
            actionWidth: {
                type: [String, Number],
                default: 52
            }
        },
        computed: {
            isGroup () {
                return this.actions.length > 0
            },
            btnStyle () {
                return {
                    '--slide-btn-color': this.color,
                    '--slide-btn-text-color': this.textColor,
                    '--slide-btn-width': toCssSize(this.width),
                    '--slide-btn-height': toCssSize(this.height),
                    '--slide-btn-badge-size': toCssSize(this.badgeSize),
                    '--slide-btn-action-width': toCssSize(this.actionWidth)
                }
            }
        },
        methods: {
            getActionStyle (action) {
                return {
                    '--slide-btn-action-color': action.color,
                    '--slide-btn-action-text-color': action.textColor,
                    '--slide-btn-action-hover-color': action.hoverTextColor,
                    '--slide-btn-action-width': toCssSize(action.width || this.actionWidth)
                }
            },
            handleActionClick (action) {
                this.$emit('action-click', action)
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

    &.hover-slide-btn--group {
        right: 0;
        bottom: 0;
        align-items: center;
        width: auto;
        height: var(--slide-btn-badge-size);
        margin: 0;
        border-radius: 4px 0 0 0;
        white-space: nowrap;
        z-index: 3;
        transition: height .2s ease-in-out, border-radius .2s ease-in-out;

        .hover-slide-btn__action {
            display: inline-flex;
            flex: 0 0 var(--slide-btn-badge-size);
            align-items: center;
            justify-content: center;
            width: var(--slide-btn-badge-size);
            height: var(--slide-btn-badge-size);
            background-color: var(--slide-btn-action-color);
            color: var(--slide-btn-action-text-color);
            font-size: 12px;
            transition: all .2s ease-in-out;
        }

        .hover-slide-btn__action-icon {
            color: var(--slide-btn-action-text-color);
        }

        .hover-slide-btn__action-text {
            display: none;
            line-height: 20px;
        }
    }
}

.hover-slide-btn--group:hover {
    height: 100%;
    border-radius: 0;

    .hover-slide-btn__action {
        flex-basis: var(--slide-btn-action-width);
        width: var(--slide-btn-action-width);
        height: 100%;
    }

    .hover-slide-btn__action:hover {
        color: var(--slide-btn-action-hover-color);
    }

    .hover-slide-btn__action-icon {
        display: none;
    }

    .hover-slide-btn__action-text {
        display: inline;
    }
}
</style>
