package main.ai;

import io.quarkiverse.langchain4j.RegisterAiService;

import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;

@RegisterAiService(retrievalAugmentor = MilvusRetrievalAugmentor.class)
public interface Chat {

    @SystemMessage("""
            You are a helpful assistant. Answer questions based on the provided context.
            Be concise and accurate.""")
    String chat(@UserMessage String message);
}
