package com.accet.projecthub.service;

import com.accet.projecthub.dto.AuthResponse;
import com.accet.projecthub.dto.LoginRequest;
import com.accet.projecthub.dto.RegisterRequest;
import com.accet.projecthub.dto.UserDto;
import com.accet.projecthub.entity.Role;
import com.accet.projecthub.entity.User;
import com.accet.projecthub.exception.BadRequestException;
import com.accet.projecthub.exception.DuplicateResourceException;
import com.accet.projecthub.exception.ResourceNotFoundException;
import com.accet.projecthub.repository.BookmarkRepository;
import com.accet.projecthub.repository.ProjectLikeRepository;
import com.accet.projecthub.repository.UserRepository;
import com.accet.projecthub.security.JwtService;
import com.accet.projecthub.util.Constants;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final ProjectLikeRepository likeRepository;
    private final BookmarkRepository bookmarkRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AuthService(UserRepository userRepository,
                       ProjectLikeRepository likeRepository,
                       BookmarkRepository bookmarkRepository,
                       PasswordEncoder passwordEncoder,
                       JwtService jwtService) {
        this.userRepository = userRepository;
        this.likeRepository = likeRepository;
        this.bookmarkRepository = bookmarkRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        String email = request.getEmail().trim().toLowerCase();

        if (userRepository.existsByEmailIgnoreCase(email)) {
            throw new DuplicateResourceException("An account with this email already exists");
        }
        if (userRepository.existsByRollNoIgnoreCase(request.getRollNo().trim())) {
            throw new DuplicateResourceException("An account with this roll number already exists");
        }
        if (!Constants.DEPARTMENTS.contains(request.getDepartment())) {
            throw new BadRequestException("Invalid department: " + request.getDepartment());
        }

        User user = User.builder()
                .name(request.getName().trim())
                .email(email)
                .rollNo(request.getRollNo().trim().toUpperCase())
                .department(request.getDepartment())
                .year(request.getYear())
                // BCrypt hash, never the raw password
                .password(passwordEncoder.encode(request.getPassword()))
                .avatar(request.getAvatar() == null || request.getAvatar().isBlank()
                        ? Constants.DEFAULT_AVATAR : request.getAvatar())
                .role(Role.STUDENT)
                .build();

        User saved = userRepository.save(user);
        return buildAuthResponse(saved);
    }

    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest request) {
        User user = userRepository.findByEmailIgnoreCase(request.getEmail().trim())
                .orElseThrow(() -> new BadCredentialsException("Invalid email or password"));

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new BadCredentialsException("Invalid email or password");
        }

        return buildAuthResponse(user);
    }

    @Transactional(readOnly = true)
    public UserDto getCurrentUser(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        return toDto(user);
    }

    private AuthResponse buildAuthResponse(User user) {
        String token = jwtService.generateToken(
                user.getEmail(), user.getId(), user.getRole().name());

        return AuthResponse.builder()
                .token(token)
                .tokenType("Bearer")
                .expiresInMs(jwtService.getExpirationMs())
                .user(toDto(user))
                .build();
    }

    private UserDto toDto(User user) {
        return UserDto.builder()
                .id(user.getId())
                .name(user.getName())
                .email(user.getEmail())
                .rollNo(user.getRollNo())
                .department(user.getDepartment())
                .year(user.getYear())
                .avatar(user.getAvatar())
                .role(user.getRole().name())
                .likedProjects(likeRepository.findProjectIdsByUserId(user.getId()))
                .bookmarks(bookmarkRepository.findProjectIdsByUserId(user.getId()))
                .build();
    }
}
