<script lang="ts" setup>
import IamIframe from '@/components/IAM-Iframe';

import {
  computed,
} from 'vue';
import { useRoute } from 'vue-router';
const route = useRoute();
const applyId = computed(() => route.params.applyId || '')
</script>

<template>
  <iam-iframe
    path="apply"
    :query="{
      applyId
    }"
  />
</template>
