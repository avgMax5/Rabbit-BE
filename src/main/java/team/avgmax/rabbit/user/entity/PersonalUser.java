package team.avgmax.rabbit.user.entity;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;
import lombok.*;
import lombok.experimental.SuperBuilder;
import team.avgmax.rabbit.global.util.UlidGenerator;
import team.avgmax.rabbit.user.dto.request.UpdatePersonalUserRequest;
import team.avgmax.rabbit.user.entity.enums.Position;
import team.avgmax.rabbit.user.exception.UserException;
import team.avgmax.rabbit.user.exception.UserError;
import team.avgmax.rabbit.user.entity.enums.Role;

@Entity
@Getter
@SuperBuilder
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PersonalUser extends User {

    @Id
    @Column(name = "personal_user_id", length = 26, updatable = false, nullable = false)
    @Builder.Default
    private String id = UlidGenerator.generateMonotonic();

    @Builder.Default
    @OneToMany(mappedBy = "personalUser", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<UserProvider> providers = new ArrayList<>();

    private String resume;

    private String portfolio;

    @Enumerated(EnumType.STRING)
    private Position position;

    @Builder.Default
    @Column(precision = 20)
    private BigDecimal carrot = BigDecimal.valueOf(50_000_000);

    private LocalDate birthdate;

    @Builder.Default
    @JoinColumn(name = "user_id")
    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Sns> sns = new ArrayList<>();

    @Builder.Default
    @JoinColumn(name = "user_id")
    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Skill> skill = new ArrayList<>();

    @Builder.Default
    @JoinColumn(name = "user_id")
    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Certification> certification = new ArrayList<>();

    @Builder.Default
    @JoinColumn(name = "user_id")
    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Career> career = new ArrayList<>();

    @Builder.Default
    @JoinColumn(name = "user_id")
    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Education> education = new ArrayList<>();

    String aiReview;

    LocalDateTime specUpdatedAt;

    // === 연관관계 편의 메서드 ===
    public void addProvider(UserProvider provider) {
        providers.add(provider);
        provider.setPersonalUser(this);
    }

    public void addCarrot(BigDecimal carrot) {
        this.carrot = this.carrot.add(carrot);
    }

    public void subtractCarrot(BigDecimal carrot) {
        if (this.carrot.compareTo(carrot) < 0) {
            throw new UserException(UserError.CARROT_NOT_ENOUGH);
        }
        this.carrot = this.carrot.subtract(carrot);
    }

    public void updateRoleToBunny() {
        this.role = Role.ROLE_BUNNY;
    }

    public void updatePersonalUser(UpdatePersonalUserRequest request) {
        this.name = request.name();
        this.image = request.image();
        this.email = request.email();
        this.birthdate = request.birthdate();
        this.resume = request.resume();
        this.portfolio = request.portfolio();
        this.position = request.position();
        
        // SNS 업데이트
        this.sns.clear();
        this.sns.addAll(request.link().stream()
            .map(Sns::create)
            .toList());
    
        // Skill 업데이트
        this.skill.clear();
        this.skill.addAll(request.skill().stream()
            .map(Skill::create)
            .toList());
    
        // Certification 업데이트
        this.certification.clear();
        this.certification.addAll(request.certification().stream()
            .map(Certification::create)
            .toList());
        
        // Career 업데이트
        this.career.clear();
        this.career.addAll(request.career().stream()
            .map(Career::create)
            .toList());
    
        // Education 업데이트
        this.education.clear();
        this.education.addAll(request.education().stream()
            .map(Education::create)
            .toList());

        this.specUpdatedAt = LocalDateTime.now();
    }

    public void updateAiReview(String aiReview) {
        this.aiReview = aiReview;
    }
}
