<template>
    <bk-dialog
        v-model="visible"
        header-position="left"
        :title="$t('pipelineVersionChnaged', [buildNum])"
        :width="640"
        @confirm="handleClose"
        :before-close="handleClose"
    >
        <div class="build-pipeline-version-diff-dialog">
            <h3>
                {{ $t('templateDiffs') }}
            </h3>
            <p class="version-diff-area">
                {{ currVersionName }}
                <span class="bk-icon icon-arrows-right-shape"></span>
                {{ lastVersionName }}

                <bk-button
                    text
                    class="check-diff-btn"
                    theme="primary"
                    size="small"
                >
                    {{ $t('checkDiff') }}
                </bk-button>
            </p>

            <h3>
                {{ $t('templateVersionDiff') }}
            </h3>
            <p class="ref-template-diff-table-title">
                {{ $t('refTemplateDiffTitle') }}
            </p>
            <bk-table :data="buildVersionDiffs">
                <bk-table-column
                    :label="$t('refTemplate')"
                    prop="templateName"
                />
                <bk-table-column
                    :label="$t('preRef')"
                    prop="preRef"
                />
                <bk-table-column
                    :label="$t('currentRef')"
                    prop="currentRef"
                />
                <bk-table-column
                    :label="$t('operation')"
                >
                    <bk-button
                        text
                        theme="primary"
                        size="small"
                    >
                        {{ $t('checkDiff') }}
                    </bk-button>
                </bk-table-column>
            </bk-table>
        </div>
    </bk-dialog>
</template>


<script>
    import { defineComponent } from 'vue'

    export default defineComponent({
        name: 'VersionDiffDialog',
        props: {
            buildNum: {
                type: String,
                required: true,
            },
            visible: {
                type: Boolean,
                required: false,
            },
        },
        emits: ['update:visible'],
        setup (props, { emit }) {
            const handleClose = () => {
                emit('update:visible', false)
            }
            const currVersionName = 'V8 (P8.T7.5)'
            const lastVersionName = 'V5 (P2.T3.3)'
            const buildVersionDiffs = [
                {
                    templateName: '模板A',
                    preRef: 'V8 (P8.T7.5)',
                    currentRef: 'V5 (P2.T3.3)',
                },
                {
                    templateName: '模板B',
                    preRef: 'commitId1',
                    currentRef: 'commitId2',
                },
            ]

            return {
                buildVersionDiffs,
                currVersionName,
                lastVersionName,
                handleClose,
            }
        },
    })
</script>

<style lang="scss">
    .build-pipeline-version-diff-dialog {
        > h3 {
            font-size: 14px;
            color: #4D4F56;
            line-height: 22px;
            margin: 24px 0 8px 0;
            display: flex;
            align-items: center;
            &:before {
                content: '';
                display: inline-block;
                width: 4px;
                height: 16px;
                background: #3A84FF;
                border-radius: 0 2px 2px 0;
                margin-right: 8px;
            }
        }
        .version-diff-area {
            display: flex;
            background: #F5F7FA;
            border-radius: 2px;
            align-items: center;
            padding: 16px 24px;
            font-size: 12px;
            .icon-arrows-right-shape {
                margin: 0 18px;
                color: #F59500;
            }
            .check-diff-btn {
                margin-left: auto;
                justify-self: flex-end;
            }
        }
        .ref-template-diff-table-title {
            margin-bottom: 8px;
            color: #4D4F56;
            font-size: 12px;
            line-height: 20px;
        }
    }
</style>