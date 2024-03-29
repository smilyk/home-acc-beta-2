package usersservice.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.modelmapper.ModelMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import usersservice.enums.ErrorMessages;
import usersservice.enums.InterfaceLang;
import usersservice.service.hystrix.SendMail;
import usersservice.models.entity.PasswordResetTokenEntity;
import usersservice.repository.PasswordResetTokenRepository;
import usersservice.utils.Utils;
import usersservice.dto.UserDto;
import usersservice.exceptions.UserServiceException;
import usersservice.models.entity.UserEntity;
import usersservice.repository.UserRepository;
import org.springframework.security.core.userdetails.User;


import java.util.ArrayList;
import java.util.List;


@Service
public class UserServiceImpl implements UserService {
    private static final Logger LOGGER = LoggerFactory.getLogger(UserServiceImpl.class);
    private ObjectMapper mapper = new ObjectMapper();

    @Value("${super.admin.email}")
    private String superAdminEmail;
    @Value("${super.admin.password}")
    private String superAdminencryptedPassword;
    @Value("${super.admin.id}")
    private String superAdminId;
    ModelMapper modelMapper = new ModelMapper();
    @Autowired
    UserRepository userRepository;
    @Autowired
    PasswordResetTokenRepository passwordResetTokenRepository;
    @Autowired
    Utils utils;
    @Autowired
    BCryptPasswordEncoder bCryptPasswordEncoder;
    @Autowired
    SendMail sendMail;

    @Override
    public UserEntity createSuperAdmin(){

        UserEntity userFromDB = userRepository.findByUserId(superAdminId);
        if(userFromDB != null){
            return userFromDB;
        }

        UserEntity superAdmin = new UserEntity();
        superAdmin.setEmailVerificationToken(null);
        superAdmin.setEmail(superAdminEmail);
        superAdmin.setEncryptedPassword(superAdminencryptedPassword);
        superAdmin.setEmailVerificationStatus(true);
        superAdmin.setFirstName("Super");
        superAdmin.setLastName("Admin");
        superAdmin.setUserId(superAdminId);
        superAdmin.setLang(InterfaceLang.RUS);
        userRepository.save(superAdmin);
        LOGGER.info("super admin created");
        return superAdmin;
    }

