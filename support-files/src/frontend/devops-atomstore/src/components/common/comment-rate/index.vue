<template>
    <p class="rate-stars">
        <svg v-for="(item, index) in maxStars"
            :key="index"
            :class="[{ yellow: index < Math.floor(rate) }, { isEdit: edit }, 'rate-star']"
            :style="{ width: width + 'px', height: height + 'px', 'min-width': width + 'px' }"
            x="0px"
            y="0px"
            viewBox="0 0 64 64"
            @click="chooseRate(index)"
        >
            <g transform="translate(-143.000000, -635.000000)">
                <g transform="translate(83.000000, 114.000000)">
                    <g transform="translate(15.000000, 384.000000)">
                        <g transform="translate(29.000000, 137.000000)">
                            <polygon class="st1" points="48,53 28.2,63.9 32,40.8 16,24.4 38.1,21 48,-0.1 57.8,21 79.9,24.4 63.9,40.8 67.7,63.9" />
                        </g>
                    </g>
                </g>
            </g>
        </svg>
    </p>
</template>

<script>
    export default {
        props: {
            rate: {
                type: Number,
                default: 0
            },
            width: {
                type: Number,
                default: 15
            },
            height: {
                type: Number,
                default: 16
            },
            edit: Boolean
        },

        data () {
            return {
                maxStars: new Array(5)
            }
        },

        methods: {
            chooseRate (index) {
                const rate = index + 1
                this.$emit('chooseRate', rate)
            }
        }
    }
</script>

<style lang="scss" scoped>
    @import '@/assets/scss/conf.scss';

    .rate-stars {
        .isEdit {
            cursor: pointer;
        }
        &:after {
            content: '';
            display: table;
            clear: both;
        }
        .rate-star {
            float: left;
            margin-right: 3px;
            enable-background: new 0 0 64 64;
            fill: $darkWhite;
            &.yellow {
                fill: $starYellow;
            }
            &:last-child {
                margin-right: 0;
            }
        }
    }
</style>
