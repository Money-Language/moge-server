package com.moge.moge.domain.user.model.req;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class PostUserPasswordValidateReq {
    private String password;
    private String rePassword;
}
