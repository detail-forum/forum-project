'use client'

import { useEffect, useState } from 'react'
import { useParams, useRouter } from 'next/navigation'
import { useSelector } from 'react-redux'
import type { RootState } from '@/store/store'
import { postApi } from '@/services/api'
import type { PostDetailDTO } from '@/types/api'
import Header from '@/components/Header'
import { getUsernameFromToken } from '@/utils/jwt'

export default function EditPostPage() {
  const params = useParams()
  const router = useRouter()
  const isAuthenticated = useSelector((state: RootState) => state.auth.isAuthenticated)
  const [post, setPost] = useState<PostDetailDTO | null>(null)
  const [formData, setFormData] = useState({ title: '', body: '' })
  const [loading, setLoading] = useState(true)
  const [saving, setSaving] = useState(false)
  const [error, setError] = useState('')

  useEffect(() => {
    if (!isAuthenticated) {
      router.push('/')
      return
    }
    fetchPost()
  }, [params.id, isAuthenticated])

  const fetchPost = async () => {
    try {
      setLoading(true)
      const response = await postApi.getPostDetail(Number(params.id))
      if (response.success && response.data) {
        const postData = response.data
        const currentUsername = getUsernameFromToken()
        
        // 작성자 확인
        if (currentUsername !== postData.username) {
          alert('본인이 작성한 게시글만 수정할 수 있습니다.')
          router.push(`/posts/${params.id}`)
          return
        }

        setPost(postData)
        setFormData({
          title: postData.title,
          body: postData.body,
        })
      }
    } catch (error) {
      console.error('게시글 조회 실패:', error)
      router.push('/')
    } finally {
      setLoading(false)
    }
  }

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault()
    setError('')
    setSaving(true)

    try {
      const updateData: { title?: string; body?: string } = {}
      if (formData.title !== post?.title) {
        updateData.title = formData.title
      }
      if (formData.body !== post?.body) {
        updateData.body = formData.body
      }

      if (Object.keys(updateData).length === 0) {
        setError('변경된 내용이 없습니다.')
        setSaving(false)
        return
      }

      const response = await postApi.updatePost(Number(params.id), updateData)
      if (response.success) {
        // 수정 후 페이지 이동 시 캐시 무효화를 위해 router.refresh() 호출
        router.push(`/posts/${params.id}`)
        router.refresh()
      }
    } catch (err: any) {
      setError(err.response?.data?.message || '게시글 수정에 실패했습니다.')
    } finally {
      setSaving(false)
    }
  }

  const handleChange = (e: React.ChangeEvent<HTMLInputElement | HTMLTextAreaElement>) => {
    setFormData({
      ...formData,
      [e.target.name]: e.target.value,
    })
  }

  if (!isAuthenticated) {
    return null
  }

  if (loading) {
    return (
      <div className="min-h-screen bg-white">
        <Header onLoginClick={() => router.push('/')} />
        <div className="max-w-4xl mx-auto px-4 sm:px-6 lg:px-8 py-12">
          <div className="text-center text-gray-500">로딩 중...</div>
        </div>
      </div>
    )
  }

  return (
    <div className="min-h-screen bg-white">
      <Header onLoginClick={() => router.push('/')} />
      <div className="max-w-4xl mx-auto px-4 sm:px-6 lg:px-8 py-12">
        <h1 className="text-3xl font-bold text-gray-900 mb-8">게시글 수정</h1>
        <form onSubmit={handleSubmit} className="space-y-6">
          <div>
            <label htmlFor="title" className="block text-sm font-medium text-gray-700 mb-2">
              제목 (10자 이상)
            </label>
            <input
              type="text"
              id="title"
              name="title"
              value={formData.title}
              onChange={handleChange}
              required
              minLength={10}
              className="w-full px-4 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-primary focus:border-transparent"
            />
          </div>

          <div>
            <label htmlFor="body" className="block text-sm font-medium text-gray-700 mb-2">
              본문 (10자 이상)
            </label>
            <textarea
              id="body"
              name="body"
              value={formData.body}
              onChange={handleChange}
              required
              minLength={10}
              rows={15}
              className="w-full px-4 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-primary focus:border-transparent"
            />
          </div>

          {error && (
            <div className="text-red-500 text-sm">{error}</div>
          )}

          <div className="flex justify-end space-x-4">
            <button
              type="button"
              onClick={() => router.push(`/posts/${params.id}`)}
              className="px-6 py-2 border border-gray-300 text-gray-700 rounded-lg hover:bg-gray-50 transition-colors"
            >
              취소
            </button>
            <button
              type="submit"
              disabled={saving || formData.title.length < 10 || formData.body.length < 10}
              className="px-6 py-2 bg-primary text-white rounded-lg font-medium hover:bg-secondary transition-colors disabled:opacity-50 disabled:cursor-not-allowed"
            >
              {saving ? '수정 중...' : '수정하기'}
            </button>
          </div>
        </form>
      </div>
    </div>
  )
}

