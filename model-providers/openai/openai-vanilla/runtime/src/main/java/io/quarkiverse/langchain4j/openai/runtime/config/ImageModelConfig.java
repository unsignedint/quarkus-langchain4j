package io.quarkiverse.langchain4j.openai.runtime.config;

import java.nio.file.Path;
import java.util.Optional;

import io.quarkus.runtime.annotations.ConfigDocDefault;
import io.quarkus.runtime.annotations.ConfigGroup;
import io.smallrye.config.WithDefault;

@ConfigGroup
public interface ImageModelConfig {

    /**
     * Model name to use
     */
    @WithDefault("dall-e-3")
    String modelName();

    /**
     * Configure whether the generated images will be saved to disk.
     * By default, persisting is disabled, but it is implicitly enabled when
     * {@code quarkus.langchain4j.openai.image-mode.directory} is set and this property is not to {@code false}
     */
    @ConfigDocDefault("false")
    Optional<Boolean> persist();

    /**
     * The path where the generated images will be persisted to disk.
     * This only applies of {@code quarkus.langchain4j.openai.image-mode.persist} is not set to {@code false}.
     */
    @ConfigDocDefault("${java.io.tmpdir}/dall-e-images")
    Optional<Path> persistDirectory();

    /**
     * The format in which the generated images are returned.
     * <p>
     * Must be one of {@code url} or {@code b64_json}.
     * <p>
     * This param is rejected by {@code gpt-image-1} and later image models, which always return base64 data. When
     * unset, the field is omitted from the request so those models are not affected.
     */
    Optional<String> responseFormat();

    /**
     * The size of the generated images.
     * <p>
     * Must be one of {@code 1024x1024}, {@code 1792x1024}, or {@code 1024x1792} when the model is {@code dall-e-3}.
     * <p>
     * Must be one of {@code 256x256}, {@code 512x512}, or {@code 1024x1024} when the model is {@code dall-e-2}.
     * <p>
     * Must be one of {@code 1024x1024}, {@code 1024x1536}, {@code 1536x1024}, or {@code auto} when the model is
     * {@code gpt-image-1}.
     */
    @WithDefault("1024x1024")
    String size();

    /**
     * The quality of the image that will be generated.
     * <p>
     * For {@code dall-e-3}: {@code standard} or {@code hd}. For {@code dall-e-2}: {@code standard} only. For
     * {@code gpt-image-1}: {@code low}, {@code medium}, {@code high}, or {@code auto}.
     */
    @WithDefault("standard")
    String quality();

    /**
     * The number of images to generate.
     * <p>
     * Must be between 1 and 10.
     * <p>
     * When the model is dall-e-3, only n=1 is supported.
     */
    @WithDefault("1")
    int number();

    /**
     * The style of the generated images.
     * <p>
     * Must be one of {@code vivid} or {@code natural}. Vivid causes the model to lean towards generating hyper-real and
     * dramatic images. Natural causes the model to produce more natural, less hyper-real looking images.
     * <p>
     * This param is only supported when the model is {@code dall-e-3}. When unset, the field is omitted from the
     * request so other models are not affected.
     */
    Optional<String> style();

    /**
     * A unique identifier representing your end-user, which can help OpenAI to monitor and detect abuse.
     */
    Optional<String> user();

    /**
     * Whether image model requests should be logged
     */
    @ConfigDocDefault("false")
    Optional<Boolean> logRequests();

    /**
     * Whether image model responses should be logged
     */
    @ConfigDocDefault("false")
    Optional<Boolean> logResponses();
}
