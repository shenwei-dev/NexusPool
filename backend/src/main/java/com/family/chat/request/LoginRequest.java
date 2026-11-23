package com.family.chat.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

@Getter
@Setter
@ToString
@AllArgsConstructor
@NoArgsConstructor
public class LoginRequest {

    @NotNull(message = "账号不能为空")
    private Long account;

    @NotBlank(message = "密码不能为空")
    private String password;
}
