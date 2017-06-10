package net.dirtydeeds.discordsoundboard;

import net.dv8tion.jda.hooks.ListenerAdapter;
// import net.dv8tion.jda.core.events.DisconnectEvent;
import net.dv8tion.jda.events.DisconnectEvent;

import net.dirtydeeds.discordsoundboard.service.SoundPlayerImpl;

import net.dv8tion.jda.utils.SimpleLog;

public class DisconnectSoundBoardListener extends ListenerAdapter {

    private static final SimpleLog LOG = SimpleLog.getLog("DisconnectListener");

    private SoundPlayerImpl soundPlayerImpl;

    public DisconnectSoundBoardListener(SoundPlayerImpl soundPlayerImpl) {
        this.soundPlayerImpl = soundPlayerImpl;
    }

    @Override
    public void onDisconnect(DisconnectEvent event)
    {
        LOG.info("Received a RESUME event.");
        this.soundPlayerImpl.shutDown();
    }
}

