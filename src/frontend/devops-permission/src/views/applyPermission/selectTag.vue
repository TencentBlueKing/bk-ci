<template>
  <div class="select-tag" @click.stop="handleTagClick">
    <span class="tag-label">{{ tagInfo.name + '：' }}</span>
    <div v-show="!isEditing">
      <span class="tag-value">
        {{ localValue }}
      </span>
      <span @click="handleTagClear" class="permission-icon permission-icon-close-samll close-icon"></span>
    </div>
  </div>
</template>

<script>
  export default {
    props: {
      tagInfo: {
        type: Object,
        default: () => ({})
      },
      list: {
        type: Array,
        default: () => ([])
      }
    },
      data () {
        return {
          localValue: '',
        }
      },
      watch: {
        tagInfo: {
          handler (val) {
            const { values, id } = this.tagInfo
            const value = values.map(item => ['actionId', 'resourceType'].includes(id) ? item.name : item).join('')
            this.localValue = value
          },
          deep: true,
          immediate: true
        }
      },
      methods: {
        handleTagClear () {
          this.$emit('handleTagClear', this.tagInfo.id)
        }
      }
  }
</script>

<style lang="postcss" scoped>
  .select-tag {
    position: relative;
    display: flex;
    margin: 4px 0 4px 5px;
    padding: 0 5px;
    min-height: 22px;
    line-height: 22px;
    background: #f0f1f5;
    border-radius: 2px;
    color: #63656e;
    .tag-label {
      flex-shrink: 0;
      align-self: start;
    }
    .tag-value {
      display: inline-block;
      line-height: 22px;
      margin-right: 22px;
    }
    .close-icon {
      position: absolute;
      top: 4px;
      right: 4px;
      color: #979ba5;
      font-size: 14px;
      font-weight: 700;
      cursor: pointer;
    }
  }
</style>
