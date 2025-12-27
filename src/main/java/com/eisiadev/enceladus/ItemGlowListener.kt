package com.eisiadev.enceladus

import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.entity.ItemDespawnEvent
import org.bukkit.event.entity.ItemSpawnEvent

class ItemGlowListener(
    private val plugin: ItemGlow,
    private val glowManager: GlowManager
) : Listener {

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    fun onItemSpawn(event: ItemSpawnEvent) {
        val item = event.entity
        val itemStack = item.itemStack
        val meta = itemStack.itemMeta ?: return

        if (!meta.hasDisplayName()) return

        val displayName = meta.displayName()?.let { ColorExtractor.componentToString(it) } ?: return
        val glowColor = ColorExtractor.extractColor(displayName) ?: return

        glowManager.setGlowing(item, glowColor)
    }

    @EventHandler(priority = EventPriority.MONITOR)
    fun onItemDespawn(event: ItemDespawnEvent) {
        glowManager.removeGlowing(event.entity)
        TeamManager.removeTeamForEntity(event.entity)
    }
}