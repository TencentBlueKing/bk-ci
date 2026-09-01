<template>
    <!-- 凭证 -->
    <BaseResourceItem
        :key="item.copyStrategy"
        :item="item"
        :header-title="item.resourceName"
        :strategies="strategyOptions"
        :is-read-only="isReadOnly"
        @strategy-change="handleChange"
    >
        <!-- 使用插槽添加额外的凭据选择配置 -->
        <template #extra-config>
            <div
                v-if="isReplaceOther && !isReadOnly"
                class="credential-select-wrapper"
            >
                <label class="credential-label">
                    {{ $t('targetProjectCredential') }}
                    <span class="required-star">*</span>
                </label>
                <bk-select
                    v-model="item.targetResourceId"
                    :placeholder="$t('selectCredentialPlaceholder')"
                    searchable
                    @change="handleCredentialSelectChange"
                >
                    <bk-option
                        v-for="option in credentialOptions"
                        :key="option.credentialId"
                        :id="option.credentialId"
                        :name="option.credentialName"
                    />
                </bk-select>
            </div>
        </template>
    </BaseResourceItem>
</template>

<script>
    import BaseResourceItem from './BaseResourceItem.vue'
    import { PipelineCopyStrategy } from '@/store/modules/crossProjectCopy/constants'

    export default {
        name: 'CredentialResourceItem',
        components: {
            BaseResourceItem
        },
        props: {
            // 凭据数据项
            item: {
                type: Object,
                required: true
            },
            // 是否只读模式
            isReadOnly: {
                type: Boolean,
                default: false
            },
            // 目标项目凭据选项列表
            credentialOptions: {
                type: Array,
                default: () => []
            }
        },
        computed: {
            isReplaceOther () {
                return this.item.copyStrategy === PipelineCopyStrategy.CREDENTIAL_REPLACE_TARGET
            },
            strategyOptions () {
                return [
                    {
                        value: PipelineCopyStrategy.CREDENTIAL_REUSE_SAME_NAME,
                        label: this.$t('reuseTargetCredential'),
                        description: this.$t('reuseTargetCredentialDesc'),
                        disabled: !this.item.targetNameExists,
                        disabledTip: !this.item.targetNameExists ? this.$t('noSameNameCredential') : '',
                        showJumpIcon: this.item.targetNameExists
                    },
                    {
                        value: PipelineCopyStrategy.CREDENTIAL_REPLACE_TARGET,
                        label: this.$t('replaceWithOtherCredential'),
                        description: this.$t('replaceWithOtherCredentialDesc'),
                        disabled: false
                    },
                    {
                        value: PipelineCopyStrategy.CREDENTIAL_CREATE_NEW,
                        label: this.$t('copyCredential'),
                        description: this.$t('copyCredentialDesc'),
                        disabled: false,
                        highRisk: true
                    }
                ]
            }
        },
        methods: {
            handleChange (value) {
                this.$emit('strategy-change', value)
            },
            handleCredentialSelectChange (selectedCredentialId) {
                this.$emit('credential-select-change', selectedCredentialId)
            }
        }
    }
</script>

<style lang="scss" scoped>
    @import '@/scss/resource-dependency';
</style>
