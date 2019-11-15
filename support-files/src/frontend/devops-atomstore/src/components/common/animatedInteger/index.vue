<template>
    <span>{{formatValue}}</span>
</template>

<script>
    export default {
        props: {
            value: {
                required: true,
                type: Number
            },

            digits: {
                type: Number,
                default: 0
            }
        },

        data () {
            return {
                tweeningValue: 0
            }
        },

        computed: {
            formatValue () {
                return Number(this.tweeningValue).toFixed(this.digits)
            }
        },

        watch: {
            value (newValue, oldValue) {
                this.tween(oldValue, newValue)
            }
        },

        mounted () {
            this.tween(0, this.value)
        },

        methods: {
            tween (startValue, endValue) {
                // 错误数据返回0
                if (Number.isNaN(+endValue)) {
                    return 0
                }

                const dis = Math.abs(endValue - startValue)
                const isPositive = endValue - startValue > 0 ? 1 : -1
                const ticDis = Math.ceil((dis / 30) * (10 ** this.digits)) / (10 ** this.digits)
                const ticTimes = Math.ceil(dis / ticDis)
                const gapTime = 25 / ticTimes
                let tickGap = 1

                const animate = () => {
                    if (tickGap < gapTime) {
                        requestAnimationFrame(animate)
                        tickGap++
                        return
                    }

                    this.tweeningValue += (ticDis * isPositive)

                    const isUnDone = isPositive === 1 ? this.tweeningValue < endValue : this.tweeningValue > endValue
                    if (isUnDone) {
                        requestAnimationFrame(animate)
                        tickGap = 1
                    } else this.tweeningValue = endValue

                    this.tweeningValue = +Number(this.tweeningValue).toFixed(this.digits)
                }

                animate()
            }
        }
    }
</script>
