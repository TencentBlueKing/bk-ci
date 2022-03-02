let vue = new Vue({
    el: ".data-holder",
    data: {
        work_data: {
            job_remote_ok: 0,
            job_remote_error: 0,
            job_local_ok: 0,
            job_local_error: 0
        },
        jobs_data: {},
        task_data: {
            client_cpu: 0,
            cpu_total: 0,
            queue_name: "未知",
            source_ip: "未知",
            client_version: "未知",
            worker_version: "未知",
            extra_record: "{\"ccache_stats\":{\"cache_direct_hit\": 0,\"cache_preprocessed_hit\": 0,\"cache_miss\": 0}}"
        },
        pre_work_longest_time: "0s",
        pre_work_avg_time: "0s",
        remote_work_process_longest_time: "0s",
        remote_work_process_avg_time: "0s",
        local_work_longest_time: "0s",
        local_work_avg_time: "0s",
        pre_work_lock_longest_time: "0s",
        pre_work_lock_avg_time: "0s",
        remote_work_lock_longest_time: "0s",
        remote_work_lock_avg_time: "0s",
        local_work_lock_longest_time: "0s",
        local_work_lock_avg_time: "0s",
        post_work_lock_longest_time: "0s",
        post_work_lock_avg_time: "0s",
        post_work_longest_time: "0s",
        post_work_avg_time: "0s",
        remote_work_pack_longest_time: "0s",
        remote_work_pack_avg_time: "0s",
        remote_work_unpack_longest_time: "0s",
        remote_work_unpack_avg_time: "0s",
        remote_work_send_longest_time: "0s",
        remote_work_send_avg_time: "0s",
        remote_work_receive_longest_time: "0s",
        remote_work_receive_avg_time: "0s",
        remote_work_pack_common_longest_time: "0s",
        remote_work_pack_common_avg_time: "0s",
        remote_work_send_common_longest_time: "0s",
        remote_work_send_common_avg_time: "0s",
        remote_work_wait_longest_time: "0s",
        remote_work_wait_avg_time: "0s",
        remote_error_longest_time: "0s",
        remote_error_avg_time: "0s",
        remote_compile_err_count: 0,
        remote_fatal_count: 0,
        remote_fatal: []
    },
    computed: {
        work_result: function () {
            if (this.work_data.end_time <= 0 && this.task_data.status != "finish" && this.task_data.status != "failed") {
                return "编译中"
            }

            if (this.work_data.success) {
                return "成功"
            }

            return "失败"
        },
        total_time: function () {
            let end = this.work_data.unregistered_time
            let start = this.work_data.registered_time

            if (start <=0 || (end <=0 && (this.task_data.status == "finish" || this.task_data.status == "failed"))) {
                return "未知"
            }

            if (end <=0) {
                end = Date.now()*1e6
            }

            return this.time_format(end - start)
        },
        apply_time: function () {
            let end = this.work_data.start_time
            let start = this.work_data.registered_time

            if (start <=0 || end <=0 && this.work_data.unregistered_time > 0) {
                return "未知"
            }

            if (end <=0) {
                end = Date.now()*1e6
            }

            return this.time_format(end - start)
        },
        real_time: function () {
            let end = this.work_data.end_time
            let start = this.work_data.start_time

            if (start <=0 || (end <=0 && (this.task_data.status == "finish" || this.task_data.status == "failed"))) {
                return "未知"
            }

            if (end <=0) {
                end = Date.now()*1e6
            }

            return this.time_format(end - start)
        },
        ccache_data: function () {
            if (this.task_data.extra_record == "") {
                return {"ccache_stats":{"cache_direct_hit": 0,"cache_preprocessed_hit": 0,"cache_miss": 0}}
            }
            return JSON.parse(this.task_data.extra_record)
        },
        ccache_direct_hit: function () {
            return this.ccache_data.ccache_stats.cache_direct_hit
        },
        ccache_preprocessed_hit: function () {
            return this.ccache_data.ccache_stats.cache_preprocessed_hit
        },
        ccache_miss: function () {
            return this.ccache_data.ccache_stats.cache_miss
        },
        ccache_hit_rate: function () {
            let total = this.ccache_direct_hit + this.ccache_preprocessed_hit + this.ccache_miss
            if (total == 0) {
                return "0.00%"
            }

            return ((this.ccache_direct_hit + this.ccache_preprocessed_hit) / total * 100).toFixed(2) + "%"
        },
        remote_timeout: function () {
            if (!this.task_data) {
                return "unknown"
            }

            let extra = JSON.parse(this.task_data.extra_project_setting)
            if (!extra || !extra.io_timeout_secs) {
                return 300
            }

            return extra.io_timeout_secs
        },
        remote_fatal_timeout_count: function () {
            let total = 0
            for (var i=0; i<this.remote_fatal.length; i++) {
                if (this.remote_fatal[i].remote_work_timeout || (this.remote_fatal[i].remote_work_end_time - this.remote_fatal[i].remote_work_start_time)/1e9 >= this.remote_timeout) {
                    total++
                }
            }
            return total
        },
        remote_fatal_without_timeout_count: function () {
            return this.remote_fatal_count-this.remote_fatal_timeout_count
        }
    },
    methods: {
        fixer: function (data, num) {
            let temp = data.toFixed(10);
            return parseFloat(temp.substring(0, temp.lastIndexOf('.') + num + 1));
        },
        time_format: function (nano_sec) {
            let hour = 0
            let min = 0
            let sec = nano_sec / 1e9
            if (sec >= 60) {
                min = Math.trunc(sec / 60)
                sec = sec - min * 60
            }
            if (min >= 60) {
                hour = Math.trunc(min / 60)
                min = min - hour * 60
            }

            sec = this.fixer(sec, 3)

            if (hour > 0) {
                return hour.toString() + "h" + min.toString() + "m" + sec.toString() + "s"
            }
            if (min > 0) {
                return min.toString() + "m" + sec.toString() + "s"
            }
            return sec.toString() + "s"
        },
        date_format: function (nano_sec) {
            let date = new Date(nano_sec / 1e6)
            let hours = date.getHours()
            let minutes = "0" + date.getMinutes()
            let seconds = "0" + date.getSeconds()
            return hours + ':' + minutes.substr(-2) + ':' + seconds.substr(-2)
        },
        date_full_format: function (nano_sec) {
            let date = new Date(nano_sec / 1e6)
            return date.getFullYear() + "." + (date.getMonth() + 1) + "." + date.getDate() + " " + this.date_format(nano_sec)
        },
        start: function () {
            let input_data = $("#input_data").val()
            if (history_data == input_data) {
                return
            }

            history_data = input_data

            fetchStatsData(input_data)
        },
        start_list: function () {
            let type = $("#input_type").val()

            if (type === "WORK") {
                $("#task-table-holder").css("display", 'none')
                $("#work-table-holder").css("display", 'inline-block')
                fetchWorkTable()
            } else {
                $("#work-table-holder").css("display", 'none')
                $("#task-table-holder").css("display", 'inline-block')
                fetchTaskTable()
            }
        },
        baseName: function (str) {
            let base = new String(str).substring(str.lastIndexOf('/') + 1)
            base = new String(base).substring(base.lastIndexOf('\\') + 1)
            if (base.lastIndexOf(".") != -1)
                base = base.substring(0, base.lastIndexOf("."))
            return base
        }
    }
})

function _arrayBufferToString(buffer) {
    let binary = '';
    let bytes = new Uint16Array(buffer);
    let len = bytes.byteLength;
    let char
    let code
    for (let i = 0; i < len; i++) {
        char = String.fromCharCode(bytes[i]);
        code = char.charCodeAt(0)
        if (code < 32 || code > 127) {
            break
        }
        binary += char
    }
    return binary;
}

function decodeJson(src) {
    let strData = atob(src)
    let charData = strData.split('').map(function (x) {
        return x.charCodeAt(0);
    });
    let data = pako.inflate(new Uint8Array(charData))
    return _arrayBufferToString(data)
}

let get = function (url, success, error) {
    $.ajax({
        url: url,
        dataType: "json",
        crossDomain: true,
        success: success,
        error: error
    });
}

