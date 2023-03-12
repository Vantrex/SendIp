package de.vantrex.sendip.connection;

import de.vantrex.sendip.SendIp;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.ServerConnectRequest;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.config.ServerInfo;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.event.ServerConnectEvent;
import net.md_5.bungee.api.scheduler.ScheduledTask;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

public class MassConnectionRequest implements ConnectionRequest {


    private final SendIp plugin;
    private final Collection<ProxiedPlayer> playersToConnect;

    private ServerInfo toConnect;
    private ProxiedPlayer executor;

    private transient int succeeded = 0;
    private transient int alreadyConnected = 0;
    private transient int failed = 0;

    public MassConnectionRequest(SendIp plugin, Collection<ProxiedPlayer> playersToConnect) {
        this.plugin = plugin;
        this.playersToConnect = playersToConnect;
    }

    @Override
    public ConnectionRequest executor(ProxiedPlayer executor) {
        this.executor = executor;
        return this;
    }

    @Override
    public ConnectionRequest server(ServerInfo serverInfo) {
        this.toConnect = serverInfo;
        return this;
    }

    private synchronized void incrementSucceeded() {
        this.succeeded++;
    }

    private synchronized void incrementAlreadyConnected() {
        this.alreadyConnected++;
    }

    private synchronized void incrementFailed() {
        this.failed++;
    }

    @Override
    public void handle() {
        executor.sendMessage(ChatMessageType.ACTION_BAR,
                TextComponent.fromLegacyText("Probiere " + this.playersToConnect.size() + " Spieler auf "
                        + toConnect.getName() + " zu senden..", ChatColor.RED));
        final List<CompletableFuture<Void>> futureList = new ArrayList<>();
        this.playersToConnect.forEach(player -> {
            player.sendMessage(ChatMessageType.ACTION_BAR,
                    TextComponent.fromLegacyText("Du wurdest von "
                            + executor.getName() + " auf " + this.toConnect.getName() + " gesendet.", ChatColor.WHITE));
            futureList.add(CompletableFuture.supplyAsync(() -> player).thenAccept(player1 -> {
                player1.connect(ServerConnectRequest.builder()
                        .target(this.toConnect)
                        .reason(ServerConnectEvent.Reason.COMMAND)
                        .callback((result, error) -> {
                            switch (result) {
                                case FAIL:
                                case EVENT_CANCEL:
                                    incrementFailed();
                                    break;
                                case ALREADY_CONNECTED:
                                    incrementAlreadyConnected();
                                    break;
                                case SUCCESS:
                                    incrementSucceeded();
                                    break;
                            }
                        })
                        .retry(false)
                        .build());
            }));
        });
        final CompletableFuture<?>[] futures = futureList.toArray(new CompletableFuture<?>[0]);
        CompletableFuture.allOf(futures).whenComplete(((unused, throwable) -> printResult()));
    }

    private void printResult() {
        final ScheduledTask task = ProxyServer.getInstance().getScheduler().schedule(plugin, () -> {
            executor.sendMessage(ChatMessageType.ACTION_BAR, TextComponent.fromLegacyText(
                    "Send Results: §f"
                            + this.succeeded + " §2Erfolgreich | §f"
                            + this.failed + " §2Fehlgeschlagen | §f"
                            + this.alreadyConnected + " §2Schon verbunden", ChatColor.GREEN
            ));
        }, 5L, 1, TimeUnit.SECONDS);
        ProxyServer.getInstance().getScheduler().schedule(plugin, task::cancel, 5, TimeUnit.SECONDS);
    }

}
