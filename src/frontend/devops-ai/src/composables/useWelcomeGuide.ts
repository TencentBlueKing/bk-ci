import { ref, shallowRef, watch } from "vue";
import { getWelcomeGuide, getHotQuestions } from "../api";
import { useParams } from "./useParams";

export type InteractionType =
  | "PROMPT_COMPLETION"
  | "FORM_COLLECT"
  | "DIRECT_TRIGGER";

export interface ActionFormField {
  key: string;
  label: string;
  placeholder?: string;
  required?: boolean;
  type?: "text" | "textarea";
}

export interface CardAction {
  label: string;
  prompt: string;
  interactionType: InteractionType;
  formFields?: ActionFormField[];
}

function escapeRegExp(s: string) {
  return s.replace(/[.*+?^${}()|[\]\\]/g, "\\$&");
}

/** 将表单值拼进 action.prompt：含 {{fieldKey}} 时做替换，否则在 prompt 后追加「标签：值」段落 */
export function buildFormCollectPrompt(
  action: Pick<CardAction, "prompt" | "formFields">,
  values: Record<string, string>,
): string {
  const fields = action.formFields || [];
  const prompt = action.prompt || "";

  if (/\{\{[^}]+\}\}/.test(prompt)) {
    let text = prompt;
    for (const f of fields) {
      const v = (values[f.key] ?? "").trim();
      text = text.replace(
        new RegExp(`\\{\\{\\s*${escapeRegExp(f.key)}\\s*\\}\\}`, "g"),
        v,
      );
    }
    return text.replace(/\{\{[^}]+\}\}/g, "").trim();
  }

  const lines = fields.map((f) => {
    const v = (values[f.key] ?? "").trim();
    return `${f.label}：${v}`;
  });
  const body = lines.join("\n");
  if (!prompt.trim()) return body;
  if (!body) return prompt.trim();
  return `${prompt.trim()}\n\n${body}`;
}

function normalizeFormFields(raw: any): ActionFormField[] | undefined {
  if (!Array.isArray(raw) || !raw.length) return undefined;
  const fields = raw
    .map((f: any) => ({
      key: String(f.key ?? "").trim(),
      label: String(f.label ?? "").trim(),
      placeholder: f.placeholder != null ? String(f.placeholder) : undefined,
      required: Boolean(f.required),
      type: f.type === "textarea" ? ("textarea" as const) : ("text" as const),
    }))
    .filter((f: ActionFormField) => f.key);
  return fields.length ? fields : undefined;
}

export interface GuideCard {
  label: string;
  description: string;
  actions: CardAction[];
}

/** 缓存结构变更时递增，避免沿用旧版已过滤的数据 */
const WELCOME_GUIDE_CACHE_VERSION = 3;

let guideCache: {
  version: number;
  projectId: string;
  cards: GuideCard[];
  hotQuestions: string[];
} | null = null;

function isGuideCacheHit(projectId: string) {
  return (
    guideCache?.version === WELCOME_GUIDE_CACHE_VERSION &&
    guideCache.projectId === projectId
  );
}

export function useWelcomeGuide() {
  const { projectId } = useParams();
  const cards = ref<GuideCard[]>(
    isGuideCacheHit(projectId.value) ? guideCache!.cards : [],
  );
  const hotQuestions = ref<string[]>(
    isGuideCacheHit(projectId.value) ? guideCache!.hotQuestions : [],
  );
  const loading = shallowRef(!isGuideCacheHit(projectId.value));
  const isRefreshing = shallowRef(false);

  watch(
    projectId,
    async (pid) => {
      if (guideCache && guideCache.version !== WELCOME_GUIDE_CACHE_VERSION) {
        guideCache = null;
      }
      if (isGuideCacheHit(pid)) {
        cards.value = guideCache!.cards;
        hotQuestions.value = guideCache!.hotQuestions;
        loading.value = false;
        return;
      }
      cards.value = [];
      hotQuestions.value = [];
      loading.value = true;
      const targetPid = pid;
      try {
        const data = await getWelcomeGuide(pid);
        if (projectId.value !== targetPid) return;
        const loadedCards = (data?.cards || []).map((card: any) => ({
          label: card.label || "",
          description: card.description || "",
          actions: (card.actions || []).map((a: any) => ({
            label: a.label || "",
            prompt: a.prompt || "",
            interactionType: (a.interactionType ||
              "DIRECT_TRIGGER") as InteractionType,
            formFields: normalizeFormFields(a.formFields),
          })),
        }));
        const loadedQuestions = (data?.hotQuestions || []).map(
          (q: any) => q.question,
        );
        cards.value = loadedCards;
        hotQuestions.value = loadedQuestions;
        guideCache = {
          version: WELCOME_GUIDE_CACHE_VERSION,
          projectId: targetPid,
          cards: loadedCards,
          hotQuestions: loadedQuestions,
        };
      } catch (err) {
        if (projectId.value === targetPid) {
          console.error("Failed to load welcome guide:", err);
        }
      } finally {
        if (projectId.value === targetPid) {
          loading.value = false;
        }
      }
    },
    { immediate: true },
  );

  const refreshHotQuestions = async () => {
    if (isRefreshing.value) return;
    isRefreshing.value = true;
    try {
      const data = await getHotQuestions();
      const questions = (data?.questions || []).map((q: any) => q.question);
      if (questions.length) {
        hotQuestions.value = questions;
      }
    } catch (err) {
      console.error("Failed to refresh hot questions:", err);
    } finally {
      isRefreshing.value = false;
    }
  };

  return { cards, hotQuestions, loading, isRefreshing, refreshHotQuestions };
}
