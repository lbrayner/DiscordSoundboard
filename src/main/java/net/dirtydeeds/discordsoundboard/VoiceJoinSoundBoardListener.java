package net.dirtydeeds.discordsoundboard;

import net.dv8tion.jda.hooks.ListenerAdapter;
import net.dv8tion.jda.events.voice.VoiceJoinEvent;
import net.dirtydeeds.discordsoundboard.service.SoundPlayerImpl;
import net.dv8tion.jda.utils.SimpleLog;
import net.dv8tion.jda.entities.User;

import java.util.List;

public class VoiceJoinSoundBoardListener extends ListenerAdapter {

    private static final SimpleLog LOG = SimpleLog.getLog("VoiceJoinListener");

    private SoundPlayerImpl soundPlayerImpl;

    public VoiceJoinSoundBoardListener(SoundPlayerImpl soundPlayerImpl) {
        this.soundPlayerImpl = soundPlayerImpl;
        LOG.setLevel(this.soundPlayerImpl.getLogLevel());
    }

    @Override
    public void onVoiceJoin(VoiceJoinEvent event)
    {
        final String fileName = this.soundPlayerImpl.getProperty("on_user_join");
        final String botName = this.soundPlayerImpl.getProperty("bot_username");
        final String userName = event.getUser().getUsername();
        final String userNameToFollow = this.soundPlayerImpl.getProperty("username_to_join_channel");
        final String userIdToFollow = this.soundPlayerImpl.getProperty("userid_to_join_channel");
        final boolean userIdIsNull;
        boolean foundUser = false;

        List<User> users = event.getGuild().getUsersByName(userNameToFollow);

        LOG.info("Received a GUILD_VOICE_JOIN event.");

        if(userIdToFollow == null)
        {
            LOG.debug("User ID is null.");
            userIdIsNull = true;
        }
        else
            userIdIsNull = false;

        for(User user : users)
        {
            LOG.debug(String.format("User ID of %s is %s.",userNameToFollow,user.getId()));
            if(!userIdIsNull)
                if(user.getId().equals(userIdToFollow))
                    foundUser = true;
        }

        if(!foundUser && !userIdIsNull)
        {
            LOG.info(String.format("Guild is other than %s's.",userNameToFollow));
            return;
        }

        if(fileName == null)
        {
            LOG.debug("Filename is null.");
            return;
        }

        if(botName != null)
            if(botName.equals(userName))
            {
                LOG.debug("The bot has joined a channel.");
                return;
            }

        if(userName.equals(userNameToFollow))
        {
            LOG.debug(String.format("%s has joined a channel.",userNameToFollow));
            return;
        }

        LOG.debug(String.format("User's name is %s.",userName));

        try
        {
            this.soundPlayerImpl.playFileForUser(fileName, null);
        }
        catch(SoundPlaybackException e)
        {
            e.printStackTrace();
        }
    }
}
