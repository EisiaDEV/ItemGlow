package com.eisiadev.enceladus

import org.bukkit.ChatColor
import org.bukkit.entity.Entity
import org.bukkit.scoreboard.Scoreboard
import org.bukkit.scoreboard.Team
import java.util.concurrent.ConcurrentHashMap

object TeamManager {

    private val entityTeams = ConcurrentHashMap<String, String>()
    private val colorTeams = ConcurrentHashMap<ChatColor, Team>()

    private val scoreboard: Scoreboard
        get() = ItemGlow.instance.server.scoreboardManager.mainScoreboard

    fun assignTeam(entity: Entity, color: ChatColor) {
        val team = getOrCreateTeam(color)
        val entry = entity.uniqueId.toString()

        entityTeams[entry]?.let { oldTeamName ->
            scoreboard.getTeam(oldTeamName)?.removeEntry(entry)
        }

        team.addEntry(entry)
        entityTeams[entry] = team.name
    }

    fun removeTeamForEntity(entity: Entity) {
        val entry = entity.uniqueId.toString()
        entityTeams.remove(entry)?.let { teamName ->
            scoreboard.getTeam(teamName)?.removeEntry(entry)
        }
    }

    fun cleanup() {
        colorTeams.values.forEach { team ->
            team.unregister()
        }
        colorTeams.clear()
        entityTeams.clear()
    }

    private fun getOrCreateTeam(color: ChatColor): Team {
        return colorTeams.getOrPut(color) {
            val teamName = "glow_${color.name.lowercase()}"

            scoreboard.getTeam(teamName)?.unregister()

            val newTeam = scoreboard.registerNewTeam(teamName)
            newTeam.color = color

            try {
                newTeam.setOption(Team.Option.COLLISION_RULE, Team.OptionStatus.NEVER)
            } catch (e: Exception) {
            }

            newTeam
        }
    }
}