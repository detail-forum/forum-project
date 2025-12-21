'use client'

import { useState, useEffect } from 'react'
import Link from 'next/link'
import Image from 'next/image'
import { useSelector, useDispatch } from 'react-redux'
import type { RootState } from '@/store/store'
import { logout } from '@/store/slices/authSlice'
import { useRouter } from 'next/navigation'
import { getUsernameFromToken } from '@/utils/jwt'

interface HeaderProps {
  onLoginClick: () => void
}

export default function Header({ onLoginClick }: HeaderProps) {
  const [mounted, setMounted] = useState(false)
  const [username, setUsername] = useState<string | null>(null)
  const isAuthenticated = useSelector((state: RootState) => state.auth.isAuthenticated)
  const dispatch = useDispatch()
  const router = useRouter()

  // Hydration 에러 방지: 클라이언트에서만 마운트된 후 인증 상태 표시
  useEffect(() => {
    setMounted(true)
  }, [])

  // 인증 상태가 변경될 때마다 username 업데이트
  useEffect(() => {
    if (mounted && isAuthenticated) {
      const user = getUsernameFromToken()
      setUsername(user)
    } else {
      setUsername(null)
    }
  }, [mounted, isAuthenticated])

  const handleLogout = () => {
    // Redux 상태에서 로그아웃 처리 (토큰 제거 포함)
    dispatch(logout())
    
    // localStorage에서 토큰 제거 (이중 확인)
    if (typeof window !== 'undefined') {
      localStorage.removeItem('accessToken')
      localStorage.removeItem('refreshToken')
    }
    
    // 메인 페이지로 이동 후 새로고침하여 완전한 로그아웃 상태로 전환
    router.push('/')
    router.refresh()
  }

  return (
    <header className="sticky top-0 z-50 bg-white border-b border-gray-200">
      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
        <div className="flex items-center justify-between h-16">
          <Link href="/" className="flex items-center space-x-3">
            <span className="text-lg font-semibold text-gray-800">
              rjgud49's forum
            </span>
          </Link>
          
          <nav className="flex items-center space-x-4">
            {mounted && isAuthenticated ? (
              <>
                {username && (
                  <span className="text-sm text-gray-600 font-medium">
                    {username}님
                  </span>
                )}
                <Link
                  href="/posts"
                  className="text-gray-700 hover:text-primary transition-colors"
                >
                  게시글 작성
                </Link>
                <Link
                  href="/my-posts"
                  className="text-gray-700 hover:text-primary transition-colors"
                >
                  내 게시글
                </Link>
                <button
                  onClick={handleLogout}
                  className="px-4 py-2 text-gray-700 hover:text-primary transition-colors"
                >
                  로그아웃
                </button>
              </>
            ) : (
              <button
                onClick={onLoginClick}
                className="px-4 py-2 bg-primary text-white rounded-lg hover:bg-secondary transition-colors"
              >
                로그인
              </button>
            )}
          </nav>
        </div>
      </div>
    </header>
  )
}

