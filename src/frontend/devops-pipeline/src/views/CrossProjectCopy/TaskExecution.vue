<template>
    <div class="task-execution">
        <h3>{{ $t('taskExecution') }}</h3>
        <!-- 任务执行的内容 -->
        <div class="task-form">
            <div class="form-item">
                <label>{{ $t('executeOption') }}</label>
                <bk-radio-group
                    :value="formData.taskExecution.executeOption"
                    @change="handleUpdate('taskExecution', 'executeOption', $event)"
                >
                    <bk-radio value="immediate">{{ $t('executeImmediately') }}</bk-radio>
                    <bk-radio value="scheduled">{{ $t('executeScheduled') }}</bk-radio>
                </bk-radio-group>
            </div>
            <div
                class="form-item"
                v-if="formData.taskExecution.executeOption === 'scheduled'"
            >
                <label>{{ $t('scheduleTime') }}</label>
                <bk-date-picker
                    type="datetime"
                    :value="formData.taskExecution.scheduleTime"
                    @change="handleUpdate('taskExecution', 'scheduleTime', $event)"
                    :placeholder="$t('selectScheduleTime')"
                />
            </div>
        </div>
    </div>
</template>

<script>
    export default {
        name: 'TaskExecution',
        props: {
            formData: {
                type: Object,
                required: true
            }
        },
        methods: {
            handleUpdate (stepName, field, value) {
                this.$emit('update-form-data', stepName, field, value)
            }
        }
    }
</script>

<style lang="scss" scoped>
    .task-execution {
        padding: 20px;
    }
    .task-form {
        margin-top: 20px;
    }
    .form-item {
        margin-bottom: 20px;
        
        label {
            display: block;
            margin-bottom: 8px;
            font-weight: 500;
            color: #63656E;
        }
    }
</style>
