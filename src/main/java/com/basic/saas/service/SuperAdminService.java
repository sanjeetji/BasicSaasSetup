package com.basic.saas.service;

import com.basic.saas.model.dto.SuperAdminLoginDto;
import com.basic.saas.model.dto.SuperAdminLoginResponse;
import com.basic.saas.model.dto.SuperAdminRegistrationDto;
import com.basic.saas.model.dto.SuperAdminRegistrationResponse;
import com.basic.saas.model.entity.Client;
import com.basic.saas.model.entity.RefreshToken;
import com.basic.saas.model.entity.SuperAdmin;
import com.basic.saas.model.entity.User;
import com.basic.saas.repository.ClientRepository;
import com.basic.saas.repository.SuperAdminRepository;
import com.basic.saas.repository.UserRepository;
import com.basic.saas.session.SessionStore;
import com.basic.saas.utils.Constant;
import com.basic.saas.utils.ValidateInputs;
import com.basic.saas.utils.globalExceptionHandller.CustomBusinessException;
import com.basic.saas.utils.globalExceptionHandller.ErrorCode;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class SuperAdminService implements UserDetailsService {

    private final JwtService jwtService;
    private final PasswordEncoder encoder;
    private final SessionStore sessionStore;
    private final SuperAdminRepository repository;
    private final ClientRepository clientRepository;
    private final UserRepository userRepository;
    private final ValidateInputs validateInputs;
    private final RefreshTokenService refreshService;

    public SuperAdminService(SuperAdminRepository repository,
                             PasswordEncoder encoder,
                             RefreshTokenService refreshService,
                             JwtService jwtService,
                             ValidateInputs validateInputs,
                             SessionStore sessionStore,
                             ClientRepository clientRepository,
                             UserRepository userRepository) {
        this.repository = repository;
        this.encoder = encoder;
        this.refreshService = refreshService;
        this.jwtService = jwtService;
        this.validateInputs = validateInputs;
        this.sessionStore = sessionStore;
        this.clientRepository = clientRepository;
        this.userRepository = userRepository;
    }

    public SuperAdminRegistrationResponse register(SuperAdminRegistrationDto dto) {
        try {
            validateInputs.handleSuperAdminRegistrationInput(dto);
            Optional<SuperAdmin> savedSuperAdmin = repository.findByEmail(dto.email());
            if (savedSuperAdmin.isPresent()){
                throw new CustomBusinessException(ErrorCode.USER_IS_ALREADY_REGISTER, HttpStatus.CONFLICT);
            }
            SuperAdmin superAdmin = new SuperAdmin();
            superAdmin.setName(dto.name());
            superAdmin.setEmail(dto.email());
            superAdmin.setPhoneNumber(dto.phoneNumber());
            superAdmin.setPassword(encoder.encode(dto.password()));
            superAdmin = repository.save(superAdmin);
            return new SuperAdminRegistrationResponse(
                    superAdmin.getName(),
                    superAdmin.getEmail(),
                    superAdmin.getPhoneNumber(),
                    superAdmin.getCreatedAt().toString()
            );
        }catch (Exception e){
            throw new CustomBusinessException(ErrorCode.FAILED_TO_REGISTER, HttpStatus.BAD_REQUEST,e);
        }
    }

    public SuperAdminLoginResponse login(SuperAdminLoginDto dto) {
        try {
            validateInputs.handleSuperAdminLoginInput(dto);
            SuperAdmin superAdmin = (SuperAdmin) loadUserByUsername(dto.email()); // Use loadUserByUsername for consistency

            if (!encoder.matches(dto.password(), superAdmin.getPassword())) {
                throw new CustomBusinessException(ErrorCode.FAILED_TO_LOGIN, HttpStatus.BAD_REQUEST, "Password is not matching");
            }

            // Rotate session ID to invalidate old access tokens
            String sid = UUID.randomUUID().toString();
            sessionStore.setSid(superAdmin.getEmail(), sid, Duration.ofDays(30));

            RefreshToken refresh = refreshService.createRefreshToken(superAdmin, sid);
            String access = jwtService.generateAccessToken(superAdmin.getEmail(), 0L, superAdmin.getId(), Constant.ROLE_SUPER_ADMIN, sid);
            superAdmin.setAccessToken(access);
            superAdmin.setToken(refresh.getToken());
            superAdmin = repository.save(superAdmin);
            return new SuperAdminLoginResponse(
                    superAdmin.getToken(),
                    superAdmin.getAccessToken(),
                    superAdmin.getName(),
                    superAdmin.getEmail(),
                    superAdmin.getPhoneNumber(),
                    superAdmin.getCreatedAt().toString()
            );
        } catch (Exception e) {
            throw new CustomBusinessException(ErrorCode.FAILED_TO_LOGIN, HttpStatus.BAD_REQUEST, e);
        }
    }

    @Cacheable("superAdmins")
    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        return repository.findByEmail(email).orElseThrow(() -> new UsernameNotFoundException("User not found with email: " + email));
    }

    @Cacheable("superAdmins")
    public SuperAdmin findById(Long id) {
        return repository.findById(id).orElse(null);
    }

    @Cacheable("clients")
    public Client fetchClientById(Long id) {
        return clientRepository.findById(id).orElse(null);
    }

    public List<Client> fetchAllClients() {
        return clientRepository.findAll();
    }

    public List<User> getAllUsers() {return userRepository.findAll();}

    public List<User> getUsersByClientId(Long clientId) {return userRepository.findByClientId(clientId).orElse(Collections.emptyList());}

    public User fetchUserById(Long id) {return userRepository.findById(id).orElse(null);}


}
