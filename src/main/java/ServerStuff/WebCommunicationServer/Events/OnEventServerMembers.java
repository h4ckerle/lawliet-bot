package ServerStuff.WebCommunicationServer.Events;

import Core.DiscordApiCollection;
import Core.PermissionCheck;
import MySQL.Modules.BannedUsers.DBBannedUsers;
import ServerStuff.WebCommunicationServer.WebComServer;
import com.corundumstudio.socketio.AckRequest;
import com.corundumstudio.socketio.SocketIOClient;
import com.corundumstudio.socketio.listener.DataListener;
import org.javacord.api.entity.server.Server;
import org.javacord.api.entity.user.User;
import org.javacord.api.entity.user.UserStatus;
import org.json.JSONObject;

import java.util.Optional;

public class OnEventServerMembers implements DataListener<JSONObject> {

    @Override
    public void onData(SocketIOClient socketIOClient, JSONObject jsonObject, AckRequest ackRequest) throws Exception {
        long userId = jsonObject.getLong("user_id");
        if (DBBannedUsers.getInstance().getBean().getUserIds().contains(userId)) return;
        long serverId = jsonObject.getLong("server_id");
        Optional<User> userOptional = DiscordApiCollection.getInstance().getUserById(userId);

        if (userOptional.isPresent()) {
            User user = userOptional.get();

            Optional<Server> serverOptional = DiscordApiCollection.getInstance().getServerById(serverId);
            if (serverOptional.isPresent()) {
                Server server = serverOptional.get();
                if (PermissionCheck.hasAdminPermissions(server, user)) {
                    JSONObject mainJSON = new JSONObject()
                            .put("user_id", userId)
                            .put("members_online", server.getMembers().stream().filter(userCheck -> userCheck.getStatus() != UserStatus.OFFLINE).count())
                            .put("members_total", server.getMemberCount());

                    //Send Data
                    socketIOClient.sendEvent(WebComServer.EVENT_SERVERMEMBERS, mainJSON.toString());
                }
            }
        }
    }

}
