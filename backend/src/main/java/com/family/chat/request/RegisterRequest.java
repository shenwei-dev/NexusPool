package com.family.chat.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;

@Getter
@Setter
@ToString
@AllArgsConstructor
@NoArgsConstructor
public class RegisterRequest {

    @NotBlank(message = "密码不能为空")
    @Size(min = 6, max = 20, message = "密码长度须在6-20位之间")
    private String password;
}
