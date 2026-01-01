package com.eisiadev.enceladus

import org.bukkit.entity.Item
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.entity.EntityPickupItemEvent
import org.bukkit.event.entity.ItemDespawnEvent
import org.bukkit.event.entity.ItemSpawnEvent
import org.bukkit.event.entity.ItemMergeEvent
import org.bukkit.event.world.ChunkLoadEvent
import org.bukkit.event.world.ChunkUnloadEvent

class ItemGlowListener(
    private val plugin: ItemGlow,
    private val glowManager: GlowManager,
    private val hologramManager: HologramManager
) : Listener {

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    fun onItemSpawn(event: ItemSpawnEvent) {
        val item = event.entity

        // 다음 틱에 처리하여 아이템이 완전히 스폰되도록 함
        plugin.server.scheduler.runTask(plugin, Runnable {
            if (!item.isValid || item.isDead) return@Runnable

            val itemStack = item.itemStack
            val meta = itemStack.itemMeta ?: return@Runnable

            if (!meta.hasDisplayName()) return@Runnable

            val displayName = meta.displayName()?.let { ColorExtractor.componentToString(it) } ?: return@Runnable
            val glowColor = ColorExtractor.extractColor(displayName)

            if (glowColor != null) {
                glowManager.setGlowing(item, glowColor)
            }

            hologramManager.createHologram(item)
        })
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    fun onItemMerge(event: ItemMergeEvent) {
        val target = event.target
        val entity = event.entity

        cleanupItem(entity)

        plugin.server.scheduler.runTaskLater(plugin, Runnable {
            if (target.isValid && !target.isDead) {
                hologramManager.updateHologram(target)
            }
        }, 1L)
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    fun onItemDespawn(event: ItemDespawnEvent) {
        val item = event.entity
        cleanupItem(item)
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    fun onItemPickup(event: EntityPickupItemEvent) {
        val item = event.item
        cleanupItem(item)
        plugin.server.scheduler.runTask(plugin, Runnable {
            if (item.isValid && !item.isDead) {
                cleanupItem(item)
            }
        })
    }

    @EventHandler(priority = EventPriority.MONITOR)
    fun onChunkUnload(event: ChunkUnloadEvent) {
        val chunk = event.chunk

        // 청크 언로드 시 해당 청크의 모든 아이템 정리
        chunk.entities.filterIsInstance<Item>().forEach { item ->
            try {
                // 홀로그램만 제거하고 글로우는 유지 (재로드 시 복원용)
                hologramManager.removeHologram(item)
            } catch (e: Exception) {
                plugin.logger.warning("Error removing hologram on chunk unload: ${e.message}")
            }
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    fun onChunkLoad(event: ChunkLoadEvent) {
        val chunk = event.chunk

        // 청크 로드 시 해당 청크의 아이템들을 다시 처리
        plugin.server.scheduler.runTaskLater(plugin, Runnable {
            chunk.entities.filterIsInstance<Item>().forEach { item ->
                if (!item.isValid || item.isDead) return@forEach

                val itemStack = item.itemStack
                val meta = itemStack.itemMeta ?: return@forEach

                if (!meta.hasDisplayName()) return@forEach

                val displayName = meta.displayName()?.let { ColorExtractor.componentToString(it) } ?: return@forEach
                val glowColor = ColorExtractor.extractColor(displayName)

                // 글로우와 홀로그램 재생성
                if (glowColor != null) {
                    glowManager.setGlowing(item, glowColor)
                }
                hologramManager.createHologram(item)
            }
        }, 5L) // 청크가 완전히 로드될 때까지 약간 대기
    }

    private fun cleanupItem(item: Item) {
        try {
            hologramManager.removeHologram(item)
        } catch (e: Exception) {
            plugin.logger.warning("Error removing hologram: ${e.message}")
        }

        try {
            glowManager.removeGlowing(item)
        } catch (e: Exception) {
            plugin.logger.warning("Error removing glow: ${e.message}")
        }

        try {
            TeamManager.removeTeamForEntity(item)
        } catch (e: Exception) {
            plugin.logger.warning("Error removing team: ${e.message}")
        }
    }
}