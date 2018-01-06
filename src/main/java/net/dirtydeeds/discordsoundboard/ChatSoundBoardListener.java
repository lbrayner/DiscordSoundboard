package net.dirtydeeds.discordsoundboard;

import java.util.stream.Collectors;
import java.util.Arrays;
import java.util.Collections;
import org.apache.commons.lang3.tuple.Pair;

import com.sun.management.OperatingSystemMXBean;
import net.dirtydeeds.discordsoundboard.beans.SoundFile;
import net.dirtydeeds.discordsoundboard.service.SoundPlayerImpl;
import net.dv8tion.jda.exceptions.PermissionException;
import net.dv8tion.jda.Permission;
import net.dv8tion.jda.entities.Message;
import net.dv8tion.jda.entities.TextChannel;
import net.dv8tion.jda.events.message.MessageReceivedEvent;
import net.dv8tion.jda.events.audio.AudioConnectEvent;
import net.dv8tion.jda.hooks.ListenerAdapter;
import net.dv8tion.jda.utils.PermissionUtil;
import net.dv8tion.jda.utils.SimpleLog;

import java.io.File;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryPoolMXBean;
import java.lang.management.MemoryType;
import java.lang.management.MemoryUsage;
import java.lang.management.RuntimeMXBean;
import java.text.DecimalFormat;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * @author dfurrer.
 *
 * This class handles listening to commands in discord text channels and responding to them.
 */
public class ChatSoundBoardListener extends ListenerAdapter {

    private static final SimpleLog LOG = SimpleLog.getLog("ChatListener");

    private SoundPlayerImpl soundPlayer;
    private String commandCharacter = "?";
    private Integer messageSizeLimit = 2000;
    private boolean respondToDms = true;
    private boolean muted;
    private static DecimalFormat df2 = new DecimalFormat("#.##");
    private static final int MAX_FILE_SIZE_IN_BYTES = 1000000; // 1 MB

    public ChatSoundBoardListener(SoundPlayerImpl soundPlayer, String commandCharacter, String messageSizeLimit,
                                  Boolean respondToDms) {
        this.soundPlayer = soundPlayer;
        if (commandCharacter != null && !commandCharacter.isEmpty()) {
            this.commandCharacter = commandCharacter;
        }
        if (messageSizeLimit != null && !messageSizeLimit.isEmpty() && messageSizeLimit.matches("^-?\\d+$")) {
            this.messageSizeLimit = Integer.parseInt(messageSizeLimit);
            if (this.messageSizeLimit > 1994) {
                this.messageSizeLimit = 1994;
            }
        }
        muted=false;
        this.respondToDms = respondToDms;
        LOG.setLevel(this.soundPlayer.getLogLevel());
    }

    @Override
    public void onAudioConnect(AudioConnectEvent event)
    {
        for(TextChannel textChannel : event.getConnectedChannel().getGuild().getTextChannels())
        {
            textChannel.sendMessage(String.format
               ("Hello! I'm online. %1$slist to list all resources available."
                +"\n%1$scat(egorie)s for a list of categories."
                +"\n%1$shelp for help."
                +"\n%1$srandom execute a random resource."
                ,commandCharacter));
        }
    }

    private Integer pageNumber(final String command,final int commandLength)
    {
        final String[] messageSplit = command.split("\\s+");
        LOG.debug(String.format("%s %d",Arrays.toString(messageSplit),commandLength));

        if(messageSplit.length < commandLength + 1)
            return null;

        final String numberToParse = messageSplit[commandLength];

        if(numberToParse.trim().equals(""))
            return null;

        return Integer.parseInt(numberToParse);
    }

