package io.quarkiverse.langchain4j.openai;

import static dev.langchain4j.internal.RetryUtils.withRetry;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.time.Duration;
import java.util.Base64;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

import dev.langchain4j.data.image.Image;
import dev.langchain4j.model.image.ImageModel;
import dev.langchain4j.model.openai.internal.image.GenerateImagesRequest;
import dev.langchain4j.model.openai.internal.image.GenerateImagesResponse;
import dev.langchain4j.model.openai.internal.image.ImageData;
import dev.langchain4j.model.output.Response;
import io.quarkiverse.langchain4j.openai.common.QuarkusOpenAiClient;

@SuppressWarnings("OptionalUsedAsFieldOrParameterType")
public class QuarkusOpenAiImageModel implements ImageModel {
    private final String modelName;
    private final String size;
    private final String quality;
    private final String style;
    private final Optional<String> user;
    private final String responseFormat;
    private final String background;
    private final Integer maxRetries;
    private final Optional<Path> persistDirectory;

    private final QuarkusOpenAiClient client;

    public QuarkusOpenAiImageModel(Builder builder) {
        this.modelName = builder.modelName;
        this.size = builder.size;
        this.quality = builder.quality;
        this.style = builder.style;
        this.user = builder.user;
        this.responseFormat = builder.responseFormat;
        this.background = builder.background;
        this.maxRetries = builder.maxRetries;
        if (this.maxRetries < 1) {
            throw new IllegalArgumentException("max-retries must be at least 1");
        }
        this.persistDirectory = builder.persistDirectory;

        this.client = QuarkusOpenAiClient.builder()
                .baseUrl(builder.baseUrl)
                .configName(builder.configName)
                .tlsConfigurationName(builder.tlsConfigurationName)
                .openAiApiKey(builder.apiKey)
                .organizationId(builder.organizationId)
                .callTimeout(builder.timeout)
                .connectTimeout(builder.timeout)
                .readTimeout(builder.timeout)
                .writeTimeout(builder.timeout)
                .logRequests(builder.logRequests)
                .logResponses(builder.logResponses)
                .logCurl(builder.logCurl != null && builder.logCurl)
                .build();
    }

    @Override
    public Response<Image> generate(String prompt) {
        GenerateImagesRequest request = buildRequest(prompt, 1);

        GenerateImagesResponse response = withRetry(() -> client.imagesGeneration(request), maxRetries).execute();
        persistIfNecessary(response);

        return Response.from(fromImageData(response.data().get(0)));
    }

    @Override
    public Response<List<Image>> generate(String prompt, int n) {
        GenerateImagesRequest request = buildRequest(prompt, n);

        GenerateImagesResponse response = withRetry(() -> client.imagesGeneration(request), maxRetries).execute();
        persistIfNecessary(response);

        return Response.from(
                response.data().stream().map(QuarkusOpenAiImageModel::fromImageData).collect(Collectors.toList()));
    }

