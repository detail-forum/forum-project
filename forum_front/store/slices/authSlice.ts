import { createSlice, PayloadAction } from '@reduxjs/toolkit'
import { setCookie, removeCookie } from '@/utils/cookies'

interface AuthState {
  accessToken: string | null
  refreshToken: string | null
  isAuthenticated: boolean
}

// 서버와 클라이언트 간 Hydration 에러 방지를 위해 초기값은 항상 false
const initialState: AuthState = {
  accessToken: null,
  refreshToken: null,
  isAuthenticated: false,
}

const authSlice = createSlice({
  name: 'auth',
  initialState,
  reducers: {
    setCredentials: (state, action: PayloadAction<{ accessToken: string; refreshToken: string }>) => {
      state.accessToken = action.payload.accessToken
      state.refreshToken = action.payload.refreshToken
      state.isAuthenticated = true
      if (typeof window !== 'undefined') {
        setCookie('accessToken', action.payload.accessToken, 1) // 1일
        setCookie('refreshToken', action.payload.refreshToken, 7) // 7일
        // 기존 localStorage 정리 (마이그레이션)
        localStorage.removeItem('accessToken')
        localStorage.removeItem('refreshToken')
      }
    },
    updateTokens: (state, action: PayloadAction<{ accessToken: string; refreshToken: string }>) => {
      state.accessToken = action.payload.accessToken
      state.refreshToken = action.payload.refreshToken
      state.isAuthenticated = true // 토큰 업데이트 시 인증 상태 유지
      if (typeof window !== 'undefined') {
        setCookie('accessToken', action.payload.accessToken, 1)
        setCookie('refreshToken', action.payload.refreshToken, 7)
        // 기존 localStorage 정리 (마이그레이션)
        localStorage.removeItem('accessToken')
        localStorage.removeItem('refreshToken')
      }
    },
    logout: (state) => {
      // Redux 상태 초기화
      state.accessToken = null
      state.refreshToken = null
      state.isAuthenticated = false
      
      // 쿠키와 localStorage에서 토큰 완전히 제거
      if (typeof window !== 'undefined') {
        removeCookie('accessToken')
        removeCookie('refreshToken')
        localStorage.removeItem('accessToken')
        localStorage.removeItem('refreshToken')
      }
    },
  },
})

export const { setCredentials, updateTokens, logout } = authSlice.actions
export default authSlice.reducer

