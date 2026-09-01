import { ref, shallowRef, unref, watch } from "vue";
import { deleteSession as deleteSessionApi, getSessions } from "../api";
import { useParams } from "./useParams";

export function useSessionHistory() {
  const { projectId } = useParams();
  const showHistory = shallowRef(false);
  const sessionList = ref<any[]>([]);
  const historyLoading = shallowRef(false);

  async function fetchSessionList() {
    historyLoading.value = true;
    try {
      const list = await getSessions(unref(projectId) || undefined);
      sessionList.value = (list || []).map((s: any) => ({
        id: s.id,
        title: s.title || "未命名对话",
        updatedAt: s.updatedTime || s.createdTime || "",
      }));
    } catch (err) {
      console.error("Failed to load sessions:", err);
    } finally {
      historyLoading.value = false;
    }
  }

  watch(projectId, () => {
    sessionList.value = [];
    if (showHistory.value) {
      void fetchSessionList();
    }
  });

  const toggleHistory = async () => {
    showHistory.value = !showHistory.value;
    if (showHistory.value) {
      await fetchSessionList();
    }
  };

  const deleteSession = async (id: string) => {
    await deleteSessionApi(id);
    sessionList.value = sessionList.value.filter((s) => s.id !== id);
  };

  function formatSessionTime(dateStr: string) {
    if (!dateStr) return "--";
    const d = new Date(dateStr);
    if (isNaN(d.getTime())) return "";
    const now = new Date();
    const isToday = d.toDateString() === now.toDateString();
    const pad = (n: number) => String(n).padStart(2, "0");
    const time = `${pad(d.getHours())}:${pad(d.getMinutes())}`;
    if (isToday) return time;
    return `${pad(d.getMonth() + 1)}-${pad(d.getDate())} ${time}`;
  }

  return {
    showHistory,
    sessionList,
    historyLoading,
    toggleHistory,
    formatSessionTime,
    deleteSession,
  };
}
