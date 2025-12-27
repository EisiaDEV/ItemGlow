package com.eisiadev.enceladus

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.format.TextDecoration
import org.bukkit.entity.Display
import org.bukkit.entity.Item
import org.bukkit.entity.TextDisplay
import org.bukkit.util.Transformation
import org.joml.AxisAngle4f
import org.joml.Vector3f
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.locks.ReentrantLock

class HologramManager(private val plugin: ItemGlow) {

    private val holograms = ConcurrentHashMap<Int, TextDisplay>()
    private val hologramLocks = ConcurrentHashMap<Int, ReentrantLock>()

    fun createHologram(item: Item) {
        val entityId = item.entityId
        val lock = hologramLocks.computeIfAbsent(entityId) { ReentrantLock() }

        lock.lock()
        try {
            if (holograms.containsKey(entityId)) {
                return
            }

            val itemStack = item.itemStack
            val meta = itemStack.itemMeta ?: return

            val displayName = if (meta.hasDisplayName()) {
                meta.displayName()
            } else {
                Component.translatable(itemStack.translationKey())
            }

            val amount = itemStack.amount

            val hologramText = displayName!!
                .append(Component.text(" "))
                .append(Component.text("x$amount", NamedTextColor.GOLD))
                .decoration(TextDecoration.ITALIC, false)

            val location = item.location.clone()
            val textDisplay = item.world.spawn(location, TextDisplay::class.java) { display ->
                display.text(hologramText)
                display.isSeeThrough = false
                display.isShadowed = true
                display.alignment = TextDisplay.TextAlignment.CENTER
                display.billboard = Display.Billboard.VERTICAL
                display.backgroundColor = org.bukkit.Color.fromARGB(0, 0, 0, 0)

                val scale = Vector3f(1.2f, 1.2f, 1.2f)
                val leftRotation = AxisAngle4f(0f, 0f, 0f, 1f)
                val translation = Vector3f(0f, 0.5f, 0f)
                val rightRotation = AxisAngle4f(0f, 0f, 0f, 1f)
                display.transformation = Transformation(translation, leftRotation, scale, rightRotation)
            }
            item.addPassenger(textDisplay)
            holograms[entityId] = textDisplay

        } catch (e: Exception) {
            plugin.logger.warning("Failed to create hologram for item $entityId: ${e.message}")
        } finally {
            lock.unlock()
        }
    }

    fun removeHologram(item: Item) {
        val entityId = item.entityId
        val lock = hologramLocks.computeIfAbsent(entityId) { ReentrantLock() }
        if (!lock.tryLock()) {
            return
        }

        try {
            val textDisplay = holograms.remove(entityId)

            if (textDisplay != null) {
                safeRemoveHologram(item, textDisplay)
            }
        } finally {
            lock.unlock()
            hologramLocks.remove(entityId)
        }
    }

    private fun safeRemoveHologram(item: Item, textDisplay: TextDisplay) {
        try {
            if (!textDisplay.isValid) {
                return
            }
            if (item.isValid && item.passengers.contains(textDisplay)) {
                item.removePassenger(textDisplay)
            }
            textDisplay.remove()

        } catch (e: Exception) {
            plugin.logger.warning("Error removing hologram: ${e.message}")
            try {
                if (textDisplay.isValid) {
                    textDisplay.remove()
                }
            } catch (ignored: Exception) {}
        }
    }

    fun startCleanupTask() {
        plugin.server.scheduler.runTaskTimer(plugin, Runnable {
            val toRemove = mutableListOf<Int>()

            holograms.forEach { (entityId, textDisplay) ->
                if (!textDisplay.isValid) {
                    toRemove.add(entityId)
                    return@forEach
                }
                val hasValidItem = plugin.server.worlds.any { world ->
                    world.entities.any { entity ->
                        entity.entityId == entityId && entity is Item && entity.isValid
                    }
                }
                if (!hasValidItem) {
                    try {
                        textDisplay.remove()
                    } catch (ignored: Exception) {}
                    toRemove.add(entityId)
                }
            }
            toRemove.forEach { entityId ->
                holograms.remove(entityId)
                hologramLocks.remove(entityId)
            }

            if (toRemove.isNotEmpty()) {
                plugin.logger.info("Cleaned up ${toRemove.size} orphaned holograms")
            }

        }, 100L, 100L)
    }

    fun cleanup() {
        holograms.values.forEach { textDisplay ->
            try {
                if (textDisplay.isValid) {
                    textDisplay.remove()
                }
            } catch (ignored: Exception) {}
        }
        holograms.clear()
        hologramLocks.clear()
    }
}