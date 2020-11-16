<template>
    <bk-dialog
        :value="isShow"
        :width="480"
        :auto-close="false"
        :show-footer="false"
        :title="$t('newlist.chooseExport')"
        @cancel="handleCancel">
        <ul class="export-list">
            <li v-for="exportItem in exportList" :key="exportItem.exportUrl" class="export-item">
                <svg class="export-icon">
                    <use :xlink:href="`#icon-${exportItem.icon}`"></use>
                </svg>
                <h5 class="export-title">{{ exportItem.title }}</h5>
                <p class="export-tip">{{ exportItem.tips }}<a :href="exportItem.tipsLink" v-if="exportItem.tipsLink" target="_blank">{{ $t('newlist.knowMore') }}</a></p>
                <a target="_blank" download :href="exportItem.exportUrl" class="export-button">{{ $t('newlist.exportPipelineJson') }}</a>
            </li>
        </ul>
    </bk-dialog>
</template>

<script>
    import { PROCESS_API_URL_PREFIX } from '@/store/constants'

    export default {
        props: {
            isShow: Boolean
        },

        computed: {
            projectId () {
                return this.$route.params.projectId
            },

            pipelineId () {
                return this.$route.params.pipelineId
            },

            exportList () {
                return [
                    {
                        title: 'Pipeline Json',
                        icon: 'export-pipeline',
                        tips: this.$t('newlist.exportJsonTip'),
                        exportUrl: `${AJAX_URL_PIRFIX}/${PROCESS_API_URL_PREFIX}/user/pipelines/${this.pipelineId}/projects/${this.projectId}/export`
                    }
                ]
            }
        },

        methods: {
            handleCancel () {
                this.$emit('update:isShow', false)
            }
        }
    }
</script>

<style lang="scss" scoped>
    .export-list {
        padding: 0px 18px 16px;
        display: flex;
        justify-content: space-around;
        font-size: 12px;
        .export-item {
            width: 240px;
            height: 360px;
            display: flex;
            flex-direction: column;
            align-items: center;
            padding: 37px 30px 30px;
            border-radius: 2px;
            .export-icon {
                width: 64px;
                height: 64px;
                margin-bottom: 16px;
            }
            .export-title {
                font-weight: 600;
                font-size: 16px;
                line-height: 24px;
                margin-bottom: 8px;
                margin-top: 0;
            }
            .export-tip {
                flex: 1;
                color: #63656e;
                line-height: 20px;
                a {
                    color: #3a84ff;
                }
            }
            .export-button {
                width: 180px;
                height: 32px;
                text-align: center;
                line-height: 30px;
                border: 1px solid #c4c6cc;
                color: #63656e;
                border-radius: 2px;
            }
        }
        .export-item:hover {
            background: #f5f6fa;
            .export-button {
                background: #3a84ff;
                border: 1px solid #3a84ff;
                color: #ffffff;
            }
        }
    }
</style>