    private void paginatedResponse
        (final Integer page
        ,final StringBuilder commandString
        ,final String message
        ,final MessageReceivedEvent event)
    {
        final String fullCommandLine = event.getMessage().getContent().toLowerCase();
        final int maxLineLength = messageSizeLimit;
        final List<String> queryResults
            = getCommandList
            // (message.length()+1 // to account for the implcit line break
            (message.length()
            ,commandString);

        LOG.debug(String.format("pageNumber is %s",page));

        if (page == null) {
            if (commandString.length() > maxLineLength) {
                replyByPrivateMessage(event, "The response to this query is composed of " +
                        queryResults.size() + " pages. Reply: ```"
                        + fullCommandLine + " PAGE_NUMBER``` to request a specific page of results.");
            } else {
                replyByPrivateMessage(event,message);
                replyByPrivateMessage(event, queryResults.get(0));
            }
        } else {
            try {
                final String resultsPage = queryResults.get(page - 1);
                LOG.debug(String.format("Message is %s."
                            ,message
                            ));
                LOG.debug(String.format("Message length is %d."
                            ,message.length()
                            ));
                LOG.debug(String.format("Results page's lenth is %d."
                            ,resultsPage.length()
                            ));
                LOG.debug(String.format("Total message length is %d."
                            ,message.length()+resultsPage.length()
                            ));
                replyByPrivateMessage(event, resultsPage);
                replyByPrivateMessage(event, message);
            } catch (IndexOutOfBoundsException e) {
                replyByPrivateMessage(event, "The page number you entered is not valid.");
            } catch (NumberFormatException e) {
                replyByPrivateMessage(event, "The page number argument must be a number.");
            }
        }
    }

    private int repeatNumber(final String command)
    {
        return nameRequestedRepeatNumberPair(command).getRight();
    }

    private Pair<String,Integer> nameRequestedRepeatNumberPair(final String command)
    {
        int repeatNumber = 1;
        String fileNameRequested = command.substring(1, command.length());
        // If there is the repeat character (~) then cut up the command string.
        int repeatIndex = command.indexOf('~');
        if (repeatIndex > -1) {
            fileNameRequested = command.substring(1, repeatIndex).trim(); // -1 to ignore the previous space
            if (repeatIndex + 1 == command.length()) { // If there is only a ~ then repeat-infinite
                repeatNumber = -1;
            } else { // If there is something after the ~ then repeat for that value
                // +1 to ignore the ~ character
                repeatNumber = Integer.parseInt(command.substring(repeatIndex + 1, command.length()));
            }
        }

        return Pair.of(fileNameRequested,repeatNumber);
    }

