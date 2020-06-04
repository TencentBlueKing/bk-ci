<template>
    <div :class="['corner', position, `corner-${theme}`]" :style="{ width: `${width}px`, height: `${height}px` }">
        <div class="triangle" :style="triangleStyle"></div>
        <template v-if="$slots.content">
            <div class="content">
                <slot name="content"></slot>
            </div>
        </template>
        <template v-else>
            <em>{{text}}</em>
        </template>
    </div>
</template>

<script>
    import { mapState } from 'vuex'

    export default {
        name: 'corner',
        props: {
            width: {
                type: Number,
                default: 38
            },
            height: {
                type: Number,
                default: 38
            },
            text: {
                type: String,
                default: 'new'
            },
            position: {
                type: String,
                default: 'top-left',
                validator (value) {
                    if (['top-left', 'top-right', 'bottom-right', 'bottom-left'].indexOf(value) === -1) {
                        console.error(`position property is not valid: '${value}'`)
                        return false
                    }
                    return true
                }
            },
            theme: {
                type: String,
                default: 'default',
                validator (value) {
                    if (['default', 'primary', 'warning', 'success', 'danger'].indexOf(value) < 0) {
                        console.error(`theme property is not valid: '${value}'`)
                        return false
                    }
                    return true
                }
            }
        },
        data () {
            return {}
        },
        computed: {
            ...mapState([
                'toolMeta'
            ]),
            supportLangs () {
                const toolLang = this.tool.lang
                const names = this.toolMeta.LANG.map(lang => {
                    if (lang.key & toolLang) {
                        return lang.name
                    }
                }).filter(name => name)
                return names.join('、')
            },
            triangleStyle () {
                const { width, height, position } = this
                const style = {
                    'top-left': { borderWidth: `${height}px ${width}px 0 0` },
                    'top-right': { borderWidth: `0 ${width}px ${height}px 0` },
                    'bottom-right': { borderWidth: ` 0 0 ${height}px ${width}px` },
                    'bottom-left': { borderWidth: `${height}px 0 0 ${width}px` }
                }

                return style[position]
            }
        }
    }
</script>

<style lang="postcss">
    @import '../../css/mixins.css';
    $defaultColor: #007bff;
    $primaryColor: #699df4;
    $warningColor: #ffb848;
    $successColor: #45e35f;
    $dangerColor: #ea3636;

    @define-mixin theme $theme: default, $border-color {
        &.corner-$(theme) {
            .triangle {
                border-color: $border-color;
            }
        }
    }
    .corner {
        width: 46px;
        height: 46px;
        position: absolute;
        font-size: 12px;
        color: #fff;

        em {
            font-size: 12px;
            font-style: normal;
            text-align: center;
            position: absolute;
        }
        .content {
            position: absolute;
        }

        .triangle {
            width: 0;
            height: 0;
            border-style: solid;
            position: absolute;
        }

        &.top-left {
            left: -1px;
            top: -1px;
            .triangle {
                border-width: 1px 1px 0 0;
                border-color: $defaultColor transparent transparent transparent;
            }

            .content,
            em {
                left: 0;
                top: 4px;
            }

            em {
                transform: rotate(-45deg) scale(0.9);
            }

            @mixin theme default, $defaultColor transparent transparent transparent;
            @mixin theme primary, $primaryColor transparent transparent transparent;
            @mixin theme warning, $warningColor transparent transparent transparent;
            @mixin theme success, $successColor transparent transparent transparent;
            @mixin theme danger, $dangerColor transparent transparent transparent;
        }

        &.top-right {
            right: -1px;
            top: -1px;
            .triangle {
                border-width: 0 1px 1px 0;
                border-color: transparent $defaultColor transparent transparent;
            }

            .content,
            em {
                right: 0;
                top: 4px;
            }

            em {
                transform: rotate(45deg) scale(0.9);
            }

            @mixin theme default, transparent $defaultColor transparent transparent;
            @mixin theme primary, transparent $primaryColor transparent transparent;
            @mixin theme warning, transparent $warningColor transparent transparent;
            @mixin theme success, transparent $successColor transparent transparent;
            @mixin theme danger, transparent $dangerColor transparent transparent;
        }

        &.bottom-right {
            right: -1px;
            bottom: -1px;
            .triangle {
                border-width: 0 0 1px 1px;
                border-color: transparent transparent $defaultColor transparent;
            }

            .content,
            em {
                right: 0;
                bottom: 4px;
            }

            em {
                transform: rotate(-45deg) scale(0.9);
            }

            @mixin theme default, transparent transparent $defaultColor transparent;
            @mixin theme primary, transparent transparent $primaryColor transparent;
            @mixin theme warning, transparent transparent $warningColor transparent;
            @mixin theme success, transparent transparent $successColor transparent;
            @mixin theme danger, transparent transparent $dangerColor transparent;
        }

        &.bottom-left {
            left: -1px;
            bottom: -1px;
            .triangle {
                border-width: 1px 0 0 1px;
                border-color: transparent transparent transparent $defaultColor;
            }

            .content,
            em {
                left: 0;
                bottom: 4px;
            }

            em {
                transform: rotate(45deg) scale(0.9);
            }

            @mixin theme default, transparent transparent transparent $defaultColor;
            @mixin theme primary, transparent transparent transparent $primaryColor;
            @mixin theme warning, transparent transparent transparent $warningColor;
            @mixin theme success, transparent transparent transparent $successColor;
            @mixin theme danger, transparent transparent  transparent $dangerColor;
        }
    }
</style>
