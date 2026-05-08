import { create } from 'zustand'
import { TabId } from './useAppStore'

export interface ChatMessage {
  id: string
  role: 'user' | 'assistant'
  content: string
  streaming?: boolean
}

type ChatHistory = Record<string, ChatMessage[]>

interface ChatState {
  histories: ChatHistory
  getMessages: (tabKey: string) => ChatMessage[]
  addMessage: (tabKey: string, msg: Omit<ChatMessage, 'id'>) => string
  appendToMessage: (tabKey: string, id: string, text: string) => void
  finalizeMessage: (tabKey: string, id: string) => void
  resetHistory: (tabKey: string) => void
}

export const useChatStore = create<ChatState>()((set, get) => ({
  histories: {},

  getMessages: (tabKey) => get().histories[tabKey] ?? [],

  addMessage: (tabKey, msg) => {
    const id = `msg-${Date.now()}-${Math.random()}`
    set((s) => ({
      histories: {
        ...s.histories,
        [tabKey]: [...(s.histories[tabKey] ?? []), { ...msg, id }],
      },
    }))
    return id
  },

  appendToMessage: (tabKey, id, text) =>
    set((s) => ({
      histories: {
        ...s.histories,
        [tabKey]: (s.histories[tabKey] ?? []).map((m) =>
          m.id === id ? { ...m, content: m.content + text } : m
        ),
      },
    })),

  finalizeMessage: (tabKey, id) =>
    set((s) => ({
      histories: {
        ...s.histories,
        [tabKey]: (s.histories[tabKey] ?? []).map((m) =>
          m.id === id ? { ...m, streaming: false } : m
        ),
      },
    })),

  resetHistory: (tabKey) =>
    set((s) => ({
      histories: { ...s.histories, [tabKey]: [] },
    })),
}))

export function chatKey(tab: TabId, context?: string): string {
  return context ? `${tab}:${context}` : tab
}
