<template>
  <svg :width="size" :height="size">
    <title v-if="title">{{ title }}</title>
    <use v-bind="{ 'xlink:href': `#${name}` }"></use>
  </svg>
</template>

<script>
export default {
  name: 'SvgLogo',
  props: {
    name: String,
    size: {
      type: [String, Number],
      default: 24,
    },
    title: {
      type: String,
    },
  },
};
</script>
