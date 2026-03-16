package com.ssafy.naeda.domain.user.dto.response;


import com.ssafy.naeda.domain.user.entity.User;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class UserResponse {
    private Long userNo;
    private String userId;
    private String username;
    private String phone;
    private boolean faceRegistered;
    private boolean secondaryAuthEnabled;

    public static UserResponse from(User user){
        return UserResponse.builder()
                .userNo(user.getUserNo())
                .userId(user.getUserId())
                .username(user.getUsername())
                .phone(user.getPhone())
                .faceRegistered(user.getFaceRegistered())
                .secondaryAuthEnabled(user.getSecondaryAuthEnabled())
                .build();
    }

    //주의: password, residentNo, pinPassword, userKey 같은 민감정보는 절대 응답에 포함하지 않음.

}
