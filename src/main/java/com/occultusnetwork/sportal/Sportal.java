package com.occultusnetwork.sportal;

import com.google.inject.Inject;
import ninja.leaping.configurate.objectmapping.ObjectMappingException;
import org.slf4j.Logger;
import org.spongepowered.api.block.BlockTypes;
import org.spongepowered.api.block.tileentity.TileEntity;
import org.spongepowered.api.block.tileentity.TileEntityTypes;
import org.spongepowered.api.data.manipulator.mutable.tileentity.SignData;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.block.CollideBlockEvent;
import org.spongepowered.api.event.filter.cause.Root;
import org.spongepowered.api.event.game.state.GamePreInitializationEvent;
import org.spongepowered.api.event.game.state.GameStartedServerEvent;
import org.spongepowered.api.event.network.ClientConnectionEvent;
import org.spongepowered.api.plugin.Plugin;
import org.spongepowered.api.plugin.PluginContainer;

import java.io.IOException;
import java.util.*;

@Plugin(
        id = "sportal",
        name = "Sportal",
        description = "A portal plugin",
        url = "https://www.occultus.network/",
        authors = {
                "thomas15v"
        }
)
public class Sportal {

    @Inject
    private Logger logger;
    @Inject
    private PluginContainer container;
    private BungeeLib bungeelib;

    private Map<UUID, Integer> connectingPlayers = new HashMap<>();


    @Listener
    public void preInit(GamePreInitializationEvent event) throws IOException, ObjectMappingException {
        this.bungeelib = new BungeeLib(container);
    }

    @Listener
    public void onPortalEnter(CollideBlockEvent event, @Root Player player) {
        if (event.getTargetBlock().getType().equals(BlockTypes.PORTAL)) {
            UUID uuid = player.getUniqueId();
            if (connectingPlayers.containsKey(uuid)) {
                int delay = connectingPlayers.get(uuid);
                if (delay == 0) {
                    connectingPlayers.remove(uuid);
                } else {
                    connectingPlayers.put(uuid, delay - 1);
                }
            } else {
                Optional<TileEntity> sign = player.getWorld().getTileEntities()
                        .stream()
                        .filter(e -> e.getType() == TileEntityTypes.SIGN &&
                                e.get(SignData.class).get().lines().get(0).toPlain().equalsIgnoreCase("bungee"))
                        .min(Comparator.comparingDouble(e ->
                                e.getLocatableBlock().getPosition().distance(player.getLocation().getPosition().toInt())));
                if (sign.isPresent()) {
                    String server = sign.get().get(SignData.class).get().lines().get(1).toPlain();
                    bungeelib.connectPlayer(player, server);
                    logger.info(String.format("Teleporting player: %s to server: %s", player.getName(), server));
                    connectingPlayers.put(player.getUniqueId(), 200);
                }
            }
        }
    }

    @Listener
    public void onPlayerLeave(ClientConnectionEvent.Disconnect event, @Root Player player){
        if (connectingPlayers.containsKey(player.getUniqueId())){
            connectingPlayers.remove(player.getUniqueId());
        }
    }



}
