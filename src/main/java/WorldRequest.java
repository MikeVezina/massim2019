import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.stream.JsonReader;
import org.apache.http.HttpHeaders;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.tools.ant.filters.StringInputStream;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;

public class WorldRequest {
    private static final String HOST = "http://localhost:8080";
    private static final String CREATE_WORLD_URI = HOST + "/api/createWorld";
    private static final String ACTIONS_URI = HOST + "/api/actions";
    private static final String MODEL_CHECK_URI = HOST + "/api/modelCheck";
    private static final String PERFORM_ACTION_URI = HOST + "/api/performAction";

    private final CloseableHttpClient client = HttpClients.createDefault();

    public WorldRequest(String aliceCards,
                        String bobCards,
                        String carlCards)
    {
        try {
            String pointedWorld = "w${alice}_" + aliceCards + "_${bob}_" + bobCards + "_${carl}_" + carlCards;
            String json = Files.readString(Path.of("./a8model.json"));
            json = json.replace("${pointedWorld}", pointedWorld);
            json = json.replace("${alice}", "alice");
            json = json.replace("${bob}", "bob");
            json = json.replace("${carl}", "carl");
//            System.out.println(json);
            sendModelRequest(json);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    private void sendModelRequest(String model)
    {
        HttpPost post = new HttpPost(CREATE_WORLD_URI);
        post.setEntity(new StringEntity(model, ContentType.APPLICATION_JSON));
        try (CloseableHttpResponse res = client.execute(post)){
            System.out.println(res.getStatusLine());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public boolean modelCheckFormula(String formula)
    {
        HttpPost getModelCheck = new HttpPost(MODEL_CHECK_URI);
        JsonElement obj = new JsonObject();
        obj.getAsJsonObject().addProperty("formula", formula);
        getModelCheck.setEntity(new StringEntity(obj.toString(), ContentType.APPLICATION_JSON));

        JsonElement e = getParsedResponse(getModelCheck, (res) -> {
            try {
                BufferedInputStream bR = new BufferedInputStream(res.getEntity().getContent());
                String jsonStr = new String(bR.readAllBytes());
                JsonObject resObj = (new JsonParser()).parse(jsonStr).getAsJsonObject();
                return resObj.get("result");

            } catch (IOException ex) {
                ex.printStackTrace();
                return null;
            }
        });

        return e != null && e.getAsBoolean();
    }

    public void performAction(String action)
    {
        HttpPost postPerformAction = new HttpPost(PERFORM_ACTION_URI);
        JsonObject rootObj = new JsonObject();
        rootObj.addProperty("action", action);
        postPerformAction.setEntity(new StringEntity(rootObj.toString(), ContentType.APPLICATION_JSON));

        getParsedResponse(postPerformAction, res -> {

        });
    }

    public String getAgentAction(String agent)
    {
        HttpGet getActions = new HttpGet(ACTIONS_URI);
        getActions.addHeader(HttpHeaders.ACCEPT, ContentType.APPLICATION_JSON.getMimeType());
        try (CloseableHttpResponse res = client.execute(getActions)){
            BufferedInputStream bR = new BufferedInputStream(res.getEntity().getContent());

            String jsonStr = new String(bR.readAllBytes());
            JsonElement elem = (new JsonParser()).parse(jsonStr);
            JsonArray arr = elem.getAsJsonArray();
            for(JsonElement e : arr)
            {
                if(e.getAsString().toLowerCase().contains(agent.toLowerCase()))
                {
                    return e.getAsString();
                }
            }


        } catch (IOException e) {
            e.printStackTrace();
        }

        return "";
    }

    private <R> R getParsedResponse(HttpRequestBase httpRequest, Function<CloseableHttpResponse, R> responseProcessFunc)
    {
        try (CloseableHttpResponse res = client.execute(httpRequest)){
            return responseProcessFunc.apply(res);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    private void getParsedResponse(HttpRequestBase httpRequest, Consumer<CloseableHttpResponse> responseProcessFunc)
    {
        try (CloseableHttpResponse res = client.execute(httpRequest)){
            responseProcessFunc.accept(res);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