function fetchStatsData(id) {
    showLoading()
    let key = "task_id"
    let needFetchTask = true
    if (id.length == 16 && id.split("_").length == 2) {
        key = "work_id"
    }

    get(
        api_prefix + "/api/v1/disttask/work_stats/?" + key + "=" + id + "&decode_job=1",
        function (resp) {
            if (!resp.result) {
                hideLoading()
                return
            }
            saveID(id)

            vue.work_data = resp.data
            // vue.jobs_data = JSON.parse(decodeJson(resp.data.job_stats)) // decompress in frontend
            vue.jobs_data = JSON.parse(resp.data.job_stats_data)

            if (needFetchTask) {
                setTimeout(fetchTaskData, 0, resp.data.task_id)
            }
            setTimeout(calculate_jobs_data, 0)
            setTimeout(initConcurrencyChart, 0, "#concurrency_chart")
            setTimeout(initConcurrencyChartInstant, 0, "#concurrency_chart_instant")
            setTimeout(initSumChart, 0, "#sum_chart")
            setTimeout(initWaitingChart, 0, "#waiting_chart")
            setTimeout(initWaitingChart_instant, 0, "#waiting_chart_instant")
            setTimeout(initProcessTimeChart, 0, "#process_chart")
            setTimeout(initCommandTimeChart, 0, "#command_chart")
            hideLoading()
        },
        function (e) {

        }
    )
}

function fetchTaskData(task_id) {
    if (task_id.length == 16 && task_id.split("_").length == 2) {
        return
    }

    get(
        api_prefix + "/api/v1/disttask/task/" + task_id,
        function (resp) {
            if (!resp.result) {
                return
            }

            vue.task_data = resp.data
        },
        function (e) {

        }
    )
}

let language = {
    search: '搜索：',
    lengthMenu: "每页显示 _MENU_ 记录",
    zeroRecords: "没找到相应的数据！",
    info: "分页 _PAGE_ / _PAGES_",
    infoEmpty: "暂无数据！",
    infoFiltered: "(从 _MAX_ 条数据中搜索)",
    paginate: {
        first: '首页',
        last: '尾页',
        previous: '上一页',
        next: '下一页',
    }
}

let table;
let current_type;

function fetchWorkTable() {
    if (table === undefined || current_type !== "WORK") {
        current_type = "WORK"
        table = $('#work_table').dataTable({
            paging: true,
            ordering: false,
            info: false,
            searching: false,
            serverSide: true,
            pageLength: 5,
            current_draw: undefined,
            processing: true,
            lengthChange: true,
            lengthMenu: [5, 10, 20, 50],
            language: language,
            columns: [
                {
                    data: null,
                    render: function (data) {
                        return "<a style='cursor: pointer;' onclick='fetchStatsData(\"" + data.work_id + "\")'>" + data.work_id + "</a>"
                    }
                },
                {
                    data: null,
                    render: function (data) {
                        return vue.date_full_format(data.registered_time)
                    }
                },
                {
                    data: null,
                    render: function (data) {
                        return vue.time_format(data.end_time - data.start_time)
                    }
                },
                {
                    data: null,
                    render: function (data) {
                        if (data.success) {
                            return '成功'
                        }
                        return '失败'
                    }
                }
            ],
            ajax: function (data, callback, oSettings) {
                let that = this
                let project_id = $("#input_project").val() + "_" + $("#input_scene").val();
                let day = $("#input_day").val()
                saveValues()
                get(
                    api_prefix + "/api/v1/disttask/work?project_id=" + project_id + "&offset=" + data.start + "&limit=" + data.length + "&day=" + day,
                    function (resp) {
                        if (!resp.result) {
                            return
                        }

                        if (that.current_draw === undefined) {
                            that.current_draw = data.draw
                        }
                        let json = {
                            data: resp.data,
                            recordsTotal: resp.total,
                            draw: that.current_draw++,
                            recordsFiltered: resp.total
                        }

                        callback(json)
                    },
                    function (e) {
                    }
                )
            }
        })
    } else {
        table.DataTable().ajax.reload()
    }
}

function fetchTaskTable() {
    if (table === undefined || current_type !== "TASK") {
        current_type = "TASK"
        table = $('#task_table').dataTable({
            paging: true,
            ordering: false,
            info: false,
            searching: false,
            serverSide: true,
            pageLength: 5,
            current_draw: undefined,
            processing: true,
            lengthChange: true,
            lengthMenu: [5, 10, 20, 50],
            language: language,
            columns: [
                {
                    data: null,
                    render: function (data) {
                        return "<a style='cursor: pointer;' onclick='fetchStatsData(\"" + data.task_id + "\")'>" + data.task_id + "</a>"
                    }
                },
                {
                    data: null,
                    render: function (data) {
                        return vue.date_full_format(data.create_time * 1e9)
                    }
                },
                {
                    data: null,
                    render: function (data) {
                        let t = data.end_time - data.start_time
                        if (t < 0) {
                            t = 0
                        }
                        return vue.time_format(t * 1e9)
                    }
                },
                {
                    data: null,
                    render: function (data) {
                        return data.user
                    }
                },
                {
                    data: null,
                    render: function (data) {
                        return data.source_ip
                    }
                },
                {
                    data: null,
                    render: function (data) {
                        return data.status
                    }
                }
            ],
            ajax: function (data, callback, oSettings) {
                let that = this
                let project_id = $("#input_project").val() + "_" + $("#input_scene").val();
                let user = $("#input_user").val()
                let client_ip = $("#input_client_ip").val()
                let day = $("#input_day").val()
                saveValues()
                get(
                    api_prefix + "/api/v1/disttask/task?project_id=" + project_id + "&user=" + user + "&source_ip=" + client_ip + "&offset=" + data.start + "&limit=" + data.length + "&day=" + day,
                    function (resp) {
                        if (!resp.result) {
                            return
                        }

                        if (that.current_draw === undefined) {
                            that.current_draw = data.draw
                        }
                        let json = {
                            data: resp.data,
                            recordsTotal: resp.total,
                            draw: that.current_draw++,
                            recordsFiltered: resp.total
                        }

                        callback(json)
                    },
                    function (e) {
                    }
                )
            }
        })
    } else {
        table.DataTable().ajax.reload()
    }
}

