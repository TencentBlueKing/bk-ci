<template>
    <div class="bkci-collapse-item">
        <bk-collapse-item
            ref="collapseItem"
            v-bind="$attrs"
            hide-arrow
            v-on="$listeners">
            <template #default>
                <icon :name="iconType" size="14" />
                <span style="display: none;">{{ iconType }}</span>
                <slot />
            </template>
            <template #content>
                <slot name="content" />
            </template>
        </bk-collapse-item>
    </div>
</template>
  <script>
    export default {
        inject: ['collapse'],
        data () {
            return {
                iconType: 'arrow-full-right'
            }
        },
        mounted () {
            const unwatch = this.$watch(() => this.$refs.collapseItem.isActive, (newValue) => {
                this.iconType = newValue ? 'arrow-full-down' : 'arrow-full-right'
            }, {
                immediate: true
            })
            this.$once('hook:beforeDestroy', () => {
                unwatch()
            })
        }
    }
  </script>
  <style lang='scss'>
    .bk-collapse {
      border-bottom: 1px solid #dcdee5;
    }
  
    .bkci-collapse-item {
      .bk-collapse-item-active {
        .bk-collapse-item-header {
          border-bottom-color: #dcdee5;
        }
      }
  
      .bk-collapse-item-header {
        position: relative;
        overflow: initial;
        font-size: 12px;
        font-weight: 600;
        background: #fafbfd;
        border: 1px solid #dcdee5;
        border-bottom-color: transparent;
      }
  
      .bk-collapse-item .bk-collapse-item-content {
        padding: 0;
        border-right: 1px solid #dcdee5;
        border-left: 1px solid #dcdee5;
      }
    }
  
  </style>
