package com.rexcantor64.triton.packetinterceptor.handlers;

import com.github.retrooper.packetevents.event.PacketSendEvent;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerTeams;

public class ScoreboardPacketHandler {

    public void onTeamsPacket(PacketSendEvent event) {
        WrapperPlayServerTeams teams = new WrapperPlayServerTeams(event);
        // TODO
    }

}
