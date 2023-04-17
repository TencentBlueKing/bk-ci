<script lang="ts" setup>
import IamIframe from '@/components/IAM-Iframe';

const urlPerfix = window.BK_ITSM_URL_PREFIX;
</script>

<template>
  <iam-iframe
    path="#/ticket/approval-iframe"
    :url-perfix="urlPerfix"
  />
</template>
