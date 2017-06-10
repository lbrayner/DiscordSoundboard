package net.dirtydeeds.discordsoundboard;

import net.dv8tion.jda.hooks.ListenerAdapter;
import net.dv8tion.jda.events.guild.member.GuildMemberJoinEvent;

import net.dirtydeeds.discordsoundboard.service.SoundPlayerImpl;

import net.dv8tion.jda.utils.SimpleLog;

// import net.dv8tion.jda.entities.User;

public class GuildMemberJoinSoundBoardListener extends ListenerAdapter {

    private static final SimpleLog LOG = SimpleLog.getLog("GuildMemberJoinListener");

    private SoundPlayerImpl soundPlayerImpl;

    public GuildMemberJoinSoundBoardListener(SoundPlayerImpl soundPlayerImpl) {
        this.soundPlayerImpl = soundPlayerImpl;
        LOG.setLevel(this.soundPlayerImpl.getLogLevel());
    }

    @Override
    public void onGuildMemberJoin(GuildMemberJoinEvent event)
    {
        final String fileName = this.soundPlayerImpl.getProperty("on_user_join");

        LOG.info("Received a GUILD_MEMBER_JOIN event.");

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

