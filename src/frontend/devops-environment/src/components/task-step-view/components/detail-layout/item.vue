<template>
    <div
        class="detail-item"
        :class="`design-layout-${layout}`">
        <div class="detail-label">
            <span>{{ label }}</span>
        </div>
        <div class="detail-content">
            <slot />
        </div>
    </div>
</template>
<script>
    export default {
        name: 'BkciDetailLayoutItem',
        props: {
            label: {
                type: String,
                default: ''
            },
            layout: {
                type: String,
                default: '' // horizontal, vertical
            }
        }
    }
</script>
<style lang='scss' scoped>
  .detail-item {
    display: flex;
    font-size: 14px;
    line-height: 32px;
    color: #313238;
    align-items: flex-start;
    justify-content: flex-end;

    &.design-layout-horizontal {
      flex-direction: row !important;
    }

    &.design-layout-vertical {
      flex-direction: column !important;

      .detail-content {
        width: 100%;
      }
    }
  }

  .detail-label {
    display: flex;
    padding-right: 2px;
    color: #63656e;
    text-align: right;
    word-break: keep-all;
    white-space: pre;
    align-items: center;
    justify-content: flex-end;
    flex: 0 0 auto;
  }

  .detail-content {
    flex: 1;
    color: #63656e;
    word-break: break-all;
  }
</style>
