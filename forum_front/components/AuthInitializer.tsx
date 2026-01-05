'use client'

import { useEffect } from 'react'
import { useDispatch } from 'react-redux'
import { setCredentials } from '@/store/slices/authSlice'
import { getCookie } from '@/utils/cookies'

// 클라이언트에서만 쿠키의 토큰을 읽어서 Redux 상태 초기화
export default function AuthInitializer() {
  const dispatch = useDispatch()

  useEffect(() => {
    // 쿠키에서 토큰 읽기 (localStorage는 마이그레이션용으로만 확인)
    const accessToken = getCookie('accessToken') || localStorage.getItem('accessToken')
    const refreshToken = getCookie('refreshToken') || localStorage.getItem('refreshToken')
    
    if (accessToken && refreshToken) {
      dispatch(setCredentials({ accessToken, refreshToken }))
      // localStorage에서 쿠키로 마이그레이션 후 정리
      if (localStorage.getItem('accessToken')) {
        localStorage.removeItem('accessToken')
        localStorage.removeItem('refreshToken')
      }
    }
  }, [dispatch])

  return null
}

