package com.roamwise.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SignupRequest {

    @NotBlank(message = "Email can't be blank")
    @Email
    private String email;

    @NotBlank(message = "Password can't be blank")
    @Size(min = 8)
    private String password;
}
