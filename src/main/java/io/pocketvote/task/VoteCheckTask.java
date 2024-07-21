package io.pocketvote.task;

import cn.nukkit.Server;
import io.pocketvote.PocketVote;
import io.pocketvote.data.TaskResult;
import io.pocketvote.event.VoteEvent;

import java.util.LinkedHashMap;
import java.util.regex.Pattern;

public class VoteCheckTask extends ApiRequest {

    private PocketVote plugin;
    private static final Pattern namePattern = Pattern.compile("^[a-zA-Z0-9_ ]{1,16}$");
    
    public VoteCheckTask(PocketVote plugin) {
        super(plugin.isDev() ? "http://127.0.0.1:9000/v2/check" : "https://api.pocketvote.io/v2/check", "GET", "VOTE", null);
        this.plugin = plugin;

        plugin.getLogger().debug("Checking for outstanding votes.");
    }

    @Override
    public void onCompletion(Server server) {
        if(!(super.getResult() instanceof TaskResult)) {
            server.getLogger().error("[PocketVote] Result of " + getClass().getCanonicalName() + " was not an instance of TaskResult.");
            return;
        }

        if(!hasResult() || !(getResult() instanceof TaskResult)) return;

        TaskResult result = (TaskResult) getResult();

        if(result.hasError()) {
            server.getLogger().error("[PocketVote] VoteCheckTask: " + result.getMessage());
            return;
        }

        // Having no claims is the same has having no votes to process.
        if(!result.hasClaims() || !result.hasVotes()) {
            server.getLogger().debug("[PocketVote] " + result.getMessage());
            if(result.hasMeta()) plugin.startScheduler(result.getMeta().containsKey("frequency") ? (int) result.getMeta().get("frequency") : 60);
            return;
        }

        for(LinkedHashMap<String, String> vote : result.getVotes()) {
            String site = vote.get("site");
            if (!site.equals("minecraftpocket-servers.com")) {
                System.out.println("Ignoring old voting site " + site);
                continue;
            }
            String player = vote.get("player");
            if(namePattern.matcher(player).find()) {
                server.getPluginManager().callEvent(new VoteEvent(player, vote.get("ip"), site));
            }
        }

        if(result.hasMeta()) plugin.startScheduler(result.getMeta().containsKey("frequency") ? (int) result.getMeta().get("frequency") : 60);
    }

}