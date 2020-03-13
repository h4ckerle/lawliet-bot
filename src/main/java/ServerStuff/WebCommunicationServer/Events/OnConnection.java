package ServerStuff.WebCommunicationServer.Events;

import CommandListeners.onTrackerRequestListener;
import CommandSupporters.Command;
import CommandSupporters.CommandContainer;
import CommandSupporters.CommandManager;
import Constants.Category;
import ServerStuff.WebCommunicationServer.WebComServer;
import com.corundumstudio.socketio.SocketIOClient;
import com.corundumstudio.socketio.listener.ConnectListener;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.HashMap;

public class OnConnection implements ConnectListener {

    private WebComServer webComServer;

    public OnConnection(WebComServer webComServer) {
        this.webComServer = webComServer;
    }

    @Override
    public void onConnect(SocketIOClient socketIOClient) {
        JSONArray mainJSON = new JSONArray();
        HashMap<String, JSONObject> categories = new HashMap<>();

        //Add every command category
        for(String categoryId: Category.LIST) {
            JSONObject categoryJSON = new JSONObject();
            categoryJSON.put("id", categoryId);
            categoryJSON.put("name", webComServer.getLanguagePack(categoryId));
            categoryJSON.put("commands", new JSONArray());
            categories.put(categoryId, categoryJSON);
            mainJSON.put(categoryJSON);
        }

        //Add every command
        for(Class c: CommandContainer.getInstance().getCommandList()) {
            try {
                Command command = CommandManager.createCommandByClass(c);
                String trigger = command.getTrigger();

                if (!command.isPrivate() && !trigger.equals("help")) {
                    JSONObject commandJSON = new JSONObject();
                    commandJSON.put("trigger", trigger);
                    commandJSON.put("emoji", command.getEmoji());
                    commandJSON.put("title", webComServer.getLanguagePack(trigger + "_title"));
                    commandJSON.put("desc_short", webComServer.getLanguagePack(trigger + "_description"));
                    commandJSON.put("desc_long", webComServer.getLanguagePack(trigger + "_helptext"));
                    commandJSON.put("usage", webComServer.getLanguagePackSpecs(trigger + "_usage", trigger));
                    commandJSON.put("examples", webComServer.getLanguagePackSpecs(trigger + "_examples", trigger));
                    commandJSON.put("user_permissions", webComServer.getCommandPermissions(command));
                    commandJSON.put("nsfw", command.isNsfw());
                    commandJSON.put("requires_user_permissions", command.getUserPermissions() != 0);
                    commandJSON.put("can_be_tracked", command instanceof onTrackerRequestListener);

                    categories.get(command.getCategory()).getJSONArray("commands").put(commandJSON);
                }
            } catch (IllegalAccessException | InstantiationException e) {
                e.printStackTrace();
            }
        }

        //Send data
        socketIOClient.sendEvent(WebComServer.EVENT_COMMANDLIST, mainJSON.toString());
    }

}