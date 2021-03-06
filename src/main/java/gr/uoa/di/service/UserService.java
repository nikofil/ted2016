package gr.uoa.di.service;

import gr.uoa.di.dao.UserEntity;
import gr.uoa.di.dto.user.SellerResponseDto;
import gr.uoa.di.dto.user.UserLoginDto;
import gr.uoa.di.dto.user.UserRegisterDto;
import gr.uoa.di.dto.user.UserResponseDto;
import gr.uoa.di.exception.user.*;
import gr.uoa.di.mapper.UserMapper;
import gr.uoa.di.repo.UserRepository;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.AuthenticationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.ResponseStatus;

import java.io.UnsupportedEncodingException;
import java.security.NoSuchAlgorithmException;
import java.util.*;

@Service
@Transactional
public class UserService {

    @Autowired
    private UserRepository userRepo;

    @Autowired
    private UserMapper userMapper;

    @Value("${secret_key}")
    private String secretKey;

    public void register(UserRegisterDto dto)
            throws UnsupportedEncodingException, NoSuchAlgorithmException {
        /* security checks */
        if (!dto.getPassword().equals(dto.getPassword2())) {
            throw new UserService.PasswordMatchingException();
        } else if (dto.getPassword().length() < 8) {
            throw new UserService.PasswordLengthException();
        } else if (!dto.getEmail().contains("@")) {
            throw new UserService.EmailException();
        } else if (userRepo.findOneByUsername(dto.getUsername()) != null) {
            throw new UserAlreadyExistsException();
        }
        UserEntity user = userMapper.mapUserRegisterDtoToUserEntity(dto);
        /* create a salt and encrypt the password */
        String salt = Long.toHexString(new Random().nextLong());
        String encPass = Utils.sha1(Utils.sha1(dto.getPassword()) + salt);
        user.setPassword(encPass);
        user.setSalt(salt);
        userRepo.save(user);
    }

    public Map<String, String> login(UserLoginDto request)
            throws UnsupportedEncodingException, NoSuchAlgorithmException {
        try {
            /* check if credentials match */
            UserEntity user = getUserEntity(request.getUsername());
            String encPass = Utils.sha1(Utils.sha1(request.getPassword()) + user.getSalt());
            if (!encPass.equals(user.getPassword())) {
                throw new UserLoginException();
            } else if (!user.getValidated()) {
                throw new UserNotValidatedException();
            }
            /* create a JWT with the necessary user info */
            return Collections.singletonMap("jwt", createJwt(user));
        } catch (UserNotFoundException ex) {
            throw new UserLoginException();
        }
    }

    public UserResponseDto getUser(int id) {
        return userMapper.mapUserEntityToUserResponseDto(getUserEntity(id));
    }

    public SellerResponseDto getSeller(String username) {
        return userMapper.mapUserEntityToSellerResponseDto(getUserEntity(username));
    }

    public Page<UserResponseDto> getValidatedUsers(String username, Pageable pageable) {
        Page<UserEntity> users;
        if (username == null || username.length() == 0) {
            users = userRepo.findByValidatedTrue(pageable);
        } else {
            users = userRepo.findByValidatedTrueAndUsernameLike("%" + username + "%", pageable);
        }
        return userMapper.mapUserEntityPageToUserResponseDtoPage(users);
    }

    public Page<UserResponseDto> getNotValidatedUsers(String username, Pageable pageable) {
        Page<UserEntity> users;
        if (username == null || username.length() == 0) {
            users = userRepo.findByValidatedFalse(pageable);
        } else {
            users = userRepo.findByValidatedFalseAndUsernameLike("%" + username + "%", pageable);
        }
        return userMapper.mapUserEntityPageToUserResponseDtoPage(users);
    }

    public void validateUser(int userId) {
        UserEntity user = getNotValidatedUserEntity(userId);
        user.setValidated(true);
        userRepo.save(user);
    }

    public void deleteUser(int userId) {
        userRepo.delete(getNotValidatedUserEntity(userId));
    }

    public UserEntity getUserEntity(int userId) {
        UserEntity user = userRepo.findOneById(userId);
        if (user == null) {
            throw new UserNotFoundException();
        }
        return user;
    }

    public UserEntity getUserEntity(String username) {
        UserEntity user = userRepo.findOneByUsername(username);
        if (user == null) {
            throw new UserNotFoundException();
        }
        return user;
    }

    private UserEntity getNotValidatedUserEntity(int userId) {
        UserEntity user = getUserEntity(userId);
        if (user.getValidated()) {
            throw new UserAlreadyValidatedException();
        }
        return user;
    }

    @ResponseStatus(code = HttpStatus.BAD_REQUEST)
    public class EmailException extends AuthenticationException {
        public EmailException() {
            super("Invalid email");
        }
    }

    @ResponseStatus(code = HttpStatus.BAD_REQUEST)
    public class PasswordMatchingException extends AuthenticationException {
        public PasswordMatchingException() {
            super("Passwords don't match");
        }
    }

    @ResponseStatus(code = HttpStatus.BAD_REQUEST)
    public class PasswordLengthException extends AuthenticationException {
        public PasswordLengthException() {
            super("Password is too short");
        }
    }

    private String createJwt(UserEntity user) {
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.MONTH, 1);

        Map<String, Object> claims = new HashMap<>();
        claims.put("user", user.getUsername());
        claims.put("admin", user.getAdmin() != null && user.getAdmin());

        return Jwts.builder()
                .setClaims(claims)
                .setSubject("TED")
                .setExpiration(cal.getTime())
                .signWith(SignatureAlgorithm.HS512, secretKey)
                .compact();
    }

}
