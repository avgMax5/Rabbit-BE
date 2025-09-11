package team.avgmax.rabbit.user.service;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import team.avgmax.rabbit.user.dto.request.UpdatePersonalUserRequest;
import team.avgmax.rabbit.user.dto.response.CarrotsResponse;
import team.avgmax.rabbit.user.dto.response.HoldBunniesResponse;
import team.avgmax.rabbit.user.dto.response.OrdersResponse;
import team.avgmax.rabbit.user.dto.response.PersonalUserResponse;
import team.avgmax.rabbit.user.entity.PersonalUser;
import team.avgmax.rabbit.user.entity.enums.Role;
import team.avgmax.rabbit.user.exception.UserError;
import team.avgmax.rabbit.user.exception.UserException;
import team.avgmax.rabbit.user.entity.UserProvider;
import team.avgmax.rabbit.user.entity.enums.ProviderType;
import team.avgmax.rabbit.user.repository.PersonalUserRepository;
import team.avgmax.rabbit.user.repository.custom.HoldBunnyRepositoryCustomImpl;
import team.avgmax.rabbit.user.repository.custom.OrderRepositoryCustomImpl;

@Service
@RequiredArgsConstructor
@Transactional
public class PersonalUserService {

    private final PersonalUserRepository personalUserRepository;
    private final OrderRepositoryCustomImpl orderRepositoryCustom;
    private final HoldBunnyRepositoryCustomImpl holdBunnyRepositoryCustom;

    public PersonalUser findOrCreateUser(String email, String name, String registrationId, String providerId) {
        PersonalUser user = personalUserRepository.findByEmail(email)
                .orElseGet(() -> personalUserRepository.save(
                        PersonalUser.builder()
                                .email(email)
                                .name(name)
                                .role(Role.ROLE_USER)
                                .build()
                ));

        ProviderType providerType = ProviderType.from(registrationId);

        boolean exists = user.getProviders().stream()
                .anyMatch(p -> p.getProviderType() == providerType);

        if (!exists) {
            UserProvider userProvider = UserProvider.of(providerType, providerId);
            user.addProvider(userProvider);
        }

        return user;
    }

    @Transactional(readOnly = true)
    public PersonalUserResponse getUserById(String personalUserId) {
        PersonalUser personalUser = findPersonalUserById(personalUserId);

        return PersonalUserResponse.from(personalUser);
    }

    public PersonalUserResponse updateUserById(String personalUserId, UpdatePersonalUserRequest request) {
        PersonalUser personalUser = findPersonalUserById(personalUserId);
        
        personalUser.updatePersonalUser(request);
        PersonalUser savedUser = personalUserRepository.save(personalUser);

        return PersonalUserResponse.from(savedUser);
    }

    @Transactional(readOnly = true)
    public CarrotsResponse getCarrotsById(String personalUserId) {
        PersonalUser personalUser = findPersonalUserById(personalUserId);
        
        return CarrotsResponse.from(personalUser);
    }

    @Transactional(readOnly = true)
    public HoldBunniesResponse getBunniesById(String personalUserId) {
        return holdBunnyRepositoryCustom.findHoldBunniesByUserId(personalUserId);
    }

    @Transactional(readOnly = true)
    public OrdersResponse getOrdersById(String personalUserId) {
        return orderRepositoryCustom.findOrdersByUserId(personalUserId);
    }

    @Transactional(readOnly = true)
    public PersonalUser findPersonalUserById(String userId) {
        return personalUserRepository.findById(userId)
                .orElseThrow(() -> new UserException(UserError.USER_NOT_FOUND));
    }
}