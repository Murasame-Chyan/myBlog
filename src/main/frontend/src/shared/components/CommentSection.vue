<template>
  <div class="comment-section">
    <el-divider />
    <h4>评论 ({{ comments.length }})</h4>

    <div v-if="auth.loggedIn" class="comment-input-box">
      <el-input v-model="newComment" type="textarea" :rows="3" placeholder="写下你的评论..." maxlength="500" show-word-limit />
      <el-button type="primary" size="small" class="comment-submit-btn" @click="submitComment" :loading="submitting">发表评论</el-button>
    </div>
    <el-alert v-else title="请先登录后发表评论" type="info" :closable="false" show-icon />

    <el-empty v-if="comments.length === 0" description="暂无评论" />

    <div v-for="c in comments" :key="c.id" class="comment-item">
      <div class="comment-header">
        <img :src="c.author_avatar || '/images/default-avatar.png'" class="comment-avatar" />
        <span class="comment-author">{{ c.author_name }}</span>
        <span class="comment-time">{{ formatTime(c.created_at) }}</span>
      </div>
      <div class="comment-body" v-html="c.content"></div>
      <el-button text size="small" @click="toggleReply(c.id)"><el-icon><ChatLineSquare /></el-icon> 回复</el-button>

      <div v-if="replyTarget === c.id" class="reply-box">
        <el-input v-model="replyContent" type="textarea" :rows="2" placeholder="写下回复..." maxlength="500" show-word-limit />
        <el-button type="primary" size="small" @click="submitReply(c.id)" :loading="replySub">回复</el-button>
        <el-button size="small" @click="replyTarget = null">取消</el-button>
      </div>

      <div v-if="c.children?.length" class="child-comments">
        <div v-for="ch in c.children" :key="ch.id" class="comment-item child">
          <div class="comment-header">
            <img :src="ch.author_avatar || '/images/default-avatar.png'" class="comment-avatar" />
            <span class="comment-author">{{ ch.author_name }}</span>
            <span class="comment-time">{{ formatTime(ch.created_at) }}</span>
          </div>
          <div class="comment-body" v-html="ch.content"></div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { ElMessage } from 'element-plus'
import { ChatLineSquare } from '@element-plus/icons-vue'
import { useAuthStore } from '@/stores/auth'
import request from '@/utils/request'

const props = defineProps({ blogId: { type: Number, required: true }, comments: { type: Array, default: () => [] } })
const emit = defineEmits(['comment-added'])
const auth = useAuthStore()

const newComment = ref(''), submitting = ref(false)
const replyTarget = ref(null), replyContent = ref(''), replySub = ref(false)

function formatTime(d) { return d ? new Date(d).toLocaleString('zh-CN', { year:'numeric',month:'2-digit',day:'2-digit',hour:'2-digit',minute:'2-digit' }) : '' }

async function submitComment() {
  if (!newComment.value.trim()) return
  submitting.value = true
  try {
    const { data } = await request.post('/user/comment/add', new URLSearchParams({ bId: props.blogId, content: newComment.value.trim() }))
    if (data.code === 200) { ElMessage.success('评论成功'); newComment.value = ''; emit('comment-added') }
  } finally { submitting.value = false }
}

function toggleReply(id) { replyTarget.value = replyTarget.value === id ? null : id; replyContent.value = '' }

async function submitReply(parentId) {
  if (!replyContent.value.trim()) return
  replySub.value = true
  try {
    const { data } = await request.post('/user/comment/add', new URLSearchParams({ bId: props.blogId, parentId, content: replyContent.value.trim() }))
    if (data.code === 200) { ElMessage.success('回复成功'); replyTarget.value = null; emit('comment-added') }
  } finally { replySub.value = false }
}
</script>

<style scoped>
.comment-section { margin-top: 24px; } .comment-input-box { margin-bottom: 16px; } .comment-submit-btn { margin-top: 8px; }
.comment-item { padding: 16px 0; border-bottom: 1px solid #f0f0f0; } .comment-item.child { margin-left: 40px; }
.comment-header { display: flex; align-items: center; gap: 8px; margin-bottom: 8px; }
.comment-avatar { width: 32px; height: 32px; border-radius: 50%; object-fit: cover; }
.comment-author { font-weight: 600; } .comment-time { font-size: 0.8rem; color: #999; margin-left: auto; }
.comment-body { color: #555; line-height: 1.6; }
.reply-box { margin: 8px 0; display: flex; gap: 8px; flex-wrap: wrap; }
.child-comments { margin-top: 12px; border-left: 2px solid #e8f4fd; padding-left: 12px; }
</style>
