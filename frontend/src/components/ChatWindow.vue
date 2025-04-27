<template>
  <div class="chat-container">
    <div class="message-container" ref="messageContainer">
      <div 
        v-for="(message, index) in messages" 
        :key="index"
        :class="['message', message.type + '-message']"
      >
        <div v-if="message.type === 'bot'" v-html="renderMarkdown(message.content)"></div>
        <div v-else>{{ message.content }}</div>
      </div>
    </div>
    <div class="input-container">
      <input 
        v-model="inputMessage" 
        @keyup.enter="sendMessage"
        placeholder="输入您的问题..."
        class="message-input"
        :disabled="!isConnected"
      >
      <button 
        @click="sendMessage" 
        class="send-button"
        :disabled="!isConnected"
      >
        发送
      </button>
    </div>
    <div v-if="!isConnected" class="connection-status">
      连接已断开，正在重新连接...
    </div>
  </div>
</template>

<script>
import SockJS from 'sockjs-client';
import { marked } from 'marked';
import DOMPurify from 'dompurify';

marked.setOptions({
  gfm: true,
  breaks: true,
  highlight: function (code, lang) {
    return code;
  }
});

export default {
  name: 'ChatWindow',
  data() {
    return {
      ws: null,
      messages: [],
      inputMessage: '',
      isConnected: false,
      reconnectAttempts: 0,
      maxReconnectAttempts: 5,
      currentResponse: ''
    }
  },
  mounted() {
    this.initWebSocket()
  },
  beforeDestroy() {
    this.closeWebSocket()
  },
  methods: {
    initWebSocket() {
      try {
        const sockjs = new SockJS('http://localhost:8080/chat');
        this.ws = sockjs;
        
        this.ws.onopen = () => {
          console.log('WebSocket连接已建立');
          this.isConnected = true;
          this.reconnectAttempts = 0;
        };
        
        this.ws.onmessage = (event) => {
          if (event.data) {
            try {
              const response = JSON.parse(event.data);
              if (response.error) {
                this.appendMessage('error', response.error);
              } else {
                this.appendMessage('bot', response.content || event.data);
              }
            } catch (e) {
              this.appendMessage('bot', event.data);
            }
          }
        };
        
        this.ws.onerror = (error) => {
          console.error('WebSocket错误:', error);
          this.isConnected = false;
          this.appendMessage('error', '连接发生错误，正在尝试重新连接...');
        };
        
        this.ws.onclose = () => {
          console.log('WebSocket连接已关闭');
          this.isConnected = false;
          this.reconnect();
        };
      } catch (error) {
        console.error('WebSocket初始化失败:', error);
        this.isConnected = false;
        this.appendMessage('error', '连接初始化失败，请刷新页面重试');
      }
    },
    
    closeWebSocket() {
      if (this.ws) {
        this.ws.close()
        this.ws = null
      }
    },
    
    reconnect() {
      if (this.reconnectAttempts < this.maxReconnectAttempts) {
        this.reconnectAttempts++
        console.log(`尝试重新连接... (${this.reconnectAttempts}/${this.maxReconnectAttempts})`)
        setTimeout(() => {
          this.initWebSocket()
        }, 3000)
      }
    },
    
    sendMessage() {
      if (this.inputMessage.trim() && this.isConnected) {
        try {
          this.ws.send(this.inputMessage)
          this.appendMessage('user', this.inputMessage)
          this.inputMessage = ''
        } catch (error) {
          console.error('发送消息失败:', error)
          this.isConnected = false
        }
      }
    },
    
    renderMarkdown(content) {
      return DOMPurify.sanitize(marked(content));
    },

    appendMessage(type, content) {
      if (type === 'user') {
        this.messages.push({
          type,
          content
        });
        this.currentResponse = '';
        this.messages.push({
          type: 'bot',
          content: ''
        });
      } else if (type === 'bot') {
        this.currentResponse += content;
        if (this.messages.length > 0) {
          this.messages[this.messages.length - 1].content = this.currentResponse;
        }
      } else {
        this.messages.push({
          type,
          content
        });
      }
      
      this.$nextTick(() => {
        const container = this.$refs.messageContainer;
        container.scrollTop = container.scrollHeight;
      });
    }
  }
}
</script>

<style scoped>
.chat-container {
  max-width: 800px;
  margin: 20px auto;
  padding: 20px;
  height: calc(100vh - 40px);
  display: flex;
  flex-direction: column;
}

.message-container {
  flex: 1;
  border: 1px solid #ddd;
  border-radius: 4px;
  padding: 20px;
  overflow-y: auto;
  margin-bottom: 20px;
  background: #f9f9f9;
}

.input-container {
  display: flex;
  gap: 10px;
}

.message-input {
  flex: 1;
  padding: 12px;
  border: 1px solid #ddd;
  border-radius: 4px;
  font-size: 14px;
}

.message-input:disabled,
.send-button:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.send-button {
  padding: 0 20px;
  background: #409EFF;
  color: white;
  border: none;
  border-radius: 4px;
  cursor: pointer;
  transition: background 0.3s;
}

.send-button:hover:not(:disabled) {
  background: #66b1ff;
}

.message {
  margin: 10px 0;
  padding: 10px;
  border-radius: 4px;
  max-width: 80%;
  white-space: pre-wrap;
  word-break: break-word;
  line-height: 1.5;
}

.user-message {
  color: #409EFF;
  background: #ecf5ff;
  margin-left: auto;
}

.bot-message {
  color: #67C23A;
  background: #f0f9eb;
  font-size: 14px;
  font-family: "Microsoft YaHei", sans-serif;
}

.bot-message :deep(h1),
.bot-message :deep(h2),
.bot-message :deep(h3),
.bot-message :deep(h4),
.bot-message :deep(h5),
.bot-message :deep(h6) {
  margin: 8px 0;
  font-weight: 600;
}

.bot-message :deep(p) {
  margin: 8px 0;
  line-height: 1.6;
}

.bot-message :deep(code) {
  background-color: #f3f3f3;
  padding: 2px 4px;
  border-radius: 3px;
  font-family: Consolas, Monaco, 'Andale Mono', monospace;
}

.bot-message :deep(pre) {
  background-color: #f6f8fa;
  padding: 16px;
  border-radius: 6px;
  overflow-x: auto;
}

.bot-message :deep(pre code) {
  background-color: transparent;
  padding: 0;
}

.bot-message :deep(ul),
.bot-message :deep(ol) {
  padding-left: 20px;
  margin: 8px 0;
}

.bot-message :deep(blockquote) {
  margin: 8px 0;
  padding-left: 10px;
  border-left: 4px solid #dfe2e5;
  color: #6a737d;
}

.bot-message :deep(table) {
  border-collapse: collapse;
  width: 100%;
  margin: 8px 0;
}

.bot-message :deep(th),
.bot-message :deep(td) {
  border: 1px solid #dfe2e5;
  padding: 6px 13px;
}

.bot-message :deep(th) {
  background-color: #f6f8fa;
}

.bot-message :deep(a) {
  color: #0366d6;
  text-decoration: none;
}

.bot-message :deep(a:hover) {
  text-decoration: underline;
}

.bot-message :deep(img) {
  max-width: 100%;
  height: auto;
}

.connection-status {
  text-align: center;
  color: #E6A23C;
  padding: 10px;
  background: #fdf6ec;
  border-radius: 4px;
  margin-top: 10px;
}

.error-message {
  color: #F56C6C;
  background: #fef0f0;
  margin-left: auto;
  margin-right: auto;
  text-align: center;
}
</style> 