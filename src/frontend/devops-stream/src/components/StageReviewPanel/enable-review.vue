<template>
    <bk-radio-group class="stage-review-radio-group" v-model="stageControl.manualTrigger">
        <bk-radio :disabled="true" :value="false">{{$t('pipeline.automation')}}</bk-radio>
        <bk-radio :disabled="true" :value="true" style="marginLeft:55px">{{$t('pipeline.manualApprovals')}}</bk-radio>
    </bk-radio-group>
</template>

<script>
    export default {
        props: {
            stageControl: {
                type: Object,
                default: () => ({})
            }
        }
    }
</script>
