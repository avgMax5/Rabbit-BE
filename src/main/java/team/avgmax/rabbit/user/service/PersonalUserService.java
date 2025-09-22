package team.avgmax.rabbit.user.service;

import lombok.RequiredArgsConstructor;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import team.avgmax.rabbit.ai.service.ChatClientService;
import team.avgmax.rabbit.bunny.dto.response.MatchListResponse;
import team.avgmax.rabbit.bunny.dto.response.MatchResponse;
import team.avgmax.rabbit.bunny.dto.response.OrderListResponse;
import team.avgmax.rabbit.user.dto.request.UpdatePersonalUserRequest;
import team.avgmax.rabbit.user.dto.response.CarrotsResponse;
import team.avgmax.rabbit.user.dto.response.FetchUserResponse;
import team.avgmax.rabbit.user.dto.response.HoldBunniesResponse;
import team.avgmax.rabbit.user.dto.response.HoldBunniesStatsResponse;
import team.avgmax.rabbit.user.dto.response.PersonalUserResponse;
import team.avgmax.rabbit.user.entity.PersonalUser;
import team.avgmax.rabbit.user.entity.enums.Role;
import team.avgmax.rabbit.user.exception.UserError;
import team.avgmax.rabbit.user.exception.UserException;
import team.avgmax.rabbit.user.entity.UserProvider;
import team.avgmax.rabbit.user.entity.enums.ProviderType;
import team.avgmax.rabbit.bunny.entity.Bunny;
import team.avgmax.rabbit.bunny.entity.Match;
import team.avgmax.rabbit.bunny.repository.BunnyRepository;
import team.avgmax.rabbit.bunny.repository.OrderRepository;
import team.avgmax.rabbit.user.entity.HoldBunny;
import team.avgmax.rabbit.user.repository.HoldBunnyRepository;
import team.avgmax.rabbit.user.repository.PersonalUserRepository;
import team.avgmax.rabbit.bunny.repository.MatchRepository;

@Service
@RequiredArgsConstructor
public class PersonalUserService {
    private final ChatClientService chatClientService;

    private final PersonalUserRepository personalUserRepository;
    private final OrderRepository orderRepository;
    private final MatchRepository matchRepository;
    private final HoldBunnyRepository holdBunnyRepository;
    private final BunnyRepository bunnyRepository;

    @Transactional
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
    public FetchUserResponse fetchUserById(String personalUserId) {
        PersonalUser personalUser = findPersonalUserById(personalUserId);
        String myBunnyName = bunnyRepository.findByUserId(personalUserId)
            .map(Bunny::getBunnyName)
            .orElse(null);

        return FetchUserResponse.from(personalUser, myBunnyName);
    }

    @Transactional(readOnly = true)
    public PersonalUserResponse getUserById(String personalUserId) {
        PersonalUser personalUser = findPersonalUserById(personalUserId);

        return PersonalUserResponse.from(personalUser);
    }

    @Transactional
    public PersonalUserResponse updateUserById(String personalUserId, UpdatePersonalUserRequest request) {
        PersonalUser personalUser = findPersonalUserById(personalUserId);
        
        personalUser.updatePersonalUser(request);
        PersonalUser savedUser = personalUserRepository.save(personalUser);

        String aiReview = chatClientService.getAiReviewOfUserProfile(savedUser);
        savedUser.updateAiReview(aiReview);

        return PersonalUserResponse.from(savedUser);
    }

    @Transactional(readOnly = true)
    public CarrotsResponse getCarrotsById(String personalUserId) {
        PersonalUser personalUser = findPersonalUserById(personalUserId);
        
        return CarrotsResponse.from(personalUser);
    }

    @Transactional(readOnly = true)
    public HoldBunniesResponse getBunniesById(String personalUserId) {
        return holdBunnyRepository.findHoldBunniesByUserId(personalUserId);
    }

    @Transactional(readOnly = true)
    public HoldBunniesStatsResponse getBunniesStatsById(String personalUserId) {
        PersonalUser personalUser = findPersonalUserById(personalUserId);
        List<HoldBunny> holdBunnies = holdBunnyRepository.findByHolder(personalUser);

        return HoldBunniesStatsResponse.from(holdBunnies);
    }

    @Transactional(readOnly = true)
    public OrderListResponse getOrdersById(String personalUserId) {
        return orderRepository.findOrdersByUserId(personalUserId);
    }

    @Transactional(readOnly = true)
    public PersonalUser findPersonalUserById(String userId) {
        return personalUserRepository.findById(userId)
                .orElseThrow(() -> new UserException(UserError.USER_NOT_FOUND));
    }

    @Transactional(readOnly = true)
    public MatchListResponse getMatchesById(String personalUserId){
        List<Match> matches = matchRepository.findMatchesByUserId(personalUserId);
        List<MatchResponse> matchResponses = matches.stream()
            .map(match -> MatchResponse.from(match, personalUserId))
            .toList();
   
        return MatchListResponse.from(matchResponses);
    }
}