    private void persistIfNecessary(GenerateImagesResponse response) {
        if (persistDirectory.isEmpty()) {
            return;
        }
        Path persistTo = persistDirectory.get();
        try {
            Files.createDirectories(persistTo);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        for (ImageData data : response.data()) {
            try {
                data.url(
                        data.url() != null
                                ? FilePersistor.persistFromUri(data.url(), persistTo).toUri()
                                : FilePersistor.persistFromBase64String(data.b64Json(), persistTo).toUri());
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        }
    }

    private static Image fromImageData(ImageData data) {
        return Image.builder().url(data.url()).base64Data(data.b64Json()).revisedPrompt(data.revisedPrompt()).build();
    }

    private GenerateImagesRequest buildRequest(String prompt, int n) {
        var builder = GenerateImagesRequest
                .builder()
                .prompt(prompt)
                .model(modelName)
                .n(n)
                .size(size);

        if (quality != null) {
            builder.quality(quality);
        }
        if (style != null) {
            builder.style(style);
        }
        if (responseFormat != null) {
            builder.responseFormat(responseFormat);
        }

        if (user.isPresent()) {
            builder.user(user.get());
        }

        if (background != null) {
            return new GenerateImagesRequestWithExtras(builder, background);
        }

        return builder.build();
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String baseUrl;
        private String configName;
        private String tlsConfigurationName;
        private String apiKey;
        private String organizationId;
        private String modelName;
        private String size;
        private String quality;
        private String style;
        private Optional<String> user;
        private String responseFormat;
        private String background;
        private Duration timeout;
        private Integer maxRetries;
        private Boolean logRequests;
        private Boolean logResponses;
        private Boolean logCurl;
        private Optional<Path> persistDirectory;

        public Builder baseUrl(String baseUrl) {
            this.baseUrl = baseUrl;
            return this;
        }

        public Builder configName(String configName) {
            this.configName = configName;
            return this;
        }

        public Builder tlsConfigurationName(String tlsConfigurationName) {
            this.tlsConfigurationName = tlsConfigurationName;
            return this;
        }

        public Builder apiKey(String apiKey) {
            this.apiKey = apiKey;
            return this;
        }

        public Builder organizationId(String organizationId) {
            this.organizationId = organizationId;
            return this;
        }

        public Builder timeout(Duration timeout) {
            this.timeout = timeout;
            return this;
        }

        public Builder maxRetries(Integer maxRetries) {
            this.maxRetries = maxRetries;
            return this;
        }

        public Builder logRequests(Boolean logRequests) {
            this.logRequests = logRequests;
            return this;
        }

        public Builder logResponses(Boolean logResponses) {
            this.logResponses = logResponses;
            return this;
        }

        public Builder logCurl(Boolean logCurl) {
            this.logCurl = logCurl;
            return this;
        }

        public Builder modelName(String modelName) {
            this.modelName = modelName;
            return this;
        }

        public Builder size(String size) {
            this.size = size;
            return this;
        }

        public Builder quality(String quality) {
            this.quality = quality;
            return this;
        }

        public Builder style(String style) {
            this.style = style;
            return this;
        }

        public Builder user(Optional<String> user) {
            this.user = user;
            return this;
        }

        public Builder responseFormat(String responseFormat) {
            this.responseFormat = responseFormat;
            return this;
        }

        public Builder background(String background) {
            this.background = background;
            return this;
        }

        public Builder persistDirectory(Optional<Path> persistDirectory) {
            this.persistDirectory = persistDirectory;
            return this;
        }

        public QuarkusOpenAiImageModel build() {
            return new QuarkusOpenAiImageModel(this);
        }
    }

    /**
     * Carries the extra OpenAI image parameters that upstream {@code GenerateImagesRequest} does not yet model (notably
     * {@code background} for {@code gpt-image} models). The REST client is typed to the upstream DTO, so we subclass it and
     * rely on Jackson to serialize the additional fields.
     */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    static class GenerateImagesRequestWithExtras extends GenerateImagesRequest {

        @JsonProperty
        private final String background;

        GenerateImagesRequestWithExtras(GenerateImagesRequest.Builder builder, String background) {
            super(builder);
            this.background = background;
        }

        public String getBackground() {
            return background;
        }
    }

    /**
     * Copied from {@code dev.langchain4j.model.openai.internal.FilePersistor}
     */
    private static class FilePersistor {

        static Path persistFromUri(URI uri, Path destinationFolder) {
            try {
                Path fileName = Paths.get(uri.getPath()).getFileName();
                Path destinationFilePath = destinationFolder.resolve(fileName);
                try (InputStream inputStream = uri.toURL().openStream()) {
                    java.nio.file.Files.copy(inputStream, destinationFilePath, StandardCopyOption.REPLACE_EXISTING);
                }

                return destinationFilePath;
            } catch (IOException e) {
                throw new RuntimeException("Error persisting file from URI: " + uri, e);
            }
        }

        public static Path persistFromBase64String(String base64EncodedString, Path destinationFolder) throws IOException {
            byte[] decodedBytes = Base64.getDecoder().decode(base64EncodedString);
            Path destinationFile = destinationFolder.resolve(randomFileName());

            Files.write(destinationFile, decodedBytes, StandardOpenOption.CREATE);

            return destinationFile;
        }

        private static String randomFileName() {
            return UUID.randomUUID().toString().replaceAll("-", "").substring(0, 20);
        }
    }
}
