package net.dirtydeeds.discordsoundboard;

import net.dv8tion.jda.hooks.ListenerAdapter;
import net.dv8tion.jda.events.voice.VoiceLeaveEvent;

import net.dirtydeeds.discordsoundboard.service.SoundPlayerImpl;

import net.dv8tion.jda.utils.SimpleLog;

public class VoiceLeaveSoundBoardListener extends ListenerAdapter {

    private static final SimpleLog LOG = SimpleLog.getLog("VoiceLeaveListener");

    private SoundPlayerImpl soundPlayerImpl;

    public VoiceLeaveSoundBoardListener(SoundPlayerImpl soundPlayerImpl) {
        this.soundPlayerImpl = soundPlayerImpl;
        LOG.setLevel(this.soundPlayerImpl.getLogLevel());
    }

    @Override
    public void onVoiceLeave(VoiceLeaveEvent event)
    {
        final String fileName = this.soundPlayerImpl.getProperty("on_user_leave");
        final String botName = this.soundPlayerImpl.getProperty("bot_username");
        final String userName = event.getUser().getUsername();
        final String userNameToFollow = this.soundPlayerImpl.getProperty("username_to_join_channel");

        LOG.info("Received a GUILD_VOICE_LEAVE event.");

        if(fileName == null)
        {
            LOG.debug("Filename is null.");
            return;
        }

        if(botName.equals(userName))
        {
            LOG.debug("The bot has left a channel.");
            return;
        }

        if(userName.equals(userNameToFollow))
        {
            LOG.debug(String.format("%s has left a channel.",userNameToFollow));
            return;
        }

        LOG.debug(String.format("User's name is %s.",userName));

        try
        {
            // this.soundPlayerImpl.playFileForUser(fileName, event.getUser().getUsername());
            this.soundPlayerImpl.playFileForUser(fileName, null);
        }
        catch(SoundPlaybackException e)
        {
            e.printStackTrace();
        }
    }
}

