package de.vantrex.sendip;

import de.vantrex.sendip.command.SendIpCommand;
import net.md_5.bungee.api.plugin.Plugin;

public final class SendIp extends Plugin {

    @Override
    public void onEnable() {
        super.getProxy().getPluginManager().registerCommand(this, new SendIpCommand(this));
    }

    @Override
    public void onDisable() {
    }
}
