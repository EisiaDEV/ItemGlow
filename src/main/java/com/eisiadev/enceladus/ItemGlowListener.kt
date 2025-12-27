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

    @EventHandler(priority = EventPriority.MONITOR)
    fun onItemDespawn(event: ItemDespawnEvent) {
        hologramManager.removeHologram(event.entity)
        glowManager.removeGlowing(event.entity)
        TeamManager.removeTeamForEntity(event.entity)
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    fun onItemPickup(event: EntityPickupItemEvent) {
        val item = event.item
        hologramManager.removeHologram(item)
        glowManager.removeGlowing(item)
        TeamManager.removeTeamForEntity(item)
    }
}