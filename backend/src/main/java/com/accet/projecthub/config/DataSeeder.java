package com.accet.projecthub.config;

import com.accet.projecthub.entity.Project;
import com.accet.projecthub.entity.ProjectStatus;
import com.accet.projecthub.entity.Role;
import com.accet.projecthub.entity.TeamMember;
import com.accet.projecthub.entity.User;
import com.accet.projecthub.repository.ProjectRepository;
import com.accet.projecthub.repository.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Seeds demo accounts and the eight sample projects from the original prototype,
 * but only when the database is still empty.
 */
@Component
public class DataSeeder implements CommandLineRunner {

    private final UserRepository userRepository;
    private final ProjectRepository projectRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.seed.enabled:true}")
    private boolean seedEnabled;

    public DataSeeder(UserRepository userRepository,
                      ProjectRepository projectRepository,
                      PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.projectRepository = projectRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional
    public void run(String... args) {
        if (!seedEnabled || userRepository.count() > 0) {
            return;
        }

        User admin = userRepository.save(User.builder()
                .name("ProjectHub Admin")
                .email("admin@accet.ac.in")
                .rollNo("ADMIN001")
                .department("Other")
                .year(4)
                .password(passwordEncoder.encode("admin@2024"))
                .avatar("photo-1560250097-0b93528c311a")
                .role(Role.ADMIN)
                .build());

        User arjun = userRepository.save(User.builder()
                .name("Arjun Selvam")
                .email("arjun@student.accet.ac.in")
                .rollNo("20CSE001")
                .department("CSE")
                .year(4)
                .password(passwordEncoder.encode("student123"))
                .avatar("photo-1507003211169-0a1dd7228f2d")
                .role(Role.STUDENT)
                .build());

        User priya = userRepository.save(User.builder()
                .name("Priya Rajan")
                .email("priya@student.accet.ac.in")
                .rollNo("20CSE002")
                .department("CSE")
                .year(4)
                .password(passwordEncoder.encode("student123"))
                .avatar("photo-1494790108377-be9c29b29330")
                .role(Role.STUDENT)
                .build());

        seedProject(arjun, "Smart Attendance System using Face Recognition",
                "An automated attendance system that uses deep learning-based face recognition to mark student attendance in real time. Integrated with college ERP for seamless data sync. Achieves 97.4% accuracy across varied lighting conditions.",
                "CSE", "AI & ML", 2024, "photo-1587825140708-dfaf72ae4b04",
                List.of("Python", "OpenCV", "TensorFlow", "Flask", "MySQL"),
                List.of(new String[]{"Arjun Selvam", "20CSE001"},
                        new String[]{"Priya Rajan", "20CSE002"},
                        new String[]{"Karthik M", "20CSE003"}),
                87, 412, LocalDate.of(2024, 3, 15));

        seedProject(priya, "IoT-Based Smart Agriculture Monitoring System",
                "A sensor-driven agricultural monitoring platform using ESP32, soil moisture, pH, and temperature sensors. Data is streamed to a cloud dashboard for real-time crop health analysis and automated irrigation control.",
                "ECE", "IoT", 2024, "photo-1560493676-04071c5f467b",
                List.of("ESP32", "MQTT", "Node.js", "React", "AWS IoT"),
                List.of(new String[]{"Divya Lakshmi", "20ECE011"},
                        new String[]{"Surya Kumar", "20ECE014"}),
                64, 298, LocalDate.of(2024, 2, 20));

        seedProject(priya, "College Campus Navigation App",
                "A mobile app for new students and visitors to navigate the college campus using indoor mapping and AR overlays. Includes real-time faculty room availability and event notifications.",
                "IT", "Mobile Applications", 2024, "photo-1519389950473-47ba0277781c",
                List.of("Flutter", "Firebase", "Google Maps SDK", "ARCore"),
                List.of(new String[]{"Meera Nair", "20IT005"},
                        new String[]{"Rohan Pillai", "20IT007"},
                        new String[]{"Ananya S", "20IT009"}),
                53, 235, LocalDate.of(2024, 3, 1));

        seedProject(arjun, "Predictive Maintenance of Industrial Motors",
                "Vibration and temperature data from industrial motors is analyzed using ML models to predict failures before they occur. Reduces unplanned downtime by up to 40% in simulated industrial environments.",
                "EEE", "AI & ML", 2023, "photo-1581092160607-ee22621dd758",
                List.of("Python", "Scikit-learn", "Raspberry Pi", "MATLAB", "InfluxDB"),
                List.of(new String[]{"Bharath Raj", "20EEE003"},
                        new String[]{"Nithya K", "20EEE006"}),
                41, 187, LocalDate.of(2023, 11, 10));

        seedProject(priya, "Structural Health Monitoring using Wireless Sensors",
                "A distributed wireless sensor network deployed on bridge structures to monitor stress, strain, and displacement in real time. Data analytics identify early-stage structural anomalies for preventive maintenance.",
                "Civil", "IoT", 2024, "photo-1486325212027-8081e485255e",
                List.of("ZigBee", "LabVIEW", "Python", "PostgreSQL", "Grafana"),
                List.of(new String[]{"Vijay Anand", "20CE001"},
                        new String[]{"Lakshmi P", "20CE004"},
                        new String[]{"Tamil Selvan", "20CE007"}),
                35, 160, LocalDate.of(2024, 1, 28));

        seedProject(arjun, "Autonomous Line-Following Robot with Obstacle Avoidance",
                "A fully autonomous robot that follows a predefined track using IR sensors and avoids dynamic obstacles using ultrasonic ranging. Implemented PID control for smooth motion at high speeds.",
                "Mechanical", "Robotics", 2023, "photo-1485827404703-89b55fcc595e",
                List.of("Arduino", "C++", "Ultrasonic Sensors", "PID Control", "SolidWorks"),
                List.of(new String[]{"Rahul S", "20ME002"},
                        new String[]{"Aishwarya G", "20ME005"}),
                72, 320, LocalDate.of(2023, 10, 5));

        seedProject(priya, "Student Grievance Management Portal",
                "A secure web portal that streamlines student complaint registration, tracking, and resolution with department-wise routing, email notifications, and SLA tracking for administration.",
                "CSE", "Web Development", 2024, "photo-1551288049-bebda4e38f71",
                List.of("React", "Node.js", "Express", "MongoDB", "Nodemailer"),
                List.of(new String[]{"Harini R", "21CSE015"},
                        new String[]{"Dev Krishnan", "21CSE018"}),
                29, 143, LocalDate.of(2024, 4, 2));

        seedProject(arjun, "Real-Time Air Quality Index Dashboard",
                "An IoT-enabled city-level air quality monitoring system that aggregates PM2.5, PM10, CO2, and VOC data from distributed sensor nodes, displaying AQI maps and health alerts on a public web dashboard.",
                "ECE", "Data Science", 2024, "photo-1473341304170-971dccb5ac1e",
                List.of("Python", "Pandas", "Plotly", "FastAPI", "InfluxDB", "Mapbox"),
                List.of(new String[]{"Siva Krishnan", "21ECE020"},
                        new String[]{"Preethi M", "21ECE022"},
                        new String[]{"Arun T", "21ECE025"}),
                48, 214, LocalDate.of(2024, 3, 20));

        System.out.println("[DataSeeder] Demo data created. "
                + "Admin: " + admin.getEmail() + " / admin@2024, "
                + "Student: arjun@student.accet.ac.in / student123");
    }

    private void seedProject(User owner, String title, String description,
                             String department, String category, int year, String image,
                             List<String> technologies, List<String[]> members,
                             int likes, int views, LocalDate submittedAt) {

        Set<String> techs = new LinkedHashSet<>(technologies);

        Project project = Project.builder()
                .title(title)
                .description(description)
                .deployLink("https://example.com")
                .department(department)
                .category(category)
                .year(year)
                .image(image)
                .status(ProjectStatus.APPROVED)
                .likesCount(likes)
                .viewsCount(views)
                .submittedAt(submittedAt)
                .submittedBy(owner)
                .technologies(techs)
                .teamMembers(new ArrayList<>())
                .build();

        for (String[] m : members) {
            project.addTeamMember(TeamMember.builder().name(m[0]).rollNo(m[1]).build());
        }

        projectRepository.save(project);
    }
}
