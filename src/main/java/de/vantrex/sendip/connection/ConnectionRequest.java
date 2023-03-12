package de.vantrex.sendip.connection;

import net.md_5.bungee.api.config.ServerInfo;
import net.md_5.bungee.api.connection.ProxiedPlayer;


public interface ConnectionRequest {

    ConnectionRequest executor(ProxiedPlayer executor);

    ConnectionRequest server(ServerInfo serverInfo);

    void handle();

}
