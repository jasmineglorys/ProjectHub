package com.accet.projecthub.service;

import com.accet.projecthub.dto.ProjectRequest;
import com.accet.projecthub.dto.ProjectDto;
import com.accet.projecthub.util.Constants;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ProjectCoverImageResolverTest {

    private final ProjectCoverImageResolver resolver = new ProjectCoverImageResolver();

    @Test
    void categoryTakesPriorityOverConflictingTechnologiesAndText() {
        ProjectRequest request = request("AI & ML", List.of("React", "Spring Boot"),
                "Cloud dashboard", "Uses Azure cloud services");

        assertEquals("photo-1677442136019-21780ecad995", resolver.resolve(request));
    }

        @Test
        void preservesAValidCoverImageSuppliedByTheFrontend() {
                ProjectRequest request = request("Embedded Systems", List.of("Python"), "Earthquake Detection", "");
                request.setCoverImage("photo-1518770660439-4636190af475");

                assertEquals(request.getCoverImage(), resolver.resolve(request));
        }

        @Test
        void requestBindsTheCoverImageJsonProperty() throws Exception {
                ProjectRequest request = new ObjectMapper().readValue(
                                "{\"coverImage\":\"photo-1518770660439-4636190af475\"}", ProjectRequest.class);

                assertEquals("photo-1518770660439-4636190af475", request.getCoverImage());
        }

        @Test
        void ignoresAnInvalidSuppliedCoverAndResolvesTheProjectDomain() {
                ProjectRequest request = request("Embedded Systems", List.of("Python"), "Earthquake Detection", "");
                request.setCoverImage("javascript:alert(1)");

                assertEquals("photo-1518770660439-4636190af475", resolver.resolve(request));
        }

    @Test
    void technologiesTakePriorityOverConflictingTitleAndDescription() {
        ProjectRequest request = request("Other", List.of("ESP32", "Sensors"),
                "AI Powered Portal", "An AI web application for data visualization");

        assertEquals("photo-1581091226825-a6a2a5aee158", resolver.resolve(request));
    }

        @Test
        void embeddedCategoryWinsOverProgrammingLanguageAndUsesHardwareCover() {
                ProjectRequest request = request("Embedded Systems", List.of("Python", "Arduino", "Accelerometer", "Sensor"),
                                "Earthquake Detection", "Detects seismic activity with a sensor-based monitoring system");

                assertEquals("photo-1518770660439-4636190af475", resolver.resolve(request));
        }

        @Test
        void detectsSeismicHardwareFromProjectTitleWhenCategoryIsOther() {
                ProjectRequest request = request("Other", List.of("Python", "Arduino", "Accelerometer"),
                                "Earthquake Detection", "Measures vibration to detect seismic events");

                assertEquals("photo-1518770660439-4636190af475", resolver.resolve(request));
        }

        @Test
        void pythonDoesNotOverrideProjectDomain() {
                ProjectRequest request = request("Other", List.of("Python", "ESP32", "Sensors", "MQTT"),
                                "Smart Agriculture Monitoring", "Monitors crop and soil conditions");

                assertEquals("photo-1581091226825-a6a2a5aee158", resolver.resolve(request));
        }

    @Test
    void recognizesRequestedProjectDomains() {
        assertEquals("photo-1498050108023-c5249f4df085", resolver.resolve(request("Web Development",
                List.of("Java"), "", "")));
        assertEquals("photo-1563013544-824ae1b704d3", resolver.resolve(request("Other",
                List.of("Encryption"), "", "")));
        assertEquals("photo-1451187580459-43490279c0fa", resolver.resolve(request("Cloud Computing",
                List.of(), "", "")));
        assertEquals("photo-1544383835-bda2bc66a55d", resolver.resolve(request("Other",
                List.of("MySQL"), "", "")));
        assertEquals("photo-1512941937669-90a1b58e7e9", resolver.resolve(request("Other",
                List.of("React Native"), "", "")));
    }

    @Test
    void usesDescriptionAsLastMatchAndDefaultForUnknownProjects() {
        assertEquals("photo-1639762681485-074b7f938ba0", resolver.resolve(request("Other",
                List.of(), "Capstone", "Ethereum based distributed ledger")));
        assertEquals(Constants.DEFAULT_IMAGE, resolver.resolve(request("Other",
                List.of("UnlistedTool"), "Capstone", "A student project")));
    }

        @Test
        void dtoSerializesCoverImageAliasAndLegacyImageProperty() {
                String coverImage = "photo-1518770660439-4636190af475";
                JsonNode json = new ObjectMapper().valueToTree(ProjectDto.builder().image(coverImage).build());

                assertEquals(coverImage, json.path("coverImage").asText());
                assertEquals(coverImage, json.path("image").asText());
        }

    private ProjectRequest request(String category, List<String> technologies, String title, String description) {
        ProjectRequest request = new ProjectRequest();
        request.setCategory(category);
        request.setTechnologies(technologies);
        request.setTitle(title);
        request.setDescription(description);
        return request;
    }
}