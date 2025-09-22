package team.avgmax.rabbit.user.entity;

import team.avgmax.rabbit.global.entity.BaseTime;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.experimental.SuperBuilder;

import lombok.NoArgsConstructor;
import team.avgmax.rabbit.user.entity.enums.Role;

@Getter
@SuperBuilder
@MappedSuperclass
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PROTECTED)
public abstract class User extends BaseTime {
    protected String email;
    protected String password;
    protected String name;
    protected String image;
    @Enumerated(EnumType.STRING)
    protected Role role;
    protected String phone;
}