    @Override
    public void onMessageReceived(MessageReceivedEvent event) {
        String requestingUser = event.getAuthor().getUsername();
        if (!event.getAuthor().isBot() && ((respondToDms && event.isPrivate()) || !event.isPrivate())) {
	        String message = event.getMessage().getContent().toLowerCase();
            final String userCommand = message.split("\\s+")[0];
            if (message.startsWith(commandCharacter)) {
	            if (soundPlayer.isUserAllowed(requestingUser) && !soundPlayer.isUserBanned(requestingUser)) {
	                // final int maxLineLength = messageSizeLimit;
	
	                //Respond
	                if (message.startsWith(commandCharacter + "list")) {
	                    StringBuilder commandString = getCommandListString();
                        paginatedResponse(pageNumber(message,1),commandString
                                ,"Type any of these preceded by"
                                + commandCharacter + " into the chat."
                                ,event);
	                } else if (message.startsWith(commandCharacter + "help")) {
	                    LOG.info("Responding to help command. Requested by " + requestingUser + ".");
	                    replyByPrivateMessage(event, "You can type any of the following commands:" +
	                            "\n```"
                                     + commandCharacter + "list                - Returns a list of "
                                    +"available resources." +
	                            "\n" + commandCharacter + "RESOURCE            - Executes the resource." +
	                            "\n" + commandCharacter + "cat(egorie)s        - Lists categories." +
	                            "\n" + commandCharacter + "cat(egory) CATEGORY - Lists CATEGORY's resources." +
	                            "\n" + commandCharacter + "random              - Executes a random resource." +
	                            "\n" + commandCharacter + "volume 0-100        - Sets the playback volume." +
	                            "\n" + commandCharacter + "stop                - Stops the sound that is"
                                    +"currently playing." +
	                            "\n" + commandCharacter + "info                - Returns info about the bot.```");
	                } else if (message.startsWith(commandCharacter + "volume")) {
		                int fadeoutIndex = message.indexOf('~');
	                    int newVol = Integer.parseInt(message.substring(8, (fadeoutIndex > -1) ? fadeoutIndex - 1 : message.length()));
	                    int fadeoutTimeout =  0;
	                    if (fadeoutIndex > -1) {
	                    	fadeoutTimeout = Integer.parseInt(message.substring(fadeoutIndex + 1, message.length()));
	                    }
	                    if (newVol >= 1 && newVol <= 100) {
	                        muted = false;
	                        soundPlayer.setSoundPlayerVolume(newVol, fadeoutTimeout * 1000);
	                        replyByPrivateMessage(event, "*Volume set to " + newVol + "%*");
	                        LOG.info("Volume set to " + newVol + "% by " + requestingUser + ".");
	                    } else if (newVol == 0) {
	                        muted = true;
	                        soundPlayer.setSoundPlayerVolume(newVol, fadeoutTimeout * 1000);
	                        replyByPrivateMessage(event, requestingUser + " muted me.");
	                        LOG.info("Bot muted by " + requestingUser + ".");
	                    }
	                } else if (message.startsWith(commandCharacter + "stop")) {
		                int fadeoutIndex = message.indexOf('~');
	                    int fadeoutTimeout =  0;
	                    if (fadeoutIndex > -1) {
	                    	fadeoutTimeout = Integer.parseInt(message.substring(fadeoutIndex + 1, message.length()));
	                    }
	                    LOG.info("Stop requested by " + requestingUser + " with a fadeout of " + fadeoutTimeout + " seconds");
	                    if (soundPlayer.stop(fadeoutTimeout * 1000)) {
	                        replyByPrivateMessage(event, "Playback stopped.");
	                    } else {
	                        replyByPrivateMessage(event, "Nothing was playing.");
	                    }
	
	                } else if (message.startsWith(commandCharacter + "info")) {
	                    LOG.info("Responding to info request by " + requestingUser + ".");
	
	                    OperatingSystemMXBean operatingSystemMXBean = (OperatingSystemMXBean) ManagementFactory.getOperatingSystemMXBean();
	                    RuntimeMXBean runtimeMXBean = ManagementFactory.getRuntimeMXBean();
	                    int availableProcessors = operatingSystemMXBean.getAvailableProcessors();
	                    long prevUpTime = runtimeMXBean.getUptime();
	                    long prevProcessCpuTime = operatingSystemMXBean.getProcessCpuTime();
	                    double cpuUsage;
	                    try {
	                        Thread.sleep(500);
	                    } catch (Exception ignored) {
	                    }
	
	                    long upTime = runtimeMXBean.getUptime();
	                    long processCpuTime = operatingSystemMXBean.getProcessCpuTime();
	                    long elapsedCpu = processCpuTime - prevProcessCpuTime;
	                    long elapsedTime = upTime - prevUpTime;
	
	                    cpuUsage = Math.min(99F, elapsedCpu / (elapsedTime * 10000F * availableProcessors));
	
	                    List<MemoryPoolMXBean> memoryPools = new ArrayList<>(ManagementFactory.getMemoryPoolMXBeans());
	                    long usedHeapMemoryAfterLastGC = 0;
	                    for (MemoryPoolMXBean memoryPool : memoryPools) {
	                        if (memoryPool.getType().equals(MemoryType.HEAP)) {
	                            MemoryUsage poolCollectionMemoryUsage = memoryPool.getCollectionUsage();
	                            usedHeapMemoryAfterLastGC += poolCollectionMemoryUsage.getUsed();
	                        }
	                    }
	
	                    Package thisPackage = getClass().getPackage();
	                    String version = null;
	                    if (thisPackage != null) {
	                        version = getClass().getPackage().getImplementationVersion();
	                    }
	                    if (version == null) {
	                        version = "DEVELOPMENT";
	                    }
	
	                    long uptimeDays = TimeUnit.DAYS.convert(upTime, TimeUnit.MILLISECONDS);
	                    long uptimeHours = TimeUnit.HOURS.convert(upTime, TimeUnit.MILLISECONDS) - TimeUnit.DAYS.toHours(TimeUnit.MILLISECONDS.toDays(upTime));
	                    long uptimeMinutes = TimeUnit.MINUTES.convert(upTime, TimeUnit.MILLISECONDS) - TimeUnit.HOURS.toMinutes(TimeUnit.MILLISECONDS.toHours(upTime));
	                    long upTimeSeconds = TimeUnit.MILLISECONDS.toSeconds(upTime) - TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(upTime));
	
	                    replyByPrivateMessage(event, "DiscordSoundboard info: ```" +
	                            "CPU: " + df2.format(cpuUsage) + "%" +
	                            "\nMemory: " + humanReadableByteCount(usedHeapMemoryAfterLastGC, true) +
	                            "\nUptime: Days: " + uptimeDays + " Hours: " + uptimeHours + " Minutes: " + uptimeMinutes + " Seconds: " + upTimeSeconds +
	                            "\nVersion: " + version +
	                            "\nSoundFiles: " + soundPlayer.getAvailableSoundFiles().size() +
	                            "\nCommand Prefix: " + commandCharacter +
	                            "```");
	                } else if (message.startsWith(commandCharacter + "remove")) {
	                    String[] messageSplit = message.split(" ");
	                    String soundToRemove = messageSplit[1];
	                    boolean hasManageServerPerm = PermissionUtil.checkPermission(event.getGuild(), event.getAuthor(), Permission.MANAGE_SERVER);
	                    if (event.getAuthor().getUsername().equalsIgnoreCase(soundToRemove)
	                            || hasManageServerPerm) {
	                        SoundFile soundFileToRemove = soundPlayer.getAvailableSoundFiles().get(soundToRemove);
	                        if (soundFileToRemove != null) {
	                            try {
	                                boolean fileRemoved = Files.deleteIfExists(Paths.get(soundFileToRemove.getSoundFileLocation()));
	                                if (fileRemoved) {
	                                    replyByPrivateMessage(event, "Sound file " + soundToRemove + " was removed.");
	                                } else {
	                                    replyByPrivateMessage(event, "Could not find sound file: " + soundToRemove + ".");
	                                }
	                            } catch (IOException e) {
	                                LOG.fatal("Could not remove sound file " + soundToRemove);
	                            }
	                        }
	                    } else {
	                        replyByPrivateMessage(event, "You do not have permission to remove sound file: " + soundToRemove + ".");
	                    }
	
	                }
                    else if (message.startsWith(commandCharacter + "categories")
                                || userCommand.equals(commandCharacter+"cats")
                            )
                    {
	                    StringBuilder commandString = getCategoriesStringBuilder();
                        paginatedResponse(pageNumber(message,1),commandString
                                ,"These are the categories. "
                                + commandCharacter + "cat(egory) CATEGORY to list its resources."
                                ,event);
	                }
                    else if (message.startsWith(commandCharacter + "category")
                                || userCommand.equals(commandCharacter+"cat")
                            )
                    {
                        final String[] messageSplit = message.split("\\s+");
                        if(messageSplit.length >= 2)
                        {
                            final String query = message.split("\\s+")[1];
                            Pair<String,StringBuilder> catBuilderPair
                                = getCategoryStringBuilder(query);
                            final String category = catBuilderPair.getLeft();
                            final StringBuilder commandString = catBuilderPair.getRight();

                            if(commandString.length() > 0)
                                paginatedResponse(pageNumber(message,2),commandString
                                        ,"These are the resources of category " + category + "."
                                        ,event);
                            else
                                replyByPrivateMessage(event,"No resources available.");
                        }
                        else
                        {
	                        replyByPrivateMessage(event, "Missing argument to command"
                                    + commandCharacter + "category.");
                        }
	                }
                    else if (message.startsWith(commandCharacter + "random")) {
	                    try {
                            final int repeatNumber = repeatNumber(message);
                            LOG.debug("repeatNumber is "+repeatNumber);
	                        soundPlayer.playRandomSoundFile(requestingUser, event,repeatNumber);
	                        deleteMessage(event);
	                    } catch (SoundPlaybackException e) {
	                        replyByPrivateMessage(event, "Problem playing random file:" + e);
	                    }
	                }
                    else if (message.startsWith(commandCharacter) && message.length() >= 2) {
	                    if (!muted) {
	                        try {
                                final Pair<String,Integer> nameRequestedRepeatNumberPair
                                    = nameRequestedRepeatNumberPair(message);

	                            final String fileNameRequested = nameRequestedRepeatNumberPair.getLeft();
	                            final int repeatNumber = nameRequestedRepeatNumberPair.getRight();

		                        // // If there is the repeat character (~) then cut up the message string.
	                            // int repeatIndex = message.indexOf('~');
	                            // if (repeatIndex > -1) {
									// fileNameRequested = message.substring(1, repeatIndex).trim(); // -1 to ignore the previous space
	                            	// if (repeatIndex + 1 == message.length()) { // If there is only a ~ then repeat-infinite
		                        //     	repeatNumber = -1;
	                            	// } else { // If there is something after the ~ then repeat for that value
	                                	// repeatNumber = Integer.parseInt(message.substring(repeatIndex + 1, message.length())); // +1 to ignore the ~ character
	                            	// }
	                            // }

	                            LOG.info("Attempting to play file: " + fileNameRequested + " " + repeatNumber + " times. Requested by " + requestingUser + ".");
	
	                            soundPlayer.playFileForEvent(fileNameRequested, event, repeatNumber);
	                            deleteMessage(event);
	                        } catch (Exception e) {
	                            e.printStackTrace();
	                        }
	                    } else {
	                        replyByPrivateMessage(event, "I seem to be muted! Try " + commandCharacter + "help");
	                        LOG.info("Attempting to play a sound file while muted. Requested by " + requestingUser + ".");
	                    }
	                } else {
	                    List<Message.Attachment> attachments = event.getMessage().getAttachments();
	                    if (attachments.size() > 0 && event.isPrivate()) {
	                        for (Message.Attachment attachment : attachments) {
	                            String name = attachment.getFileName();
	                            String extension = name.substring(name.indexOf(".") + 1);
	                            if (extension.equals("wav") || extension.equals("mp3")) {
	                                if (attachment.getSize() < MAX_FILE_SIZE_IN_BYTES) {
	                                    if (!Files.exists(Paths.get(soundPlayer.getSoundsPath() + "/" + name))) {
	                                        File newSoundFile = new File(soundPlayer.getSoundsPath(), name);
	                                        attachment.download(newSoundFile);
	                                        event.getChannel().sendMessage("Downloaded file `" + name + "` and added to list of sounds " + event.getAuthor().getAsMention() + ".");
	                                    } else {
	                                        boolean hasManageServerPerm = PermissionUtil.checkPermission(event.getGuild(), event.getAuthor(), Permission.MANAGE_SERVER);
	                                        if (event.getAuthor().getUsername().equalsIgnoreCase(name.substring(0, name.indexOf(".")))
	                                                || hasManageServerPerm) {
	                                            try {
	                                                Files.deleteIfExists(Paths.get(soundPlayer.getSoundsPath() + "/" + name));
	                                                File newSoundFile = new File(soundPlayer.getSoundsPath(), name);
	                                                attachment.download(newSoundFile);
	                                                event.getChannel().sendMessage("Downloaded file `" + name + "` and updated list of sounds " + event.getAuthor().getAsMention() + ".");
	                                            } catch (IOException e1) {
	                                                LOG.fatal("Problem deleting and re-adding sound file: " + name);
	                                            }
	                                        } else {
	                                            event.getChannel().sendMessage("The file '" + name + "' already exists. Only " + name.substring(0, name.indexOf(".")) + " can change update this sound.");
	                                        }
	                                    }
	                                } else {
	                                    replyByPrivateMessage(event, "File `" + name + "` is too large to add to library.");
	                                }
	                            }
	                        }
	                    } else {
	                        if (message.startsWith(commandCharacter) || event.isPrivate()) {
	                            nonRecognizedCommand(event, requestingUser);
	                        }
	                    }
	                }
	            } else {
	                if (!soundPlayer.isUserAllowed(requestingUser)) {
	                    replyByPrivateMessage(event, "I don't take orders from you.");
	                }
	                if (soundPlayer.isUserBanned(requestingUser)) {
	                    replyByPrivateMessage(event, "You've been banned from using this soundboard bot.");
	                }
	            }
	        }
        }
    }
    
    private void nonRecognizedCommand(MessageReceivedEvent event, String requestingUser) {
        replyByPrivateMessage(event, "Hello @" + requestingUser + ". I don't know how to respond to this message!");
        replyByPrivateMessage(event, "You can type " + commandCharacter + "help to see a list of recognized commands.");
        LOG.info("Responding to PM of " + requestingUser + ". Unknown Command. Sending help text.");
    }

    private List<String> getCommandList
        (final int deduction
        ,StringBuilder commandString)
    {
        final int maxLineLength = messageSizeLimit;
        List<String> soundFiles = new ArrayList<>();

        //if text has \n, \r or \t symbols it's better to split by \s+
        final String SPLIT_REGEXP= "(?<=[ \\n])";
        final String PREFIX = "```\n";
        final String SUFFIX = "```\n";
        final int PADDING_LENGTH = PREFIX.length() + SUFFIX.length();

        String[] tokens = commandString.toString().split(SPLIT_REGEXP);
        int lineLen = 0;
        StringBuilder output = new StringBuilder();
        output.append(PREFIX);
        for (int i = 0; i < tokens.length; i++) {
            String word = tokens[i];

            if (lineLen + (word).length() + PADDING_LENGTH + deduction > maxLineLength) {
                if (i > 0) {
                    output.append(SUFFIX);
                    soundFiles.add(output.toString());

                    output = new StringBuilder(maxLineLength);
                    output.append(PREFIX);
                }
                lineLen = 0;
            }
            output.append(word);
            lineLen += word.length();
        }
        if (output.length() > 0) {
            output.append(SUFFIX);
        }
        soundFiles.add(output.toString());
        return soundFiles;
    }

    private StringBuilder getCommandListString() {
        StringBuilder sb = new StringBuilder();

        Set<Map.Entry<String, SoundFile>> entrySet = soundPlayer.getAvailableSoundFiles().entrySet();

        if (entrySet.size() > 0) {
            for (Map.Entry entry : entrySet) {
                sb.append(entry.getKey()).append("\n");
            }
        }
        return sb;
    }

    private Set<String> getCategories()
    {
        Set<String> categories = new HashSet<>();
        Map<String, SoundFile> soundMap = soundPlayer.getAvailableSoundFiles();
        categories.addAll(soundMap.entrySet().stream().map(entry ->
                    entry.getValue().getCategory()).collect(Collectors.toList()));

        return categories;
    }

    private StringBuilder getCategoriesStringBuilder() {

        StringBuilder sb = new StringBuilder();
        Set<String> categories = getCategories();

        for (String category : categories) {
            sb.append(category).append("\n");
        }
        return sb;
    }

    private Pair<String,StringBuilder> getCategoryStringBuilder(final String query) {

        StringBuilder sb = new StringBuilder();
        Map<String, SoundFile> soundMap = soundPlayer.getAvailableSoundFiles();
        List<SoundFile> files = soundMap.entrySet().stream().map(entry ->
                entry.getValue()).collect(Collectors.toList());
        List<SoundFile> filesOfCategory = Collections.emptyList();
        String category = query;

        Map<String,List<SoundFile>> category2SoundFile
            = files.stream()
                .collect(Collectors.groupingBy(s -> s.getCategory()));

        if(category2SoundFile.containsKey(query))
        {
            filesOfCategory = category2SoundFile.get(query);
        }
        else
        {
            for(String key : category2SoundFile.keySet())
            {
                if(key.startsWith(query.toLowerCase()))
                {
                    filesOfCategory = category2SoundFile.get(key);
                    category = key;
                    break;
                }
            }
        }

        for (SoundFile file : filesOfCategory)
        {
            sb.append(file.getSoundFileId()).append("\n");
        }

        return Pair.of(category,sb);
    }

    private void replyByPrivateMessage(MessageReceivedEvent event, String message) {
        event.getAuthor().getPrivateChannel().sendMessage(message);
        deleteMessage(event);
    }

    private static String humanReadableByteCount(long bytes, boolean si) {
        int unit = si ? 1000 : 1024;
        if (bytes < unit) return bytes + " B";
        int exp = (int) (Math.log(bytes) / Math.log(unit));
        String pre = (si ? "kMGTPE" : "KMGTPE").charAt(exp-1) + (si ? "" : "i");
        return String.format("%.1f %sB", bytes / Math.pow(unit, exp), pre);
    }

    private void deleteMessage(MessageReceivedEvent event) {
        if (!event.isPrivate()) {
        	try {
            	event.getMessage().deleteMessage();
            } catch (PermissionException e) {
	            LOG.warn("Unable to delete message");
            }
        }
    }
}
