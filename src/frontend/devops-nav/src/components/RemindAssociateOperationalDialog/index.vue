<template>
    <bk-dialog
        v-model="isShow"
        :mask-close="false"
        :esc-close="false"
        :close-icon="false"
        header-position="left"
        :width="700"
        :title="$t('关联运营产品')"
    >
        {{ $t('检测到您尚未将蓝盾项目关联运营产品，将影响正常使用，请立即前往关联。') }}
        <template slot="footer">
            <bk-button class="mr10" theme="primary" @click="handleToAssociate">{{ $t('立即前往') }}</bk-button>
            <bk-button icon-right="bk-icon icon-refresh" @click="handleCheckAssociate">
                {{ $t('已关联') }}
            </bk-button>
        </template>
    </bk-dialog>
</template>
<script>
    export default {
        name: 'ApplyProjectDialog',
        props: {
            isShow: Boolean
        },
        methods: {
            handleToAssociate () {
                this.$emit('to-associate')
            },
            handleCheckAssociate () {
                this.$emit('check-associate')
            }
        }
    }
</script>