function calculate_jobs_data() {
    let pre_work_longest_time = 0
    let pre_work_avg_time = 0
    let pre_work_avg_count = 0

    let remote_work_process_longest_time = 0
    let remote_work_process_avg_time = 0
    let remote_work_process_avg_count = 0

    let local_work_longest_time = 0
    let local_work_avg_time = 0
    let local_work_avg_count = 0

    let post_work_longest_time = 0
    let post_work_avg_time = 0
    let post_work_avg_count = 0

    let pre_work_lock_longest_time = 0
    let pre_work_lock_avg_time = 0
    let pre_work_lock_avg_count = 0

    let remote_work_lock_longest_time = 0
    let remote_work_lock_avg_time = 0
    let remote_work_lock_avg_count = 0

    let local_work_lock_longest_time = 0
    let local_work_lock_avg_time = 0
    let local_work_lock_avg_count = 0

    let post_work_lock_longest_time = 0
    let post_work_lock_avg_time = 0
    let post_work_lock_avg_count = 0

    let remote_work_pack_longest_time = 0
    let remote_work_pack_avg_time = 0
    let remote_work_pack_avg_count = 0

    let remote_work_unpack_longest_time = 0
    let remote_work_unpack_avg_time = 0
    let remote_work_unpack_avg_count = 0

    let remote_work_send_longest_time = 0
    let remote_work_send_avg_time = 0
    let remote_work_send_avg_count = 0

    let remote_work_receive_longest_time = 0
    let remote_work_receive_avg_time = 0
    let remote_work_receive_avg_count = 0

    let remote_work_pack_common_longest_time = 0
    let remote_work_pack_common_avg_time = 0
    let remote_work_pack_common_avg_count = 0

    let remote_work_send_common_longest_time = 0
    let remote_work_send_common_avg_time = 0
    let remote_work_send_common_avg_count = 0

    let remote_work_wait_longest_time = 0
    let remote_work_wait_avg_time = 0
    let remote_work_wait_avg_count = 0

    let remote_error_longest_time = 0
    let remote_error_avg_time = 0
    let remote_error_avg_count = 0

    let remote_compile_err_count = 0
    let remote_fatal_count = 0
    let remote_fatal = []

    let time
    for (let i = 0; i < vue.jobs_data.length; i++) {
        let data = vue.jobs_data[i]

        if (data.remote_work_start_time > 0 && !data.remote_work_success) {
            remote_fatal_count += 1
            remote_fatal.push(data)
        }

        if (data.remote_work_success && !data.post_work_success) {
            remote_compile_err_count += 1
        }

        // pre work
        if (data.pre_work_success) {
            time = data.pre_work_end_time - data.pre_work_start_time

            if (time > pre_work_longest_time) {
                pre_work_longest_time = time
            }

            if (time > 0) {
                pre_work_avg_time += time
                pre_work_avg_count += 1
            }
        }

        // remote work process
        if (data.remote_work_success && data.post_work_success) {
            time = data.remote_work_process_end_time - data.remote_work_process_start_time

            if (time > remote_work_process_longest_time) {
                remote_work_process_longest_time = time
            }

            if (time > 0) {
                remote_work_process_avg_time += time
                remote_work_process_avg_count += 1
            }
        }

        // local work
        if (data.local_work_success) {
            time = data.local_work_end_time - data.local_work_start_time

            if (time > local_work_longest_time) {
                local_work_longest_time = time
            }

            if (time > 0) {
                local_work_avg_time += time
                local_work_avg_count += 1
            }
        }

        // post work
        if (data.post_work_success) {
            time = data.post_work_end_time - data.post_work_start_time

            if (time > post_work_longest_time) {
                post_work_longest_time = time
            }

            if (time > 0) {
                post_work_avg_time += time
                post_work_avg_count += 1
            }
        }

        // pre work lock
        if (data.pre_work_lock_time > 0) {
            time = data.pre_work_lock_time - data.pre_work_enter_time

            if (time > pre_work_lock_longest_time) {
                pre_work_lock_longest_time = time
            }

            if (time > 0) {
                pre_work_lock_avg_time += time
                pre_work_lock_avg_count += 1
            }
        }

        // remote work lock
        if (data.pre_work_success) {
            time = data.remote_work_lock_time - data.remote_work_enter_time

            if (time > remote_work_lock_longest_time) {
                remote_work_lock_longest_time = time
            }

            if (time > 0) {
                remote_work_lock_avg_time += time
                remote_work_lock_avg_count += 1
            }
        }

        // local work lock
        if (data.local_work_lock_time > 0) {
            time = data.local_work_lock_time - data.local_work_enter_time

            if (time > local_work_lock_longest_time) {
                local_work_lock_longest_time = time
            }

            if (time > 0) {
                local_work_lock_avg_time += time
                local_work_lock_avg_count += 1
            }
        }

        // post work lock
        if (data.post_work_lock_time > 0) {
            time = data.post_work_lock_time - data.post_work_enter_time

            if (time > post_work_lock_longest_time) {
                post_work_lock_longest_time = time
            }

            if (time > 0) {
                post_work_lock_avg_time += time
                post_work_lock_avg_count += 1
            }
        }

        // remote work pack
        if (data.remote_work_success) {
            time = data.remote_work_pack_end_time - data.remote_work_pack_start_time

            if (time > remote_work_pack_longest_time) {
                remote_work_pack_longest_time = time
            }

            if (time > 0) {
                remote_work_pack_avg_time += time
                remote_work_pack_avg_count += 1
            }
        }

        // remote work unpack
        if (data.remote_work_success) {
            time = data.remote_work_unpack_end_time - data.remote_work_unpack_start_time

            if (time > remote_work_unpack_longest_time) {
                remote_work_unpack_longest_time = time
            }

            if (time > 0) {
                remote_work_unpack_avg_time += time
                remote_work_unpack_avg_count += 1
            }
        }

        // remote work send
        if (data.remote_work_success) {
            time = data.remote_work_send_end_time - data.remote_work_send_start_time

            if (time > remote_work_send_longest_time) {
                remote_work_send_longest_time = time
            }

            if (time > 0) {
                remote_work_send_avg_time += time
                remote_work_send_avg_count += 1
            }
        }

        // remote work receive
        if (data.remote_work_success) {
            time = data.remote_work_receive_end_time - data.remote_work_receive_start_time

            if (time > remote_work_receive_longest_time) {
                remote_work_receive_longest_time = time
            }

            if (time > 0) {
                remote_work_receive_avg_time += time
                remote_work_receive_avg_count += 1
            }
        }

        // remote work pack common
        if (data.remote_work_success) {
            time = data.remote_work_pack_common_end_time - data.remote_work_pack_common_start_time

            if (time > remote_work_pack_common_longest_time) {
                remote_work_pack_common_longest_time = time
            }

            if (time > 0) {
                remote_work_pack_common_avg_time += time
                remote_work_pack_common_avg_count += 1
            }
        }

        // remote work send
        if (data.remote_work_success) {
            time = data.remote_work_send_common_end_time - data.remote_work_send_common_start_time

            if (time > remote_work_send_common_longest_time) {
                remote_work_send_common_longest_time = time
            }

            if (time > 0) {
                remote_work_send_common_avg_time += time
                remote_work_send_common_avg_count += 1
            }
        }

        // remote wait
        if (data.remote_work_success) {
            time = data.remote_work_process_start_time - data.remote_work_send_end_time

            if (time > remote_work_wait_longest_time) {
                remote_work_wait_longest_time = time
            }

            if (time > 0) {
                remote_work_wait_avg_time += time
                remote_work_wait_avg_count += 1
            }
        }

        // remote error
        if (!data.remote_work_success) {
            time = data.remote_work_end_time - data.remote_work_start_time

            if (time > remote_error_longest_time) {
                remote_error_longest_time = time
            }

            if (time > 0) {
                remote_error_avg_time += time
                remote_error_avg_count += 1
            }
        }
    }

    // fatal
    vue.remote_compile_err_count = remote_compile_err_count
    vue.remote_fatal_count = remote_fatal_count
    vue.remote_fatal = remote_fatal

    // pre work
    vue.pre_work_longest_time = vue.time_format(pre_work_longest_time)
    vue.pre_work_avg_time = vue.time_format(pre_work_avg_time / pre_work_avg_count)

    // remote work process
    vue.remote_work_process_longest_time = vue.time_format(remote_work_process_longest_time)
    vue.remote_work_process_avg_time = vue.time_format(remote_work_process_avg_time / remote_work_process_avg_count)

    // local work
    vue.local_work_longest_time = vue.time_format(local_work_longest_time)
    vue.local_work_avg_time = vue.time_format(local_work_avg_time / local_work_avg_count)

    // post work
    vue.post_work_longest_time = vue.time_format(post_work_longest_time)
    vue.post_work_avg_time = vue.time_format(post_work_avg_time / post_work_avg_count)

    // pre work lock
    vue.pre_work_lock_longest_time = vue.time_format(pre_work_lock_longest_time)
    vue.pre_work_lock_avg_time = vue.time_format(pre_work_lock_avg_time / pre_work_lock_avg_count)

    // remote work lock
    vue.remote_work_lock_longest_time = vue.time_format(remote_work_lock_longest_time)
    vue.remote_work_lock_avg_time = vue.time_format(remote_work_lock_avg_time / remote_work_lock_avg_count)

    // local work lock
    vue.local_work_lock_longest_time = vue.time_format(local_work_lock_longest_time)
    vue.local_work_lock_avg_time = vue.time_format(local_work_lock_avg_time / local_work_lock_avg_count)

    // post work lock
    vue.post_work_lock_longest_time = vue.time_format(post_work_lock_longest_time)
    vue.post_work_lock_avg_time = vue.time_format(post_work_lock_avg_time / post_work_lock_avg_count)

    // remote work pack
    vue.remote_work_pack_longest_time = vue.time_format(remote_work_pack_longest_time)
    vue.remote_work_pack_avg_time = vue.time_format(remote_work_pack_avg_time / remote_work_pack_avg_count)

    // remote work unpack
    vue.remote_work_unpack_longest_time = vue.time_format(remote_work_unpack_longest_time)
    vue.remote_work_unpack_avg_time = vue.time_format(remote_work_unpack_avg_time / remote_work_unpack_avg_count)

    // remote work send
    vue.remote_work_send_longest_time = vue.time_format(remote_work_send_longest_time)
    vue.remote_work_send_avg_time = vue.time_format(remote_work_send_avg_time / remote_work_send_avg_count)

    // remote work receive
    vue.remote_work_receive_longest_time = vue.time_format(remote_work_receive_longest_time)
    vue.remote_work_receive_avg_time = vue.time_format(remote_work_receive_avg_time / remote_work_receive_avg_count)

    // remote work pack common
    vue.remote_work_pack_common_longest_time = vue.time_format(remote_work_pack_common_longest_time)
    vue.remote_work_pack_common_avg_time = vue.time_format(remote_work_pack_common_avg_time / remote_work_pack_common_avg_count)

    // remote work send common
    vue.remote_work_send_common_longest_time = vue.time_format(remote_work_send_common_longest_time)
    vue.remote_work_send_common_avg_time = vue.time_format(remote_work_send_common_avg_time / remote_work_send_common_avg_count)

    // remote wait
    vue.remote_work_wait_longest_time = vue.time_format(remote_work_wait_longest_time)
    vue.remote_work_wait_avg_time = vue.time_format(remote_work_wait_avg_time / remote_work_wait_avg_count)

    // remote error
    vue.remote_error_longest_time = vue.time_format(remote_error_longest_time)
    vue.remote_error_avg_time = vue.time_format(remote_error_avg_time / remote_error_avg_count)
}

let line_scale = 150
let categories_step = 5
let marker_size = 3
let history_data = ""

