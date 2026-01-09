import { useEffect, useRef, useState, useCallback } from 'react'
import { Client, IMessage } from '@stomp/stompjs'
import SockJS from 'sockjs-client'
import { store } from '@/store/store'
import { getCookie } from '@/utils/cookies'
import type { DirectChatMessageDTO } from '@/types/api'

interface UseDirectWebSocketOptions {
  roomId: number | null
  onMessage?: (message: DirectChatMessageDTO) => void
  onTyping?: (data: { username: string; isTyping: boolean }) => void
  onRead?: (data: { messageId: number; username: string; isRead: boolean }) => void
  enabled?: boolean
}

export function useDirectWebSocket({
  roomId,
  onMessage,
  onTyping,
  onRead,
  enabled = true,
}: UseDirectWebSocketOptions) {
  const [isConnected, setIsConnected] = useState(false)
  const [typingUsers, setTypingUsers] = useState<Set<string>>(new Set())
  const clientRef = useRef<Client | null>(null)
  const typingTimeoutRef = useRef<Map<string, NodeJS.Timeout>>(new Map())
  const subscriptionsRef = useRef<any[]>([])
  
  // 콜백 함수들을 ref로 저장하여 의존성 변경으로 인한 재연결 방지
  const onMessageRef = useRef(onMessage)
  const onTypingRef = useRef(onTyping)
  const onReadRef = useRef(onRead)
    
  // 콜백 함수 업데이트
  useEffect(() => {
    onMessageRef.current = onMessage
    onTypingRef.current = onTyping
    onReadRef.current = onRead
  }, [onMessage, onTyping, onRead])

  // 토큰 가져오기
  const getToken = useCallback(() => {
    if (typeof window === 'undefined') return null
    // Redux store에서 먼저 확인, 없으면 쿠키에서
    const state = store.getState()
    return state.auth.accessToken || getCookie('accessToken')
  }, [])

  useEffect(() => {
    if (!enabled || !roomId) return

    const token = getToken()
    if (!token) {
      console.warn('WebSocket 연결 실패: 토큰이 없습니다.')
      return
    }

    // API URL에서 /api 제거하고 WebSocket 엔드포인트 생성
    const apiUrl = process.env.NEXT_PUBLIC_API_URL || 'https://forum.rjsgud.com/api'
    // /api를 제거하고 기본 URL 사용
    const baseUrl = apiUrl.replace('/api', '')
    // SockJS는 상대 경로를 사용하므로 전체 URL 구성
    const socketUrl = `${baseUrl}/ws`
    
    console.log('일반 채팅 WebSocket 연결 시도:', socketUrl)

    const client = new Client({
      webSocketFactory: () => {
        return new SockJS(socketUrl) as unknown as WebSocket
      },
      connectHeaders: {
        Authorization: `Bearer ${token}`,
      },
      reconnectDelay: 5000,
      heartbeatIncoming: 4000,
      heartbeatOutgoing: 4000,
      debug: (str) => {
        // 개발 환경에서만 디버그 로그 출력
        if (process.env.NODE_ENV === 'development') {
          console.log('STOMP:', str)
        }
      },
      onConnect: () => {
        setIsConnected(true)
        console.log('일반 채팅 WebSocket 연결 성공')
        
        if (!clientRef.current) return

        // 기존 구독 정리
        subscriptionsRef.current.forEach(sub => {
          try {
            sub.unsubscribe()
          } catch (e) {
            console.warn('구독 해제 오류:', e)
          }
        })
        subscriptionsRef.current = []

        // 일반 채팅방 메시지 구독
        const messageSubscription = clientRef.current.subscribe(
          `/topic/direct/${roomId}`,
          (message: IMessage) => {
            try {
              console.log('일반 채팅 메시지 수신:', message.body)
              const data = JSON.parse(message.body) as DirectChatMessageDTO
              console.log('파싱된 일반 채팅 메시지 데이터:', data)
              onMessageRef.current?.(data)
            } catch (error) {
              console.error('일반 채팅 메시지 파싱 오류:', error, message.body)
            }
          },
          {
            // 구독 헤더 (필요시)
          }
        )
        subscriptionsRef.current.push(messageSubscription)
        console.log('일반 채팅 메시지 구독 완료:', `/topic/direct/${roomId}`)

        // 타이핑 인디케이터 구독
        const typingSubscription = clientRef.current.subscribe(
          `/topic/direct/${roomId}/typing`,
          (message: IMessage) => {
            try {
              const data = JSON.parse(message.body)
              onTypingRef.current?.(data)
              
              if (data.isTyping) {
                setTypingUsers(prev => new Set(prev).add(data.username))
                
                // 3초 후 자동으로 타이핑 종료
                const existingTimeout = typingTimeoutRef.current.get(data.username)
                if (existingTimeout) {
                  clearTimeout(existingTimeout)
                }
                
                const timeout = setTimeout(() => {
                  setTypingUsers(prev => {
                    const next = new Set(prev)
                    next.delete(data.username)
                    return next
                  })
                }, 3000)
                
                typingTimeoutRef.current.set(data.username, timeout)
              } else {
                setTypingUsers(prev => {
                  const next = new Set(prev)
                  next.delete(data.username)
                  return next
                })
                
                const timeout = typingTimeoutRef.current.get(data.username)
                if (timeout) {
                  clearTimeout(timeout)
                  typingTimeoutRef.current.delete(data.username)
                }
              }
            } catch (error) {
              console.error('타이핑 데이터 파싱 오류:', error)
            }
          }
        )
        subscriptionsRef.current.push(typingSubscription)

        // 읽음 표시 구독
        const readSubscription = clientRef.current.subscribe(
          `/topic/direct/${roomId}/read`,
          (message: IMessage) => {
            try {
              const data = JSON.parse(message.body)
              onReadRef.current?.(data)
            } catch (error) {
              console.error('읽음 데이터 파싱 오류:', error)
            }
          }
        )
        subscriptionsRef.current.push(readSubscription)
      },
      onDisconnect: () => {
        setIsConnected(false)
        console.log('일반 채팅 WebSocket 연결 종료')
        // 구독 정리
        subscriptionsRef.current = []
      },
      onStompError: (frame) => {
        console.error('STOMP 오류:', frame)
        console.error('STOMP 오류 상세:', {
          command: frame.command,
          headers: frame.headers,
          body: frame.body,
        })
      },
      onWebSocketError: (event) => {
        console.error('WebSocket 오류:', event)
      },
    })

    clientRef.current = client
    client.activate()

    return () => {
      // 타이핑 타임아웃 정리
      typingTimeoutRef.current.forEach(timeout => clearTimeout(timeout))
      typingTimeoutRef.current.clear()
      
      // 구독 정리
      subscriptionsRef.current.forEach(sub => {
        try {
          sub.unsubscribe()
        } catch (e) {
          console.warn('구독 해제 오류:', e)
        }
      })
      subscriptionsRef.current = []
      
      // 연결 종료
      if (clientRef.current) {
        try {
          clientRef.current.deactivate()
        } catch (e) {
          console.warn('WebSocket 연결 종료 오류:', e)
        }
        clientRef.current = null
      }
    }
  }, [roomId, enabled, getToken])

  const sendMessage = useCallback((message: string) => {
    if (!clientRef.current) {
      console.error('WebSocket 클라이언트가 없습니다.')
      return false
    }
    
    if (!clientRef.current.connected) {
      console.error('WebSocket이 연결되지 않았습니다.')
      return false
    }
    
    if (!roomId) {
      console.error('roomId가 없습니다.')
      return false
    }
    
    try {
      const destination = `/app/direct/${roomId}/send`
      const payload = { message }
      const body = JSON.stringify(payload)
      console.log('일반 채팅 메시지 전송:', { destination, body, connected: clientRef.current.connected })
      
      clientRef.current.publish({
        destination,
        body,
      })
      console.log('일반 채팅 메시지 전송 완료')
      return true
    } catch (error) {
      console.error('일반 채팅 메시지 전송 중 오류:', error)
      return false
    }
  }, [roomId])

  const startTyping = useCallback(() => {
    if (clientRef.current && clientRef.current.connected && roomId) {
      clientRef.current.publish({
        destination: `/app/direct/${roomId}/typing/start`,
        body: JSON.stringify({}),
      })
    }
  }, [roomId])

  const stopTyping = useCallback(() => {
    if (clientRef.current && clientRef.current.connected && roomId) {
      clientRef.current.publish({
        destination: `/app/direct/${roomId}/typing/stop`,
        body: JSON.stringify({}),
      })
    }
  }, [roomId])

  const markAsRead = useCallback((messageId: number) => {
    if (clientRef.current && clientRef.current.connected && roomId) {
      clientRef.current.publish({
        destination: `/app/direct/${roomId}/read`,
        body: JSON.stringify({ messageId }),
      })
    }
  }, [roomId])

  return {
    isConnected,
    sendMessage,
    startTyping,
    stopTyping,
    markAsRead,
    typingUsers: Array.from(typingUsers),
  }
}