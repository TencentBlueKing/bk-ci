import { onBeforeUnmount, onMounted, reactive, toRefs } from "vue";
import { ParentBridgeAction } from "../constants";

interface ParentParams {
  projectId: string;
  projectName: string;
  pipelineId: string;
  buildId: string;
}

/**
 * Module-level singleton state. Every `useParams()` call shares the same
 * reactive context for the current tab/iframe instance. Context is delivered
 * from the parent window via `postMessage`, which is naturally scoped to the
 * current browser tab (each tab has its own iframe), avoiding the cross-tab
 * collision that the previous `localStorage` channel suffered from.
 */
const sharedState = reactive<ParentParams>({
  projectId: "",
  projectName: "",
  pipelineId: "",
  buildId: "",
});

let listenerInstalled = false;
let mountCount = 0;

function applyContext(ctx?: Partial<ParentParams> | null) {
  if (!ctx) return;
  sharedState.projectId = ctx.projectId || "";
  sharedState.projectName = ctx.projectName || "";
  sharedState.pipelineId = ctx.pipelineId || "";
  sharedState.buildId = ctx.buildId || "";
}

function onMessage(e: MessageEvent) {
  const data = e?.data;
  if (!data || typeof data !== "object") return;
  if (data.action === ParentBridgeAction.SyncContext) {
    applyContext(data.params as Partial<ParentParams>);
  }
}

function requestFromParent() {
  try {
    if (window.parent && window.parent !== window) {
      window.parent.postMessage(
        { action: ParentBridgeAction.RequestContext },
        "*"
      );
    }
  } catch {
    /* parent inaccessible */
  }
}

export function useParams() {
  function refresh() {
    requestFromParent();
  }

  onMounted(() => {
    if (!listenerInstalled) {
      window.addEventListener("message", onMessage);
      listenerInstalled = true;
    }
    mountCount += 1;
    requestFromParent();
  });

  onBeforeUnmount(() => {
    mountCount = Math.max(0, mountCount - 1);
    if (mountCount === 0 && listenerInstalled) {
      window.removeEventListener("message", onMessage);
      listenerInstalled = false;
    }
  });

  function getContext() {
    return (
      [
        "projectId",
        "projectName",
        "pipelineId",
        "buildId",
      ] as const
    )
      .filter((key) => sharedState[key])
      .map((key) => ({ description: key, value: sharedState[key] }));
  }

  return {
    ...toRefs(sharedState),
    refresh,
    getContext,
  };
}
