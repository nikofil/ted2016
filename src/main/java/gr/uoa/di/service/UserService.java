package gr.uoa.di.service;

import gr.uoa.di.dao.UserEntity;
import gr.uoa.di.dto.LoginDto;
import gr.uoa.di.dto.RegisterDto;
import gr.uoa.di.exception.user.*;
import gr.uoa.di.repo.UserRepository;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.AuthenticationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.ResponseStatus;

import java.io.UnsupportedEncodingException;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;

@Service
@Transactional
public class UserService {

    @Autowired
    private UserRepository userRepo;

    @Value("${secret_key}")
    private String secretKey;

    public void register(RegisterDto request)
            throws UnsupportedEncodingException, NoSuchAlgorithmException {
        if (!request.getPassword().equals(request.getPassword2())) {
            throw new UserService.PasswordMatchingException();
        } else if (request.getPassword().length() < 8) {
            throw new UserService.PasswordLengthException();
        } else if (!request.getEmail().contains("@")) {
            throw new UserService.EmailException();
        } else if (userRepo.findOneByUsername(request.getUsername()) != null) {
            throw new UserAlreadyExistsException();
        }
        UserEntity dto = new UserEntity();
        dto.setUsername(request.getUsername());
        String salt = Long.toHexString(new Random().nextLong());
        String encPass = sha1(sha1(request.getPassword()) + salt);
        dto.setPassword(encPass);
        dto.setSalt(salt);
        dto.setName(request.getName());
        dto.setSurname(request.getSurname());
        dto.setEmail(request.getEmail());
        dto.setPhone(request.getTelephone());
        dto.setLocation(request.getAddress());
        dto.setCountry(request.getCountry());
        dto.setLat(request.getLatitude());
        dto.setLon(request.getLongitude());
        dto.setAfm(request.getAfm());
        userRepo.save(dto);
    }

    public Map<String, String> login(LoginDto request)
            throws UnsupportedEncodingException, NoSuchAlgorithmException {
        UserEntity user = getUser(request.getUsername());
        String encPass = sha1(sha1(request.getPassword()) + user.getSalt());
        if (!encPass.equals(user.getPassword())) {
            throw new UserLoginException();
        } else if (!user.getValidated()) {
            throw new UserNotValidatedException();
        }

        return Collections.singletonMap("jwt", createJwt(user));
    }

    public List<UserEntity> getUsers() {
        return userRepo.findAll();
    }

    public List<UserEntity> getNotValidatedUsers() {
        return userRepo.findByValidatedFalse();
    }

    public void validateUser(int userId) {
        UserEntity user = getUser(userId);
        if (user.getValidated()) {
            throw new UserAlreadyValidatedException();
        }
        user.setValidated(true);
        userRepo.save(user);
    }

    public UserEntity getUser(int userId) {
        UserEntity user = userRepo.findOneById(userId);
        if (user == null) {
            throw new UserNotFoundException();
        }
        return user;
    }

    public UserEntity getUser(String username) {
        UserEntity user = userRepo.findOneByUsername(username);
        if (user == null) {
            throw new UserNotFoundException();
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

    private static String sha1(String text) throws NoSuchAlgorithmException, UnsupportedEncodingException {
        MessageDigest crypt = MessageDigest.getInstance("SHA-1");
        crypt.reset();
        crypt.update(text.getBytes("UTF-8"));

        return new BigInteger(1, crypt.digest()).toString(16);
    }
}