function calculate_sink_concurrency(l) {
    let r = []
    for (let i = 0; i < l.length; i++) {
        if (!l[i]) {
            r.push(0)
            continue
        }

        let s = {}
        for (let j = 0; j < l[i].length; j++) {
            s[l[i][j][0]] = 1
            s[l[i][j][1]] = 1
        }
        let indexList = Object.keys(s).sort(function (a, b) {
            return a - b
        });
        s = {}
        for (let j = 0; j < indexList.length; j++) {
            s[indexList[j]] = j
        }

        let e = []
        for (let j = 0; j < indexList.length; j++) {
            e.push(0)
        }
        for (let j = 0; j < l[i].length; j++) {
            e[s[l[i][j][0]]] += 1
            e[s[l[i][j][1]]] -= 1
        }

        let max = 0
        for (let j = 0; j < e.length; j++) {
            if (j > 0) {
                e[j] += e[j - 1]
            }

            if (max < e[j]) {
                max = e[j]
            }
        }

        r.push(max)
    }
    return r
}

function calculate_concurrency(l) {
    let start_time = vue.work_data.start_time
    let end_time = vue.work_data.end_time
    let origin_l = [];
    let sink_l = [];

    for (let i = 0; i < l.length; i++) {
        l[i][0] = l[i][0] - start_time
        l[i][1] = l[i][1] - start_time
        origin_l[i] = [l[i][0], l[i][1]]
    }

    let step = (end_time - start_time) / line_scale
    for (let i = 0; i < l.length; i++) {
        if (l[i][0] < 0 || l[i][1] < 0) {
            l[i][0] = -1
            l[i][1] = -1
            continue
        } else {
            l[i][0] = Math.trunc(l[i][0] / step)
            l[i][1] = Math.trunc(l[i][1] / step)
        }


        if (!sink_l[l[i][0]]) {
            sink_l[l[i][0]] = []
        }
        if (!sink_l[l[i][1]]) {
            sink_l[l[i][1]] = []
        }

        // same step
        if (l[i][0] === l[i][1]) {
            sink_l[l[i][0]].push(origin_l[i])
            continue
        }

        sink_l[l[i][0]].push([origin_l[i][0], (l[i][0] + 1) * step])
        sink_l[l[i][1]].push([(l[i][1] * step), origin_l[i][1]])

        l[i][0] += 1
    }
    let sink_r = calculate_sink_concurrency(sink_l)

    let data = []
    for (let i = 0; i <= line_scale; i++) {
        data.push(0)
    }

    for (let i = 0; i < l.length; i++) {
        if (l[i][0] >= l[i][1]) {
            continue
        }

        if (l[i][0] >= 0) {
            data[l[i][0]] += 1
        }

        if (l[i][1] >= 0) {
            data[l[i][1]] -= 1
        }
    }

    for (let i = 1; i <= line_scale; i++) {
        data[i] += data[i - 1]
        if (sink_r[i - 1]) {
            data[i - 1] += sink_r[i - 1]
        }
    }
    if (sink_r[line_scale]) {
        data[line_scale] += sink_r[line_scale]
    }

    return data
}

function calculate_concurrency_instant(l) {
    let start_time = vue.work_data.start_time
    let end_time = vue.work_data.end_time
    let step = (end_time - start_time) / line_scale

    let sum = start_time
    let time_mark = []
    let data = []
    for (let i = 0; i < line_scale; i++) {
        time_mark.push(sum)
        sum += step

        data.push(0)
    }

    for (let i = 0; i < l.length; i++) {
        if (l[i][0] < 0 || l[i][1] < 0) {
            continue
        }
        for (let j=0; j < line_scale; j++) {
            if (l[i][0] <= time_mark[j] && time_mark[j] <= l[i][1]) {
                data[j] += 1
            }
        }
    }

    return data
}

function calculate_sum(l) {
    let start_time = vue.work_data.start_time
    let end_time = vue.work_data.end_time

    for (let i = 0; i < l.length; i++) {
        l[i] = l[i] - start_time
    }

    let step = (end_time - start_time) / line_scale
    for (let i = 0; i < l.length; i++) {
        if (l[i] < 0) {
            l[i] = -1
        } else {
            l[i] = Math.trunc(l[i] / step) + (l[i] % step > step / 2)
        }
    }

    let data = []
    for (let i = 0; i <= line_scale; i++) {
        data.push(0)
    }

    for (let i = 0; i < l.length; i++) {
        if (l[i] >= 0) {
            data[l[i]] += 1
        }
    }

    for (let i = 1; i <= line_scale; i++) {
        data[i] += data[i - 1]
    }

    return data
}

var chartDict = {}

function createLineChart(conf) {
    var datasets = []
    for (var i=0; i<conf.data.series.length; i++) {
        var d = conf.data.series[i];
        datasets.push({
            label: d.name,
            data: d.data,
            pointRadius: 1.5,
            borderWidth: 2,
            backgroundColor: d.color,
            borderColor: d.color
        })
    }

    var target = conf.selector.replace('#','')
    if (chartDict[target]) {
        chartDict[target].destroy()
    }
    var ctx = document.getElementById(target).getContext('2d');
    chartDict[target] = new Chart(ctx, {
        type: 'line',
        data: {
            labels: conf.data.categories,
            datasets: datasets
        },
        options: {
            responsive: true,
            maintainAspectRatio: false,
            plugins: {
                legend: {
                    position: 'bottom'
                }
            },
            scales: {
                y: {
                    beginAtZero: true
                }
            }
        }
    });
}

let color_set = ["#2d73f5", "#28b4c8", "#78d237", "#ffd246", "orange", "#ff6358", "#aa46be", "pink", "#b99c39", "#b8728b", "#70a34f"]

function initConcurrencyChart(target) {
    let lockList = []
    for (let i = 0; i < vue.jobs_data.length; i++) {
        lockList.push([vue.jobs_data[i].remote_work_lock_time, vue.jobs_data[i].remote_work_unlock_time])
    }
    let lockData = calculate_concurrency(lockList)

    let publicPrepareList = []
    for (let i = 0; i < vue.jobs_data.length; i++) {
        publicPrepareList.push([vue.jobs_data[i].remote_work_lock_time, vue.jobs_data[i].remote_work_send_start_time])
    }
    let publicPrepareData = calculate_concurrency(publicPrepareList)

    let sendList = []
    for (let i = 0; i < vue.jobs_data.length; i++) {
        sendList.push([vue.jobs_data[i].remote_work_send_start_time, vue.jobs_data[i].remote_work_send_end_time])
    }
    let sendData = calculate_concurrency(sendList)

    let processList = []
    for (let i = 0; i < vue.jobs_data.length; i++) {
        processList.push([vue.jobs_data[i].remote_work_process_start_time, vue.jobs_data[i].remote_work_process_end_time])
    }
    let processData = calculate_concurrency(processList)

    let localLockList = []
    for (let i = 0; i < vue.jobs_data.length; i++) {
        localLockList.push([vue.jobs_data[i].local_work_lock_time, vue.jobs_data[i].local_work_unlock_time])
    }
    let localLockData = calculate_concurrency(localLockList)

    let localPreList = []
    for (let i = 0; i < vue.jobs_data.length; i++) {
        localPreList.push([vue.jobs_data[i].pre_work_lock_time, vue.jobs_data[i].pre_work_unlock_time])
    }
    let localPreData = calculate_concurrency(localPreList)

    let receiveList = []
    for (let i = 0; i < vue.jobs_data.length; i++) {
        receiveList.push([vue.jobs_data[i].remote_work_receive_start_time, vue.jobs_data[i].remote_work_receive_end_time])
    }
    let receiveData = calculate_concurrency(receiveList)

    let localPostList = []
    for (let i = 0; i < vue.jobs_data.length; i++) {
        localPostList.push([vue.jobs_data[i].post_work_start_time, vue.jobs_data[i].post_work_end_time])
    }
    let localPostData = calculate_concurrency(localPostList)

    let localTotalList = []
    for (let i = 0; i < vue.jobs_data.length; i++) {
        localTotalList.push([vue.jobs_data[i].enter_time, vue.jobs_data[i].leave_time])
    }
    let localTotalData = calculate_concurrency(localTotalList)

    let categories = []
    let delta = (vue.work_data.end_time - vue.work_data.start_time) / line_scale
    let sum = 0
    for (let i = 0; i < line_scale; i++) {
        categories.push(vue.time_format(sum) + "(" + vue.date_format(vue.work_data.start_time + sum) + ")")
        sum += delta
    }

    createLineChart({
        selector: target,
        type: "line",
        data: {
            "series": [{
                "name": "分布式处理",
                "data": lockData,
                "color": color_set[0],
                "markers": {
                    size: marker_size
                }
            }, {
                "name": "分发文件",
                "data": sendData,
                "color": color_set[1],
                "markers": {
                    size: marker_size
                }
            }, {
                "name": "远程实际处理",
                "data": processData,
                "color": color_set[2],
                "markers": {
                    size: marker_size
                }
            }, {
                "name": "本地处理",
                "data": localLockData,
                "color": color_set[3],
                "markers": {
                    size: marker_size
                }
            }, {
                "name": "本地预处理",
                "data": localPreData,
                "color": color_set[4],
                "markers": {
                    size: marker_size
                }
            }, {
                "name": "接收文件",
                "data": receiveData,
                "color": color_set[5],
                "markers": {
                    size: marker_size
                }
            }, {
                "name": "本地后置处理",
                "data": localPostData,
                "color": color_set[6],
                "markers": {
                    size: marker_size
                }
            }, {
                "name": "本地总并发",
                "data": localTotalData,
                "color": color_set[7],
                "markers": {
                    size: marker_size
                }
            }, {
                "name": "分发公共文件",
                "data": publicPrepareData,
                "color": color_set[8],
                "markers": {
                    size: marker_size
                }
            }],
            "categories": categories
        }
    });
}

