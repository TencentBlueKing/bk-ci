import { defineComponent, h } from 'vue'
import { ActivityType } from '../../constants'
import DataTableActivity from './DataTableActivity'
import GroupedListActivity from './GroupedListActivity'
import KeyValueActivity from './KeyValueActivity'
import OperationResultActivity from './OperationResultActivity'

type ActivityComponent = ReturnType<typeof defineComponent>

const activityComponentMap: Record<string, ActivityComponent> = {
  [ActivityType.DataTable]: DataTableActivity,
  [ActivityType.KeyValue]: KeyValueActivity,
  [ActivityType.GroupedList]: GroupedListActivity,
  [ActivityType.OperationResult]: OperationResultActivity,
}

export default defineComponent({
  name: 'ActivityMessage',
  props: {
    message: { type: Object, required: true },
  },
  setup(props) {
    return () => {
      const { activityType, content } = props.message
      const Comp = activityComponentMap[activityType]

      if (!Comp) {
        return <pre>{content}</pre>
      }

      return h(Comp, { content })
    }
  },
})
