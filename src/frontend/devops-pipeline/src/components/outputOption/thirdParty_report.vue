<template>
    <div class="pipeline-report-third-party">
        <table class="bk-table report-table">
            <thead>
                <tr>
                    <th>名称</th>
                </tr>
            </thead>
            <tbody v-if="reportList.length">
                <tr v-for="(report, index) in reportList" :key="index">
                    <td>
                        <span class="report-td-name" @click="linkToReportDetail(report.indexFileUrl)">
                            <logo name="tiaozhuan" size="18" class="jump-icon" style="fill:#3c96ff;;" />{{ report.name }}
                        </span>
                    </td>
                </tr>
            </tbody>
        </table>
    </div>
</template>

<script>
    import Logo from '@/components/Logo'

    export default {
        components: { Logo },
        props: {
            reportList: Array
        },
        methods: {
            linkToReportDetail (url) {
                window.open(url, '_blank')
            }
        }
    }
</script>

<style lang="scss">
    @import './../../scss/conf';

    .pipeline-report-third-party {
        margin-top: 16px;
        .report-table {
            border: 1px solid $borderWeightColor;
            tbody {
                background-color: #fff;
            }
            tr {
                height: 42px;
            }
            th {
                color: #333C48;
                font-weight: normal;
            }
            th, td {
                padding-left: 53px;
                height: 42px;
            }
            td {
                padding-left: 32px;
            }
            .report-td-name {
                color: $primaryColor;
                cursor: pointer;
            }
            .jump-icon {
                position: relative;
                top: 4px;
                margin-right: 4px;
            }
        }
    }
</style>