function initConcurrencyChartInstant(target) {
    let lockList = []
    for (let i = 0; i < vue.jobs_data.length; i++) {
        lockList.push([vue.jobs_data[i].remote_work_lock_time, vue.jobs_data[i].remote_work_unlock_time])
    }
    let lockData = calculate_concurrency_instant(lockList)

    let sendList = []
    for (let i = 0; i < vue.jobs_data.length; i++) {
        sendList.push([vue.jobs_data[i].remote_work_send_start_time, vue.jobs_data[i].remote_work_send_end_time])
    }
    let sendData = calculate_concurrency_instant(sendList)

    let processList = []
    for (let i = 0; i < vue.jobs_data.length; i++) {
        processList.push([vue.jobs_data[i].remote_work_process_start_time, vue.jobs_data[i].remote_work_process_end_time])
    }
    let processData = calculate_concurrency_instant(processList)

    let localLockList = []
    for (let i = 0; i < vue.jobs_data.length; i++) {
        localLockList.push([vue.jobs_data[i].local_work_lock_time, vue.jobs_data[i].local_work_unlock_time])
    }
    let localLockData = calculate_concurrency_instant(localLockList)

    let localPreList = []
    for (let i = 0; i < vue.jobs_data.length; i++) {
        localPreList.push([vue.jobs_data[i].pre_work_lock_time, vue.jobs_data[i].pre_work_unlock_time])
    }
    let localPreData = calculate_concurrency_instant(localPreList)

    let receiveList = []
    for (let i = 0; i < vue.jobs_data.length; i++) {
        receiveList.push([vue.jobs_data[i].remote_work_receive_start_time, vue.jobs_data[i].remote_work_receive_end_time])
    }
    let receiveData = calculate_concurrency_instant(receiveList)

    let localPostList = []
    for (let i = 0; i < vue.jobs_data.length; i++) {
        localPostList.push([vue.jobs_data[i].post_work_start_time, vue.jobs_data[i].post_work_end_time])
    }
    let localPostData = calculate_concurrency_instant(localPostList)

    let localTotalList = []
    for (let i = 0; i < vue.jobs_data.length; i++) {
        localTotalList.push([vue.jobs_data[i].enter_time, vue.jobs_data[i].leave_time])
    }
    let localTotalData = calculate_concurrency_instant(localTotalList)

    let categories = []
    let delta = (vue.work_data.end_time - vue.work_data.start_time) / line_scale
    let sum = 0
    for (let i = 0; i < line_scale; i++) {
        categories.push(vue.time_format(sum) + "(" + vue.date_format(vue.work_data.start_time + sum) + ")")
        sum += delta
    }

    createLineChart({
        selector: target,
        type: "line",
        data: {
            "series": [{
                "name": "分布式处理",
                "data": lockData,
                "color": color_set[0],
                "markers": {
                    size: marker_size
                }
            }, {
                "name": "分发文件",
                "data": sendData,
                "color": color_set[1],
                "markers": {
                    size: marker_size
                }
            }, {
                "name": "远程实际处理",
                "data": processData,
                "color": color_set[2],
                "markers": {
                    size: marker_size
                }
            }, {
                "name": "本地处理",
                "data": localLockData,
                "color": color_set[3],
                "markers": {
                    size: marker_size
                }
            }, {
                "name": "本地预处理",
                "data": localPreData,
                "color": color_set[4],
                "markers": {
                    size: marker_size
                }
            }, {
                "name": "接收文件",
                "data": receiveData,
                "color": color_set[5],
                "markers": {
                    size: marker_size
                }
            }, {
                "name": "本地后置处理",
                "data": localPostData,
                "color": color_set[6],
                "markers": {
                    size: marker_size
                }
            }, {
                "name": "本地总并发",
                "data": localTotalData,
                "color": color_set[7],
                "markers": {
                    size: marker_size
                }
            }],
            "categories": categories
        }
    });
}

function initConcurrencyChartInstant(target) {
    let lockList = []
    for (let i = 0; i < vue.jobs_data.length; i++) {
        lockList.push([vue.jobs_data[i].remote_work_lock_time, vue.jobs_data[i].remote_work_unlock_time])
    }
    let lockData = calculate_concurrency_instant(lockList)

    let sendList = []
    for (let i = 0; i < vue.jobs_data.length; i++) {
        sendList.push([vue.jobs_data[i].remote_work_send_start_time, vue.jobs_data[i].remote_work_send_end_time])
    }
    let sendData = calculate_concurrency_instant(sendList)

    let processList = []
    for (let i = 0; i < vue.jobs_data.length; i++) {
        processList.push([vue.jobs_data[i].remote_work_process_start_time, vue.jobs_data[i].remote_work_process_end_time])
    }
    let processData = calculate_concurrency_instant(processList)

    let localLockList = []
    for (let i = 0; i < vue.jobs_data.length; i++) {
        localLockList.push([vue.jobs_data[i].local_work_lock_time, vue.jobs_data[i].local_work_unlock_time])
    }
    let localLockData = calculate_concurrency_instant(localLockList)

    let localPreList = []
    for (let i = 0; i < vue.jobs_data.length; i++) {
        localPreList.push([vue.jobs_data[i].pre_work_lock_time, vue.jobs_data[i].pre_work_unlock_time])
    }
    let localPreData = calculate_concurrency_instant(localPreList)

    let receiveList = []
    for (let i = 0; i < vue.jobs_data.length; i++) {
        receiveList.push([vue.jobs_data[i].remote_work_receive_start_time, vue.jobs_data[i].remote_work_receive_end_time])
    }
    let receiveData = calculate_concurrency_instant(receiveList)

    let localPostList = []
    for (let i = 0; i < vue.jobs_data.length; i++) {
        localPostList.push([vue.jobs_data[i].post_work_start_time, vue.jobs_data[i].post_work_end_time])
    }
    let localPostData = calculate_concurrency_instant(localPostList)

    let localTotalList = []
    for (let i = 0; i < vue.jobs_data.length; i++) {
        localTotalList.push([vue.jobs_data[i].enter_time, vue.jobs_data[i].leave_time])
    }
    let localTotalData = calculate_concurrency_instant(localTotalList)

    let categories = []
    let delta = (vue.work_data.end_time - vue.work_data.start_time) / line_scale
    let sum = 0
    for (let i = 0; i < line_scale; i++) {
        categories.push(vue.time_format(sum) + "(" + vue.date_format(vue.work_data.start_time + sum) + ")")
        sum += delta
    }

    createLineChart({
        selector: target,
        type: "line",
        data: {
            "series": [{
                "name": "分布式处理",
                "data": lockData,
                "color": color_set[0],
                "markers": {
                    size: marker_size
                }
            }, {
                "name": "分发文件",
                "data": sendData,
                "color": color_set[1],
                "markers": {
                    size: marker_size
                }
            }, {
                "name": "远程实际处理",
                "data": processData,
                "color": color_set[2],
                "markers": {
                    size: marker_size
                }
            }, {
                "name": "本地处理",
                "data": localLockData,
                "color": color_set[3],
                "markers": {
                    size: marker_size
                }
            }, {
                "name": "本地预处理",
                "data": localPreData,
                "color": color_set[4],
                "markers": {
                    size: marker_size
                }
            }, {
                "name": "接收文件",
                "data": receiveData,
                "color": color_set[5],
                "markers": {
                    size: marker_size
                }
            }, {
                "name": "本地后置处理",
                "data": localPostData,
                "color": color_set[6],
                "markers": {
                    size: marker_size
                }
            }, {
                "name": "本地总并发",
                "data": localTotalData,
                "color": color_set[7],
                "markers": {
                    size: marker_size
                }
            }],
            "categories": categories
        }
    });
}

