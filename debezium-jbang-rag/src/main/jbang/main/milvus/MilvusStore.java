package main.milvus;

import java.util.List;

import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;

import org.eclipse.microprofile.config.inject.ConfigProperty;

import io.milvus.v2.client.ConnectConfig;
import io.milvus.v2.client.MilvusClientV2;
import io.milvus.v2.common.DataType;
import io.milvus.v2.common.IndexParam;
import io.milvus.v2.service.collection.request.CreateCollectionReq;
import io.milvus.v2.service.collection.request.CreateCollectionReq.CollectionSchema;
import io.milvus.v2.service.collection.request.DropCollectionReq;
import io.milvus.v2.service.vector.request.QueryReq;
import io.quarkus.arc.Unremovable;

import main.printer.Console;

@ApplicationScoped
@Unremovable
public class MilvusStore {
    @ConfigProperty(name = "milvus.uri")
    String milvusUri;

    @ConfigProperty(name = "milvus.collection.name")
    String milvusCollectionName;

    @ConfigProperty(name = "milvus.collection.floatVector.dim")
    Integer dimension;

    private MilvusClientV2 client;

    @PostConstruct
    void connect() {
        Console.info("Connecting to Milvus at %s", milvusUri);

        final var config = ConnectConfig.builder()
                .uri(milvusUri)
                .build();
        client = new MilvusClientV2(config);
    }

    public void init() {
        try {
            client.dropCollection(DropCollectionReq.builder().collectionName(milvusCollectionName).build());
        }
        catch (Exception e) {
            // Ignore drop errors for non-existing collection
        }

        final var pkField = CreateCollectionReq.FieldSchema.builder()
                .name("id")
                .isPrimaryKey(true)
                .dataType(DataType.VarChar)
                .maxLength(64)
                .build();
        final var titleField = CreateCollectionReq.FieldSchema.builder()
                .name("metadata")
                .dataType(DataType.JSON)
                .isNullable(true)
                .build();
        final var contentsField = CreateCollectionReq.FieldSchema.builder()
                .name("text")
                .dataType(DataType.VarChar)
                .maxLength(4096)
                .build();
        final var vectorField = CreateCollectionReq.FieldSchema.builder()
                .name("vector")
                .dataType(DataType.FloatVector)
                .dimension(dimension)
                .build();
        final var collectionSchema = CollectionSchema.builder()
                .fieldSchemaList(List.of(pkField, titleField, contentsField, vectorField))
                .build();
        final var index = IndexParam.builder()
                .fieldName("vector")
                .indexType(IndexParam.IndexType.AUTOINDEX)
                .build();
        final var request = CreateCollectionReq.builder()
                .collectionName(milvusCollectionName)
                .collectionSchema(collectionSchema)
                .indexParams(List.of(index))
                .build();
        client.createCollection(request);

        Console.success("Created collection '%s'", milvusCollectionName);
    }

    public void list() {
        final var request = QueryReq.builder()
                .collectionName(milvusCollectionName)
                .filter("id != \"\"")
                .outputFields(List.of("id", "metadata", "text"))
                .build();

        final var response = client.query(request);
        Console.header("Milvus Documents (" + response.getQueryResults().size() + ")");
        response.getQueryResults().forEach(result -> {
            var entity = result.getEntity();
            var text = entity.containsKey("text") ? entity.get("text").toString() : "";
            var preview = text.substring(0, Math.min(80, text.length()));
            Console.label("  " + Console.BOLD + Console.MAGENTA + entity.get("id") + Console.RESET,
                    Console.DIM + preview + "..." + Console.RESET);
        });
    }
}
