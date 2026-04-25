package org.acme.examples.aiservices;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.io.IOException;
import java.util.Map;

import jakarta.inject.Inject;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import dev.langchain4j.data.image.Image;
import dev.langchain4j.model.image.ImageModel;
import dev.langchain4j.model.output.Response;
import io.quarkiverse.langchain4j.openai.testing.internal.OpenAiBaseTest;
import io.quarkiverse.langchain4j.testing.internal.WiremockAware;
import io.quarkus.test.QuarkusUnitTest;

/**
 * Covers image generation with the DALL·E family, where {@code style} and {@code response_format} are supported and
 * expected to be serialized.
 */
public class ImageModelTest extends OpenAiBaseTest {

    @RegisterExtension
    static final QuarkusUnitTest unitTest = new QuarkusUnitTest()
            .setArchiveProducer(
                    () -> ShrinkWrap.create(JavaArchive.class))
            .overrideRuntimeConfigKey("quarkus.langchain4j.openai.base-url",
                    WiremockAware.wiremockUrlForConfig("/v1"))
            .overrideRuntimeConfigKey("quarkus.langchain4j.openai.image-model.model-name", "dall-e-3")
            .overrideRuntimeConfigKey("quarkus.langchain4j.openai.image-model.quality", "standard")
            .overrideRuntimeConfigKey("quarkus.langchain4j.openai.image-model.style", "vivid")
            .overrideRuntimeConfigKey("quarkus.langchain4j.openai.image-model.response-format", "url");

    @Inject
    ImageModel imageModel;

    @Test
    public void generatesImageAndSendsDalle3Params() throws IOException {
        Response<Image> response = imageModel.generate("whatever");
        assertNotNull(response);
        assertNotNull(response.content().url());

        assertThat(wiremock().getServeEvents()).hasSize(1);

        Map<String, Object> body = getRequestAsMap();
        assertThat(body)
                .containsEntry("model", "dall-e-3")
                .containsEntry("prompt", "whatever")
                .containsEntry("size", "1024x1024")
                .containsEntry("quality", "standard")
                .containsEntry("style", "vivid")
                .containsEntry("response_format", "url");
        assertThat(body).doesNotContainKey("background");
    }
}
