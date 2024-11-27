package ws.palladian.retrieval.search.web;

import org.apache.commons.configuration.Configuration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ws.palladian.helper.collection.MapBuilder;
import ws.palladian.persistence.json.JsonArray;
import ws.palladian.persistence.json.JsonObject;
import ws.palladian.retrieval.DocumentRetriever;
import ws.palladian.retrieval.helper.TimeWindowRequestThrottle;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * A wrapper for Together's APIs.
 * https://docs.together.ai/reference/inference
 *
 * List of models: https://docs.together.ai/docs/inference-models
 *
 * @author David Urbansky
 * Created 02.01.2024
 */
public class TogetherApi extends AiApi {
    private final TimeWindowRequestThrottle throttle;
    private static final Logger LOGGER = LoggerFactory.getLogger(TogetherApi.class);

    private final String apiKey;

    private static final String DEFAULT_MODEL = "mistralai/Mixtral-8x7B-Instruct-v0.1";
    public static final String CONFIG_API_KEY = "api.together.key";
    public static final String CONFIG_API_QPS = "api.together.qps";

    public TogetherApi(String apiKey, int qps) {
        this.apiKey = apiKey;
        throttle = new TimeWindowRequestThrottle(1, TimeUnit.SECONDS, qps);
    }

    public TogetherApi(Configuration configuration) {
        this(configuration.getString(CONFIG_API_KEY), configuration.getInt(CONFIG_API_QPS, 1));
    }

    @Override
    public String chat(JsonArray messages, double temperature, AtomicInteger usedTokens) throws Exception {
        return chat(messages, temperature, usedTokens, DEFAULT_MODEL, null, null);
    }

    @Override
    public String chat(JsonArray messages, double temperature, AtomicInteger usedTokens, String modelName, Integer maxTokens, JsonObject jsonSchema) throws Exception {
        DocumentRetriever documentRetriever = new DocumentRetriever();
        documentRetriever.setGlobalHeaders(MapBuilder.createPut("Content-Type", "application/json").put("Authorization", "Bearer " + apiKey).create());
        JsonObject requestJson = new JsonObject();
        requestJson.put("messages", messages);
        requestJson.put("model", modelName);
        requestJson.put("temperature", temperature);
        if (maxTokens != null) {
            requestJson.put("max_tokens", maxTokens);
        }
        throttle.hold();
        String postResponseText = documentRetriever.postJsonObject("https://api.together.xyz/inference", requestJson, false);
        JsonObject responseJson = JsonObject.tryParse(postResponseText);
        if (responseJson == null) {
            throw new Exception("Could not parse json " + postResponseText);
        }
        if (responseJson.tryQueryString("error/message") != null) {
            throw new Exception(responseJson.tryQueryString("error/message"));
        }

        String content = null;

        try {
            content = responseJson.tryQueryString("output/choices[0]/text");

            if (usedTokens != null) {
                usedTokens.addAndGet(responseJson.tryQueryInt("output/usage/total_tokens"));
            }
        } catch (Exception e) {
            LOGGER.error(e.getMessage(), e);
        }

        return content;
    }
}
