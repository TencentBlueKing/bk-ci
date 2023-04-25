/**
 * normalKey: 灰色icon后的文字
 * normalVal: 灰色icon后的数字对应的key
 * activeKey: 高亮icon后的文字
 * activeVal: 高亮icon后的数字对应的key
 * mainKey: 圆环中的文字
 * mainVal: 圆环中数字对应的key
 * activeColor: 高亮icon的颜色
 * hasChange: 是否有变化的值
 * changeNormalVal: 灰色icon后变化的值对应的key
 * changeActiveVal: 高亮icon后变化的值对应的key
 * charts: 详情中图表类型及指定配置
 */

export default {
    COVERITY: {
        title: 'Coverity',
        normalLegend: [
            {
                key: '新增',
                value: 'latest_new_add_count'
            },
            {
                key: '关闭',
                value: 'latest_closed_count'
            }
        ],
        normalKey: '关闭',
        normalVal: 'latest_closed_count',
        activeKey: '新增',
        activeVal: 'latest_new_add_count',
        mainKey: '遗留告警数',
        mainVal: 'latest_exist_count',
        charts: [
            {
                type: 'pie',
                title: '所有告警状态',
                color: ['#ffb400', '#30d878', '#3c96ff', '#dde4eb'],
                opts: [
                    {
                        key: 'total_new',
                        text: '待修复'
                    },
                    {
                        key: 'total_close',
                        text: '已修复'
                    },
                    {
                        key: 'total_ignore',
                        text: '已忽略'
                    },
                    {
                        key: 'total_excluded',
                        text: '已屏蔽'
                    }
                ],
                enable: true
            },
            {
                type: 'bar',
                title: '待修复告警级别',
                opts: [
                    {
                        key: 'total_new_serious',
                        text: '严重'
                    },
                    {
                        key: 'total_new_normal',
                        text: '一般'
                    },
                    {
                        key: 'total_new_prompt',
                        text: '提示'
                    }
                ],
                enable: true
            },
            {
                type: 'bar',
                title: '待修复告警作者',
                opts: 'author_list',
                xKey: 'name',
                yKey: 'total_count',
                level: [
                    {
                        key: 'serious_count',
                        text: '严重'
                    },
                    {
                        key: 'normal_count',
                        text: '一般'
                    },
                    {
                        key: 'prompt_count',
                        text: '提示'
                    }
                ],
                enable: true
            }
        ]
    },
    KLOCWORK: {
        title: 'Klocwork',
        normalLegend: [
            {
                key: '新增',
                value: 'latest_new_add_count'
            },
            {
                key: '关闭',
                value: 'latest_closed_count'
            }
        ],
        normalKey: '关闭',
        normalVal: 'latest_closed_count',
        activeKey: '新增',
        activeVal: 'latest_new_add_count',
        mainKey: '遗留告警数',
        mainVal: 'latest_exist_count',
        charts: [
            {
                type: 'pie',
                title: '所有告警状态',
                color: ['#ffb400', '#30d878', '#3c96ff', '#dde4eb'],
                opts: [
                    {
                        key: 'total_new',
                        text: '待修复'
                    },
                    {
                        key: 'total_close',
                        text: '已修复'
                    },
                    {
                        key: 'total_ignore',
                        text: '已忽略'
                    },
                    {
                        key: 'total_excluded',
                        text: '已屏蔽'
                    }
                ],
                enable: true
            },
            {
                type: 'bar',
                title: '待修复告警级别',
                opts: [
                    {
                        key: 'total_new_serious',
                        text: '严重'
                    },
                    {
                        key: 'total_new_normal',
                        text: '一般'
                    },
                    {
                        key: 'total_new_prompt',
                        text: '提示'
                    }
                ],
                enable: true
            },
            {
                type: 'bar',
                title: '待修复告警作者',
                opts: 'author_list',
                xKey: 'name',
                yKey: 'total_count',
                level: [
                    {
                        key: 'serious_count',
                        text: '严重'
                    },
                    {
                        key: 'normal_count',
                        text: '一般'
                    },
                    {
                        key: 'prompt_count',
                        text: '提示'
                    }
                ],
                enable: true
            }
        ]
    },
    PINPOINT: {
        title: 'Pinpoint',
        normalLegend: [
            {
                key: '新增',
                value: 'latest_new_add_count'
            },
            {
                key: '关闭',
                value: 'latest_closed_count'
            }
        ],
        normalKey: '关闭',
        normalVal: 'latest_closed_count',
        activeKey: '新增',
        activeVal: 'latest_new_add_count',
        mainKey: '遗留告警数',
        mainVal: 'latest_exist_count',
        charts: [
            {
                type: 'pie',
                title: '所有告警状态',
                color: ['#ffb400', '#30d878', '#3c96ff', '#dde4eb'],
                opts: [
                    {
                        key: 'total_new',
                        text: '待修复'
                    },
                    {
                        key: 'total_close',
                        text: '已修复'
                    },
                    {
                        key: 'total_ignore',
                        text: '已忽略'
                    },
                    {
                        key: 'total_excluded',
                        text: '已屏蔽'
                    }
                ],
                enable: true
            },
            {
                type: 'bar',
                title: '待修复告警级别',
                opts: [
                    {
                        key: 'total_new_serious',
                        text: '严重'
                    },
                    {
                        key: 'total_new_normal',
                        text: '一般'
                    },
                    {
                        key: 'total_new_prompt',
                        text: '提示'
                    }
                ],
                enable: true
            },
            {
                type: 'bar',
                title: '待修复告警作者',
                opts: 'author_list',
                xKey: 'name',
                yKey: 'total_count',
                level: [
                    {
                        key: 'serious_count',
                        text: '严重'
                    },
                    {
                        key: 'normal_count',
                        text: '一般'
                    },
                    {
                        key: 'prompt_count',
                        text: '提示'
                    }
                ],
                enable: true
            }
        ]
    },
    CPPLINT: {
        title: 'CppLint',
        normalLegend: [
            {
                key: '告警数',
                value: 'newfile_total_defect_count'
            },
            {
                key: '文件数',
                value: 'newfile_total_count'
            }
        ],
        normalKey: '文件数',
        normalVal: 'newfile_total_count',
        normalChange: 'newfile_changed_count',
        activeKey: '告警数',
        activeVal: 'newfile_total_defect_count',
        activeChange: 'newfile_changed_defect_count',
        mainKey: '告警总数',
        mainVal: 'newfile_total_defect_count',
        hasChange: true,
        changeNormalVal: 'newfile_changed_count',
        changeActiveVal: 'newfile_changed_defect_count',
        charts: [
            {
                type: 'bar',
                title: '待修复告警级别',
                opts: [
                    {
                        key: 'total_new_serious',
                        text: '严重'
                    },
                    {
                        key: 'total_new_normal',
                        text: '一般'
                    },
                    {
                        key: 'total_new_prompt',
                        text: '提示'
                    }
                ],
                enable: true
            },
            {
                type: 'bar',
                title: '待修复告警作者',
                opts: 'author_list',
                xKey: 'name',
                yKey: 'prompt_count',
                level: [
                    {
                        key: 'serious_count',
                        text: '严重'
                    },
                    {
                        key: 'normal_count',
                        text: '一般'
                    },
                    {
                        key: 'prompt_count',
                        text: '提示'
                    }
                ],
                enable: true
            }
        ]
    },
    PYLINT: {
        title: 'PyLint',
        normalLegend: [
            {
                key: '告警数',
                value: 'newfile_total_defect_count'
            },
            {
                key: '文件数',
                value: 'newfile_total_count'
            }
        ],
        normalKey: '文件数',
        normalVal: 'newfile_total_count',
        normalChange: 'newfile_changed_count',
        activeKey: '告警数',
        activeVal: 'newfile_total_defect_count',
        activeChange: 'newfile_changed_defect_count',
        mainKey: '告警总数',
        mainVal: 'newfile_total_defect_count',
        hasChange: true,
        changeNormalVal: 'newfile_changed_count',
        changeActiveVal: 'newfile_changed_defect_count',
        charts: [
            {
                type: 'bar',
                title: '待修复告警级别',
                opts: [
                    {
                        key: 'total_new_serious',
                        text: '严重'
                    },
                    {
                        key: 'total_new_normal',
                        text: '一般'
                    },
                    {
                        key: 'total_new_prompt',
                        text: '提示'
                    }
                ],
                enable: true
            },
            {
                type: 'bar',
                title: '待修复告警作者',
                opts: 'author_list',
                xKey: 'name',
                yKey: 'prompt_count',
                level: [
                    {
                        key: 'serious_count',
                        text: '严重'
                    },
                    {
                        key: 'normal_count',
                        text: '一般'
                    },
                    {
                        key: 'prompt_count',
                        text: '提示'
                    }
                ],
                enable: true
            }
        ]
    },
    TSCLUA: {
        title: 'TSCLUA',
        normalKey: '关闭',
        normalVal: 'latest_closed_count',
        activeKey: '新增',
        activeVal: 'latest_new_add_count',
        mainKey: '遗留告警数',
        mainVal: 'latest_exist_count',
        charts: [
            {
                type: 'pie',
                title: '所有告警状态',
                opts: [
                    {
                        key: 'total_new',
                        text: '待修复'
                    },
                    {
                        key: 'total_close',
                        text: '已修复'
                    },
                    {
                        key: 'total_ignore',
                        text: '已忽略'
                    },
                    {
                        key: 'total_excluded',
                        text: '已屏蔽'
                    }
                ],
                enable: true
            },
            {
                type: 'bar',
                title: '待修复告警级别',
                opts: [
                    {
                        key: 'total_new_serious',
                        text: '严重'
                    },
                    {
                        key: 'total_new_normal',
                        text: '一般'
                    },
                    {
                        key: 'total_new_prompt',
                        text: '提示'
                    }
                ],
                enable: true
            },
            {
                type: 'bar',
                title: '待修复告警作者',
                opts: 'author_list',
                xKey: 'name',
                yKey: 'prompt_count',
                level: [
                    {
                        key: 'serious_count',
                        text: '严重'
                    },
                    {
                        key: 'normal_count',
                        text: '一般'
                    },
                    {
                        key: 'prompt_count',
                        text: '提示'
                    }
                ],
                enable: true
            }
        ]
    },
    ESLINT: {
        title: 'ESLint',
        normalLegend: [
            {
                key: '告警数',
                value: 'newfile_total_defect_count'
            },
            {
                key: '文件数',
                value: 'newfile_total_count'
            }
        ],
        normalKey: '文件数',
        normalVal: 'newfile_total_count',
        normalChange: 'newfile_changed_count',
        activeKey: '告警数',
        activeVal: 'newfile_total_defect_count',
        activeChange: 'newfile_changed_defect_count',
        mainKey: '告警总数',
        mainVal: 'newfile_total_defect_count',
        hasChange: true,
        changeNormalVal: 'newfile_changed_count',
        changeActiveVal: 'newfile_changed_defect_count',
        charts: [
            {
                type: 'bar',
                title: '待修复告警级别',
                opts: [
                    {
                        key: 'total_new_serious',
                        text: '严重'
                    },
                    {
                        key: 'total_new_normal',
                        text: '一般'
                    },
                    {
                        key: 'total_new_prompt',
                        text: '提示'
                    }
                ],
                enable: true
            },
            {
                type: 'bar',
                title: '待修复告警作者',
                opts: 'author_list',
                xKey: 'name',
                yKey: 'prompt_count',
                level: [
                    {
                        key: 'serious_count',
                        text: '严重'
                    },
                    {
                        key: 'normal_count',
                        text: '一般'
                    },
                    {
                        key: 'prompt_count',
                        text: '提示'
                    }
                ],
                enable: true
            }
        ]
    },
    CHECKSTYLE: {
        title: 'Checkstyle',
        normalLegend: [
            {
                key: '告警数',
                value: 'newfile_total_defect_count'
            },
            {
                key: '文件数',
                value: 'newfile_total_count'
            }
        ],
        normalKey: '文件数',
        normalVal: 'newfile_total_count',
        normalChange: 'newfile_changed_count',
        activeKey: '告警数',
        activeVal: 'newfile_total_defect_count',
        activeChange: 'newfile_changed_defect_count',
        mainKey: '告警总数',
        mainVal: 'newfile_total_defect_count',
        hasChange: true,
        changeNormalVal: 'newfile_changed_count',
        changeActiveVal: 'newfile_changed_defect_count',
        charts: [
            {
                type: 'bar',
                title: '待修复告警级别',
                opts: [
                    {
                        key: 'total_new_serious',
                        text: '严重'
                    },
                    {
                        key: 'total_new_normal',
                        text: '一般'
                    },
                    {
                        key: 'total_new_prompt',
                        text: '提示'
                    }
                ],
                enable: true
            },
            {
                type: 'bar',
                title: '待修复告警作者',
                opts: 'author_list',
                xKey: 'name',
                yKey: 'prompt_count',
                level: [
                    {
                        key: 'serious_count',
                        text: '严重'
                    },
                    {
                        key: 'normal_count',
                        text: '一般'
                    },
                    {
                        key: 'prompt_count',
                        text: '提示'
                    }
                ],
                enable: true
            }
        ]
    },
    STYLECOP: {
        title: 'StyleCop',
        normalLegend: [
            {
                key: '告警数',
                value: 'newfile_total_defect_count'
            },
            {
                key: '文件数',
                value: 'newfile_total_count'
            }
        ],
        normalKey: '文件数',
        normalVal: 'newfile_total_count',
        normalChange: 'newfile_changed_count',
        activeKey: '告警数',
        activeVal: 'newfile_total_defect_count',
        activeChange: 'newfile_changed_defect_count',
        mainKey: '告警总数',
        mainVal: 'newfile_total_defect_count',
        hasChange: true,
        changeNormalVal: 'newfile_changed_count',
        changeActiveVal: 'newfile_changed_defect_count',
        charts: [
            {
                type: 'bar',
                title: '待修复告警级别',
                opts: [
                    {
                        key: 'total_new_serious',
                        text: '严重'
                    },
                    {
                        key: 'total_new_normal',
                        text: '一般'
                    },
                    {
                        key: 'total_new_prompt',
                        text: '提示'
                    }
                ],
                enable: true
            },
            {
                type: 'bar',
                title: '待修复告警作者',
                opts: 'author_list',
                xKey: 'name',
                yKey: 'prompt_count',
                level: [
                    {
                        key: 'serious_count',
                        text: '严重'
                    },
                    {
                        key: 'normal_count',
                        text: '一般'
                    },
                    {
                        key: 'prompt_count',
                        text: '提示'
                    }
                ],
                enable: true
            }
        ]
    },
    DETEKT: {
        title: 'detekt',
        normalLegend: [
            {
                key: '告警数',
                value: 'newfile_total_defect_count'
            },
            {
                key: '文件数',
                value: 'newfile_total_count'
            }
        ],
        normalKey: '文件数',
        normalVal: 'newfile_total_count',
        normalChange: 'newfile_changed_count',
        activeKey: '告警数',
        activeVal: 'newfile_total_defect_count',
        activeChange: 'newfile_changed_defect_count',
        mainKey: '告警总数',
        mainVal: 'newfile_total_defect_count',
        hasChange: true,
        changeNormalVal: 'newfile_changed_count',
        changeActiveVal: 'newfile_changed_defect_count',
        charts: [
            {
                type: 'bar',
                title: '待修复告警级别',
                opts: [
                    {
                        key: 'total_new_serious',
                        text: '严重'
                    },
                    {
                        key: 'total_new_normal',
                        text: '一般'
                    },
                    {
                        key: 'total_new_prompt',
                        text: '提示'
                    }
                ],
                enable: true
            },
            {
                type: 'bar',
                title: '待修复告警作者',
                opts: 'author_list',
                xKey: 'name',
                yKey: 'prompt_count',
                level: [
                    {
                        key: 'serious_count',
                        text: '严重'
                    },
                    {
                        key: 'normal_count',
                        text: '一般'
                    },
                    {
                        key: 'prompt_count',
                        text: '提示'
                    }
                ],
                enable: true
            }
        ]
    },
    PHPCS: {
        title: 'PHPCS',
        normalLegend: [
            {
                key: '告警数',
                value: 'newfile_total_defect_count'
            },
            {
                key: '文件数',
                value: 'newfile_total_count'
            }
        ],
        normalKey: '文件数',
        normalVal: 'newfile_total_count',
        normalChange: 'newfile_changed_count',
        activeKey: '告警数',
        activeVal: 'newfile_total_defect_count',
        activeChange: 'newfile_changed_defect_count',
        mainKey: '告警总数',
        mainVal: 'newfile_total_defect_count',
        hasChange: true,
        changeNormalVal: 'newfile_changed_count',
        changeActiveVal: 'newfile_changed_defect_count',
        charts: [
            {
                type: 'bar',
                title: '待修复告警级别',
                opts: [
                    {
                        key: 'total_new_serious',
                        text: '严重'
                    },
                    {
                        key: 'total_new_normal',
                        text: '一般'
                    },
                    {
                        key: 'total_new_prompt',
                        text: '提示'
                    }
                ],
                enable: true
            },
            {
                type: 'bar',
                title: '待修复告警作者',
                opts: 'author_list',
                xKey: 'name',
                yKey: 'prompt_count',
                level: [
                    {
                        key: 'serious_count',
                        text: '严重'
                    },
                    {
                        key: 'normal_count',
                        text: '一般'
                    },
                    {
                        key: 'prompt_count',
                        text: '提示'
                    }
                ],
                enable: true
            }
        ]
    },
    SENSITIVE: {
        title: '敏感信息检测',
        normalLegend: [
            {
                key: '告警数',
                value: 'newfile_total_defect_count'
            },
            {
                key: '文件数',
                value: 'newfile_total_count'
            }
        ],
        normalKey: '文件数',
        normalVal: 'newfile_total_count',
        normalChange: 'newfile_changed_count',
        activeKey: '告警数',
        activeVal: 'newfile_total_defect_count',
        activeChange: 'newfile_changed_defect_count',
        mainKey: '告警总数',
        mainVal: 'newfile_total_defect_count',
        hasChange: true,
        changeNormalVal: 'newfile_changed_count',
        changeActiveVal: 'newfile_changed_defect_count',
        charts: [
            {
                type: 'bar',
                title: '待修复告警级别',
                opts: [
                    {
                        key: 'total_new_serious',
                        text: '严重'
                    },
                    {
                        key: 'total_new_normal',
                        text: '一般'
                    },
                    {
                        key: 'total_new_prompt',
                        text: '提示'
                    }
                ],
                enable: true
            },
            {
                type: 'bar',
                title: '待修复告警作者',
                opts: 'author_list',
                xKey: 'name',
                yKey: 'prompt_count',
                level: [
                    {
                        key: 'serious_count',
                        text: '严重'
                    },
                    {
                        key: 'normal_count',
                        text: '一般'
                    },
                    {
                        key: 'prompt_count',
                        text: '提示'
                    }
                ],
                enable: true
            }
        ]
    },
    OCCHECK: {
        title: 'OCCheck',
        normalLegend: [
            {
                key: '告警数',
                value: 'newfile_total_defect_count'
            },
            {
                key: '文件数',
                value: 'newfile_total_count'
            }
        ],
        normalKey: '文件数',
        normalVal: 'newfile_total_count',
        normalChange: 'newfile_changed_count',
        activeKey: '告警数',
        activeVal: 'newfile_total_defect_count',
        activeChange: 'newfile_changed_defect_count',
        mainKey: '告警总数',
        mainVal: 'newfile_total_defect_count',
        hasChange: true,
        changeNormalVal: 'newfile_changed_count',
        changeActiveVal: 'newfile_changed_defect_count',
        charts: [
            {
                type: 'bar',
                title: '待修复告警级别',
                opts: [
                    {
                        key: 'total_new_serious',
                        text: '严重'
                    },
                    {
                        key: 'total_new_normal',
                        text: '一般'
                    },
                    {
                        key: 'total_new_prompt',
                        text: '提示'
                    }
                ],
                enable: true
            },
            {
                type: 'bar',
                title: '待修复告警作者',
                opts: 'author_list',
                xKey: 'name',
                yKey: 'prompt_count',
                level: [
                    {
                        key: 'serious_count',
                        text: '严重'
                    },
                    {
                        key: 'normal_count',
                        text: '一般'
                    },
                    {
                        key: 'prompt_count',
                        text: '提示'
                    }
                ],
                enable: true
            }
        ]
    },
    GOML: {
        title: 'Gometalinter',
        normalLegend: [
            {
                key: '告警数',
                value: 'newfile_total_defect_count'
            },
            {
                key: '文件数',
                value: 'newfile_total_count'
            }
        ],
        normalKey: '文件数',
        normalVal: 'newfile_total_count',
        normalChange: 'newfile_changed_count',
        activeKey: '告警数',
        activeVal: 'newfile_total_defect_count',
        activeChange: 'newfile_changed_defect_count',
        mainKey: '告警总数',
        mainVal: 'newfile_total_defect_count',
        hasChange: true,
        changeNormalVal: 'newfile_changed_count',
        changeActiveVal: 'newfile_changed_defect_count',
        charts: [
            {
                type: 'bar',
                title: '待修复告警级别',
                opts: [
                    {
                        key: 'total_new_serious',
                        text: '严重'
                    },
                    {
                        key: 'total_new_normal',
                        text: '一般'
                    },
                    {
                        key: 'total_new_prompt',
                        text: '提示'
                    }
                ],
                enable: true
            },
            {
                type: 'bar',
                title: '待修复告警作者',
                opts: 'author_list',
                xKey: 'name',
                yKey: 'prompt_count',
                level: [
                    {
                        key: 'serious_count',
                        text: '严重'
                    },
                    {
                        key: 'normal_count',
                        text: '一般'
                    },
                    {
                        key: 'prompt_count',
                        text: '提示'
                    }
                ],
                enable: true
            }
        ]
    },
    GOCILINT: {
        title: 'GolangCI-Lint',
        normalLegend: [
            {
                key: '告警数',
                value: 'newfile_total_defect_count'
            },
            {
                key: '文件数',
                value: 'newfile_total_count'
            }
        ],
        normalKey: '文件数',
        normalVal: 'newfile_total_count',
        normalChange: 'newfile_changed_count',
        activeKey: '告警数',
        activeVal: 'newfile_total_defect_count',
        activeChange: 'newfile_changed_defect_count',
        mainKey: '告警总数',
        mainVal: 'newfile_total_defect_count',
        hasChange: true,
        changeNormalVal: 'newfile_changed_count',
        changeActiveVal: 'newfile_changed_defect_count',
        charts: [
            {
                type: 'bar',
                title: '待修复告警级别',
                opts: [
                    {
                        key: 'total_new_serious',
                        text: '严重'
                    },
                    {
                        key: 'total_new_normal',
                        text: '一般'
                    },
                    {
                        key: 'total_new_prompt',
                        text: '提示'
                    }
                ],
                enable: true
            },
            {
                type: 'bar',
                title: '待修复告警作者',
                opts: 'author_list',
                xKey: 'name',
                yKey: 'prompt_count',
                level: [
                    {
                        key: 'serious_count',
                        text: '严重'
                    },
                    {
                        key: 'normal_count',
                        text: '一般'
                    },
                    {
                        key: 'prompt_count',
                        text: '提示'
                    }
                ],
                enable: true
            }
        ]
    },
    CCN: {
        title: '圈复杂度',
        normalLegend: [
            {
                key: '风险函数',
                value: 'total_risk_func_count'
            },
            {
                key: '平均圈复杂度',
                value: 'average_ccn'
            }
        ],
        normalKey: '平均圈复杂度',
        normalVal: 'average_ccn',
        normalChange: 'changed_ccn',
        activeKey: '风险函数',
        activeVal: 'total_risk_func_count',
        activeChange: 'changed_risk_func_count',
        mainKey: '平均圈复杂度',
        mainVal: 'average_ccn',
        hasChange: true,
        changeNormalVal: 'changed_ccn',
        changeActiveVal: 'changed_risk_func_count',
        charts: [
            {
                type: 'bar',
                title: '风险函数级别分布',
                opts: [
                    {
                        key: 'super_high',
                        text: '极高风险'
                    },
                    {
                        key: 'high',
                        text: '高风险'
                    },
                    {
                        key: 'medium',
                        text: '中风险'
                    },
                    {
                        key: 'low',
                        text: '低风险'
                    }
                ],
                enable: true
            },
            {
                type: 'line',
                title: '平均圈复杂度趋势',
                opts: 'average_ccn_chart',
                xKey: 'date',
                yKey: 'averageCCN',
                enable: true
            }
        ]
    },
    DUPC: {
        title: '代码重复率',
        normalLegend: [
            {
                key: '重复文件',
                value: 'total_dupfile_count'
            },
            {
                key: '代码重复率',
                value: 'current_dup_rate'
            }
        ],
        normalKey: '代码重复率',
        normalVal: 'current_dup_rate',
        normalChange: 'changed_dup_rate',
        activeKey: '重复文件',
        activeVal: 'total_dupfile_count',
        activeChange: 'changed_dupfile_count',
        mainKey: '代码重复率',
        mainVal: 'current_dup_rate',
        hasChange: true,
        changeNormalVal: 'changed_dup_rate',
        changeActiveVal: 'changed_dupfile_count',
        charts: [
            {
                type: 'bar',
                title: '重复文件级别分布',
                opts: [
                    {
                        key: 'super_high',
                        text: '极高风险'
                    },
                    {
                        key: 'high',
                        text: '高风险'
                    },
                    {
                        key: 'medium',
                        text: '中风险'
                    }
                ],
                enable: true
            },
            {
                type: 'line',
                title: '代码重复率趋势',
                opts: 'dupc_chart',
                xKey: 'date',
                yKey: 'averageCCN',
                enable: true
            }
        ]
    },
    HORUSPY: {
        title: '荷鲁斯高危组件',
        normalLegend: [
            {
                key: '告警数',
                value: 'newfile_total_defect_count'
            },
            {
                key: '文件数',
                value: 'newfile_total_count'
            }
        ],
        normalKey: '文件数',
        normalVal: 'newfile_total_count',
        normalChange: 'newfile_changed_count',
        activeKey: '告警数',
        activeVal: 'newfile_total_defect_count',
        activeChange: 'newfile_changed_defect_count',
        mainKey: '告警总数',
        mainVal: 'newfile_total_defect_count',
        hasChange: true,
        changeNormalVal: 'newfile_changed_count',
        changeActiveVal: 'newfile_changed_defect_count',
        charts: [
            {
                type: 'bar',
                title: '待修复告警级别',
                opts: [
                    {
                        key: 'total_new_serious',
                        text: '严重'
                    },
                    {
                        key: 'total_new_normal',
                        text: '一般'
                    },
                    {
                        key: 'total_new_prompt',
                        text: '提示'
                    }
                ],
                enable: true
            },
            {
                type: 'bar',
                title: '待修复告警作者',
                opts: 'author_list',
                xKey: 'name',
                yKey: 'prompt_count',
                level: [
                    {
                        key: 'serious_count',
                        text: '严重'
                    },
                    {
                        key: 'normal_count',
                        text: '一般'
                    },
                    {
                        key: 'prompt_count',
                        text: '提示'
                    }
                ],
                enable: true
            }
        ]
    },
    WOODPECKER_SENSITIVE: {
        title: '啄木鸟敏感信息',
        normalLegend: [
            {
                key: '告警数',
                value: 'newfile_total_defect_count'
            },
            {
                key: '文件数',
                value: 'newfile_total_count'
            }
        ],
        normalKey: '文件数',
        normalVal: 'newfile_total_count',
        normalChange: 'newfile_changed_count',
        activeKey: '告警数',
        activeVal: 'newfile_total_defect_count',
        activeChange: 'newfile_changed_defect_count',
        mainKey: '告警总数',
        mainVal: 'newfile_total_defect_count',
        hasChange: true,
        changeNormalVal: 'newfile_changed_count',
        changeActiveVal: 'newfile_changed_defect_count',
        charts: [
            {
                type: 'bar',
                title: '待修复告警级别',
                opts: [
                    {
                        key: 'total_new_serious',
                        text: '严重'
                    },
                    {
                        key: 'total_new_normal',
                        text: '一般'
                    },
                    {
                        key: 'total_new_prompt',
                        text: '提示'
                    }
                ],
                enable: true
            },
            {
                type: 'bar',
                title: '待修复告警作者',
                opts: 'author_list',
                xKey: 'name',
                yKey: 'prompt_count',
                level: [
                    {
                        key: 'serious_count',
                        text: '严重'
                    },
                    {
                        key: 'normal_count',
                        text: '一般'
                    },
                    {
                        key: 'prompt_count',
                        text: '提示'
                    }
                ],
                enable: true
            }
        ]
    },
    RIPS: {
        title: 'RIPS',
        normalLegend: [
            {
                key: '告警数',
                value: 'newfile_total_defect_count'
            },
            {
                key: '文件数',
                value: 'newfile_total_count'
            }
        ],
        normalKey: '文件数',
        normalVal: 'newfile_total_count',
        normalChange: 'newfile_changed_count',
        activeKey: '告警数',
        activeVal: 'newfile_total_defect_count',
        activeChange: 'newfile_changed_defect_count',
        mainKey: '告警总数',
        mainVal: 'newfile_total_defect_count',
        hasChange: true,
        changeNormalVal: 'newfile_changed_count',
        changeActiveVal: 'newfile_changed_defect_count',
        charts: [
            {
                type: 'bar',
                title: '待修复告警级别',
                opts: [
                    {
                        key: 'total_new_serious',
                        text: '严重'
                    },
                    {
                        key: 'total_new_normal',
                        text: '一般'
                    },
                    {
                        key: 'total_new_prompt',
                        text: '提示'
                    }
                ],
                enable: true
            },
            {
                type: 'bar',
                title: '待修复告警作者',
                opts: 'author_list',
                xKey: 'name',
                yKey: 'prompt_count',
                level: [
                    {
                        key: 'serious_count',
                        text: '严重'
                    },
                    {
                        key: 'normal_count',
                        text: '一般'
                    },
                    {
                        key: 'prompt_count',
                        text: '提示'
                    }
                ],
                enable: true
            }
        ]
    }
}