function initSumChart(target) {
    let processList = []
    let processErrorList = []
    let processBreakList = []
    for (let i = 0; i < vue.jobs_data.length; i++) {
        if (vue.jobs_data[i].remote_work_success && vue.jobs_data[i].post_work_success) {
            processList.push(vue.jobs_data[i].remote_work_leave_time)
            continue
        }

        if (vue.jobs_data[i].remote_work_success && !vue.jobs_data[i].post_work_success) {
            processErrorList.push(vue.jobs_data[i].remote_work_leave_time)
            continue
        }

        if (vue.jobs_data[i].pre_work_success && !vue.jobs_data[i].remote_work_success) {
            processBreakList.push(vue.jobs_data[i].remote_work_leave_time)
            continue
        }
    }
    let processData = calculate_sum(processList)
    let processErrorData = calculate_sum(processErrorList)
    let processBreakData = calculate_sum(processBreakList)

    let localList = []
    let localErrorList = []
    for (let i = 0; i < vue.jobs_data.length; i++) {
        if (vue.jobs_data[i].local_work_success) {
            localList.push(vue.jobs_data[i].local_work_leave_time)
            continue
        }

        if (!vue.jobs_data[i].remote_work_success) {
            localErrorList.push(vue.jobs_data[i].local_work_leave_time)
        }
    }
    let localData = calculate_sum(localList)
    let localErrorData = calculate_sum(localErrorList)

    let preList = []
    let preErrorList = []
    for (let i = 0; i < vue.jobs_data.length; i++) {
        if (vue.jobs_data[i].pre_work_success) {
            preList.push(vue.jobs_data[i].pre_work_leave_time)
            continue
        }

        preErrorList.push(vue.jobs_data[i].pre_work_leave_time)
    }
    let preData = calculate_sum(preList)
    let preErrorData = calculate_sum(preErrorList)

    let fileSentList = []
    let fileReceivedList = []
    for (let i = 0; i < vue.jobs_data.length; i++) {
        if (vue.jobs_data[i].remote_work_send_end_time > 0) {
            fileSentList.push(vue.jobs_data[i].remote_work_send_end_time)
        }

        if (vue.jobs_data[i].remote_work_receive_end_time > 0) {
            fileReceivedList.push(vue.jobs_data[i].remote_work_receive_end_time)
        }
    }
    let fileSentData = calculate_sum(fileSentList)
    let fileReceivedData = calculate_sum(fileReceivedList)

    let categories = []
    let delta = (vue.work_data.end_time - vue.work_data.start_time) / line_scale
    let sum = 0
    for (let i = 0; i < line_scale; i++) {
        categories.push(vue.time_format(sum) + "(" + vue.date_format(vue.work_data.start_time + sum) + ")")
        sum += delta
    }

    createLineChart({
        selector: target,
        type: "line",
        data: {
            "series": [{
                "name": "远程处理成功任务数",
                "data": processData,
                "color": color_set[0],
                "markers": {
                    size: marker_size,
                }
            }, {
                "name": "本地处理成功任务数",
                "data": localData,
                "color": color_set[1],
                "markers": {
                    size: marker_size,
                }
            }, {
                "name": "本地预处理成功任务数",
                "data": preData,
                "color": color_set[2],
                "markers": {
                    size: marker_size,
                }
            }, {
                "name": "远程处理失败任务数",
                "data": processErrorData,
                "color": color_set[3],
                "markers": {
                    size: marker_size,
                }
            }, {
                "name": "远程处理中断任务数",
                "data": processBreakData,
                "color": color_set[4],
                "markers": {
                    size: marker_size,
                }
            }, {
                "name": "本地处理失败任务数",
                "data": localErrorData,
                "color": color_set[5],
                "markers": {
                    size: marker_size,
                }
            }, {
                "name": "本地预处理未通过任务数",
                "data": preErrorData,
                "color": color_set[6],
                "markers": {
                    size: marker_size,
                }
            }, {
                "name": "分发任务数",
                "data": fileSentData,
                "color": color_set[7],
                "markers": {
                    size: marker_size,
                }
            }, {
                "name": "接收任务数",
                "data": fileReceivedData,
                "color": color_set[8],
                "markers": {
                    size: marker_size,
                }
            }],
            "categories": categories
        }
    });
}

function initWaitingChart(target) {
    let preWorkWaitList = []
    for (let i = 0; i < vue.jobs_data.length; i++) {
        if (vue.jobs_data[i].pre_work_enter_time <= 0) {
            continue
        }
        preWorkWaitList.push([vue.jobs_data[i].pre_work_enter_time, vue.jobs_data[i].pre_work_lock_time])
    }
    let preWorkWaitData = calculate_concurrency(preWorkWaitList)

    let localWorkWaitList = []
    for (let i = 0; i < vue.jobs_data.length; i++) {
        if (vue.jobs_data[i].local_work_enter_time <= 0) {
            continue
        }
        localWorkWaitList.push([vue.jobs_data[i].local_work_enter_time, vue.jobs_data[i].local_work_lock_time])
    }
    let localWorkWaitData = calculate_concurrency(localWorkWaitList)

    let postWorkWaitList = []
    for (let i = 0; i < vue.jobs_data.length; i++) {
        if (vue.jobs_data[i].post_work_enter_time <= 0) {
            continue
        }
        postWorkWaitList.push([vue.jobs_data[i].post_work_enter_time, vue.jobs_data[i].post_work_lock_time])
    }
    let postWorkWaitData = calculate_concurrency(postWorkWaitList)

    let remoteWorkWaitList = []
    for (let i = 0; i < vue.jobs_data.length; i++) {
        if (vue.jobs_data[i].remote_work_enter_time <= 0) {
            continue
        }
        remoteWorkWaitList.push([vue.jobs_data[i].remote_work_enter_time, vue.jobs_data[i].remote_work_lock_time])
    }
    let remoteWorkWaitData = calculate_concurrency(remoteWorkWaitList)

    let remoteProcessWaitList = []
    for (let i = 0; i < vue.jobs_data.length; i++) {
        if (vue.jobs_data[i].remote_work_process_start_time <= 0) {
            continue
        }
        remoteProcessWaitList.push([vue.jobs_data[i].remote_work_send_end_time, vue.jobs_data[i].remote_work_process_start_time])
    }
    let remoteProcessWaitData = calculate_concurrency(remoteProcessWaitList)

    let categories = []
    let delta = (vue.work_data.end_time - vue.work_data.start_time) / line_scale
    let sum = 0
    for (let i = 0; i < line_scale; i++) {
        categories.push(vue.time_format(sum) + "(" + vue.date_format(vue.work_data.start_time + sum) + ")")
        sum += delta
    }

    createLineChart({
        selector: target,
        type: "line",
        data: {
            "series": [{
                "name": "预处理锁等待队列",
                "data": preWorkWaitData,
                "color": color_set[0],
                "markers": {
                    size: marker_size
                }
            }, {
                "name": "本地处理锁等待队列",
                "data": localWorkWaitData,
                "color": color_set[1],
                "markers": {
                    size: marker_size
                }
            }, {
                "name": "后置处理锁等待队列",
                "data": postWorkWaitData,
                "color": color_set[2],
                "markers": {
                    size: marker_size
                }
            }, {
                "name": "分布式处理等待队列",
                "data": remoteWorkWaitData,
                "color": color_set[3],
                "markers": {
                    size: marker_size
                }
            }, {
                "name": "分布式实际处理等待队列",
                "data": remoteProcessWaitData,
                "color": color_set[4],
                "markers": {
                    size: marker_size
                }
            }],
            "categories": categories
        }
    });
}

