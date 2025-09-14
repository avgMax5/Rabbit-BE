package team.avgmax.rabbit.auth.oauth2;

import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;

import team.avgmax.rabbit.user.entity.PersonalUser;

import java.util.Map;
import java.util.Collections;


public class CustomOAuth2User extends DefaultOAuth2User {

    private final PersonalUser personalUser;

    public CustomOAuth2User(PersonalUser personalUser, Map<String, Object> attributes, String nameAttributeKey) {
        super(
            Collections.singleton(new SimpleGrantedAuthority(personalUser.getRole().name())),
            attributes,
            nameAttributeKey
        );
        this.personalUser = personalUser;
    }

    public String getEmail() {
        return personalUser.getEmail();
    }

    public String getName() {
        return personalUser.getName();
    }

    public String getRole() {
        return personalUser.getRole().name();
    }

    public PersonalUser getPersonalUser() {
        return personalUser;
    }
}