'use client'

import { useState } from 'react'
import { commentApi } from '@/services/api'

interface CommentFormProps {
  postId: number
  parentCommentId?: number | null
  onCommentCreated: () => void
  placeholder?: string
  onCancel?: () => void
}

export default function CommentForm({
  postId,
  parentCommentId,
  onCommentCreated,
  placeholder = '댓글을 입력하세요...',
  onCancel,
}: CommentFormProps) {
  const [body, setBody] = useState('')
  const [isSubmitting, setIsSubmitting] = useState(false)

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault()

    if (!body.trim()) {
      alert('댓글 내용을 입력해주세요.')
      return
    }

    try {
      setIsSubmitting(true)
      const response = await commentApi.createComment({
        body: body.trim(),
        postId,
        parentCommentId: parentCommentId || null,
      })

      if (response.success) {
        setBody('')
        onCommentCreated()
        if (onCancel) {
          onCancel()
        }
      }
    } catch (err: any) {
      alert(err.response?.data?.message || '댓글 작성에 실패했습니다.')
      console.error('댓글 작성 실패:', err)
    } finally {
      setIsSubmitting(false)
    }
  }

  return (
    <form onSubmit={handleSubmit} className="space-y-3">
      <textarea
        value={body}
        onChange={(e) => setBody(e.target.value)}
        placeholder={placeholder}
        className="w-full p-3 border border-gray-300 rounded-lg focus:ring-2 focus:ring-primary focus:border-transparent resize-none"
        rows={3}
        disabled={isSubmitting}
      />
      <div className="flex justify-end space-x-2">
        {onCancel && (
          <button
            type="button"
            onClick={onCancel}
            className="px-4 py-2 text-gray-700 bg-gray-100 rounded-lg hover:bg-gray-200 transition-colors"
          >
            취소
          </button>
        )}
        <button
          type="submit"
          disabled={isSubmitting || !body.trim()}
          className="px-4 py-2 bg-primary text-white rounded-lg hover:bg-secondary transition-colors disabled:opacity-50 disabled:cursor-not-allowed"
        >
          {isSubmitting ? '작성 중...' : '작성'}
        </button>
      </div>
    </form>
  )
}

