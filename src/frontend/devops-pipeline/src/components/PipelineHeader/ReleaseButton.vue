<template>
    <div style="height: 100%">
        <span v-bk-tooltips="disableTooltips" :class="['publish-pipeline-btn', {
            'publish-diabled': !canRelease
        }]" @click="showReleaseSlider" v-perm="{
            hasPermission: canEdit,
            disablePermissionApi: true,
            permissionData: {
                projectId,
                resourceType: 'pipeline',
                resourceCode: pipelineId,
                action: RESOURCE_ACTION.EDIT
            }
        }">
            <i class="devops-icon icon-check-small" />
            {{ $t('release') }}
        </span>
        <ReleasePipelineSideSlider v-model="isReleaseSliderShow" :version="currentVersion"
            :draft-base-version-name="draftBaseVersionName" />
    </div>
</template>

<script>
    import {
        RESOURCE_ACTION
    } from '@/utils/permission'
    import { mapActions, mapGetters, mapState } from 'vuex'
    import ReleasePipelineSideSlider from './ReleasePipelineSideSlider'
    export default {
        components: {
            ReleasePipelineSideSlider
        },
        props: {
            projectId: {
                type: String,
                required: true
            },
            pipelineId: {
                type: String,
                required: true
            },
            canRelease: {
                type: Boolean,
                required: true
            }
        },
        data () {
            return {
                RESOURCE_ACTION,
                isReleaseSliderShow: false
            }
        },
        computed: {
            ...mapState('atom', [
                'pipelineInfo',
                'showVariable'
            ]),
            ...mapGetters({
                draftBaseVersionName: 'atom/getDraftBaseVersionName'
            }),
            disableTooltips () {
                return {
                    content: this.$t('alreadyReleasedTips'),
                    disabled: this.canRelease
                }
            },
            canEdit () {
                return this.pipelineInfo?.permissions?.canEdit ?? true
            },
            currentVersion () {
                return this.pipelineInfo?.version ?? ''
            }
        },
        methods: {
            ...mapActions('atom', [
                'setShowVariable'
            ]),
            showReleaseSlider () {
                if (this.canRelease) {
                    this.setShowVariable(false)
                    this.isReleaseSliderShow = true
                }
            }
        }
    }
</script>

<style lang="scss">
@import "@/scss/conf";

.publish-pipeline-btn {

    display: flex;
    height: 100%;
    padding: 0 20px;
    background: $primaryColor;
    align-items: center;
    color: white;
    cursor: pointer;
    font-size: 14px;

    &.publish-diabled {
        background: #DCDEE5;
        cursor: not-allowed;
    }

    .icon-check-small {
        font-size: 18px;
    }

    &.disabled {
        background: #DCDEE5;
        cursor: not-allowed;
    }
}
</style>
