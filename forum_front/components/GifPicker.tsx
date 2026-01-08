'use client'

import { useState, useEffect, useCallback, useRef } from 'react'
import { giphyApi, type GiphyGif } from '@/services/giphy'
import { motion, AnimatePresence } from 'framer-motion'

interface GifPickerProps {
  isOpen: boolean
  onClose: () => void
  onSelect: (gifUrl: string) => void
}

export default function GifPicker({ isOpen, onClose, onSelect }: GifPickerProps) {
  const [searchQuery, setSearchQuery] = useState('')
  const [gifs, setGifs] = useState<GiphyGif[]>([])
  const [loading, setLoading] = useState(false)
  const [offset, setOffset] = useState(0)
  const [hasMore, setHasMore] = useState(true)
  const searchTimeoutRef = useRef<NodeJS.Timeout | null>(null)
  const containerRef = useRef<HTMLDivElement>(null)

  // 트렌딩 GIF 로드
  const loadTrending = useCallback(async (reset: boolean = false) => {
    try {
      setLoading(true)
      const currentOffset = reset ? 0 : offset
      const response = await giphyApi.getTrending(25, currentOffset)
      
      // API 키가 없으면 빈 배열 반환 (오류 던지지 않음)
      if (!response.data || response.data.length === 0) {
        if (reset) {
          setGifs([])
          setOffset(0)
        }
        setHasMore(false)
        return
      }
      
      if (reset) {
        setGifs(response.data)
        setOffset(25)
      } else {
        setGifs(prev => [...prev, ...response.data])
        setOffset(prev => prev + 25)
      }
      
      setHasMore(response.data.length === 25)
    } catch (error) {
      console.error('트렌딩 GIF 로드 실패:', error)
      // 오류 발생 시 빈 배열 유지 (사용자에게 오류 표시하지 않음)
      if (reset) {
        setGifs([])
        setOffset(0)
      }
      setHasMore(false)
    } finally {
      setLoading(false)
    }
  }, [offset])

  // GIF 검색
  const searchGifs = useCallback(async (query: string, reset: boolean = false) => {
    if (!query.trim()) {
      loadTrending(reset)
      return
    }

    try {
      setLoading(true)
      const currentOffset = reset ? 0 : offset
      const response = await giphyApi.search(query, 25, currentOffset)
      
      // API 키가 없으면 빈 배열 반환 (오류 던지지 않음)
      if (!response.data || response.data.length === 0) {
        if (reset) {
          setGifs([])
          setOffset(0)
        }
        setHasMore(false)
        return
      }
      
      if (reset) {
        setGifs(response.data)
        setOffset(25)
      } else {
        setGifs(prev => [...prev, ...response.data])
        setOffset(prev => prev + 25)
      }
      
      setHasMore(response.data.length === 25)
    } catch (error) {
      console.error('GIF 검색 실패:', error)
      // 오류 발생 시 빈 배열 유지
      if (reset) {
        setGifs([])
        setOffset(0)
      }
      setHasMore(false)
    } finally {
      setLoading(false)
    }
  }, [offset, loadTrending])

  // 검색어 변경 시 디바운싱
  useEffect(() => {
    if (searchTimeoutRef.current) {
      clearTimeout(searchTimeoutRef.current)
    }

    searchTimeoutRef.current = setTimeout(() => {
      if (searchQuery.trim()) {
        searchGifs(searchQuery, true)
      } else {
        loadTrending(true)
      }
    }, 500)

    return () => {
      if (searchTimeoutRef.current) {
        clearTimeout(searchTimeoutRef.current)
      }
    }
  }, [searchQuery, searchGifs, loadTrending])

  // 모달이 열릴 때 트렌딩 GIF 로드
  useEffect(() => {
    if (isOpen && gifs.length === 0) {
      loadTrending(true)
    }
  }, [isOpen, gifs.length, loadTrending])

  // 스크롤로 더 불러오기
  const handleScroll = useCallback(() => {
    if (!containerRef.current || loading || !hasMore) return

    const { scrollTop, scrollHeight, clientHeight } = containerRef.current
    if (scrollHeight - scrollTop - clientHeight < 200) {
      if (searchQuery.trim()) {
        searchGifs(searchQuery, false)
      } else {
        loadTrending(false)
      }
    }
  }, [loading, hasMore, searchQuery, searchGifs, loadTrending])

  const handleSelectGif = (gif: GiphyGif) => {
    onSelect(gif.images.original.url)
    onClose()
  }

  if (!isOpen) return null

  return (
    <AnimatePresence>
      <div className="fixed inset-0 bg-black bg-opacity-50 z-50 flex items-center justify-center" onClick={onClose}>
        <motion.div
          initial={{ opacity: 0, scale: 0.9 }}
          animate={{ opacity: 1, scale: 1 }}
          exit={{ opacity: 0, scale: 0.9 }}
          className="bg-white rounded-lg w-full max-w-2xl max-h-[80vh] flex flex-col shadow-xl"
          onClick={(e) => e.stopPropagation()}
        >
          {/* 헤더 */}
          <div className="p-4 border-b border-gray-200">
            <div className="flex items-center justify-between mb-3">
              <h3 className="text-lg font-semibold text-gray-800">GIF 선택</h3>
              <button
                onClick={onClose}
                className="text-gray-400 hover:text-gray-600 transition"
              >
                <svg className="w-6 h-6" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M6 18L18 6M6 6l12 12" />
                </svg>
              </button>
            </div>
            {/* 검색 입력 */}
            <input
              type="text"
              value={searchQuery}
              onChange={(e) => setSearchQuery(e.target.value)}
              placeholder="GIF 검색..."
              className="w-full px-4 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-primary focus:border-transparent"
              autoFocus
            />
          </div>

          {/* GIF 그리드 */}
          <div
            ref={containerRef}
            onScroll={handleScroll}
            className="flex-1 overflow-y-auto p-4"
          >
            {loading && gifs.length === 0 ? (
              <div className="flex items-center justify-center py-12">
                <div className="text-gray-500">로딩 중...</div>
              </div>
            ) : gifs.length === 0 ? (
              <div className="flex items-center justify-center py-12">
                <div className="text-center text-gray-500">
                  <p className="mb-2">GIF를 찾을 수 없습니다.</p>
                  <p className="text-sm">GIPHY API 키가 설정되지 않았거나 검색 결과가 없습니다.</p>
                </div>
              </div>
            ) : (
              <div className="grid grid-cols-2 sm:grid-cols-3 gap-2">
                {gifs.map((gif) => (
                  <motion.div
                    key={gif.id}
                    whileHover={{ scale: 1.05 }}
                    whileTap={{ scale: 0.95 }}
                    className="relative cursor-pointer rounded-lg overflow-hidden bg-gray-100 aspect-square"
                    onClick={() => handleSelectGif(gif)}
                  >
                    <img
                      src={gif.images.fixed_height.url}
                      alt={gif.title}
                      className="w-full h-full object-cover"
                      loading="lazy"
                    />
                  </motion.div>
                ))}
              </div>
            )}
            {loading && gifs.length > 0 && (
              <div className="text-center py-4 text-gray-500 text-sm">로딩 중...</div>
            )}
          </div>
        </motion.div>
      </div>
    </AnimatePresence>
  )
}
