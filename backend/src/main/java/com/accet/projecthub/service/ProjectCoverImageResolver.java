package com.accet.projecthub.service;

import com.accet.projecthub.dto.ProjectRequest;
import com.accet.projecthub.entity.Project;
import com.accet.projecthub.util.Constants;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Locale;
import java.util.Collection;
import java.net.URI;
import java.util.regex.Pattern;

@Component
public class ProjectCoverImageResolver {

    private static final List<CoverRule> RULES = List.of(
            new CoverRule("photo-1677442136019-21780ecad995", "artificial intelligence", "machine learning",
                    "deep learning", "tensorflow", "pytorch", "neural network", "computer vision",
                    "natural language processing", "generative ai", "large language model", "llm", "ai", "ml", "nlp"),
            new CoverRule("photo-1512941937669-90a1b58e7e9", "mobile applications", "mobile app",
                    "react native", "android", "flutter", "kotlin", "ios"),
            new CoverRule("photo-1498050108023-c5249f4df085", "web development", "frontend", "backend",
                    "full stack", "javascript", "react", "angular", "vue", "html", "css", "spring boot", "node.js"),
            new CoverRule("photo-1518770660439-4636190af475", "embedded systems", "embedded system", "embedded",
                    "microcontroller", "microprocessor", "accelerometer", "vibration sensor", "earthquake",
                    "seismic", "circuit board", "electronics", "hardware prototype", "hardware", "8051", "stm32", "pic"),
            new CoverRule("photo-1581091226825-a6a2a5aee158", "internet of things", "smart agriculture",
                    "smart home", "smart device", "connected device", "connected sensors", "raspberry pi",
                    "esp8266", "esp32", "mqtt", "arduino", "sensor", "iot"),
            new CoverRule("photo-1563013544-824ae1b704d3", "cybersecurity", "cyber security",
                    "network security", "ethical hacking", "cyber attack", "information security",
                    "encryption", "firewall", "security"),
            new CoverRule("photo-1451187580459-43490279c0fa", "cloud computing", "google cloud", "kubernetes",
                    "docker", "aws", "azure", "gcp", "cloud"),
            new CoverRule("photo-1551288049-bebda4e38f71", "data science", "data analytics", "visualization",
                    "data visualization", "power bi", "statistics", "pandas", "numpy", "dataset", "analytics"),
            new CoverRule("photo-1544383835-bda2bc66a55d", "database", "postgresql", "mongodb", "mysql",
                    "database management", "oracle", "sql"),
            new CoverRule("photo-1485827404703-89b55fcc595e", "robotics", "robot", "automation"),
            new CoverRule("photo-1639762681485-074b7f938ba0", "blockchain", "distributed ledger",
                    "smart contract", "cryptocurrency", "ethereum", "web3")
    );

    public String resolve(ProjectRequest request) {
                if (isValidCoverImage(request.getCoverImage())) return request.getCoverImage().trim();
                if (isValidCoverImage(request.getImage())) return request.getImage().trim();
                return resolveDomain(request);
        }

        public String resolveDomain(ProjectRequest request) {
                return resolve(request.getCategory(), request.getTechnologies(), request.getTitle(), request.getDescription());
        }

        public String resolve(Project project) {
                return resolve(project.getCategory(), project.getTechnologies(), project.getTitle(), project.getDescription());
        }

        public boolean isValidCoverImage(String value) {
                if (value == null || value.isBlank() || value.length() > 200) return false;
                if (value.matches("photo-[a-zA-Z0-9-]+")) return true;
                try {
                        URI uri = URI.create(value);
                        return "https".equalsIgnoreCase(uri.getScheme()) && uri.getHost() != null;
                } catch (IllegalArgumentException exception) {
                        return false;
                }
        }

        private String resolve(String category, Collection<String> technologies, String title, String description) {
                String image = findImage(category);
        if (image != null) return image;

                image = findImage(technologies == null ? "" : String.join(" ", technologies));
        if (image != null) return image;

                image = findImage(title);
        if (image != null) return image;

                image = findImage(description);
        return image == null ? Constants.DEFAULT_IMAGE : image;
    }

    private String findImage(String value) {
        if (value == null || value.isBlank()) return null;
        String text = value.toLowerCase(Locale.ROOT);
        for (CoverRule rule : RULES) {
            for (String keyword : rule.keywords()) {
                boolean matches = keyword.length() <= 3
                        ? Pattern.compile("(?<![a-z0-9])" + Pattern.quote(keyword) + "(?![a-z0-9])")
                                .matcher(text).find()
                        : text.contains(keyword);
                if (matches) return rule.image();
            }
        }
        return null;
    }

    private record CoverRule(String image, String... keywords) {
    }
}