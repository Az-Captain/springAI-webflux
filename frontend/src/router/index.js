import Vue from 'vue'
import VueRouter from 'vue-router'
import ChatWindow from '../components/ChatWindow.vue'

Vue.use(VueRouter)

const routes = [
  {
    path: '/',
    name: 'Chat',
    component: ChatWindow
  }
]

const router = new VueRouter({
  mode: 'history',
  base: process.env.BASE_URL,
  routes
})

export default router 