package net.dirtydeeds.discordsoundboard;

import net.dv8tion.jda.hooks.ListenerAdapter;
import net.dv8tion.jda.events.user.UserTypingEvent;

import net.dirtydeeds.discordsoundboard.service.SoundPlayerImpl;

import net.dv8tion.jda.utils.SimpleLog;

// import net.dv8tion.jda.entities.User;

public class UserTypingSoundBoardListener extends ListenerAdapter {

    private static final SimpleLog LOG = SimpleLog.getLog("UserTypingListener");

    private SoundPlayerImpl soundPlayerImpl;

    public UserTypingSoundBoardListener(SoundPlayerImpl soundPlayerImpl) {
        this.soundPlayerImpl = soundPlayerImpl;
        LOG.setLevel(this.soundPlayerImpl.getLogLevel());
    }

    @Override
    public void onUserTyping(UserTypingEvent event)
    {
        final String fileName = this.soundPlayerImpl.getProperty("on_user_typing");

        LOG.info("Received a USER_TYPING event.");

        if(fileName == null)
        {
            LOG.debug("Filename is null.");
            return;
        }

        LOG.debug(String.format("User's name is %s.",event.getUser().getUsername()));

        try
        {
            this.soundPlayerImpl.playFileForUser(fileName, event.getUser().getUsername());
        }
        catch(SoundPlaybackException e)
        {
            e.printStackTrace();
        }
    }
}

