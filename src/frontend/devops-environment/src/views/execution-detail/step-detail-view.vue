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
                    // this.stepInfo = await this.getStepInstanceDetail({
                    //     projectId: this.projectId,
                    //     stepInstanceId: this.stepInstanceId,
                    //     jobInstanceId: this.jobInstanceId
                    // })
                    this.stepInfo = {
                        id: 0,
                        type: 2,
                        name: '快速执行分发文件_20231219174253621',
                        fileStepInfo: {
                            fileSourceList: [
                                {
                                    fileType: 1,
                                    fileList: [
                                        '/asdwdaw.txt'
                                    ],
                                    server: {
                                        variable: 'string',
                                        hostList: [
                                            {
                                                bkHostId: 0,
                                                bkCloudId: 0,
                                                ip: '9.146.98.65',
                                                ipv6: 'fe80:0000:0000:0000:5054:00ff:fe79:8098'
                                            },
                                            {
                                                bkHostId: 0,
                                                bkCloudId: 0,
                                                ip: '9.146.98.65',
                                                ipv6: 'fe80:0000:0000:0000:5054:00ff:fe79:8098'
                                            },
                                            {
                                                bkHostId: 0,
                                                bkCloudId: 0,
                                                ip: '9.146.98.65',
                                                ipv6: 'fe80:0000:0000:0000:5054:00ff:fe79:8098'
                                            }, {
                                                bkHostId: 0,
                                                bkCloudId: 0,
                                                ip: '9.146.98.65',
                                                ipv6: 'fe80:0000:0000:0000:5054:00ff:fe79:8098'
                                            }, {
                                                bkHostId: 0,
                                                bkCloudId: 0,
                                                ip: '9.146.98.65',
                                                ipv6: 'fe80:0000:0000:0000:5054:00ff:fe79:8098'
                                            }, {
                                                bkHostId: 0,
                                                bkCloudId: 0,
                                                ip: '9.146.98.65',
                                                ipv6: 'fe80:0000:0000:0000:5054:00ff:fe79:8098'
                                            }, {
                                                bkHostId: 0,
                                                bkCloudId: 0,
                                                ip: '9.146.98.65',
                                                ipv6: 'fe80:0000:0000:0000:5054:00ff:fe79:8098'
                                            }, {
                                                bkHostId: 0,
                                                bkCloudId: 0,
                                                ip: '9.146.98.65',
                                                ipv6: 'fe80:0000:0000:0000:5054:00ff:fe79:8098'
                                            }, {
                                                bkHostId: 0,
                                                bkCloudId: 0,
                                                ip: '9.146.98.65',
                                                ipv6: 'fe80:0000:0000:0000:5054:00ff:fe79:8098'
                                            }, {
                                                bkHostId: 0,
                                                bkCloudId: 0,
                                                ip: '9.146.98.65',
                                                ipv6: 'fe80:0000:0000:0000:5054:00ff:fe79:8098'
                                            }, {
                                                bkHostId: 0,
                                                bkCloudId: 0,
                                                ip: '9.146.98.65',
                                                ipv6: 'fe80:0000:0000:0000:5054:00ff:fe79:8098'
                                            }, {
                                                bkHostId: 0,
                                                bkCloudId: 0,
                                                ip: '9.146.98.65',
                                                ipv6: 'fe80:0000:0000:0000:5054:00ff:fe79:8098'
                                            }, {
                                                bkHostId: 0,
                                                bkCloudId: 0,
                                                ip: '9.146.98.65',
                                                ipv6: 'fe80:0000:0000:0000:5054:00ff:fe79:8098'
                                            }, {
                                                bkHostId: 0,
                                                bkCloudId: 0,
                                                ip: '9.146.98.65',
                                                ipv6: 'fe80:0000:0000:0000:5054:00ff:fe79:8098'
                                            }, {
                                                bkHostId: 0,
                                                bkCloudId: 0,
                                                ip: '9.146.98.65',
                                                ipv6: 'fe80:0000:0000:0000:5054:00ff:fe79:8098'
                                            }, {
                                                bkHostId: 0,
                                                bkCloudId: 0,
                                                ip: '9.146.98.65',
                                                ipv6: 'fe80:0000:0000:0000:5054:00ff:fe79:8098'
                                            }, {
                                                bkHostId: 0,
                                                bkCloudId: 0,
                                                ip: '9.146.98.65',
                                                ipv6: 'fe80:0000:0000:0000:5054:00ff:fe79:8098'
                                            }, {
                                                bkHostId: 0,
                                                bkCloudId: 0,
                                                ip: '9.146.98.65',
                                                ipv6: 'fe80:0000:0000:0000:5054:00ff:fe79:8098'
                                            }, {
                                                bkHostId: 0,
                                                bkCloudId: 0,
                                                ip: '9.146.98.65',
                                                ipv6: 'fe80:0000:0000:0000:5054:00ff:fe79:8098'
                                            }, {
                                                bkHostId: 0,
                                                bkCloudId: 0,
                                                ip: '9.146.98.65',
                                                ipv6: 'fe80:0000:0000:0000:5054:00ff:fe79:8098'
                                            }, {
                                                bkHostId: 0,
                                                bkCloudId: 0,
                                                ip: '9.146.98.65',
                                                ipv6: 'fe80:0000:0000:0000:5054:00ff:fe79:8098'
                                            }
                                        ],
                                        topoNodeList: [
                                            {
                                                nodeType: 'string',
                                                id: 0
                                            }
                                        ],
                                        dynamicGroupList: [
                                            {
                                                id: 'string'
                                            }
                                        ]
                                    },
                                    account: {
                                        id: 0,
                                        name: 'root'
                                    },
                                    fileSourceId: 0
                                }
                            ],
                            fileDestination: {
                                path: '/fwafwafwf',
                                account: {
                                    id: 0,
                                    name: 'root'
                                },
                                server: {
                                    variable: 'string',
                                    hostList: [
                                        {
                                            bkHostId: 0,
                                            bkCloudId: 0,
                                            ip: 'string',
                                            ipv6: 'string'
                                        }
                                    ],
                                    topoNodeList: [
                                        {
                                            nodeType: 'string',
                                            id: 0
                                        }
                                    ],
                                    dynamicGroupList: [
                                        {
                                            id: 'string'
                                        }
                                    ]
                                }
                            },
                            timeout: 7000,
                            sourceSpeedLimit: 0,
                            destinationSpeedLimit: 0,
                            transferMode: 2
                        }
                    }
                } catch (err) {
                    console.error(err)
                } finally {
                    this.isLoading = false
                }
            }
        }
    }
  </script>
