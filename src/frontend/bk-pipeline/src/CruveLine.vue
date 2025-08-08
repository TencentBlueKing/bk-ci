<template>
    <svg
        xmlns="http://www.w3.org/2000/svg"
        xmlns:xlink="http://www.w3.org/1999/xlink"
        :width="width"
        :height="svgHeight"
    >
        <path
            v-bind="$attrs"
            :d="d"
        />
    </svg>
</template>

<script>
    export default {
        props: {
            width: {
                type: Number,
                default: 100
            },
            height: {
                type: Number,
                default: 100
            },
            cruveRadius: {
                type: Number,
                default: 5
            },
            direction: {
                type: Boolean,
                default: true
            },
            distance: {
                type: Number,
                default: 5
            },
            straight: {
                type: Boolean
            }
        },
        computed: {
            svgHeight () {
                return this.height + this.cruveRadius * 2
            },
            cruveD () {
                const isPositive = this.direction ? 1 : -1
                const cruveRadius = this.cruveRadius * isPositive
                const startPoint = [this.direction ? 0 : this.width, 2]
                const endPoint = [this.direction ? this.width : 2, this.height + this.cruveRadius]
                const distance = startPoint[0] + this.distance * isPositive
                const QPoint1 = [distance + cruveRadius, startPoint[1], distance + cruveRadius, startPoint[1] + this.cruveRadius]
                const QPoint2 = [QPoint1[0], endPoint[1], QPoint1[0] + cruveRadius, endPoint[1]]
                return `M ${startPoint.join(' ')} L ${distance} ${startPoint[1]} Q ${QPoint1.join(' ')} L ${QPoint1[0]} ${this.height} Q ${QPoint2.join(' ')} L ${endPoint.join(' ')}`
            },
            straightD () {
                const isPositive = this.direction ? 1 : -1
                const cruveRadius = this.cruveRadius * isPositive
                const startPoint = [this.direction ? 1 : this.width - 1, 0]
                const endPoint = [this.direction ? this.width : 0, this.height + this.cruveRadius]
                const QPoint = [startPoint[0], endPoint[1], startPoint[0] + cruveRadius, endPoint[1]]
                return `M ${startPoint.join(' ')} L ${startPoint[0]} ${this.height} Q ${QPoint.join(' ')} L ${endPoint.join(' ')}`
            },
            d () {
                return this.straight ? this.straightD : this.cruveD
            }
        }
    }
</script>
