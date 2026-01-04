package com.eisiadev.enceladus

import org.bukkit.plugin.java.JavaPlugin

class ItemGlow : JavaPlugin() {

    companion object {
        lateinit var instance: ItemGlow
            private set
    }

    private lateinit var glowManager: GlowManager
    private lateinit var hologramManager: HologramManager

    override fun onEnable() {
        instance = this

        if (!server.pluginManager.isPluginEnabled("ProtocolLib")) {
            logger.severe("ProtocolLib not found.")
            server.pluginManager.disablePlugin(this)
            return
        }

        glowManager = GlowManager(this)
        hologramManager = HologramManager(this)
        server.pluginManager.registerEvents(ItemGlowListener(this, glowManager, hologramManager), this)

        hologramManager.removeAllExistingHolograms()
        hologramManager.startCleanupTask()

        logger.info("ItemGlow Enabled.")
    }

    override fun onDisable() {
        if (::glowManager.isInitialized) {
            glowManager.cleanup()
        }
        if (::hologramManager.isInitialized) {
            hologramManager.cleanup()
        }
        TeamManager.cleanup()
        logger.info("ItemGlow Disabled.")
    }
}