var default_work_data = {
    job_remote_ok: 0,
    job_remote_error: 0,
    job_local_ok: 0,
    job_local_error: 0,
    remote_work_waiting: 0,
    remote_work_holding: 0,
    remote_work_held: 0,
    remote_work_released: 0,
    pre_work_waiting: 0,
    pre_work_holding: 0,
    pre_work_held: 0,
    pre_work_released: 0,
    local_work_waiting: 0,
    local_work_holding: 0,
    local_work_held: 0,
    local_work_released: 0,
    post_work_waiting: 0,
    post_work_holding: 0,
    post_work_held: 0,
    post_work_released: 0,
    dependent_waiting: 0,
    sending: 0,
    receiving: 0,
    real_remote_error: 0
}
var default_task_data = {
    concurrency_chart_created: false,
    sum_chart_created: false,
    work_status: "未知",
    total_time: "未开始",
    apply_time: "未开始",
    real_time: "未开始",
    client_cpu: 0,
    cpu_total: 0,
    queue_name: "未知",
    source_ip: "未知",
    client_version: "未知",
    worker_version: "未知"
}
var default_jobs_data = {
}

var vue = new Vue({
    el: ".data-holder",
    data: {
        current: "default",
        work_list: [],
        current_data: default_work_data,
        current_task: default_task_data,
        work_data: {
            "default": default_work_data
        },
        task_data: {
            "default": default_task_data
        },
        jobs_data: {
            "default": default_jobs_data
        }
    },
    computed: {
        work_status: function() {
            var status = this.current_data.status

            if (status == "registered") {
                return "申请资源中"
            }

            if (status == "working") {
                return "编译中"
            }

            if (status == "ended" || status == "unregistered" || status == "removable") {
                if (this.current_data.success) {
                    return "编译成功"
                }

                return "编译失败"
            }

            return "未知"
        },
        total_time: function() {
            var data = this.current_data

            if (data.registered_time > 0) {
                return this.time_format(data.current_time - data.registered_time)
            }

            return "未知"
        },
        apply_time: function() {
            var data = this.current_data

            if (data.start_time > 0) {
                return this.time_format(data.start_time - data.registered_time)
            }

            return this.time_format(data.current_time - data.registered_time)
        },
        real_time: function() {
            var data = this.current_data

            if (data.start_time > 0) {
                return this.time_format(data.current_time - data.start_time)
            }

            return "未开始"
        }
    },
    methods: {
        fixer: function (data, num) {
            var temp = data.toFixed(10);
            return parseFloat(temp.substring(0, temp.lastIndexOf('.')+num+1));
        },
        time_format: function (nano_sec) {
            var hour = 0
            var min = 0
            var sec = nano_sec / 1e9
            if (sec >= 60) {
                min = Math.trunc(sec / 60)
                sec = sec - min * 60
            }
            if (min >= 60) {
                hour = Math.trunc(min / 60)
                min = min - hour * 60
            }

            sec = this.fixer(sec, 0)

            if (hour > 0) {
                return hour.toString() + "h" + min.toString() + "m" + sec.toString() + "s"
            }
            if (min > 0) {
                return min.toString() + "m" + sec.toString() + "s"
            }
            return sec.toString() + "s"
        },
        date_format: function (nano_sec) {
            var date = new Date(nano_sec/1e6);
            var hours = date.getHours();
            var minutes = "0" + date.getMinutes();
            var seconds = "0" + date.getSeconds();
            var formattedTime = hours + ':' + minutes.substr(-2) + ':' + seconds.substr(-2);
            return formattedTime
        },
        start: function(work_id) {
            this.current = work_id
            this.current_data = this.work_data[work_id]
            setTimeout(refresh_work_detail, 1000, work_id, 0)
        }
    }
})