function initWaitingChart_instant(target) {
    let preWorkWaitList = []
    for (let i = 0; i < vue.jobs_data.length; i++) {
        if (vue.jobs_data[i].pre_work_enter_time <= 0) {
            continue
        }
        preWorkWaitList.push([vue.jobs_data[i].pre_work_enter_time, vue.jobs_data[i].pre_work_lock_time])
    }
    let preWorkWaitData = calculate_concurrency_instant(preWorkWaitList)

    let localWorkWaitList = []
    for (let i = 0; i < vue.jobs_data.length; i++) {
        if (vue.jobs_data[i].local_work_enter_time <= 0) {
            continue
        }
        localWorkWaitList.push([vue.jobs_data[i].local_work_enter_time, vue.jobs_data[i].local_work_lock_time])
    }
    let localWorkWaitData = calculate_concurrency_instant(localWorkWaitList)

    let postWorkWaitList = []
    for (let i = 0; i < vue.jobs_data.length; i++) {
        if (vue.jobs_data[i].post_work_enter_time <= 0) {
            continue
        }
        postWorkWaitList.push([vue.jobs_data[i].post_work_enter_time, vue.jobs_data[i].post_work_lock_time])
    }
    let postWorkWaitData = calculate_concurrency_instant(postWorkWaitList)

    let remoteWorkWaitList = []
    for (let i = 0; i < vue.jobs_data.length; i++) {
        if (vue.jobs_data[i].remote_work_enter_time <= 0) {
            continue
        }
        remoteWorkWaitList.push([vue.jobs_data[i].remote_work_enter_time, vue.jobs_data[i].remote_work_lock_time])
    }
    let remoteWorkWaitData = calculate_concurrency_instant(remoteWorkWaitList)

    let remoteProcessWaitList = []
    for (let i = 0; i < vue.jobs_data.length; i++) {
        if (vue.jobs_data[i].remote_work_process_start_time <= 0) {
            continue
        }
        remoteProcessWaitList.push([vue.jobs_data[i].remote_work_send_end_time, vue.jobs_data[i].remote_work_process_start_time])
    }
    let remoteProcessWaitData = calculate_concurrency_instant(remoteProcessWaitList)

    let categories = []
    let delta = (vue.work_data.end_time - vue.work_data.start_time) / line_scale
    let sum = 0
    for (let i = 0; i < line_scale; i++) {
        categories.push(vue.time_format(sum) + "(" + vue.date_format(vue.work_data.start_time + sum) + ")")
        sum += delta
    }

    createLineChart({
        selector: target,
        type: "line",
        data: {
            "series": [{
                "name": "预处理锁等待队列",
                "data": preWorkWaitData,
                "color": color_set[0],
                "markers": {
                    size: marker_size
                }
            }, {
                "name": "本地处理锁等待队列",
                "data": localWorkWaitData,
                "color": color_set[1],
                "markers": {
                    size: marker_size
                }
            }, {
                "name": "后置处理锁等待队列",
                "data": remoteWorkWaitData,
                "color": color_set[2],
                "markers": {
                    size: marker_size
                }
            }, {
                "name": "分布式处理等待队列",
                "data": remoteWorkWaitData,
                "color": color_set[3],
                "markers": {
                    size: marker_size
                }
            }, {
                "name": "分布式实际处理等待队列",
                "data": remoteProcessWaitData,
                "color": color_set[4],
                "markers": {
                    size: marker_size
                }
            }],
            "categories": categories
        }
    });
}

function initProcessTimeChart(target) {
    pre_l = []
    exe_l = []
    post_l = []
    remote_l = []

    stone = [1, 2, 5, 10, 20, 50, 100, 200, 500, 99999999]

    for (let i = 0; i < vue.jobs_data.length; i++) {
        data = vue.jobs_data[i]

        pre_t = (data.pre_work_end_time - data.pre_work_start_time) / 1e9
        exe_t = (data.local_work_end_time - data.local_work_start_time) / 1e9
        post_t = (data.post_work_end_time - data.post_work_start_time) / 1e9
        remote_t = (data.remote_work_process_end_time - data.remote_work_process_start_time) / 1e9

        if (pre_t > 0 && data.pre_work_success) {
            for (let x = 0; x < stone.length; x++) {
                if (pre_t <= stone[x]) {
                    if (pre_l[x] == undefined) {
                        pre_l[x] = 0
                    }
                    pre_l[x] += 1
                    break
                }
            }
        }

        if (exe_t > 0 && data.local_work_success) {
            for (let x = 0; x < stone.length; x++) {
                if (exe_t <= stone[x]) {
                    if (exe_l[x] == undefined) {
                        exe_l[x] = 0
                    }
                    exe_l[x] += 1
                    break
                }
            }
        }

        if (post_t > 0 && data.post_work_success) {
            for (let x = 0; x < stone.length; x++) {
                if (post_t <= stone[x]) {
                    if (post_l[x] == undefined) {
                        post_l[x] = 0
                    }
                    post_l[x] += 1
                    break
                }
            }
        }

        if (remote_t > 0 && data.remote_work_success && data.post_work_success) {
            for (let x = 0; x < stone.length; x++) {
                if (remote_t <= stone[x]) {
                    if (remote_l[x] == undefined) {
                        remote_l[x] = 0
                    }
                    remote_l[x] += 1
                    break
                }
            }
        }
    }

    var datasets = [{
        label: "预处理执行耗时分布",
        data: pre_l,
        backgroundColor: color_set[0],
        borderColor: color_set[0]
    },{
        label: "本地处执行理耗时分布",
        data: exe_l,
        backgroundColor: color_set[1],
        borderColor: color_set[1]
    },{
        label: "后置处执行理耗时分布",
        data: post_l,
        backgroundColor: color_set[2],
        borderColor: color_set[2]
    },{
        label: "分布式处理执行耗时分布",
        data: remote_l,
        backgroundColor: color_set[3],
        borderColor: color_set[3]
    }]

    target = target.replace('#','')
    if (chartDict[target]) {
        chartDict[target].destroy()
    }
    var ctx = document.getElementById(target).getContext('2d');
    chartDict[target] = new Chart(ctx, {
        type: 'bar',
        data: {
            labels: ["1s", "2s", "5s", "10s", "20s", "50s", "100s", "200s", "500s", ">500s"],
            datasets: datasets
        },
        options: {
            responsive: true,
            maintainAspectRatio: false,
            plugins: {
                legend: {
                    position: 'bottom'
                }
            },
            scales: {
                y: {
                    beginAtZero: true
                }
            }
        }
    });
}

function initCommandTimeChart(target) {
    pre_l = []
    exe_l = []
    post_l = []
    remote_l = []

    stone = [1, 2, 5, 10, 20, 50, 100, 200, 500, 99999999]
    datasets = []

    for (let i = 0; i < vue.jobs_data.length; i++) {
        data = vue.jobs_data[i]
        if (!data.origin_args) {
            continue
        }
        name = vue.baseName(data.origin_args[0])
        pre_t = (data.pre_work_end_time - data.pre_work_start_time) / 1e9
        exe_t = (data.local_work_end_time - data.local_work_start_time) / 1e9
        remote_t = (data.remote_work_process_end_time - data.remote_work_process_start_time) / 1e9

        if (pre_t > 0 && data.pre_work_success) {
            let pre_name = name + "预处理"
            let j
            for (j = 0; j < datasets.length; j++) {
                if (datasets[j].label == pre_name) {
                    break
                }
            }
            if (j >= datasets.length) {
                datasets.push({
                    label: pre_name,
                    data: [],
                    backgroundColor: color_set[j],
                    borderColor: color_set[j]
                })
            }

            for (let x = 0; x < stone.length; x++) {
                if (pre_t <= stone[x]) {
                    if (datasets[j].data[x] == undefined) {
                        datasets[j].data[x] = 0
                    }
                    datasets[j].data[x] += 1
                    break
                }
            }
        }

        if (exe_t > 0 && data.local_work_success) {
            let exe_name = name + "本地"
            let j
            for (j = 0; j < datasets.length; j++) {
                if (datasets[j].label == exe_name) {
                    break
                }
            }
            if (j >= datasets.length) {
                datasets.push({
                    label: exe_name,
                    data: [],
                    backgroundColor: color_set[j],
                    borderColor: color_set[j]
                })
            }

            for (let x = 0; x < stone.length; x++) {
                if (exe_t <= stone[x]) {
                    if (datasets[j].data[x] == undefined) {
                        datasets[j].data[x] = 0
                    }
                    datasets[j].data[x] += 1
                    break
                }
            }
        }

        if (remote_t > 0 && data.remote_work_success && data.post_work_success) {
            let remote_name = name + "远程"
            let j
            for (j = 0; j < datasets.length; j++) {
                if (datasets[j].label == remote_name) {
                    break
                }
            }
            if (j >= datasets.length) {
                datasets.push({
                    label: remote_name,
                    data: [],
                    backgroundColor: color_set[j],
                    borderColor: color_set[j]
                })
            }

            for (let x = 0; x < stone.length; x++) {
                if (remote_t <= stone[x]) {
                    if (datasets[j].data[x] == undefined) {
                        datasets[j].data[x] = 0
                    }
                    datasets[j].data[x] += 1
                    break
                }
            }
        }
    }

    target = target.replace('#','')
    if (chartDict[target]) {
        chartDict[target].destroy()
    }
    var ctx = document.getElementById(target).getContext('2d');
    chartDict[target] = new Chart(ctx, {
        type: 'bar',
        data: {
            labels: ["1s", "2s", "5s", "10s", "20s", "50s", "100s", "200s", "500s", ">500s"],
            datasets: datasets
        },
        options: {
            responsive: true,
            maintainAspectRatio: false,
            plugins: {
                legend: {
                    position: 'bottom'
                }
            },
            scales: {
                y: {
                    beginAtZero: true
                }
            }
        }
    });
}

