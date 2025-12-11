/**
 * 权限指令
 * 用法：
 * v-permission="'TEACHER'" - 必须是教师
 * v-permission="['TEACHER', 'ADMIN']" - 教师或管理员
 */

import type { Directive, DirectiveBinding } from 'vue'
import { useUserStore } from '@/stores/user'
import { UserRole } from '@/types'

export const permission: Directive = {
    mounted(el: HTMLElement, binding: DirectiveBinding) {
        const userStore = useUserStore()
        const value = binding.value

        if (!value) {
            return
        }

        let hasPermission = false

        if (typeof value === 'string') {
            // 单一角色
            hasPermission = userStore.hasRole(value as UserRole)
        } else if (Array.isArray(value)) {
            // 多个角色（满足任一即可）
            hasPermission = userStore.hasAnyRole(value as UserRole[])
        }

        if (!hasPermission) {
            // 没有权限，移除元素
            el.parentNode?.removeChild(el)
        }
    },
}

/**
 * 最小角色等级指令
 * 用法：v-min-role="'TEACHER'" - 至少是教师（教师或管理员都可以看到）
 */
export const minRole: Directive = {
    mounted(el: HTMLElement, binding: DirectiveBinding) {
        const userStore = useUserStore()
        const value = binding.value as UserRole

        if (!value) {
            return
        }

        const hasPermission = userStore.hasMinRole(value)

        if (!hasPermission) {
            el.parentNode?.removeChild(el)
        }
    },
}
