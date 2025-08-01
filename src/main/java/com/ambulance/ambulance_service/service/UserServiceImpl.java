package com.ambulance.ambulance_service.service;

import com.ambulance.ambulance_service.dto.UserDto;
import com.ambulance.ambulance_service.entity.User;
import com.ambulance.ambulance_service.entity.Role;
import com.ambulance.ambulance_service.exception.*;
import com.ambulance.ambulance_service.repository.UserRepository;
import org.apache.commons.lang3.StringUtils;
import org.modelmapper.ModelMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
@Transactional
public class UserServiceImpl implements UserService {
    private static final Logger logger = LoggerFactory.getLogger(UserServiceImpl.class);
    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[A-Za-z0-9+_.-]+@(.+)$");
    private static final Pattern PASSWORD_PATTERN = Pattern.compile("^(?=.*[0-9])(?=.*[a-z])(?=.*[A-Z])(?=.*[@#$%^&+=])(?=\\S+$).{8,}$");

    private final UserRepository userRepository;
    private final ModelMapper modelMapper;
    private final PasswordEncoder passwordEncoder;

    @Autowired
    public UserServiceImpl(UserRepository userRepository, 
                         ModelMapper modelMapper,
                         PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.modelMapper = modelMapper;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional(readOnly = true)
    public List<UserDto> getAllUsers() {
        try {
            return userRepository.findAll().stream()
                    .map(this::convertToDto)
                    .collect(Collectors.toList());
        } catch (Exception e) {
            logger.error("Error fetching all users: {}", e.getMessage(), e);
            throw new ServiceException("Error occurred while fetching users", e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public UserDto getUserById(Long id) {
        if (id == null) {
            throw new ValidationException("User ID cannot be null");
        }
        
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + id));
        
        // Check if current user has permission to view this user
        checkUserAccess(user);
        
        return convertToDto(user);
    }

    @Override
    @Transactional(readOnly = true)
    public UserDto getUserByUsername(String username) {
        if (StringUtils.isBlank(username)) {
            throw new ValidationException("Username cannot be empty");
        }
        
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with username: " + username));
        
        // Check if current user has permission to view this user
        checkUserAccess(user);
        
        return convertToDto(user);
    }

    @Override
    public UserDto createUser(UserDto userDto) {
        validateUserDto(userDto, true);
        
        if (userRepository.existsByUsername(userDto.getUsername())) {
            throw new DuplicateResourceException("Username is already taken!");
        }
        
        if (userRepository.existsByEmail(userDto.getEmail())) {
            throw new DuplicateResourceException("Email is already in use!");
        }
        
        try {
            User user = convertToEntity(userDto);
            user.setPassword(passwordEncoder.encode(userDto.getPassword()));
            user.setEnabled(true);
            user.setCreatedAt(LocalDateTime.now());
            user.setUpdatedAt(LocalDateTime.now());
            
            // Validate role assignment
            if (user.getRole() == null) {
                user.setRole(Role.USER); // Default role
            }
            
            User savedUser = userRepository.save(user);
            logger.info("Created new user with ID: {}", savedUser.getId());
            
            return convertToDto(savedUser);
        } catch (DataIntegrityViolationException e) {
            logger.error("Data integrity violation while creating user: {}", e.getMessage());
            throw new DataIntegrityException("Could not create user due to data integrity violation", e);
        } catch (Exception e) {
            logger.error("Error creating user: {}", e.getMessage(), e);
            throw new ServiceException("Error occurred while creating user", e);
        }
    }

    @Override
    public UserDto updateUser(Long id, UserDto userDto) {
        if (id == null) {
            throw new ValidationException("User ID cannot be null");
        }
        
        User existingUser = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + id));
        
        // Check if current user has permission to update this user
        checkUserAccess(existingUser);
        
        validateUserDto(userDto, false);
        
        try {
            // Prevent changing username to an existing one
            if (userDto.getUsername() != null && !existingUser.getUsername().equals(userDto.getUsername()) && 
                userRepository.existsByUsername(userDto.getUsername())) {
                throw new DuplicateResourceException("Username is already taken!");
            }
            
            // Prevent changing email to an existing one
            if (userDto.getEmail() != null && !existingUser.getEmail().equals(userDto.getEmail()) && 
                userRepository.existsByEmail(userDto.getEmail())) {
                throw new DuplicateResourceException("Email is already in use!");
            }
            
            // Update fields if they are not null in DTO
            if (userDto.getUsername() != null) {
                existingUser.setUsername(userDto.getUsername());
            }
            if (userDto.getEmail() != null) {
                existingUser.setEmail(userDto.getEmail());
            }
            if (userDto.getPassword() != null) {
                validatePassword(userDto.getPassword());
                existingUser.setPassword(passwordEncoder.encode(userDto.getPassword()));
            }
            if (userDto.getFirstName() != null) {
                existingUser.setFirstName(userDto.getFirstName());
            }
            if (userDto.getLastName() != null) {
                existingUser.setLastName(userDto.getLastName());
            }
            if (userDto.getPhoneNumber() != null) {
                existingUser.setPhoneNumber(userDto.getPhoneNumber());
            }
            
            // Role can only be updated by admins
            if (userDto.getRole() != null && !existingUser.getRole().equals(userDto.getRole())) {
                checkAdminAccess();
                existingUser.setRole(userDto.getRole());
            }
            
            existingUser.setUpdatedAt(LocalDateTime.now());
            
            User updatedUser = userRepository.save(existingUser);
            logger.info("Updated user with ID: {}", updatedUser.getId());
            
            return convertToDto(updatedUser);
        } catch (DataIntegrityViolationException e) {
            logger.error("Data integrity violation while updating user: {}", e.getMessage());
            throw new DataIntegrityException("Could not update user due to data integrity violation", e);
        } catch (Exception e) {
            logger.error("Error updating user: {}", e.getMessage(), e);
            throw new ServiceException("Error occurred while updating user", e);
        }
    }

