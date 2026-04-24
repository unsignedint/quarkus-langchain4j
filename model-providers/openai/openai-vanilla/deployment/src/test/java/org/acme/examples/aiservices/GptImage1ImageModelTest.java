package org.acme.examples.aiservices;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.io.IOException;
import java.util.Map;

import jakarta.inject.Inject;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import dev.langchain4j.data.image.Image;
import dev.langchain4j.model.image.ImageModel;
import dev.langchain4j.model.output.Response;
import io.quarkiverse.langchain4j.openai.testing.internal.OpenAiBaseTest;
import io.quarkiverse.langchain4j.testing.internal.WiremockAware;
import io.quarkus.test.QuarkusUnitTest;

/**
 * Covers image generation with {@code gpt-image-1}, which rejects {@code style} and {@code response_format}. The
 * serialized body must omit both fields, and the {@code background} parameter must be forwarded when configured.
 */
public class GptImage1ImageModelTest extends OpenAiBaseTest {

    @RegisterExtension
    static final QuarkusUnitTest unitTest = new QuarkusUnitTest()
            .setArchiveProducer(
                    () -> ShrinkWrap.create(JavaArchive.class))
            .overrideRuntimeConfigKey("quarkus.langchain4j.openai.base-url",
                    WiremockAware.wiremockUrlForConfig("/v1"))
            .overrideRuntimeConfigKey("quarkus.langchain4j.openai.image-model.model-name", "gpt-image-1")
            .overrideRuntimeConfigKey("quarkus.langchain4j.openai.image-model.quality", "high")
            .overrideRuntimeConfigKey("quarkus.langchain4j.openai.image-model.size", "1024x1024")
            .overrideRuntimeConfigKey("quarkus.langchain4j.openai.image-model.background", "transparent");

    @Inject
    ImageModel imageModel;

    @BeforeEach
    void registerStub() {
        resetRequests();
        wiremock().register(post(urlEqualTo("/v1/images/generations"))
                .atPriority(1)
                .willReturn(aResponse()
                        .withHeader("Content-Type", "application/json")
                        .withBody(
                                """
                                        {
                                          "created": 1708633271,
                                          "data": [
                                            {
                                              "b64_json": "aGVsbG8="
                                            }
                                          ]
                                        }
                                        """)));
    }

    @Test
    public void omitsStyleAndResponseFormatAndForwardsBackground() throws IOException {
        Response<Image> response = imageModel.generate("whatever");
        assertNotNull(response);
        assertNotNull(response.content().base64Data());

        assertThat(wiremock().getServeEvents()).hasSize(1);

        Map<String, Object> body = getRequestAsMap();
        assertThat(body)
                .containsEntry("model", "gpt-image-1")
                .containsEntry("prompt", "whatever")
                .containsEntry("size", "1024x1024")
                .containsEntry("quality", "high")
                .containsEntry("background", "transparent");
        assertThat(body)
                .doesNotContainKey("style")
                .doesNotContainKey("response_format");
    }
}
