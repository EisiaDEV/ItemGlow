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
    private val pendingRemovals = ConcurrentHashMap.newKeySet<Int>()

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

        if (!pendingRemovals.add(entityId)) {
            return
        }

        val lock = hologramLocks.computeIfAbsent(entityId) { ReentrantLock() }

        if (!lock.tryLock()) {
            plugin.server.scheduler.runTask(plugin, Runnable {
                pendingRemovals.remove(entityId)
                removeHologram(item)
            })
            return
        }

        try {
            val textDisplay = holograms.remove(entityId)

            if (textDisplay != null) {
                if (item.isValid && item.passengers.contains(textDisplay)) {
                    item.removePassenger(textDisplay)
                }
                plugin.server.scheduler.runTask(plugin, Runnable {
                    try {
                        if (textDisplay.isValid) {
                            textDisplay.remove()
                        }
                    } catch (e: Exception) {
                        plugin.logger.warning("Error removing hologram entity: ${e.message}")
                    }
                    pendingRemovals.remove(entityId)
                })
            } else {
                pendingRemovals.remove(entityId)
            }
        } catch (e: Exception) {
            plugin.logger.warning("Error in removeHologram: ${e.message}")
            pendingRemovals.remove(entityId)
        } finally {
            lock.unlock()
            hologramLocks.remove(entityId)
        }
    }

    fun startCleanupTask() {
        plugin.server.scheduler.runTaskTimer(plugin, Runnable {
            val toRemove = mutableListOf<Int>()

            holograms.forEach { (entityId, textDisplay) ->
                var shouldRemove = false
                if (!textDisplay.isValid) {
                    shouldRemove = true
                } else {
                    val hasVehicle = textDisplay.vehicle != null
                    if (!hasVehicle) {
                        shouldRemove = true
                        try {
                            textDisplay.remove()
                        } catch (ignored: Exception) {}
                    }
                }

                if (shouldRemove) {
                    toRemove.add(entityId)
                }
            }

            toRemove.forEach { entityId ->
                holograms.remove(entityId)
                hologramLocks.remove(entityId)
                pendingRemovals.remove(entityId)
            }

            if (toRemove.isNotEmpty()) {
                plugin.logger.fine("Fast cleanup: removed ${toRemove.size} holograms")
            }

        }, 4L, 4L) // 0.2초마다

        plugin.server.scheduler.runTaskTimer(plugin, Runnable {
            val toRemove = mutableListOf<Int>()

            val validItemIds = plugin.server.worlds
                .flatMap { it.entities }
                .filterIsInstance<Item>()
                .filter { it.isValid }
                .map { it.entityId }
                .toSet()

            holograms.forEach { (entityId, textDisplay) ->
                if (!textDisplay.isValid || entityId !in validItemIds) {
                    try {
                        if (textDisplay.isValid) {
                            textDisplay.remove()
                        }
                    } catch (ignored: Exception) {}
                    toRemove.add(entityId)
                }
            }

            toRemove.forEach { entityId ->
                holograms.remove(entityId)
                hologramLocks.remove(entityId)
                pendingRemovals.remove(entityId)
            }

            if (toRemove.isNotEmpty()) {
                plugin.logger.info("Deep cleanup: removed ${toRemove.size} orphaned holograms")
            }

        }, 100L, 100L) // 5초마다
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
        pendingRemovals.clear()
    }
}