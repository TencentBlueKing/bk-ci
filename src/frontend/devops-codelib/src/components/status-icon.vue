<template>
    <span :class="statusClass">
        <i />
    </span>
</template>

<script>
    export default {
        props: {
            status: {
                type: String,
                default: ''
            },
            statusColorMap: {
                type: Object,
                default: () => ({
                    success: 'green',
                    error: 'red',
                    normal: 'gray',
                    succeed: 'green',
                    failed: 'red'
                })
            }
        },
        computed: {
            statusClass () {
                return `status-icon status-${this.statusColorMap[this.status.toLowerCase()]}`
            }
        }
    }
</script>

<style lang="scss" scoped>
.status-icon {
    display: inline-block;
    position: relative;
    top: 1px;
    width: 13px;
    height: 13px;
    border-radius: 50%;
    margin-right: 5px;
    i {
        background: #979ba5;
        border-radius: 50%;
        height: 7px;
        left: 3px;
        position: absolute;
        top: 3px;
        width: 7px;
    }
}

.status-red {
    background: rgba(234, 54, 54, 0.16);
    i {
        background: rgb(234, 54, 54);
    }
}

.status-green {
    background: rgba(45, 203, 86, 0.16);
    i {
        background: rgb(45, 203, 86);
    }
}

.status-blue {
    background: rgba(105, 157, 244, 0.16);
    i {
        background: rgb(105, 157, 244);
    }
}

.status-gray {
    background: rgba(151, 155, 165, 0.16);
    i {
        background: rgb(151, 155, 165);
    }
}
</style>
