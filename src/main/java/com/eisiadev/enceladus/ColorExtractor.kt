package com.eisiadev.enceladus

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer
import org.bukkit.ChatColor
import java.awt.Color

object ColorExtractor {

    private val legacyPattern = Regex("&([0-9a-fA-Fklmnor])")
    private val hexPattern = Regex("&#([0-9a-fA-F]{6})")
    private val minecraftHexPattern = Regex("&x(&[0-9a-fA-F]){6}")

    private val colorMap = mapOf(
        '0' to ChatColor.BLACK,
        '1' to ChatColor.DARK_BLUE,
        '2' to ChatColor.DARK_GREEN,
        '3' to ChatColor.DARK_AQUA,
        '4' to ChatColor.DARK_RED,
        '5' to ChatColor.DARK_PURPLE,
        '6' to ChatColor.GOLD,
        '7' to ChatColor.GRAY,
        '8' to ChatColor.DARK_GRAY,
        '9' to ChatColor.BLUE,
        'a' to ChatColor.GREEN,
        'b' to ChatColor.AQUA,
        'c' to ChatColor.RED,
        'd' to ChatColor.LIGHT_PURPLE,
        'e' to ChatColor.YELLOW,
        'f' to ChatColor.WHITE
    )

    private val chatColorRgb = mapOf(
        ChatColor.BLACK to Color(0, 0, 0),
        ChatColor.DARK_BLUE to Color(0, 0, 170),
        ChatColor.DARK_GREEN to Color(0, 170, 0),
        ChatColor.DARK_AQUA to Color(0, 170, 170),
        ChatColor.DARK_RED to Color(170, 0, 0),
        ChatColor.DARK_PURPLE to Color(170, 0, 170),
        ChatColor.GOLD to Color(255, 170, 0),
        ChatColor.GRAY to Color(170, 170, 170),
        ChatColor.DARK_GRAY to Color(85, 85, 85),
        ChatColor.BLUE to Color(85, 85, 255),
        ChatColor.GREEN to Color(85, 255, 85),
        ChatColor.AQUA to Color(85, 255, 255),
        ChatColor.RED to Color(255, 85, 85),
        ChatColor.LIGHT_PURPLE to Color(255, 85, 255),
        ChatColor.YELLOW to Color(255, 255, 85),
        ChatColor.WHITE to Color(255, 255, 255)
    )

    fun extractColor(name: String): ChatColor? {
        // Minecraft hex 형식 확인: &x&f&f&0&0&0&0
        minecraftHexPattern.find(name)?.let { match ->
            val hexString = match.value
                .replace("&x", "")
                .replace("&", "")
            return findClosestChatColor(hexString)
        }

        hexPattern.find(name)?.let { match ->
            val hexCode = match.groupValues[1]
            return findClosestChatColor(hexCode)
        }

        legacyPattern.find(name)?.let { match ->
            val colorCode = match.groupValues[1].lowercase()[0]
            val color = colorMap[colorCode]
            return color
        }

        return null
    }

    fun componentToString(component: Component): String {
        return LegacyComponentSerializer.legacyAmpersand().serialize(component)
    }

    private fun findClosestChatColor(hexCode: String): ChatColor {
        val targetColor = try {
            Color.decode("#$hexCode")
        } catch (e: Exception) {
            return ChatColor.WHITE
        }

        val closest = chatColorRgb.minByOrNull { (chatColor, color) ->
            val distance = colorDistance(targetColor, color)
            distance
        }

        val result = closest?.key ?: ChatColor.WHITE

        return result
    }

    private fun colorDistance(c1: Color, c2: Color): Double {
        val rDiff = c1.red - c2.red
        val gDiff = c1.green - c2.green
        val bDiff = c1.blue - c2.blue

        return (rDiff * rDiff + gDiff * gDiff + bDiff * bDiff).toDouble()
    }
}