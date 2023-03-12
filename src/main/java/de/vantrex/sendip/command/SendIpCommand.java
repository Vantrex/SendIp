package de.vantrex.sendip.command;

import de.vantrex.sendip.SendIp;
import de.vantrex.sendip.connection.MassConnectionRequest;
import io.netty.channel.unix.DomainSocketAddress;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.config.ServerInfo;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.plugin.Command;
import net.md_5.bungee.api.plugin.TabExecutor;

import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class SendIpCommand extends Command implements TabExecutor {

    private final SendIp plugin;

    public SendIpCommand(SendIp plugin) {
        super("sendip", "bungeecord.command.send");
        this.plugin = plugin;
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        if (!(sender instanceof ProxiedPlayer)) {
            TextComponent component = new TextComponent("Nur für Spieler!");
            component.setColor(ChatColor.RED);
            sender.sendMessage(component);
            return;
        }
        final ProxiedPlayer player = (ProxiedPlayer) sender;
        if (args.length != 2) {
            BaseComponent[] component = TextComponent.fromLegacyText("§cBenutze §f/sendip <Spieler> <Server>");
            player.sendMessage(ChatMessageType.ACTION_BAR, component);
            return;
        }
        final ProxiedPlayer targetPlayer = ProxyServer.getInstance().getPlayer(args[0]);
        if (targetPlayer == null) {
          player.sendMessage(ChatMessageType.ACTION_BAR, TextComponent.fromLegacyText("Es wurde kein Spieler mit diesem Namen gefunden.", ChatColor.RED));
          return;
        }
        final String targetServerName = args[1].toLowerCase();
        final ServerInfo targetServer;
        if ((targetServer = ProxyServer.getInstance().getServers().entrySet().stream().filter(entry -> entry.getKey().toLowerCase().equals(targetServerName)).map(Map.Entry::getValue).findFirst().orElse(null)) == null) {
           player.sendMessage(ChatMessageType.ACTION_BAR, TextComponent.fromLegacyText("Es wurde kein Server mit diesem Namen gefunden.", ChatColor.RED));
           return;
        }
        final Collection<ProxiedPlayer> playersToSend = getPlayersFromSameHost(targetPlayer);
        new MassConnectionRequest(plugin, playersToSend).executor(player).server(targetServer).handle();
    }

    @Override
    public Iterable<String> onTabComplete(CommandSender sender, String[] args) {
        if (args.length == 1) {
            final String current = args[0].toLowerCase();
            return ProxyServer.getInstance().getPlayers().parallelStream().map(player -> player.getName().toLowerCase()).filter(player -> player.startsWith(current)).collect(Collectors.toList());
        } else if (args.length == 2) {
            final String current = args[1].toLowerCase();
            return ProxyServer.getInstance().getServers().keySet().parallelStream().filter(s -> s.toLowerCase().startsWith(current)).collect(Collectors.toList());
        }
        return new ArrayList<>();
    }

    private Collection<ProxiedPlayer> getPlayersFromSameHost(final ProxiedPlayer player) {
        final String ip = getIpFrom(player);
        if (ip == null) return Collections.singletonList(player);
        final List<ProxiedPlayer> playerList = new ArrayList<>();
        for (ProxiedPlayer otherPlayer : ProxyServer.getInstance().getPlayers()) {
            final String otherIp = getIpFrom(otherPlayer);
            if (otherIp == null) continue;
            if (otherIp.equals(ip))
                playerList.add(otherPlayer);
        }
        return playerList;
    }

    private String getIpFrom(final ProxiedPlayer player) {
        SocketAddress socketAddress = player.getSocketAddress();
        if (socketAddress instanceof InetSocketAddress) {
            return ((InetSocketAddress) socketAddress).getAddress().getHostAddress();
        } else if (socketAddress instanceof DomainSocketAddress) {
            DomainSocketAddress domainSocketAddress = (DomainSocketAddress) socketAddress;
            return domainSocketAddress.path();
        }
        return null;
    }
}