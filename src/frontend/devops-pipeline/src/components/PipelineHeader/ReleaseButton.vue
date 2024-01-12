<template>
    <div style="height: 100%" @click="showReleaseSlider">
        <span :class="['publish-pipeline-btn', {
            'publish-diabled': !canRelease
        }]">
            <i class="devops-icon icon-check-small" />
            {{ $t('release') }}
        </span>
        <ReleasePipelineSideSlider
            v-model="isReleaseSliderShow"
            :version="currentVersion"
            :base-version-name="baseVersionName"
            :version-name="versionName"
        />
    </div>
</template>

<script>
    import { mapState, mapActions } from 'vuex'
    import ReleasePipelineSideSlider from './ReleasePipelineSideSlider'
    export default {
        components: {
            ReleasePipelineSideSlider
        },
        props: {
            canRelease: {
                type: Boolean,
                required: true
            }
        },
        data () {
            return {
                isReleaseSliderShow: false
            }
        },
        computed: {
            ...mapState('atom', [
                'pipelineInfo',
                'showVariable'
            ]),
            currentVersion () {
                return this.pipelineInfo?.version ?? ''
            },
            versionName () {
                return this.pipelineInfo?.versionName ?? '--'
            },
            baseVersionName () {
                return this.pipelineInfo?.baseVersionName ?? '--'
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
