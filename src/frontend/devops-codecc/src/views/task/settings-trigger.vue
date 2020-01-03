<template>
    <div>
        <div class="settings-trigger-header">
            <b>{{$t('scan.触发方式')}}</b>
        </div>
        <div class="settings-trigger-body">
            <bk-form :label-width="136" :model="formData">
                <bk-form-item :label="$t('scan.定时触发')">
                    <!-- 周选择器 -->
                    <div>
                        <ul>
                            <li
                                :class="formData.executeDate && formData.executeDate.includes(week.id) ? 'active' : ''"
                                @click="selectedWeek(week.id)"
                                class="settings-trigger-week"
                                v-for="week in weekList"
                                :key="week.label">
                                {{week.name}}
                            </li>
                        </ul>
                    </div>
                    <!-- /周选择器 -->
                </bk-form-item>
                <bk-form-item property="time" :required="true">
                    <bk-time-picker
                        style="width:272px"
                        v-model="formData.executeTime"
                        :placeholder="$t('scan.选择时间')"
                        :format="'HH:mm'">
                    </bk-time-picker>
                </bk-form-item>
                <bk-form-item>
                    <bk-button class="save-button" theme="primary" :title="$t('op.保存')" @click.stop.prevent="saveTime">{{$t('op.保存')}}</bk-button>
                </bk-form-item>
            </bk-form>
        </div>
    </div>
</template>

<script>
    import { mapState } from 'vuex'

    export default {
        components: {
        },
        data () {
            return {
                weekList: [
                    {
                        id: '1',
                        name: this.$t('scan.weekdays.一'),
                        label: 'Mon'
                    },
                    {
                        id: '2',
                        name: this.$t('scan.weekdays.二'),
                        label: 'Tues'
                    },
                    {
                        id: '3',
                        name: this.$t('scan.weekdays.三'),
                        label: 'Wed'
                    },
                    {
                        id: '4',
                        name: this.$t('scan.weekdays.四'),
                        label: 'Thur'
                    },
                    {
                        id: '5',
                        name: this.$t('scan.weekdays.五'),
                        label: 'Fri'
                    },
                    {
                        id: '6',
                        name: this.$t('scan.weekdays.六'),
                        label: 'Sat'
                    },
                    {
                        id: '7',
                        name: this.$t('scan.weekdays.日'),
                        label: 'Sun'
                    }
                ]
            }
        },
        computed: {
            ...mapState('task', {
                taskDetail: 'detail'
            }),
            formData () {
                const formData = {}
                if (!this.taskDetail.executeDate) {
                    this.taskDetail.executeDate = []
                }
                Object.assign(formData, this.taskDetail)
                return formData
            }
        },
        methods: {
            saveTime () {
                const data = {
                    executeTime: this.formData.executeTime,
                    executeDate: this.formData.executeDate
                }
                this.$store.dispatch('task/trigger', data).then(res => {
                    if (res === true) {
                        this.$bkMessage({ theme: 'success', message: this.$t('op.保存成功') })
                        this.$store.dispatch('task/detail', { showLoading: true })
                    }
                }).catch(e => {
                    this.$bkMessage({ theme: 'error', message: this.$t('op.保存失败') })
                    this.$store.dispatch('task/detail', { showLoading: true })
                })
            },
            // 保存选择的周
            selectedWeek (id) {
                if (!this.formData.executeDate.includes(id)) {
                    this.formData.executeDate.push(id)
                } else if (this.formData.executeDate.includes(id)) {
                    const i = this.formData.executeDate.indexOf(id)
                    this.formData.executeDate.splice(i, 1)
                }
            }
        }
    }
</script>

<style lang="postcss" scoped>
    @import '../../css/variable.css';
    /* 标题与分隔线 start */
    .settings-trigger-header {
        border-bottom: 1px solid $bgHoverColor;
        padding-bottom: 8px;
        margin-bottom: 19px;
        b {
            font-size: 14px;
            color: #63656e
        }
    }
    /* 标题与分隔线 end */
    .settings-trigger-body {
        /* 星期列表 start */
        .settings-trigger-week {
            margin-right: 8px;
            display: inline-block;
            width: 32px;
            height: 32px;
            border-radius: 2px;
            border: 1px solid $itemBorderColor;
            cursor: pointer;
            line-height: 32px;
            text-align: center;
        }
        /* 星期列表 end */
        .active {
            border: 1px solid $goingColor;
            color: $goingColor;
        }
        .save-button {
            width: 86px
        }
    }
</style>
