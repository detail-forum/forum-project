'use client'

import { useState, useEffect, useCallback } from 'react'
import { useSelector } from 'react-redux'
import type { RootState } from '@/store/store'
import { notificationApi } from '@/services/api'
import type { NotificationDTO } from '@/types/api'
import { useRouter } from 'next/navigation'
import Link from 'next/link'
import Image from 'next/image'
import { formatDistanceToNow } from 'date-fns'
import { ko } from 'date-fns/locale'

export default function NotificationsPage() {
  const [notifications, setNotifications] = useState<NotificationDTO[]>([])
  const [loading, setLoading] = useState(true)
  const [page, setPage] = useState(0)
  const [hasMore, setHasMore] = useState(true)
  const [unreadCount, setUnreadCount] = useState(0)
  const isAuthenticated = useSelector((state: RootState) => state.auth.isAuthenticated)
  const router = useRouter()

  const fetchNotifications = useCallback(async (pageNum: number = 0) => {
    if (!isAuthenticated) {
      setLoading(false)
      return
    }

    try {
      setLoading(true)
      const response = await notificationApi.getNotifications(pageNum, 20)
      if (response.success && response.data) {
        const newNotifications = response.data.content || []
        if (pageNum === 0) {
          setNotifications(newNotifications)
        } else {
          setNotifications(prev => [...prev, ...newNotifications])
        }
        setHasMore(!response.data.last && newNotifications.length > 0)
        setPage(pageNum)
      }
    } catch (error) {
      console.error('알림 조회 실패:', error)
    } finally {
      setLoading(false)
    }
  }, [isAuthenticated])

  const fetchUnreadCount = useCallback(async () => {
    if (!isAuthenticated) return
    try {
      const response = await notificationApi.getUnreadCount()
      if (response.success) {
        setUnreadCount(response.data)
      }
    } catch (error) {
      console.error('알림 개수 조회 실패:', error)
    }
  }, [isAuthenticated])

  const handleMarkAsRead = async (notificationId: number) => {
    try {
      await notificationApi.markAsRead(notificationId)
      setNotifications(prev =>
        prev.map(n => n.id === notificationId ? { ...n, isRead: true } : n)
      )
      setUnreadCount(prev => Math.max(0, prev - 1))
    } catch (error) {
      console.error('알림 읽음 처리 실패:', error)
    }
  }

  const handleMarkAllAsRead = async () => {
    try {
      await notificationApi.markAllAsRead()
      setNotifications(prev => prev.map(n => ({ ...n, isRead: true })))
      setUnreadCount(0)
    } catch (error) {
      console.error('모든 알림 읽음 처리 실패:', error)
    }
  }

  const getNotificationLink = (notification: NotificationDTO): string => {
    switch (notification.type) {
      case 'POST_LIKE':
        if (notification.relatedGroupPostId) {
          return `/social-gathering/${notification.relatedGroupPostId}/posts/${notification.relatedGroupPostId}`
        }
        return notification.relatedPostId ? `/posts/${notification.relatedPostId}` : '#'
      case 'COMMENT_REPLY':
        if (notification.relatedGroupPostId) {
          return `/social-gathering/${notification.relatedGroupPostId}/posts/${notification.relatedGroupPostId}`
        }
        return notification.relatedPostId ? `/posts/${notification.relatedPostId}` : '#'
      case 'NEW_FOLLOWER':
        return notification.relatedUserId ? `/users/${notification.relatedUserNickname || ''}` : '#'
      case 'NEW_MESSAGE':
        return '/chat'
      case 'ADMIN_NOTICE':
        return '#'
      default:
        return '#'
    }
  }

  const getNotificationIcon = (type: NotificationDTO['type']) => {
    switch (type) {
      case 'POST_LIKE':
        return '❤️'
      case 'COMMENT_REPLY':
        return '💬'
      case 'NEW_FOLLOWER':
        return '👤'
      case 'NEW_MESSAGE':
        return '📨'
      case 'ADMIN_NOTICE':
        return '📢'
      default:
        return '🔔'
    }
  }

  useEffect(() => {
    if (isAuthenticated) {
      fetchNotifications(0)
      fetchUnreadCount()
    } else {
      router.push('/')
    }
  }, [isAuthenticated, fetchNotifications, fetchUnreadCount, router])

  if (!isAuthenticated) {
    return null
  }

  return (
    <div className="min-h-screen bg-gray-50">
      <div className="max-w-4xl mx-auto px-4 py-8">
        <div className="bg-white rounded-lg shadow-sm border border-gray-200">
          <div className="px-6 py-4 border-b border-gray-200 flex items-center justify-between">
            <h1 className="text-2xl font-bold text-gray-900">알림</h1>
            {unreadCount > 0 && (
              <button
                onClick={handleMarkAllAsRead}
                className="text-sm text-primary hover:text-secondary transition-colors"
              >
                모두 읽음 처리
              </button>
            )}
          </div>

          {loading && notifications.length === 0 ? (
            <div className="px-6 py-12 text-center text-gray-500">
              알림을 불러오는 중...
            </div>
          ) : notifications.length === 0 ? (
            <div className="px-6 py-12 text-center text-gray-500">
              알림이 없습니다.
            </div>
          ) : (
            <div className="divide-y divide-gray-200">
              {notifications.map((notification) => {
                const link = getNotificationLink(notification)
                const content = (
                  <div
                    className={`px-6 py-4 hover:bg-gray-50 transition-colors cursor-pointer ${
                      !notification.isRead ? 'bg-blue-50' : ''
                    }`}
                    onClick={() => {
                      if (!notification.isRead) {
                        handleMarkAsRead(notification.id)
                      }
                      if (link !== '#') {
                        router.push(link)
                      }
                    }}
                  >
                    <div className="flex items-start space-x-4">
                      <div className="flex-shrink-0 text-2xl">
                        {getNotificationIcon(notification.type)}
                      </div>
                      <div className="flex-1 min-w-0">
                        <div className="flex items-start justify-between">
                          <div className="flex-1">
                            <p className="text-sm font-medium text-gray-900">
                              {notification.title}
                            </p>
                            <p className="mt-1 text-sm text-gray-600">
                              {notification.message}
                            </p>
                            {notification.relatedUserNickname && (
                              <div className="mt-2 flex items-center space-x-2">
                                {notification.relatedUserProfileImageUrl ? (
                                  <Image
                                    src={
                                      notification.relatedUserProfileImageUrl.startsWith('http')
                                        ? notification.relatedUserProfileImageUrl
                                        : `${process.env.NEXT_PUBLIC_UPLOAD_BASE_URL || ''}${notification.relatedUserProfileImageUrl}`
                                    }
                                    alt={notification.relatedUserNickname}
                                    width={24}
                                    height={24}
                                    className="rounded-full object-cover"
                                    unoptimized
                                  />
                                ) : (
                                  <div className="w-6 h-6 rounded-full bg-gray-300 flex items-center justify-center">
                                    <span className="text-xs text-gray-600">
                                      {notification.relatedUserNickname.charAt(0).toUpperCase()}
                                    </span>
                                  </div>
                                )}
                                <span className="text-xs text-gray-500">
                                  {notification.relatedUserNickname}
                                </span>
                              </div>
                            )}
                            <p className="mt-2 text-xs text-gray-400">
                              {formatDistanceToNow(new Date(notification.createdTime), {
                                addSuffix: true,
                                locale: ko,
                              })}
                            </p>
                          </div>
                          {!notification.isRead && (
                            <div className="ml-4 flex-shrink-0">
                              <div className="w-2 h-2 bg-blue-500 rounded-full"></div>
                            </div>
                          )}
                        </div>
                      </div>
                    </div>
                  </div>
                )

                return link !== '#' ? (
                  <Link key={notification.id} href={link} prefetch={true}>
                    {content}
                  </Link>
                ) : (
                  <div key={notification.id}>{content}</div>
                )
              })}
            </div>
          )}

          {hasMore && !loading && (
            <div className="px-6 py-4 border-t border-gray-200 text-center">
              <button
                onClick={() => fetchNotifications(page + 1)}
                className="text-sm text-primary hover:text-secondary transition-colors"
              >
                더 보기
              </button>
            </div>
          )}

          {loading && notifications.length > 0 && (
            <div className="px-6 py-4 text-center text-gray-500 text-sm">
              로딩 중...
            </div>
          )}
        </div>
      </div>
    </div>
  )
}
