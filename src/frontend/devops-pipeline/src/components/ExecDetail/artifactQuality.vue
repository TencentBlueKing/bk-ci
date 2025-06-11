<template>
    <ul>
        <li
            class="quality-tags"
            v-for="(colorList, title) in data"
            :key="title"
            @click="goOutputs(title)"
        >
            <span class="tag-label">{{ title }}</span>
            <div
                v-for="(tag, index) in colorList"
                :key="index"
                class="color-item"
            >
                <span
                    :style="{ backgroundColor: tag.color }"
                    class="color-block"
                >{{ tag.count }}</span>
            </div>
        </li>
    </ul>
</template>

<script>
    export default {
        props: {
            data: {
                type: Object,
                default: () => { }
            }
        },
        methods: {
            goOutputs (title) {
                this.$emit('goOutputs', title)
            }
        }
    }
</script>

<style lang="scss" scoped>
ul {
  flex: 1;
  display: flex;
  flex-wrap: wrap;
  margin: 0;
  padding: 0;
  list-style-type: none;

  .quality-tags {
    display: inline-flex;
    align-items: center;
    margin: 3px 12px 3px 0;
    margin-right: 12px;
    padding: 4px 8px;
    background: #FAFBFD;
    border: 1px solid #DCDEE5;
    border-radius: 2px;
    cursor: pointer;

    .tag-label {
      color: #4D4F56;
    }

    .color-item {
      margin-left: 3px;
      position: relative;
    }

    .color-block {
      display: inline-block;
      padding: 0 4px;
      height: 16px;
      color: #FFF;
      border-radius: 11px;

      transition: background-color 0.3s ease;

      &:hover {
        opacity: 0.8;
      }
    }

    &:hover {
      background-color: #F0F1F5;
    }
  }
}
</style>