    @Override
    public void deleteUser(Long id) {
        if (id == null) {
            throw new ValidationException("User ID cannot be null");
        }
        
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + id));
        
        // Check if current user has permission to delete this user
        checkUserAccess(user);
        
        try {
            userRepository.delete(user);
            logger.info("Deleted user with ID: {}", id);
        } catch (Exception e) {
            logger.error("Error deleting user with ID {}: {}", id, e.getMessage(), e);
            throw new ServiceException("Error occurred while deleting user", e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public boolean existsByUsername(String username) {
        if (StringUtils.isBlank(username)) {
            return false;
        }
        return userRepository.existsByUsername(username);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean existsByEmail(String email) {
        if (StringUtils.isBlank(email)) {
            return false;
        }
        return userRepository.existsByEmail(email);
    }

    private UserDto convertToDto(User user) {
        if (user == null) {
            return null;
        }
        
        try {
            UserDto userDto = modelMapper.map(user, UserDto.class);
            userDto.setPassword(null); // Never return password in DTO
            return userDto;
        } catch (Exception e) {
            logger.error("Error converting user to DTO: {}", e.getMessage(), e);
            throw new ConversionException("Error converting user to DTO", e);
        }
    }

    private User convertToEntity(UserDto userDto) {
        if (userDto == null) {
            return null;
        }
        
        try {
            return modelMapper.map(userDto, User.class);
        } catch (Exception e) {
            logger.error("Error converting DTO to user: {}", e.getMessage(), e);
            throw new ConversionException("Error converting DTO to user", e);
        }
    }
    
    private void validateUserDto(UserDto userDto, boolean isNew) {
        if (userDto == null) {
            throw new ValidationException("User data cannot be null");
        }
        
        if (isNew) {
            if (StringUtils.isBlank(userDto.getUsername())) {
                throw new ValidationException("Username is required");
            }
            if (StringUtils.isBlank(userDto.getEmail())) {
                throw new ValidationException("Email is required");
            }
            if (StringUtils.isBlank(userDto.getPassword())) {
                throw new ValidationException("Password is required");
            }
        }
        
        if (userDto.getUsername() != null && userDto.getUsername().length() < 3) {
            throw new ValidationException("Username must be at least 3 characters long");
        }
        
        if (userDto.getEmail() != null && !EMAIL_PATTERN.matcher(userDto.getEmail()).matches()) {
            throw new ValidationException("Invalid email format");
        }
        
        if (userDto.getPassword() != null) {
            validatePassword(userDto.getPassword());
        }
        
        if (userDto.getRole() != null && !Role.isValid(userDto.getRole().name())) {
            throw new ValidationException("Invalid user role");
        }
    }
    
    private void validatePassword(String password) {
        if (password.length() < 8) {
            throw new ValidationException("Password must be at least 8 characters long");
        }
        
        if (!PASSWORD_PATTERN.matcher(password).matches()) {
            throw new ValidationException("Password must contain at least one digit, one lowercase, one uppercase letter, one special character and no whitespace");
        }
    }
    
    private void checkUserAccess(User user) {
        String currentUsername = SecurityContextHolder.getContext().getAuthentication().getName();
        
        // Allow access if the current user is the same as the requested user
        if (user.getUsername().equals(currentUsername)) {
            return;
        }
        
        // Check if current user is admin
        checkAdminAccess();
    }
    
    private void checkAdminAccess() {
        boolean isAdmin = SecurityContextHolder.getContext().getAuthentication()
                .getAuthorities().stream()
                .anyMatch(grantedAuthority -> grantedAuthority.getAuthority().equals("ROLE_ADMIN"));
                
        if (!isAdmin) {
            throw new AccessDeniedException("Access denied. Admin privileges required.");
        }
    }
}
