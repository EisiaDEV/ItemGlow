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

class HologramManager(private val plugin: ItemGlow) {

    private val holograms = ConcurrentHashMap<Int, TextDisplay>()

    fun createHologram(item: Item) {
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
            val translation = Vector3f(0f, 0.5f, 0f)  // 아이템 위 0.75블록
            val rightRotation = AxisAngle4f(0f, 0f, 0f, 1f)
            display.transformation = Transformation(translation, leftRotation, scale, rightRotation)
        }

        item.addPassenger(textDisplay)
        holograms[item.entityId] = textDisplay
    }

    fun removeHologram(item: Item) {
        holograms.remove(item.entityId)?.let { textDisplay ->
            item.removePassenger(textDisplay)
            textDisplay.remove()
        }
    }

    fun cleanup() {
        holograms.values.forEach { it.remove() }
        holograms.clear()
    }
}