package net.dirtydeeds.discordsoundboard;

import net.dv8tion.jda.hooks.ListenerAdapter;
// import net.dv8tion.jda.core.events.ShutdownEvent;
import net.dv8tion.jda.events.ShutdownEvent;

import net.dirtydeeds.discordsoundboard.service.SoundPlayerImpl;

import net.dv8tion.jda.utils.SimpleLog;

public class ShutdownSoundBoardListener extends ListenerAdapter {

    private static final SimpleLog LOG = SimpleLog.getLog("ShutdownListener");

    private SoundPlayerImpl soundPlayerImpl;

    public ShutdownSoundBoardListener(SoundPlayerImpl soundPlayerImpl) {
        this.soundPlayerImpl = soundPlayerImpl;
    }

    @Override
    public void onShutdown(ShutdownEvent event)
    {
        LOG.info("Received a SHUTDOWN event.");
        this.soundPlayerImpl.initialize();
    }
}

