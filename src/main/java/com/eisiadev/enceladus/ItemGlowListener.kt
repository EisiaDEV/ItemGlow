package com.eisiadev.enceladus

import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.entity.EntityPickupItemEvent
import org.bukkit.event.entity.ItemDespawnEvent
import org.bukkit.event.entity.ItemSpawnEvent

class ItemGlowListener(
    private val plugin: ItemGlow,
    private val glowManager: GlowManager,
    private val hologramManager: HologramManager
) : Listener {

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    fun onItemSpawn(event: ItemSpawnEvent) {
        val item = event.entity
        val itemStack = item.itemStack
        val meta = itemStack.itemMeta ?: return

        if (!meta.hasDisplayName()) return

        val displayName = meta.displayName()?.let { ColorExtractor.componentToString(it) } ?: return
        val glowColor = ColorExtractor.extractColor(displayName)

        if (glowColor != null) {
            glowManager.setGlowing(item, glowColor)
        }

        hologramManager.createHologram(item)
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

    private fun cleanupItem(item: org.bukkit.entity.Item) {
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