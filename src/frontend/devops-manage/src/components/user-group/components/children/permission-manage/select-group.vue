<template>
  <bk-select
    class="select"
    :model-value="value"
    :prefix="prefix"
    @change="handleChange"
    multiple
    filterable
    collapse-tags
    show-select-all
    multiple-mode="tag"
  >
    <bk-option
      v-for="item in options"
      :id="item.value"
      :key="item.value"
      :name="item.label"
    />
  </bk-select>
</template>

<script setup>
import { defineProps, defineEmits } from 'vue';

defineProps({
  value: {
    type: Array,
    required: true,
  },
  options: {
    type: Array,
    required: true,
  },
  prefix: {
    type: String,
    default: '',
  },
});

const emit = defineEmits(['handleChange']);

function handleChange(newValue) {
  emit('handleChange', newValue);
}
</script>

<style scoped lang="scss">
.select{
  margin-right: 8px;
  width: 200px !important;
}
</style>