    @Override
    public UserDto createUser(UserDto user) throws JsonProcessingException {

        if (userRepository.findByEmail(user.getEmail()) != null) {
            throw new UserServiceException(ErrorMessages.USER_ALREADY_EXISTS.name());
        }
        UserEntity userEntity = modelMapper.map(user, UserEntity.class);

        String publicUserId = utils.generateUserId(30);
        userEntity.setUserId(publicUserId);
        userEntity.setEncryptedPassword(bCryptPasswordEncoder.encode(user.getPassword()));
        userEntity.setEmailVerificationToken(utils.generateEmailVerificationToken(publicUserId));

        UserEntity storedUserDetails = userRepository.save(userEntity);
        UserDto returnValue = modelMapper.map(storedUserDetails, UserDto.class);
        Boolean sendingResult = sendMail.verifyEmail(returnValue);
        LOGGER.info("verificatiWebSecurity on email was send to email " + storedUserDetails.getEmail());
        return returnValue;
    }

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        UserEntity userEntity = userRepository.findByEmail(email);
        if (userEntity == null)
            throw new UsernameNotFoundException(email);
        /**
         * если userEntity.getEmailVerificationStatus() == false -> login вернет error
         */
        return new User(userEntity.getEmail(), userEntity.getEncryptedPassword(), userEntity.getEmailVerificationStatus(),
                true, true, true, new ArrayList<>());
    }

    @Override
    public UserDto getUser(String email) {
        UserEntity userEntity = userRepository.findByEmail(email);

        if (userEntity == null)
            throw new UserServiceException(ErrorMessages.AUTHENTICATION_FAILED + email);

        UserDto returnValue = modelMapper.map(userEntity, UserDto.class);
        return returnValue;
    }

    @Override
    public UserDto getUserByUserId(String userId) {
        UserDto returnValue = new UserDto();
        UserEntity userEntity = userRepository.findByUserId(userId);
        if (userEntity == null)
            throw new UsernameNotFoundException("User with ID: " + userId + " not found");
        returnValue = modelMapper.map(userEntity, UserDto.class);
        return returnValue;
    }


    @Override
    public List<UserDto> getUsers(int page, int limit) {
        if (page > 0) page = page - 1;
        Pageable pageableRequest = PageRequest.of(page, limit);
        Page<UserEntity> usersPage = userRepository.findAll(pageableRequest);
        List<UserEntity> users = usersPage.getContent();

        List<UserDto> returnValue = new ArrayList<>();
        users.stream().map(user -> toDto(user)).forEachOrdered(returnValue::add);

        return returnValue;
    }

    @Override
    public UserDto updateUser(String userId, UserDto userDto) {
        UserDto returnValue = new UserDto();

        UserEntity userEntity = userRepository.findByUserId(userId);

        if (userEntity == null)
            throw new UserServiceException(ErrorMessages.NO_RECORD_FOUND.getErrorMessage());

        userEntity.setFirstName(userDto.getFirstName());
        userEntity.setLastName(userDto.getLastName());

        UserEntity updatedUserDetails = userRepository.save(userEntity);
        returnValue = new ModelMapper().map(updatedUserDetails, UserDto.class);

        return returnValue;
    }

    private UserDto toDto(UserEntity userEntitty) {
        UserDto userDto = modelMapper.map(userEntitty, UserDto.class);
        return userDto;
    }

    @Transactional
    @Override
    public void deleteUser(String userId) {
        UserEntity userEntity = userRepository.findByUserId(userId);

        if (userEntity == null)
            throw new UserServiceException(ErrorMessages.NO_RECORD_FOUND.getErrorMessage());
        userRepository.delete(userEntity);
    }

    @Override
    public boolean verifyEmailToken(String token) {
        boolean returnValue = false;
        // Find user by token
        UserEntity userEntity = userRepository.findUserByEmailVerificationToken(token);
// если tokrn есть, значит верификация не пройдена.
        if (userEntity != null) {
            boolean hastokenExpired = Utils.hasTokenExpired(token);
            if (!hastokenExpired) {
                userEntity.setEmailVerificationToken(null);
                userEntity.setEmailVerificationStatus(Boolean.TRUE);
                userRepository.save(userEntity);
                returnValue = true;
            }
        }
        return returnValue;
    }

    /**
     * изменение пароля
     * @param token
     * @param password
     * @return
     */
    @Override
    public boolean resetPassword(String token, String password) {
        boolean returnValue = false;

        if( Utils.hasTokenExpired(token) )
        {
            return returnValue;
        }
        PasswordResetTokenEntity passwordResetTokenEntity = passwordResetTokenRepository.findByToken(token);
        if (passwordResetTokenEntity == null) {
            return returnValue;
        }
        // Prepare new password
        String encodedPassword = bCryptPasswordEncoder.encode(password);
        // Update User password in database
        UserEntity userEntity = passwordResetTokenEntity.getUserDetails();
        userEntity.setEncryptedPassword(encodedPassword);
        UserEntity savedUserEntity = userRepository.save(userEntity);
        // Verify if password was saved successfully
        if (savedUserEntity != null && savedUserEntity.getEncryptedPassword().equalsIgnoreCase(encodedPassword)) {
            returnValue = true;
        }
        // Remove Password Reset token from database
        passwordResetTokenRepository.delete(passwordResetTokenEntity);
        return returnValue;
    }

    /**
     * запрос на обноелние пароля
     * @param email
     * @return
     */
    @Override
    public boolean requestPasswordReset(String email) {

        boolean returnValue = false;

        UserEntity userEntity = userRepository.findByEmail(email);

        if (userEntity == null) {
            return returnValue;
        }

        String token = new Utils().generatePasswordResetToken(userEntity.getUserId());

        PasswordResetTokenEntity passwordResetTokenEntity = new PasswordResetTokenEntity();
        passwordResetTokenEntity.setToken(token);
        passwordResetTokenEntity.setUserDetails(userEntity);
        passwordResetTokenRepository.save(passwordResetTokenEntity);
//        отправляем письмо. Если письмоотправлено - вернется True
        returnValue = new SendMail().sendPasswordResetRequest(
                userEntity.getFirstName(),
                userEntity.getEmail(),
                token);

        return returnValue;
    }


}
