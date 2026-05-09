import { computed, ref, shallowRef, watch } from "vue";
import type { Ref } from "vue";
import {
  MessageContentType,
  MessageRole,
  MessageStatus,
} from "@blueking/chat-x";
import {
  createChatClient,
  createSession,
  getChatStatus,
  getLatestSession,
  getSessionMessages,
  stopChat,
  updateSessionTitle,
} from "../api";
import { CustomEventName, ServerRole } from "../constants";
import { useParams } from "./useParams";

const ROLE_MAP: Record<string, any> = {
  [ServerRole.User]: MessageRole.User,
  [ServerRole.Assistant]: MessageRole.Assistant,
  [ServerRole.System]: MessageRole.Assistant,
  [ServerRole.Tool]: MessageRole.Tool,
  [ServerRole.Reasoning]: MessageRole.Reasoning,
};

function getSubAgentMsgId(agentName: string, parentMsgId: string | null) {
  return `subagent-${agentName}-${parentMsgId || "global"}`;
}

function parseExtraData(raw: unknown): any {
  if (!raw) return null;
  if (typeof raw === "object") return raw;
  if (typeof raw === "string") {
    try {
      return JSON.parse(raw);
    } catch {
      return null;
    }
  }
  return null;
}

function mapHistoryMessage(msg: any): any[] {
  const base: any = {
    id: msg.id,
    messageId: msg.id,
    role: ROLE_MAP[msg.role] ?? MessageRole.Assistant,
    content: msg.content,
    status: MessageStatus.Complete,
  };
  if (msg.role === ServerRole.Reasoning) {
    base.content = Array.isArray(msg.content) ? msg.content : [msg.content];
    if (msg.duration) base.duration = msg.duration;
  }
  if (msg.role === ServerRole.Assistant) {
    if (msg.toolCalls?.length) {
      base.toolCalls = msg.toolCalls.map((tc: any) => ({
        id: tc.id,
        type: MessageContentType.Function,
        function: {
          name: tc.function?.name || tc.name || "",
          arguments: tc.function?.arguments || tc.arguments || "",
          description: tc.function?.description || tc.description || "",
          mcpName: tc.function?.mcpName || tc.mcpName || "",
        },
        ...(tc.toolMessage && {
          toolMessage: {
            ...tc.toolMessage,
            status: MessageStatus.Complete,
          },
        }),
      }));
    }
  }
  if (msg.role === ServerRole.Tool) {
    base.toolCallId = msg.toolCallId || "";
    base.duration = msg.duration || 0;
    if (msg.error) base.error = msg.error;
  }

  const result: any[] = [base];
  const extraList = parseExtraData(msg.extraData);
  if (Array.isArray(extraList)) {
    for (const item of extraList) {
      if (item?.activityType) {
        result.push({
          id: `${msg.id}-activity-${result.length}`,
          messageId: msg.id,
          role: MessageRole.Activity,
          activityType: item.activityType,
          content: item.content,
          status: MessageStatus.Complete,
        });
      }
    }
  }
  return result;
}

export interface InterruptState {
  id?: string;
  reason?: string;
  payload?: any;
}

