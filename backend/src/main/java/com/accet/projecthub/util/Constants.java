package com.accet.projecthub.util;

import java.util.List;

public final class Constants {

    private Constants() {
    }

    public static final List<String> DEPARTMENTS = List.of(
            "CSE", "ECE", "EEE", "Mechanical", "Civil", "IT", "Other"
    );

    public static final List<String> CATEGORIES = List.of(
            "Web Development", "AI & ML", "IoT", "Mobile Applications",
            "Data Science", "Embedded Systems", "Robotics", "Cloud Computing", "Other"
    );

    public static final String DEFAULT_AVATAR = "photo-1535713875002-d1d0cf377fde";
    public static final String DEFAULT_IMAGE = "photo-1517694712202-14dd9538aa97";
}
