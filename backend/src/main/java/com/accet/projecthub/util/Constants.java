package com.accet.projecthub.util;

import java.util.List;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.stream.Collectors;

public final class Constants {

    private Constants() {
    }

    public static final List<String> DEPARTMENTS = List.of(
            "CSE", "ECE", "EEE", "Mechanical", "Civil", "IT", "Other"
    );

    public static final Map<String, List<String>> DEPARTMENT_DOMAINS = Map.of(
            "CSE", List.of("Web Development", "AI & ML", "Data Science", "Cybersecurity",
                    "Cloud Computing", "IoT", "Mobile App Development", "Blockchain",
                    "Generative AI", "Software Engineering"),
            "IT", List.of("Web Development", "Cloud Computing", "Cybersecurity", "Data Analytics",
                    "AI Applications", "Mobile Applications", "E-Commerce", "DevOps",
                    "Database Systems", "IoT"),
            "ECE", List.of("Embedded Systems", "IoT", "VLSI", "Robotics",
                    "Communication Systems", "Signal Processing", "Computer Vision",
                    "Wireless Networks", "FPGA", "Biomedical Electronics"),
            "EEE", List.of("Renewable Energy", "Power Systems", "Electric Vehicles",
                    "Power Electronics", "Smart Grid", "Industrial Automation", "Embedded Systems",
                    "Energy Management", "Motor Control", "Battery Management"),
            "Civil", List.of("Structural Engineering", "Construction Management",
                    "Transportation Engineering", "Environmental Engineering",
                    "Geotechnical Engineering", "Smart Cities", "Water Resources", "Green Building",
                    "Surveying & GIS", "Disaster Management"),
            "Mechanical", List.of("Robotics", "Automation", "CAD/CAM", "Automotive Engineering",
                    "Thermal Engineering", "Manufacturing", "Mechatronics", "Renewable Energy",
                    "Material Science", "Industrial Engineering"),
            "Other", List.of()
    );

    public static final List<String> CATEGORIES;

    static {
        LinkedHashSet<String> categories = DEPARTMENT_DOMAINS.values().stream()
                .flatMap(List::stream)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        categories.add("Other");
        CATEGORIES = List.copyOf(categories);
    }

    public static final String DEFAULT_AVATAR = "photo-1535713875002-d1d0cf377fde";
    public static final String DEFAULT_IMAGE = "photo-1517694712202-14dd9538aa97";

    public static String resolveProjectImage(String category) {
        if (category == null || category.isBlank()) {
            return DEFAULT_IMAGE;
        }

        return switch (category.trim()) {
            case "Embedded Systems" -> "photo-1518770660439-4636190af475";
            case "IoT" -> "photo-1504384308090-c894fdcc538d";
            case "AI & ML", "AI Applications", "Generative AI" -> "photo-1485827404703-89b55fcc595e";
            case "Web Development" -> DEFAULT_IMAGE;
            case "Cybersecurity" -> "photo-1550751827-4bd374c3f58b";
            case "Database", "Database Systems" -> "photo-1558494949cc5c2d9b8f1a3b3";
            case "Cloud Computing" -> "photo-1451187580459-43490279c0fa";
            case "Robotics", "Automation", "Industrial Automation" -> "photo-1485827404703-89b55fcc595e";
            case "Mobile App Development", "Mobile Applications" -> "photo-1522542550221-31fd19575a2d";
            case "Data Science", "Data Analytics" -> "photo-1551288049-bebda4e38f71";
            case "Blockchain" -> "photo-1516321318423-f06f85e504b3";
            default -> DEFAULT_IMAGE;
        };
    }
}
