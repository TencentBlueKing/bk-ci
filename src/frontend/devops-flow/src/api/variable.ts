/**
 * Variable management API
 */
import { post } from '@/utils/http'
import { type ReadOnlyVariableGroup } from '@/types/variable'
import type { FlowModel, Param } from './flowModel'

const PROCESS_API_URL_PREFIX = '/process/api'
const STORE_API_URL_PREFIX = '/store/api'

/**
 * Get system variables (grouped)
 * Reference: devops-pipeline/src/store/modules/atom/actions.js requestCommonParams
 */
export async function getSystemVariables(): Promise<ReadOnlyVariableGroup[]> {
  try {
    const data = await post<ReadOnlyVariableGroup[]>(
      `${PROCESS_API_URL_PREFIX}/user/buildParam/common`,
    )
    return data || []
  } catch (error) {
    console.error('Failed to get system variables:', error)
    // Fallback to mock data for development
    return getMockSystemVariableGroups()
  }
}

/**
 * Update flow model's trigger stage container params with variables
 * @param model Flow model
 * @param variables Flow variables
 */
export function updateFlowModelParams(model: FlowModel | null, variables: Param[]): void {
  if (!model || !model.stages || model.stages.length === 0) {
    return
  }

  // Get trigger stage (first stage)
  const triggerStage = model.stages[0]
  if (!triggerStage) {
    return
  }

  // Ensure containers array exists
  if (!triggerStage.containers) {
    triggerStage.containers = []
  }

  // Update params in the first container
  const triggerContainer = triggerStage.containers[0]
  if (!triggerContainer) {
    return
  }
  triggerContainer.params = variables
}

/**
 * Get plugin output variables from API
 * Reference: devops-pipeline/src/store/modules/atom/actions.js fetchAtomsOutput
 * @param elements Array of elements from flow model to get atom codes and versions
 */
export async function getPluginOutputVariables(
  elements: Array<{ atomCode?: string; version?: string }>,
): Promise<Record<string, any>> {
  try {
    // Extract unique atom codes with versions
    const arr = elements
      .filter((ele) => ele.atomCode && ele.version)
      .map((ele) => `${ele.atomCode}@${ele.version}`)
    const data = Array.from(new Set(arr))

    if (data.length === 0) {
      return {}
    }

    const response = await post<Record<string, string>>(
      `${STORE_API_URL_PREFIX}/user/pipeline/atom/output/info/list`,
      data,
    )

    // Parse JSON strings in response
    const map: Record<string, any> = {}
    for (const item in response) {
      try {
        const value = response[item]
        if (value && typeof value === 'string') {
          map[item] = JSON.parse(value)
        } else {
          map[item] = value
        }
      } catch (e) {
        console.error('Failed to parse output for', item, e)
      }
    }

    return map
  } catch (error) {
    console.error('Failed to get plugin output variables:', error)
    return {}
  }
}
/**
 * Mock system variables (grouped)
 */
