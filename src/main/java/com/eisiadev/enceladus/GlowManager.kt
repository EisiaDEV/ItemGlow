package com.eisiadev.enceladus

import com.comphenix.protocol.PacketType
import com.comphenix.protocol.ProtocolLibrary
import com.comphenix.protocol.events.PacketContainer
import com.comphenix.protocol.wrappers.WrappedDataValue
import com.comphenix.protocol.wrappers.WrappedDataWatcher
import org.bukkit.Bukkit
import org.bukkit.ChatColor
import org.bukkit.entity.Item
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerJoinEvent
import java.util.concurrent.ConcurrentHashMap

class GlowManager(private val plugin: ItemGlow) : Listener {

    private val glowingEntities = ConcurrentHashMap<Int, ChatColor>()
    private val protocolManager = ProtocolLibrary.getProtocolManager()

    init {
        Bukkit.getPluginManager().registerEvents(this, plugin)
    }

    fun setGlowing(item: Item, color: ChatColor) {
        glowingEntities[item.entityId] = color

        Bukkit.getOnlinePlayers().forEach { player ->
            addToPlayerTeam(player, color, item)
            sendGlowPacket(player, item, true)
        }
    }

    fun removeGlowing(item: Item) {
        val color = glowingEntities.remove(item.entityId)

        Bukkit.getOnlinePlayers().forEach { player ->
            if (color != null) {
                removeFromPlayerTeam(player, color, item)
            }
            sendGlowPacket(player, item, false)
        }
    }

    @EventHandler
    fun onPlayerJoin(event: PlayerJoinEvent) {
        Bukkit.getScheduler().runTaskLater(plugin, Runnable {
            glowingEntities.forEach { (entityId, color) ->
                Bukkit.getWorld(event.player.world.name)?.entities
                    ?.filterIsInstance<Item>()
                    ?.find { it.entityId == entityId }
                    ?.let { item ->
                        addToPlayerTeam(event.player, color, item)
                        sendGlowPacket(event.player, item, true)
                    }
            }
        }, 20L)
    }

    private fun addToPlayerTeam(player: Player, color: ChatColor, item: Item) {
        try {
            val teamName = "glow_${color.name.lowercase()}"
            val entry = item.uniqueId.toString()

            var scoreboard = player.scoreboard
            if (scoreboard == Bukkit.getScoreboardManager()?.mainScoreboard) {
                scoreboard = Bukkit.getScoreboardManager()?.newScoreboard ?: return
                player.scoreboard = scoreboard
            }

            var team = scoreboard.getTeam(teamName)
            if (team == null) {
                team = scoreboard.registerNewTeam(teamName)
                team.color = color
            }

            if (!team.hasEntry(entry)) {
                team.addEntry(entry)
            }

        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun removeFromPlayerTeam(player: Player, color: ChatColor, item: Item) {
        try {
            val teamName = "glow_${color.name.lowercase()}"
            val entry = item.uniqueId.toString()

            val scoreboard = player.scoreboard
            val team = scoreboard.getTeam(teamName)

            team?.removeEntry(entry)
        } catch (e: Exception) {
        }
    }

    private fun sendGlowPacket(player: Player, item: Item, glowing: Boolean) {
        try {
            val packet = PacketContainer(PacketType.Play.Server.ENTITY_METADATA)
            packet.integers.write(0, item.entityId)

            val dataWatcher = WrappedDataWatcher.getEntityWatcher(item).deepClone()

            val currentFlags: Byte = 0
            val glowingFlag: Byte = if (glowing) {
                (currentFlags.toInt() or 0x40).toByte()
            } else {
                (currentFlags.toInt() and 0x40.inv()).toByte()
            }

            dataWatcher.setObject(0, glowingFlag)

            val wrappedDataValueList = mutableListOf<WrappedDataValue>()

            for (entry in dataWatcher.watchableObjects) {
                if (entry == null) continue

                val watcherObject = entry.watcherObject
                wrappedDataValueList.add(
                    WrappedDataValue(
                        watcherObject.index,
                        watcherObject.serializer,
                        entry.rawValue
                    )
                )
            }

            packet.dataValueCollectionModifier.write(0, wrappedDataValueList)

            protocolManager.sendServerPacket(player, packet)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun cleanup() {
        glowingEntities.clear()
    }
}