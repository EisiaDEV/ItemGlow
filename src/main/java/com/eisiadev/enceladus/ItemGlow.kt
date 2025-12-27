package com.eisiadev.enceladus

import org.bukkit.plugin.java.JavaPlugin

class ItemGlow : JavaPlugin() {

    companion object {
        lateinit var instance: ItemGlow
            private set
    }

    private lateinit var glowManager: GlowManager

    override fun onEnable() {
        instance = this

        if (!server.pluginManager.isPluginEnabled("ProtocolLib")) {
            logger.severe("ProtocolLib not found.")
            server.pluginManager.disablePlugin(this)
            return
        }

        glowManager = GlowManager(this)
        server.pluginManager.registerEvents(ItemGlowListener(this, glowManager), this)

        logger.info("ItemGlow Enabled.")
    }

    override fun onDisable() {
        if (::glowManager.isInitialized) {
            glowManager.cleanup()
        }
        TeamManager.cleanup()
        logger.info("ItemGlow Disabled.")
    }
}