function _arrayBufferToString(buffer) {
    var binary = '';
    var bytes = new Uint16Array(buffer);
    var len = bytes.byteLength;
    var char
    var code
    for (var i = 0; i < len; i++) {
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
    var strData = atob(src)
    var charData = strData.split('').map(function(x){return x.charCodeAt(0);});
    var data = pako.inflate(new Uint8Array(charData))
    return _arrayBufferToString(data)
}

var get = function(url, success, error) {
    $.ajax({
        url: url,
        dataType: "json",
        crossDomain: true,
        success: success,
        error: error
    });
}

var line_scale = 150
var marker_size = 3
var history_data = ""

function calculate_concurrency(l, start_time, end_time) {
    for (var i=0; i<l.length; i++) {
        l[i][0] = l[i][0] - start_time
        l[i][1] = l[i][1] - start_time
    }

    var step = (end_time - start_time) / line_scale
    for (var i=0; i<l.length; i++) {
        if (l[i][0] < 0) {
            l[i][0] = -1
        } else {
            l[i][0] = Math.trunc(l[i][0] / step) + (l[i][0] % step > step / 2)
        }

        if (l[i][1] < 0) {
            l[i][1] = line_scale + 1
        } else {
            l[i][1] = Math.trunc(l[i][1] / step) + (l[i][0] % step > step / 2)
        }
    }

    var data = []
    for (var i=0; i<=line_scale; i++) {
        data.push(0)
    }

    for (var i=0; i<l.length; i++) {
        if (l[i][0] > l[i][1]) {
            continue
        }

        if (l[i][0] >= 0) {
            data[l[i][0]] += 1
        }

        if (l[i][1] >= 0) {
            data[l[i][1]] -= 1
        }
    }

    for (var i=1; i<=line_scale; i++) {
        data[i] += data[i-1]
    }

    return data
}

function calculate_sum(l, start_time, end_time) {
    for (var i=0; i<l.length; i++) {
        l[i] = l[i] - start_time
    }

    var step = (end_time - start_time) / line_scale
    for (var i=0; i<l.length; i++) {
        if (l[i] < 0) {
            l[i] = -1
        } else {
            l[i] = Math.trunc(l[i] / step) + (l[i] % step > step / 2)
        }
    }

    var data = []
    for (var i=0; i<=line_scale; i++) {
        data.push(0)
    }

    for (var i=0; i<l.length; i++) {
        if (l[i] >= 0) {
            data[l[i]] += 1
        }
    }

    for (var i=1; i<=line_scale; i++) {
        data[i] += data[i-1]
    }

    return data
}

var chartDict = {}

function createLineChart(conf){
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
            animation: {
                duration: 0 // general animation time
            },
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

function initConcurrencyChart(target, data, start_time, end_time) {
    var lockList = []
    for (var i=0; i<data.length; i++) {
        lockList.push([data[i].remote_work_lock_time, data[i].remote_work_unlock_time])
    }
    var lockData = calculate_concurrency(lockList, start_time, end_time)

    var sendList = []
    for (var i=0; i<data.length; i++) {
        sendList.push([data[i].remote_work_send_start_time, data[i].remote_work_send_end_time])
    }
    var sendData = calculate_concurrency(sendList, start_time, end_time)

    var processList = []
    for (var i=0; i<data.length; i++) {
        if (data[i].remote_work_process_start_time > 0) {
            processList.push([data[i].remote_work_process_start_time, data[i].remote_work_process_end_time])
        } else {
            processList.push([data[i].remote_work_start_time, data[i].remote_work_process_end_time])
        }
    }
    var processData = calculate_concurrency(processList, start_time, end_time)

    var localLockList = []
    for (var i=0; i<data.length; i++) {
        localLockList.push([data[i].local_work_lock_time, data[i].local_work_unlock_time])
    }
    var localLockData = calculate_concurrency(localLockList, start_time, end_time)

    var localPreList = []
    for (var i=0; i<data.length; i++) {
        localPreList.push([data[i].pre_work_start_time, data[i].pre_work_end_time])
    }
    var localPreData = calculate_concurrency(localPreList, start_time, end_time)

    var receiveList = []
    for (var i=0; i<data.length; i++) {
        receiveList.push([data[i].remote_work_receive_start_time, data[i].remote_work_receive_end_time])
    }
    var receiveData = calculate_concurrency(receiveList, start_time, end_time)

    var localPostList = []
    for (var i=0; i<data.length; i++) {
        localPostList.push([data[i].post_work_start_time, data[i].post_work_end_time])
    }
    var localPostData = calculate_concurrency(localPostList, start_time, end_time)

    var categories = []
    for (var i=1; i<=line_scale; i++) {
        categories.push("")
    }

    createLineChart({
        selector: target,
        type: "line",
        data: {
            "series":[{
                "name": "分布式处理",
                "data": lockData,
                "color": color_set[0],
                "markers": {
                    size: marker_size
                }
            },{
                "name": "分发文件",
                "data": sendData,
                "color": color_set[1],
                "markers": {
                    size: marker_size
                }
            },{
                "name": "远程实际处理",
                "data": processData,
                "color": color_set[2],
                "markers": {
                    size: marker_size
                }
            },{
                "name": "本地处理",
                "data": localLockData,
                "color": color_set[3],
                "markers": {
                    size: marker_size
                }
            },{
                "name": "本地预处理",
                "data": localPreData,
                "color": color_set[4],
                "markers": {
                    size: marker_size
                }
            },{
                "name": "接收文件",
                "data": receiveData,
                "color": color_set[5],
                "markers": {
                    size: marker_size
                }
            },{
                "name": "本地后置处理",
                "data": localPostData,
                "color": color_set[6],
                "markers": {
                    size: marker_size
                }
            }],
            "categories": categories
        }
    });
}

function initSumChart(target, data, start_time, end_time) {
    var processList = []
    var processErrorList = []
    for (var i=0; i<data.length; i++) {
        if (data[i].remote_work_success && data[i].post_work_success) {
            processList.push(data[i].remote_work_leave_time)
            continue
        }

        if (data[i].pre_work_success) {
            processErrorList.push(data[i].remote_work_leave_time)
        }
    }
    var processData = calculate_sum(processList, start_time, end_time)
    var processErrorData = calculate_sum(processErrorList, start_time, end_time)

    var localList = []
    var localErrorList = []
    for (var i=0; i<data.length; i++) {
        if (data[i].local_work_success) {
            localList.push(data[i].local_work_leave_time)
            continue
        }

        if (!data[i].remote_work_success) {
            localErrorList.push(data[i].local_work_leave_time)
        }
    }
    var localData = calculate_sum(localList, start_time, end_time)
    var localErrorData = calculate_sum(localErrorList, start_time, end_time)

    var preList = []
    var preErrorList = []
    for (var i=0; i<data.length; i++) {
        if (data[i].pre_work_success) {
            preList.push(data[i].pre_work_leave_time)
            continue
        }

        preErrorList.push(data[i].pre_work_leave_time)
    }
    var preData = calculate_sum(preList, start_time, end_time)
    var preErrorData = calculate_sum(preErrorList, start_time, end_time)

    var fileSentList = []
    var fileReceivedList = []
    for (var i=0; i<data.length; i++) {
        if (data[i].remote_work_send_end_time > 0) {
            fileSentList.push(data[i].remote_work_send_end_time)
        }

        if (data[i].remote_work_receive_end_time > 0) {
            fileReceivedList.push(data[i].remote_work_receive_end_time)
        }
    }
    var fileSentData = calculate_sum(fileSentList, start_time, end_time)
    var fileReceivedData = calculate_sum(fileReceivedList, start_time, end_time)

    var categories = []
    for (var i=1; i<=line_scale; i++) {
        categories.push("")
    }

    createLineChart({
        selector: target,
        type: "line",
        data: {
            "series":[{
                "name": "远程处理成功文件数",
                "data": processData,
                "color": color_set[0],
                "markers": {
                    size: marker_size,
                }
            },{
                "name": "本地处理成功文件数",
                "data": localData,
                "color": color_set[1],
                "markers": {
                    size: marker_size,
                }
            },{
                "name": "本地预处理成功文件数",
                "data": preData,
                "color": color_set[2],
                "markers": {
                    size: marker_size,
                }
            },{
                "name": "远程处理失败文件数",
                "data": processErrorData,
                "color": color_set[3],
                "markers": {
                    size: marker_size,
                }
            },{
                "name": "本地处理失败文件数",
                "data": localErrorData,
                "color": color_set[4],
                "markers": {
                    size: marker_size,
                }
            },{
                "name": "本地预处理失败文件数",
                "data": preErrorData,
                "color": color_set[5],
                "markers": {
                    size: marker_size,
                }
            },{
                "name": "分发文件数",
                "data": fileSentData,
                "color": color_set[6],
                "markers": {
                    size: marker_size,
                }
            },{
                "name": "接收文件数",
                "data": fileReceivedData,
                "color": color_set[7],
                "markers": {
                    size: marker_size,
                }
            }],
            "categories": categories
        }
    });
}

var work_list_tick = 5000;

function refresh_work_list() {
    get(
        "/api/v1/dist/work/list",
        function(resp) {
            if (!resp.result) {
                return
            }

            for (var i=0; i<resp.data.length; i++) {
                var work_id = resp.data[i].work_id

                if (!vue.work_data[work_id]) {
                    vue.work_list.push(resp.data[i])
                    vue.work_data[work_id] = default_work_data
                    vue.task_data[work_id] = default_task_data
                    vue.jobs_data[work_id] = default_jobs_data

                    if (i == resp.data.length-1 && vue.current == "default") {
                        vue.start(work_id);
                    }

                    setTimeout(initConcurrencyChart, 0, "#concurrency_chart-"+work_id, [], resp.data[i].registered_time, resp.data[i].current_time)
                    setTimeout(initSumChart, 0, "#sum_chart-"+work_id, [], resp.data[i].registered_time, resp.data[i].current_time)
                }
            }
            setTimeout(refresh_work_list, work_list_tick)
        },
        function(e) {
            setTimeout(refresh_work_list, work_list_tick)
        }
    )
}

var work_detail_tick = 1000

function refresh_work_detail(work_id, job_least_leave_time) {
    get(
        "/api/v1/dist/work/" + work_id + "/detail?job_least_leave_time="+job_least_leave_time,
        function(resp) {
            if (!resp.result) {
                return
            }

            var work_id = resp.data.work_id
            vue.work_data[work_id] = resp.data
            if (vue.current == work_id) {
                vue.current_data = calculate_information(resp.data)
            }

            if (!resp.data.jobs) {
                resp.data.jobs = []
            }

            if (!vue.jobs_data[work_id].length) {
                vue.jobs_data[work_id] = resp.data.jobs
            } else {
                old_length = vue.jobs_data[work_id].length
                for (var i=0; i<resp.data.jobs.length; i++) {
                    var found = false
                    for (var j=0; j<old_length; j++) {
                        if (vue.jobs_data[work_id][j].id == resp.data.jobs[i].id) {
                            found = true
                            break
                        }
                    }
                    if (found) {
                        vue.jobs_data[work_id][j] = resp.data.jobs[i]
                    } else {
                        vue.jobs_data[work_id].push(resp.data.jobs[i])
                    }
                }
            }

            for (var i=0; i<vue.jobs_data[work_id].length; i++) {
                var leave_time = vue.jobs_data[work_id][i].leave_time
                if (leave_time > job_least_leave_time) {
                    job_least_leave_time = leave_time
                }
            }

            if (resp.data.start_time > 0) {
                setTimeout(initConcurrencyChart, 0, "#concurrency_chart-"+work_id, vue.jobs_data[work_id], resp.data.start_time, resp.data.current_time)
                setTimeout(initSumChart, 0, "#sum_chart-"+work_id, vue.jobs_data[work_id], resp.data.start_time, resp.data.current_time)
            }

            if (resp.data.status == "ended" || resp.data.status == "unregistered" || resp.data.status == "removable") {
                return
            }

            if (vue.current == work_id) {
                setTimeout(refresh_work_detail, work_detail_tick, work_id, job_least_leave_time)
            }
        },
        function(e) {
            if (vue.current == work_id) {
                setTimeout(refresh_work_detail, work_detail_tick, work_id, job_least_leave_time)
            }
        }
    )
}

function calculate_information(data) {
    data.remote_work_waiting = 0
    data.remote_work_holding = 0
    data.remote_work_held = 0
    data.remote_work_released = 0
    data.pre_work_waiting = 0
    data.pre_work_holding = 0
    data.pre_work_held = 0
    data.pre_work_released = 0
    data.local_work_waiting = 0
    data.local_work_holding = 0
    data.local_work_held = 0
    data.local_work_released = 0
    data.post_work_waiting = 0
    data.post_work_holding = 0
    data.post_work_held = 0
    data.post_work_released = 0
    data.dependent_waiting = 0
    data.sending = 0
    data.receiving = 0
    data.real_remote_error = 0

    if (!data.jobs) {
        return data
    }

    for (var i=0; i<data.jobs.length; i++) {
        job = data.jobs[i];
        if (job.remote_work_enter_time > 0 && job.remote_work_lock_time <= 0)
            data.remote_work_waiting += 1
        if (job.remote_work_lock_time > 0 && job.remote_work_unlock_time <= 0)
            data.remote_work_holding += 1
        if (job.remote_work_lock_time > 0)
            data.remote_work_held += 1
        if (job.remote_work_unlock_time > 0)
            data.remote_work_released += 1

        if (job.pre_work_enter_time > 0 && job.pre_work_lock_time <= 0 && job.pre_work_start_time <= 0)
            data.pre_work_waiting += 1
        if (job.pre_work_lock_time > 0 && job.pre_work_unlock_time <= 0)
            data.pre_work_holding += 1
        if (job.pre_work_lock_time > 0)
            data.pre_work_held += 1
        if (job.pre_work_unlock_time > 0)
            data.pre_work_released += 1

        if (job.local_work_enter_time > 0 && job.local_work_lock_time <= 0)
            data.local_work_waiting += 1
        if (job.local_work_lock_time > 0 && job.local_work_unlock_time <= 0)
            data.local_work_holding += 1
        if (job.local_work_lock_time > 0)
            data.local_work_held += 1
        if (job.local_work_unlock_time > 0)
            data.local_work_released += 1

        if (job.post_work_enter_time > 0 && job.post_work_lock_time <= 0 && job.post_work_start_time <= 0)
            data.post_work_waiting += 1
        if (job.post_work_lock_time > 0 && job.post_work_unlock_time <= 0)
            data.post_work_holding += 1
        if (job.post_work_lock_time > 0)
            data.post_work_held += 1
        if (job.post_work_unlock_time > 0)
            data.post_work_released += 1

        if (job.remote_work_lock_time > 0 && job.remote_work_start_time <= 0)
            data.dependent_waiting += 1
        if (job.remote_work_send_start_time > 0 && job.remote_work_send_end_time <= 0)
            data.sending += 1
        if (job.remote_work_receive_start_time > 0 && job.remote_work_receive_end_time <= 0)
            data.receiving += 1

        if (job.remote_work_leave_time > 0 && !job.remote_work_success || job.post_work_leave_time > 0 && !job.post_work_success)
            data.real_remote_error += 1
    }

    console.log(
        "Stats: remote_work(" + data.remote_work_waiting + ", " + data.remote_work_holding + ", " + data.remote_work_held + ", " + data.remote_work_released +
        ")  pre_work(" + data.pre_work_waiting + ", " + data.pre_work_holding + ", " + data.pre_work_held + ", " + data.pre_work_released +
        ")  local_work(" + data.local_work_waiting + ", " + data.local_work_holding + ", " + data.local_work_held + ", " + data.local_work_released +
        ")  post_work(" + data.post_work_waiting + ", " + data.post_work_holding + ", " + data.post_work_held + ", " + data.post_work_released +
        ")  files(" + data.dependent_waiting + ", " + data.sending + ", " + data.receiving + ")"
    )

    return data
}

$(document).ready(function() {
    refresh_work_list()
});