function getTD(content) {
    return '<td>' + content + '</td>'
}

function CreateReporter() {
    let type = 'application/vnd.ms-excel'
    let data = '<table><tr>'
    data += getTD("命令类型")
    data += getTD("完整命令")
    data += getTD("精简命令")
    data += getTD("在远程执行成功")
    data += getTD("在远程执行失败")
    data += getTD("在远程异常中断")
    data += getTD("直接本地执行成功")
    data += getTD("直接本地执行失败")
    data += getTD("远程转本地执行成功")
    data += getTD("远程转本地执行失败")
    data += getTD("总时间")
    data += getTD("预处理时间")
    data += getTD("远程执行时间")
    data += getTD("后置处理时间")
    data += getTD("本地处理时间")
    data += getTD("开始时间")
    data += getTD("结束时间")
    data += '</tr>'

    console.log("generate excel line " + vue.jobs_data.length)
    for (let i = 0; i < vue.jobs_data.length; i++) {
        let item = vue.jobs_data[i]

        let simple_args = []
        for (let j = 0; j < item.origin_args.length; j++) {
            let arg = item.origin_args[j]
            if (arg.length > 2 && (arg.startsWith("-I") || arg.startsWith("-D") || arg.startsWith("-Wno"))) {
                continue
            }

            if (arg === "-isystem") {
                j++
                continue
            }

            simple_args.push(arg)
        }

        data += '<tr>'
        data += getTD(vue.baseName(item.origin_args[0]))
        data += getTD(item.origin_args.join("&nbsp;"))
        data += getTD(simple_args.join("&nbsp;"))
        data += getTD(item.remote_work_success && item.post_work_success)
        data += getTD(item.remote_work_success && !item.post_work_success)
        data += getTD(item.remote_work_start_time > 0 && !item.remote_work_success)
        data += getTD(!item.pre_work_success && item.local_work_success)
        data += getTD(!item.pre_work_success && !item.local_work_success)
        data += getTD(item.remote_work_start_time > 0 && !item.post_work_success && item.local_work_success)
        data += getTD(item.remote_work_start_time > 0 && !item.post_work_success && !item.local_work_success)
        data += getTD((item.leave_time - item.enter_time) / 1e9)
        data += getTD((item.pre_work_end_time - item.pre_work_start_time) / 1e9)
        data += getTD((item.remote_work_process_end_time - item.remote_work_process_start_time) / 1e9)
        data += getTD((item.post_work_end_time - item.post_work_start_time) / 1e9)
        data += getTD((item.local_work_end_time - item.local_work_start_time) / 1e9)
        data += getTD(vue.date_format(item.enter_time))
        data += getTD(vue.date_format(item.leave_time))

        data += '</tr>'
    }
    data += '</table>'

    let dl = document.createElement("a")
    document.body.appendChild(dl)

    dl.href = 'data:' + type + ', ' + data
    dl.download = "reporter_" + vue.work_data.work_id + ".xls"
    dl.click()
}

function setCookie(name, value, days) {
    let expires = "";
    if (days) {
        let date = new Date();
        date.setTime(date.getTime() + (days * 24 * 60 * 60 * 1000));
        expires = "; expires=" + date.toUTCString();
    }
    document.cookie = name + "=" + (value || "") + expires + "; path=/";
}

function getCookie(name) {
    let nameEQ = name + "=";
    let ca = document.cookie.split(';');
    for (let i = 0; i < ca.length; i++) {
        let c = ca[i];
        while (c.charAt(0) == ' ') c = c.substring(1, c.length);
        if (c.indexOf(nameEQ) == 0) return c.substring(nameEQ.length, c.length);
    }
    return null;
}

function eraseCookie(name) {
    document.cookie = name + '=; Path=/; Expires=Thu, 01 Jan 1970 00:00:01 GMT;';
}

let scene_cookie_key = "bk_scene"
let type_cookie_key = "bk_type"
let user_cookie_key = "bk_user"
let client_ip_cookie_key = "bk_client_ip"
let project_cookie_key = "bk_project"
let id_cookie_key = "bk_id"
let day_cookie_key = "bk_day"

function setScene(scene) {
    if (scene === null) {
        scene = getCookie(scene_cookie_key)
    }

    if (scene === null) {
        return
    }

    $("#input_scene").val(scene)
}

function saveScene() {
    eraseCookie(scene_cookie_key)
    setCookie(scene_cookie_key, $("#input_scene").val(), 180)
}

function setType(type) {
    if (type === null) {
        type = getCookie(type_cookie_key)
    }

    if (type === null) {
        return
    }

    $("#input_type").val(type)
}

function saveType() {
    eraseCookie(type_cookie_key)
    setCookie(type_cookie_key, $("#input_type").val(), 180)
}

function setUser(user) {
    if (user === null) {
        user = getCookie(user_cookie_key)
    }

    if (user === null) {
        return
    }

    $("#input_user").val(user)
}

function saveUser() {
    eraseCookie(user_cookie_key)
    setCookie(user_cookie_key, $("#input_user").val(), 180)
}

function setClientIP(client_ip) {
    if (client_ip === null) {
        client_ip = getCookie(client_ip_cookie_key)
    }

    if (client_ip === null) {
        return
    }

    $("#input_client_ip").val(client_ip)
}

function saveClientIP() {
    eraseCookie(client_ip_cookie_key)
    setCookie(client_ip_cookie_key, $("#input_client_ip").val(), 180)
}

function setProject(project) {
    if (project === null) {
        project = getCookie(project_cookie_key)
    }
    if (project === null) {
        return
    }

    $("#input_project").val(project)
    vue.start_list()
}

function saveProject() {
    eraseCookie(project_cookie_key)
    setCookie(project_cookie_key, $("#input_project").val(), 180)
}

function saveDay() {
    eraseCookie(day_cookie_key)
    setCookie(day_cookie_key, $("#input_day").val(), 180)
}

function setDay(day) {
    if (day === null) {
        day = getCookie(day_cookie_key)
    }
    if (day === null) {
        return
    }

    $("#input_day").val(day)
}

function saveID(id) {
    $("#input_data").val(id)
    eraseCookie(id_cookie_key)
    setCookie(id_cookie_key, id, 180)
}

function saveValues() {
    saveScene();
    saveType();
    saveUser();
    saveClientIP();
    saveDay();
    saveProject();
}

function getParameterByName(name, url = window.location.href) {
    name = name.replace(/[\[\]]/g, '\\$&');
    let regex = new RegExp('[?&]' + name + '(=([^&#]*)|&|#|$)'),
        results = regex.exec(url);
    if (!results) return null;
    if (!results[2]) return '';
    return decodeURIComponent(results[2].replace(/\+/g, ' '));
}

function copy2Clipboard(text) {
    let input = document.createElement('textarea');
    input.innerHTML = text;
    document.body.appendChild(input);
    input.select();
    let successful = document.execCommand('copy');
    document.body.removeChild(input);

    if (successful) {
        toastr.info('<br>已复制链接到剪贴板', "info")
    } else {
        toastr.error('<br>复制链接失败', "error")
    }
}

function notNull(text) {
    if (text === null) {
        return ""
    }
    return text
}

function shareListLink() {
    let base = window.location.href.split("?")[0]
    let project_id = notNull(getCookie(project_cookie_key))
    let scene = notNull(getCookie(scene_cookie_key))
    let type = notNull(getCookie(type_cookie_key))
    let user = notNull(getCookie(user_cookie_key))
    let client_ip = notNull(getCookie(client_ip_cookie_key))
    let day = notNull(getCookie(day_cookie_key))
    copy2Clipboard(base + "?project_id=" + project_id + "&scene=" + scene + "&type=" + type + "&user=" + user + "&client_ip=" + client_ip + "&day=" + day)
}

function shareDetailLink() {
    let base = window.location.href.split("?")[0]
    let id = notNull(getCookie(id_cookie_key))
    copy2Clipboard(base + "?id=" + id)
}

function fast_re() {
    for (let i=0; i<vue.jobs_data.length; i++) {
        let data = vue.jobs_data[i];
        if (data.remote_work_start_time > 0 && (!data.remote_work_success || !data.post_work_success)) {
            console.log(data.origin_args.join(" "))
            console.log(data.remote_error_message)
        }
    }
}

function showLoading() {
    $('#loding').hide();
    $('#loding').show();
}

function hideLoading() {
    $('#loding').hide();
}

$(function () {
    setScene(getParameterByName("scene"))
    setType(getParameterByName("type"))
    setUser(getParameterByName("user"))
    setClientIP(getParameterByName("client_ip"))
    setDay(getParameterByName("day"))
    setProject(getParameterByName("project_id"))

    let id = getParameterByName("id")
    if (id) {
        $("#input_data").val(id)
        vue.start()
    }
});