function getMockSystemVariableGroups(): ReadOnlyVariableGroup[] {
  return [
    {
      name: '流水线内置变量',
      params: [
        {
          id: 'ci.actor',
          name: 'ci.actor',
          desc: '当前构建的启动人',
        },
        {
          id: 'ci.build-no',
          name: 'ci.build-no',
          desc: '构建号，开启推荐版本号时有效',
        },
        {
          id: 'ci.build_id',
          name: 'ci.build_id',
          desc: '当前构建ID',
        },
        {
          id: 'ci.build_msg',
          name: 'ci.build_msg',
          desc: '构建信息, 最长 128 个字, 取值规则：<br/>- Push 事件：commit message<br/>- Mr 事件：Mr title<br/>- Note 事件：评论内容<br/>- Issue 事件：Issue标题<br/>- Cr 事件：Cr title<br/>- Tag 事件：Tag name<br/>- 手动触发：触发时页面上填写的构建信息',
        },
        {
          id: 'ci.build_num',
          name: 'ci.build_num',
          desc: '当前构建的唯一标示ID，从1开始自增',
        },
        {
          id: 'ci.build_start_type',
          name: 'ci.build_start_type',
          desc: '构建启动方式可能的值有 MANUAL、TIME_TRIGGER、WEB_HOOK、SERVICE、FLOW或者REMOTE',
        },
        {
          id: 'ci.failed_tasknames',
          name: 'ci.failed_tasknames',
          desc: '创作流执行失败的所有TASK，值格式：TASK别名,TASK别名,TASK别名',
        },
        {
          id: 'ci.failed_tasks',
          name: 'ci.failed_tasks',
          desc: '创作流执行失败的所有TASK，值格式：[STAGE别名][JOB别名]TASK别名。若有多个并发JOB失败，使用换行(\\n)分隔',
        },
        {
          id: 'ci.flow_creator',
          name: 'ci.flow_creator',
          desc: '创作流创建者',
        },
        {
          id: 'ci.flow_id',
          name: 'ci.flow_id',
          desc: '创作流ID',
        },
        {
          id: 'ci.flow_modifier',
          name: 'ci.flow_modifier',
          desc: '创作流最新修改者',
        },
        {
          id: 'ci.flow_name',
          name: 'ci.flow_name',
          desc: '创作流名称',
        },
        {
          id: 'ci.flow_version',
          name: 'ci.flow_version',
          desc: '创作流版本号',
        },
        {
          id: 'ci.project_id',
          name: 'ci.project_id',
          desc: '项目ID，项目的唯一标识',
        },
        {
          id: 'ci.project_name',
          name: 'ci.project_name',
          desc: '项目名称',
        },
        {
          id: 'ci.remark',
          name: 'ci.remark',
          desc: '流水线备注，可在脚本插件中通过 echo ::set-remark xxx 的方式设置值',
        },
        {
          id: 'ci.workspace',
          name: 'ci.workspace',
          desc: '当前 Job 的工作空间',
        },
      ],
    },
    {
      name: 'Job 内置变量',
      params: [
        {
          id: 'job.container.network',
          name: 'job.container.network',
          desc: '当前 job 所在的网络区域, 可能的取值为: IDC/DEVNET/OA',
        },
        {
          id: 'job.container.node_alias',
          name: 'job.container.node_alias',
          desc: '使用第三方构建机集群时生效, 当前job调度到的节点别名',
        },
        {
          id: 'job.id',
          name: 'job.id',
          desc: '当前创作流下 job 的唯一标识',
          remark: '当用户自定义标识时, 自行保证唯一性. 缺省时, 系统内置生成',
        },
        {
          id: 'job.index',
          name: 'job.index',
          desc: '在 matrix job 下, 从0开始的索引. 和 matrix job 解析出来的顺序有关',
          remark: '可以根据 index 去获取 matrix job 下指定 job 的具体步骤输出',
        },
        {
          id: 'job.name',
          name: 'job.name',
          desc: 'job 名称',
          remark: '缺省时, 按照配置顺序为 job-1、job-2……job-N',
        },
        {
          id: 'job.os',
          name: 'job.os',
          desc: '当前 job 执行机器的操作系统: LINUX/WINDOWS/MACOS',
        },
        {
          id: 'job.stage_id',
          name: 'job.stage_id',
          desc: 'job 所属的 stage id',
        },
        {
          id: 'job.stage_name',
          name: 'job.stage_name',
          desc: 'job 所属的 stage name',
        },
        {
          id: 'jobs.<job-id>.outcome',
          name: 'jobs.<job-id>.outcome',
          desc: '当前 job 的结果, 可能的取值为: SUCCEED/FAILED/CANCELED/SKIP',
          remark:
            '当 continue-on-error=true 且 job 执行失败时, status=SUCCEED, outcome=FAILED. 在下游步骤中才可以获取到.',
        },
        {
          id: 'jobs.<job-id>.status',
          name: 'jobs.<job-id>.status',
          desc: '当前 job 的状态, 可能的取值为: SUCCEED/FAILED/CANCELED/SKIP',
          remark:
            '当 continue-on-error=true 且 job 执行失败时, status=SUCCEED, outcome=FAILED. 在下游步骤中才可以获取到.',
        },
      ],
    },
    {
      name: 'Step 内置变量',
      params: [
        {
          id: 'step.id',
          name: 'step.id',
          desc: '当前插件 TASK ID, 32位, 全局唯一',
          remark: '系统自动生成 (e-开头)',
        },
        {
          id: 'step.name',
          name: 'step.name',
          desc: 'step 名称',
          remark: '缺省时, 为对应的插件的名称',
        },
        {
          id: 'step.retry-count-auto',
          name: 'step.retry-count-auto',
          desc: '当前步骤的自动重试次数',
        },
        {
          id: 'step.retry-count-manual',
          name: 'step.retry-count-manual',
          desc: '当前步骤的手动重试次数',
        },
        {
          id: 'steps.<step-id>.outcome',
          name: 'steps.<step-id>.outcome',
          desc: 'step 的结果, 可能的取值为: SUCCEED/FAILED/CANCELED/SKIP',
          remark:
            '当 continue-on-error=true 且 step 执行失败时, status=SUCCEED, outcome=FAILED. 在下游步骤中才可以获取到.',
        },
        {
          id: 'steps.<step-id>.status',
          name: 'steps.<step-id>.status',
          desc: 'step 的状态, 可能的取值为: SUCCEED/FAILED/CANCELED/SKIP',
          remark:
            '当 continue-on-error=true 且 step 执行失败时, status=SUCCEED, outcome=FAILED. 在下游步骤中才可以获取到.',
        },
      ],
    },
  ]
}
