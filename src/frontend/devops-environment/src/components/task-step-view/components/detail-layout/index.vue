<template>
    <div
        ref="detailLayout"
        class="bkci-detail-layout"
        :class="[mode, layout]">
        <slot />
    </div>
</template>
<script>
    export default {
        name: 'BkciDetailLayout',
        props: {
            mode: {
                type: String,
                default: 'normal' // normal, see
            },
            layout: {
                type: String,
                default: 'horizontal' // horizontal, vertical
            },
            isWarp: {
                type: Boolean
            }
        },
        created () {
            this.childrenNum = this.$slots.default
        },
        updated () {
            const childrenNum = this.$slots.default
            if (this.childrenNum !== childrenNum) {
                this.init()
                this.childrenNum = childrenNum
            }
        },
        mounted () {
            const isShowLayout = this.$refs.detailLayout.getBoundingClientRect().width > 0
            if (isShowLayout) {
                this.init()
            }
        },
        methods: {
            init () {
                if (this.layout === 'vertical') {
                    return
                }
                const $layoutEle = this.$refs.detailLayout
                const $layoutDetailList = $layoutEle.querySelectorAll('.detail-label')

                let max = 0
                $layoutDetailList.forEach((item) => {
                    const { width } = item.getBoundingClientRect()
                    max = Math.max(max, width)
                })
                $layoutDetailList.forEach((item) => {
                    item.style.width = `${max}px`
                })
            }
        }
    }
</script>
<style lang='scss'>
  .bkci-detail-layout {
    padding: 20px 30px;
    &.see {
      .detail-label {
        color: #b2b5bd;
      }
    }

    &.vertical {
      .detail-label {
        justify-content: flex-start;
      }

      .detail-item {
        flex-direction: column;
        align-items: stretch;
      }
    }
  }
</style>
