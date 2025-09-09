package com.basic.saas.service;

import com.basic.saas.model.dto.ClientLoginDto;
import com.basic.saas.model.dto.ClientLoginResponse;
import com.basic.saas.model.dto.ClientRegistrationDto;
import com.basic.saas.model.dto.ClientRegistrationResponse;
import com.basic.saas.model.entity.Client;
import com.basic.saas.model.entity.RefreshToken;
import com.basic.saas.model.entity.User;
import com.basic.saas.repository.ClientRepository;
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
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class ClientService implements UserDetailsService {

    private final JwtService jwtService;
    private final UserService userService;
    private final PasswordEncoder encoder;
    private final SessionStore sessionStore;
    private final ClientRepository repository;
    private final ValidateInputs validateInputs;
    private final RefreshTokenService refreshService;

    public ClientService(ClientRepository repository,
                         PasswordEncoder encoder,
                         RefreshTokenService refreshService,
                         JwtService jwtService,
                         ValidateInputs validateInputs,
                         SessionStore sessionStore,
                         UserService userService) {
        this.repository = repository;
        this.encoder = encoder;
        this.refreshService = refreshService;
        this.jwtService = jwtService;
        this.validateInputs = validateInputs;
        this.sessionStore = sessionStore;
        this.userService = userService;
    }

    public ClientRegistrationResponse register(ClientRegistrationDto dto) {
        try {
            validateInputs.handleClientRegistrationInput(dto);
            Optional<Client> savedClient = repository.findByEmail(dto.email());
            if (savedClient.isPresent()){
                throw new CustomBusinessException(ErrorCode.USER_IS_ALREADY_REGISTER, HttpStatus.CONFLICT);
            }
            Client client = new Client();
            client.setName(dto.name());
            client.setEmail(dto.email());
            client.setPhoneNumber(dto.phoneNumber());
            client.setPassword(encoder.encode(dto.password()));
            client = repository.save(client);
            return new ClientRegistrationResponse(
                    client.getName(),
                    client.getEmail(),
                    client.getPhoneNumber(),
                    client.getClientApiKey(),
                    client.getCreatedAt().toString(),
                    client.isActive()
            );
        }catch (Exception e){
            throw new CustomBusinessException(ErrorCode.FAILED_TO_REGISTER, HttpStatus.BAD_REQUEST,e);
        }
    }

    public ClientLoginResponse login(ClientLoginDto dto) {
        try {
            validateInputs.handleClientLoginInput(dto);
            Client client = (Client) loadUserByUsername(dto.email()); // Use loadUserByUsername for consistency

            if (!encoder.matches(dto.password(), client.getPassword())) {
                throw new CustomBusinessException(ErrorCode.FAILED_TO_LOGIN, HttpStatus.BAD_REQUEST, "Password is not matching");
            }

            // Rotate session ID to invalidate old access tokens
            String sid = UUID.randomUUID().toString();
            sessionStore.setSid(client.getEmail(), sid, Duration.ofDays(30));

            RefreshToken refresh = refreshService.createRefreshToken(client, sid);
            String access = jwtService.generateAccessToken(client.getEmail(), client.getId(), 0L, Constant.ROLE_CLIENT, sid);

            client.setAccessToken(access);
            client.setToken(refresh.getToken());
            client = repository.save(client);

            return new ClientLoginResponse(
                    access,
                    refresh.getToken(),
                    client.getName(),
                    client.getEmail(),
                    client.getPhoneNumber(),
                    client.getClientApiKey(),
                    client.getCreatedAt().toString(),
                    client.isActive()
            );
        } catch (Exception e) {
            throw new CustomBusinessException(ErrorCode.FAILED_TO_LOGIN, HttpStatus.BAD_REQUEST, e);
        }
    }

    @Cacheable("clients")
    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        return repository.findByEmail(email).orElseThrow(() -> new UsernameNotFoundException("Client not found with email: " + email));
    }

    @Cacheable("clients")
    public Client findById(Long id) {
        return repository.findById(id).orElse(null);
    }

    public List<Client> getAllClients() {
        return repository.findAll();
    }

    public List<User> fetchUsers(Long clientId) {
        return userService.fetchUsers(clientId);
    }

    public User fetchUsersById(Long clientId, Long id) {
        return userService.fetchUserByClientIdAndUserId(clientId, id);
    }


}
