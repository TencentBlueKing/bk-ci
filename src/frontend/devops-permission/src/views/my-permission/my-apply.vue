<script lang="ts" setup>
import IamIframe from '@/components/IAM-Iframe';

import {
  computed,
} from 'vue';
import { useRoute } from 'vue-router';
const route = useRoute();
const query = <any>{};
const applyId = computed(() => route.params.applyId || '')
if (applyId.value) query.id = applyId.value
</script>

<template>
  <iam-iframe
    path="apply"
    :query="query"
  />
</template>
