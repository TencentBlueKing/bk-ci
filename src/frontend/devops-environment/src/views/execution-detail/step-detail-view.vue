<template>
    <div
        v-bkloading="{ isLoading }"
        class="history-step-detail-view">
        <task-step-view
            :data="stepInfo">
        </task-step-view>
    </div>
</template>
  <script>
    import { mapActions } from 'vuex'
    import TaskStepView from '@/components/task-step-view/index'
    export default {
        name: '',
        components: {
            TaskStepView
        },
        props: {
            stepInstanceId: {
                type: Number
            },
            jobInstanceId: {
                type: Number
            },
            taskId: {
                type: Number,
                required: true
            },
            id: {
                type: [Number, String]
            }
        },
        data () {
            return {
                isLoading: true,
                stepInfo: {}
            }
        },
        computed: {
            projectId () {
                return this.$route.params.projectId
            }
        },
        created () {
            this.fetchStep()
        },
        methods: {
            ...mapActions('environment', [
                'getStepInstanceDetail'
            ]),
            //  步骤详情
            async fetchStep () {
                this.isLoading = true
                try {
                    this.stepInfo = await this.getStepInstanceDetail({
                        projectId: this.projectId,
                        stepInstanceId: this.stepInstanceId,
                        jobInstanceId: this.jobInstanceId
                    })
                } catch (err) {
                    console.error(err)
                } finally {
                    this.isLoading = false
                }
            }
        }
    }
  </script>