export function useChat(resources: Ref<any[]>) {
  let bootstrapGen = 0;
  const { getContext, projectId, pipelineId, buildId } = useParams();
  const sessionId = ref<string | null>(null);
  const sessionName = ref("新对话");
  const messages = ref<any[]>([]);
  const messageStatus = shallowRef(MessageStatus.Complete);
  const chatLoading = shallowRef(false);
  const userInput = shallowRef("");
  const cite = shallowRef("");
  const selectedShortcut = ref<any>(null);
  const currentInterrupt = ref<InterruptState | null>(null);

  let selectedResources: any[] = [];
  let chatClient: any = null;

  const showWelcome = computed(
    () => !chatLoading.value && messages.value.length === 0,
  );

  function createStreamCallbacks() {
    let assistantMsgId: string | null = null;
    let reasoningMsgId: string | null = null;
    let reasoningStartTime: number | null = null;
    const toolCallStartTimes: Record<string, number> = {};
    const subAgentMsgIds: Record<string, string> = {};

    function getAssistantMsg() {
      console.log("getAssistantMsg", assistantMsgId, messages.value);
      return messages.value.find((m) => m.id === assistantMsgId);
    }

    function handleActivitySnapshot(event: any) {
      const activityId =
        event.activityId || event.messageId || `activity-${Date.now()}`;
      const existing = messages.value.find((m) => m.id === activityId);
      if (existing && event.replace !== false) {
        existing.content = event.content;
        existing.activityType = event.activityType;
      } else if (!existing) {
        const activityMsg = {
          id: activityId,
          messageId: activityId,
          role: MessageRole.Activity,
          activityType: event.activityType,
          content: event.content,
          status: MessageStatus.Complete,
        };
        messages.value.push(activityMsg);
      }
    }

    return {
      onReasoningStart() {
        reasoningMsgId = `reasoning-${Date.now()}`;
        reasoningStartTime = Date.now();
        messages.value.push({
          id: reasoningMsgId,
          messageId: reasoningMsgId,
          role: MessageRole.Reasoning,
          content: [],
          status: MessageStatus.Streaming,
        });
      },
      onReasoningDelta(delta: string) {
        const msg = messages.value.find((m) => m.id === reasoningMsgId);
        if (msg) {
          if (msg.content.length === 0) {
            msg.content.push(delta);
          } else {
            msg.content[msg.content.length - 1] += delta;
          }
        }
      },
      onReasoningEnd() {
        const msg = messages.value.find((m) => m.id === reasoningMsgId);
        if (msg) {
          msg.status = MessageStatus.Complete;
          msg.duration = reasoningStartTime
            ? Date.now() - reasoningStartTime
            : undefined;
        }
      },
      onMessageStart(event: any) {
        assistantMsgId = event.messageId;
        messages.value.push({
          id: assistantMsgId,
          messageId: assistantMsgId,
          role: MessageRole.Assistant,
          content: "",
          status: MessageStatus.Streaming,
          toolCalls: [],
        });
      },
      onDelta(delta: string) {
        const msg = getAssistantMsg();
        if (msg) msg.content += delta;
      },
      onToolCallStart(event: any) {
        const msg = getAssistantMsg();
        if (!msg) return;
        if (!msg.toolCalls) msg.toolCalls = [];
        toolCallStartTimes[event.toolCallId] = Date.now();
        msg.toolCalls.push({
          id: event.toolCallId,
          type: MessageContentType.Function,
          function: {
            name: event.name || "",
            arguments: "",
            description: event.description || "",
            mcpName: event.mcpName || "",
          },
        });
        msg.status = MessageStatus.Streaming;
      },
      onToolCallArgs(delta: string, event: any) {
        const msg = getAssistantMsg();
        if (!msg) return;
        const tc = msg.toolCalls?.find((t: any) => t.id === event.toolCallId);
        if (tc) tc.function.arguments += delta;
      },
      onToolCallEnd(event: any) {
        const msg = getAssistantMsg();
        if (!msg) return;
        const tc = msg.toolCalls?.find((t: any) => t.id === event.toolCallId);
        if (tc) {
          const startTime = toolCallStartTimes[event.toolCallId];
          const duration = startTime
            ? Date.now() - startTime
            : event.duration || 0;
          tc.toolMessage = {
            content: event.content || "",
            status: event.error ? MessageStatus.Error : MessageStatus.Complete,
            duration,
            toolCallId: event.toolCallId,
            ...(event.error && { error: event.error }),
          };
        }
      },
      onToolCallResult(event: any) {
        const msg = getAssistantMsg();
        if (!msg) return;
        const tc = msg.toolCalls?.find((t: any) => t.id === event.toolCallId);
        if (tc) {
          tc.toolMessage = {
            ...tc.toolMessage,
            content: event.content || "",
            status: MessageStatus.Complete,
            toolCallId: event.toolCallId,
          };
        }
      },
      onMessageEnd() {
        const msg = getAssistantMsg();
        if (msg) {
          msg.status = MessageStatus.Complete;
          if (msg.toolCalls?.length === 0) delete msg.toolCalls;
        }
      },
      onMessagesSnapshot(snapshotMessages: any[]) {
        if (Array.isArray(snapshotMessages)) {
          messages.value = snapshotMessages.flatMap(mapHistoryMessage);
        }
      },
      onStepStarted(event: any) {
        const msg = getAssistantMsg();
        if (msg) {
          msg.stepName = event.stepName;
        }
      },
      onStepFinished(_event: any) {
        const msg = getAssistantMsg();
        if (msg) {
          delete msg.stepName;
        }
      },
      onActivitySnapshot: handleActivitySnapshot,
      onActivityDelta(event: any) {
        const activityId = event.activityId || event.messageId;
        const msg = messages.value.find((m) => m.id === activityId);
        if (msg) {
          msg.content = event.content;
        }
      },
      onCustom(event: any) {
        if (event.name === CustomEventName.ActivitySnapshot) {
          handleActivitySnapshot(event.value);
          return;
        }
        if (event.name === CustomEventName.SubAgent) {
          const { agentName, content, eventType, isLast, toolCalls } =
            event.value || {};
          if (!subAgentMsgIds[agentName]) {
            subAgentMsgIds[agentName] = getSubAgentMsgId(
              agentName,
              event.runId,
            );
          }
          const agentMsgId = subAgentMsgIds[agentName];
          const existing = messages.value.find((m) => m.id === agentMsgId);

          if (existing) {
            if (!existing.blocks) existing.blocks = [];

            if (content) {
              if (isLast) {
                if (Array.isArray(existing.content)) {
                  existing.content = [content];
                } else {
                  existing.content = content;
                }
                const lastContentIdx = existing.blocks.findLastIndex(
                  (b: any) => b.type === "content",
                );
                if (lastContentIdx >= 0) {
                  existing.blocks[lastContentIdx].content = content;
                } else {
                  existing.blocks.push({ type: "content", content });
                }
              } else {
                if (Array.isArray(existing.content)) {
                  if (existing.content.length > 0) {
                    existing.content[existing.content.length - 1] += content;
                  } else {
                    existing.content.push(content);
                  }
                } else if (typeof existing.content === "string") {
                  existing.content += content;
                }
                const lastBlock = existing.blocks[existing.blocks.length - 1];
                if (lastBlock?.type === "content") {
                  lastBlock.content += content;
                } else {
                  existing.blocks.push({ type: "content", content });
                }
              }
            }

            if (toolCalls?.length) {
              if (!existing.toolCalls) existing.toolCalls = [];
              for (const tc of toolCalls) {
                const existingTc = existing.toolCalls.find(
                  (t: any) => t.id === tc.toolCallId,
                );
                if (!existingTc) {
                  existing.toolCalls.push({
                    id: tc.toolCallId,
                    type: MessageContentType.Function,
                    function: {
                      name: tc.toolName || "",
                      arguments: "",
                      description: "",
                      mcpName: "",
                    },
                  });
                  existing.blocks.push({
                    type: "toolCall",
                    toolCallId: tc.toolCallId,
                  });
                }
              }
            }

            if (isLast) {
              existing.status = MessageStatus.Complete;
              delete subAgentMsgIds[agentName];
              if (existing.toolCalls?.length) {
                for (const tc of existing.toolCalls) {
                  if (!tc.toolMessage) {
                    tc.toolMessage = {
                      content: "",
                      status: MessageStatus.Complete,
                      toolCallId: tc.id,
                    };
                  }
                }
              }
            }
          } else {
            const isReasoning = eventType === ServerRole.Reasoning;
            const blocks: any[] = [];
            if (content) {
              blocks.push({ type: "content", content });
            }
            const tcArray: any[] = [];
            if (toolCalls?.length) {
              for (const tc of toolCalls) {
                tcArray.push({
                  id: tc.toolCallId,
                  type: MessageContentType.Function,
                  function: {
                    name: tc.toolName || "",
                    arguments: "",
                    description: "",
                    mcpName: "",
                  },
                });
                blocks.push({
                  type: "toolCall",
                  toolCallId: tc.toolCallId,
                });
              }
            }
            messages.value.push({
              id: agentMsgId,
              messageId: agentMsgId,
              role: isReasoning ? MessageRole.Reasoning : MessageRole.Assistant,
              agentName,
              content: isReasoning ? (content ? [content] : []) : content || "",
              blocks,
              ...(tcArray.length && { toolCalls: tcArray }),
              status: MessageStatus.Streaming,
            });
          }
        }
      },
      onFinish(event: any) {
        messages.value
          .filter((m) => m.status === MessageStatus.Streaming)
          .forEach((m) => {
            m.status = MessageStatus.Complete;
            if (m.agentName && m.toolCalls?.length) {
              for (const tc of m.toolCalls) {
                if (!tc.toolMessage) {
                  tc.toolMessage = {
                    content: "",
                    status: MessageStatus.Complete,
                    toolCallId: tc.id,
                  };
                }
              }
            }
          });

        if (event?.outcome === "interrupt" && event.interrupt) {
          currentInterrupt.value = {
            id: event.interrupt.id,
            reason: event.interrupt.reason,
            payload: event.interrupt.payload,
          };
          const interruptMsgId = `interrupt-${
            event.interrupt.id || Date.now()
          }`;
          messages.value.push({
            id: interruptMsgId,
            messageId: interruptMsgId,
            role: MessageRole.Pause,
            interrupt: currentInterrupt.value,
            content:
              event.interrupt.payload?.message ||
              event.interrupt.reason ||
              "Agent 需要您的确认才能继续执行",
            status: MessageStatus.Pending,
          });
        }

        messageStatus.value = MessageStatus.Complete;
      },
      onError(err: any) {
        console.error("Chat error:", err);
        if (assistantMsgId) {
          const msg = getAssistantMsg();
          if (msg) {
            msg.content += msg.content
              ? `\n\n**错误：** ${err}`
              : `**错误：** ${err}`;
            msg.status = MessageStatus.Error;
          }
        } else {
          messages.value.push({
            id: `err-${Date.now()}`,
            messageId: `err-${Date.now()}`,
            role: MessageRole.Assistant,
            content: `**错误：** ${err}`,
            status: MessageStatus.Error,
          });
        }
        messageStatus.value = MessageStatus.Complete;
      },
    };
  }

  async function initSession() {
    const gen = ++bootstrapGen;
    chatLoading.value = true;
    try {
      if (chatClient) {
        chatClient.close();
        chatClient = null;
      }
      messages.value = [];
      userInput.value = "";
      selectedShortcut.value = null;
      currentInterrupt.value = null;
      messageStatus.value = MessageStatus.Complete;
      selectedResources = [];

      let session = await getLatestSession(projectId.value || undefined);
      if (gen !== bootstrapGen) return;
      if (!session) {
        session = await createSession(projectId.value, "新对话");
      }
      if (gen !== bootstrapGen) return;

      sessionId.value = session.id;
      sessionName.value = session.title || "新对话";
      chatClient = createChatClient(sessionId.value, {
        getContext,
      });

      const status = await getChatStatus(sessionId.value);
      if (gen !== bootstrapGen) return;

      if (status?.active) {
        const history = await getSessionMessages(sessionId.value);
        if (history?.length) {
          messages.value = history.flatMap(mapHistoryMessage);
        }
        messageStatus.value = MessageStatus.Fetching;
        chatClient.subscribe(createStreamCallbacks());
      } else {
        const history = await getSessionMessages(sessionId.value);
        if (gen !== bootstrapGen) return;
        messageStatus.value = MessageStatus.Complete;
        if (!retryLastUserMessageIfNeeded(history) && history?.length) {
          messages.value = history.flatMap(mapHistoryMessage);
        }
      }
    } catch (err) {
      console.error("Failed to init session:", err);
    } finally {
      if (gen === bootstrapGen) {
        chatLoading.value = false;
      }
    }
  }

  watch(projectId, () => {
    void initSession();
  });

  /** Strip last USER turn from history UI, then send again so chat-x can append loading after the new user bubble. */
  function retryLastUserMessageIfNeeded(history: any[] | undefined | null) {
    if (!chatClient || !history?.length) return false;
    const last = history[history.length - 1];
    if (last.role !== ServerRole.User) return false;
    const textContent =
      typeof last.content === "string"
        ? last.content
        : last.content?.[0]?.[0]?.text || "";
    if (!textContent.trim()) return false;
    messages.value = history.slice(0, -1).flatMap(mapHistoryMessage);
    void sendMessage(textContent);
    return true;
  }

  async function sendMessage(content: any) {
    if (!sessionId.value) {
      try {
        const session = await createSession(projectId.value, "新对话");
        sessionId.value = session.id;
        sessionName.value = session.title || "新对话";
        chatClient = createChatClient(sessionId.value, {
          getContext,
        });
      } catch (err) {
        console.error("Failed to create session:", err);
        return;
      }
    }

    const textContent =
      typeof content === "string" ? content : content?.[0]?.[0]?.text || "";
    const userMsgId = `user-${Date.now()}`;
    messages.value.push({
      id: userMsgId,
      messageId: userMsgId,
      role: MessageRole.User,
      content: textContent,
      status: MessageStatus.Complete,
    });
    selectedShortcut.value = null;

    const tools = selectedResources.map((r) => ({
      name: r.name,
      description: r.description || "",
      parameters: r.parameters || {},
    }));
    selectedResources = [];
    messageStatus.value = MessageStatus.Fetching;
    chatClient.send(textContent, createStreamCallbacks(), { tools });
  }

  const stopSending = () => {
    if (chatClient) chatClient.abort();
    if (sessionId.value) {
      stopChat(sessionId.value).catch((err: any) => {
        console.error("Failed to stop chat:", err);
      });
    }
    messages.value
      .filter(
        (m) =>
          m.status === MessageStatus.Streaming ||
          m.status === MessageStatus.Pending,
      )
      .forEach((m) => {
        m.status = MessageStatus.Complete;
      });
    messageStatus.value = MessageStatus.Complete;
  };

  const resumeInterrupt = (payload: any) => {
    if (!currentInterrupt.value || !chatClient) return;
    const interruptId = currentInterrupt.value.id;

    messages.value
      .filter(
        (m) =>
          m.role === MessageRole.Pause && m.status === MessageStatus.Pending,
      )
      .forEach((m) => {
        m.status = MessageStatus.Complete;
      });
    currentInterrupt.value = null;

    messageStatus.value = MessageStatus.Fetching;
    chatClient.resume(interruptId, payload, createStreamCallbacks());
  };

  const dismissInterrupt = () => {
    if (!currentInterrupt.value) return;
    messages.value
      .filter(
        (m) =>
          m.role === MessageRole.Pause && m.status === MessageStatus.Pending,
      )
      .forEach((m) => {
        m.status = MessageStatus.Complete;
      });
    currentInterrupt.value = null;

    if (sessionId.value) {
      stopChat(sessionId.value).catch((err: any) => {
        console.error("Failed to stop chat after dismiss:", err);
      });
    }
  };

  const selectShortcut = (shortcut: any) => {
    selectedShortcut.value = { ...shortcut };
    const resource = resources.value.find((r) => r.id === shortcut.id);
    if (resource && !selectedResources.some((r) => r.id === shortcut.id)) {
      selectedResources.push(resource);
    }
  };

  const deleteShortcut = () => {
    if (selectedShortcut.value) {
      selectedResources = selectedResources.filter(
        (r) => r.id !== selectedShortcut.value.id,
      );
    }
    selectedShortcut.value = null;
    userInput.value = "";
  };

  const closeShortcut = () => {
    if (selectedShortcut.value) {
      selectedResources = selectedResources.filter(
        (r) => r.id !== selectedShortcut.value.id,
      );
    }
    selectedShortcut.value = null;
    userInput.value = "";
  };

  const submitShortcut = (_formModel: any) => {
    selectedShortcut.value = null;
    userInput.value = "";
  };

  const newChat = async () => {
    if (chatClient) {
      chatClient.close();
      chatClient = null;
    }
    messages.value = [];
    userInput.value = "";
    selectedShortcut.value = null;
    currentInterrupt.value = null;
    messageStatus.value = MessageStatus.Complete;
    selectedResources = [];

    try {
      const session = await createSession(projectId.value, "新对话");
      sessionId.value = session.id;
      sessionName.value = session.title || "新对话";
      chatClient = createChatClient(sessionId.value, {
        getContext,
      });
    } catch (err) {
      console.error("Failed to create new session:", err);
      sessionId.value = null;
      sessionName.value = "新对话";
    }
  };

  const loadSession = async (id: string, title?: string) => {
    if (id === sessionId.value) return;
    if (chatClient) {
      chatClient.close();
      chatClient = null;
    }
    messages.value = [];
    messageStatus.value = MessageStatus.Complete;
    chatLoading.value = true;

    try {
      sessionId.value = id;
      sessionName.value = title || "新对话";
      chatClient = createChatClient(sessionId.value, {
        getContext,
      });
      const history = await getSessionMessages(sessionId.value);
      const status = await getChatStatus(sessionId.value);
      if (status?.active) {
        if (history?.length) {
          messages.value = history.flatMap(mapHistoryMessage);
        }
        messageStatus.value = MessageStatus.Fetching;
        chatClient.subscribe(createStreamCallbacks());
      } else {
        messageStatus.value = MessageStatus.Complete;
        if (!retryLastUserMessageIfNeeded(history) && history?.length) {
          messages.value = history.flatMap(mapHistoryMessage);
        }
      }
    } catch (err) {
      console.error("Failed to load session:", err);
    } finally {
      chatLoading.value = false;
    }
  };

  const renameSession = async (title: string) => {
    if (!sessionId.value || !title.trim()) return;
    const result = await updateSessionTitle(sessionId.value, title.trim());
    if (result) {
      sessionName.value = title.trim();
    } else {
      console.error("Failed to rename session:", result);
    }
  };

  return {
    sessionId,
    sessionName,
    messages,
    messageStatus,
    chatLoading,
    userInput,
    cite,
    selectedShortcut,
    currentInterrupt,
    showWelcome,
    initSession,
    sendMessage,
    stopSending,
    resumeInterrupt,
    dismissInterrupt,
    selectShortcut,
    deleteShortcut,
    closeShortcut,
    submitShortcut,
    newChat,
    loadSession,
    renameSession,
  };
}
