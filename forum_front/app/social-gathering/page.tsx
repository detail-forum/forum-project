'use client'

import { useState, useEffect } from 'react'
import { useRouter } from 'next/navigation'
import Header from '@/components/Header'
import { groupApi } from '@/services/api'
import type { GroupListDTO } from '@/types/api'

export default function SocialGatheringPage() {
  const router = useRouter()
  const [groups, setGroups] = useState<GroupListDTO[]>([])
  const [loading, setLoading] = useState(true)

  useEffect(() => {
    fetchGroups()
  }, [])

  const fetchGroups = async () => {
    try {
      setLoading(true)
      const response = await groupApi.getGroupList(0, 100)
      if (response.success && response.data) {
        // Page 객체에서 content 추출
        const content = response.data.content || response.data
        setGroups(Array.isArray(content) ? content : [])
      }
    } catch (error) {
      console.error('모임 목록 조회 실패:', error)
    } finally {
      setLoading(false)
    }
  }

  const handleGroupClick = (groupId: number) => {
    router.push(`/social-gathering/${groupId}`)
  }

  const handleCreateGroup = () => {
    router.push('/social-gathering/create')
  }

  return (
    <div>
      <Header onLoginClick={() => {}} />
      {/* 배너 영역 */}
      <div className="bg-gray-200 p-8 text-center">
        <h1 className="text-2xl font-bold">마음이 맞는 사람들끼리 모여서 활동해봐요!</h1>
      </div>
      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-12">
        {/* 소모임 목록영역 */}
        <div>
          <div className="flex justify-between items-center">
            <h2 className="text-xl font-bold">소모임 목록</h2>
            <button
              className="bg-blue-500 hover:bg-blue-600 text-white px-4 py-2 rounded transition"
              onClick={handleCreateGroup}
            >
              모임 추가하기
            </button>
          </div>
          {/* 소모임 리스트를 여기에 렌더링 */}
          {loading ? (
            <div className="mt-6 text-center">로딩 중...</div>
          ) : groups.length === 0 ? (
            <div className="mt-6 text-center text-gray-500">등록된 모임이 없습니다.</div>
          ) : (
            <div className="mt-6 grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-6">
              {groups.map((group) => (
                <div
                  key={group.id}
                  className="border p-4 rounded shadow hover:shadow-lg transition cursor-pointer"
                  onClick={() => handleGroupClick(group.id)}
                >
                  {group.profileImageUrl && (
                    <img
                      src={group.profileImageUrl}
                      alt={group.name}
                      className="w-full h-48 object-cover rounded mb-4"
                    />
                  )}
                  <h3 className="text-lg font-semibold">{group.name}</h3>
                  <p className="text-gray-600 text-sm mt-2 line-clamp-2">{group.description}</p>
                  <div className="mt-4 flex justify-between items-center text-sm text-gray-500">
                    <span>멤버: {group.memberCount}명</span>
                    <span>주인: {group.ownerNickname}</span>
                  </div>
                  {group.isMember && (
                    <span className="mt-2 inline-block bg-green-100 text-green-800 text-xs px-2 py-1 rounded">
                      가입됨
                    </span>
                  )}
                </div>
              ))}
            </div>
          )}
        </div>
        {/* 푸터 영역 */}
        <div className="mt-12 text-center text-gray-500">
          © 2023 소모임 플랫폼. All rights reserved.
        </div>
      </div>
    </div>
  )
}
