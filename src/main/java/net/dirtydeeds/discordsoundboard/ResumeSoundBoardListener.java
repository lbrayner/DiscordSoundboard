package net.dirtydeeds.discordsoundboard;

import net.dv8tion.jda.hooks.ListenerAdapter;
// import net.dv8tion.jda.core.events.ResumedEvent;
import net.dv8tion.jda.events.ResumedEvent;

import net.dirtydeeds.discordsoundboard.service.SoundPlayerImpl;

import net.dv8tion.jda.utils.SimpleLog;

public class ResumeSoundBoardListener extends ListenerAdapter {

    private static final SimpleLog LOG = SimpleLog.getLog("ResumeListener");

    private SoundPlayerImpl soundPlayerImpl;

    public ResumeSoundBoardListener(SoundPlayerImpl soundPlayerImpl) {
        this.soundPlayerImpl = soundPlayerImpl;
    }

    @Override
    public void onResume(ResumedEvent event)
    {
        LOG.info("Received a RESUME event.");
        this.soundPlayerImpl.shutDown();
    }
}
