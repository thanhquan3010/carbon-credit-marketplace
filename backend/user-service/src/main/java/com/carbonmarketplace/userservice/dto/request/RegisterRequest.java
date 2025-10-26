package com.carbonmarketplace.userservice.dto.request;

import com.carbonmarketplace.userservice.entity.User;
import com.carbonmarketplace.userservice.validation.ValidPassword;
import com.carbonmarketplace.userservice.validation.ValidPhone;
import com.carbonmarketplace.userservice.validation.ValidRole;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RegisterRequest {

    @NotBlank(message = "Email is required")
    @Email(message = "Please enter a valid email address")
    @Size(max = 255, message = "Email is too long")
    private String email;

    @NotBlank(message = "Phone is required")
    @ValidPhone
    private String phone;

    @NotBlank(message = "Password is required")
    @Size(min = 8, message = "Password must be at least 8 characters")
    @ValidPassword
    private String password;

    @NotBlank(message = "Full name is required")
    @Size(min = 2, max = 255, message = "Name must be between 2-255 characters")
    @Pattern(regexp = "^[a-zA-ZÀ-ỹ\\s'-]+$", message = "Name contains invalid characters")
    private String fullName;

    @NotNull(message = "Role is required")
    @ValidRole
    private String role;

    // Optional fields for corporate buyers
    private String companyName;

    @Pattern(regexp = "^[0-9]{10,13}$", message = "Tax code must be 10-13 digits")
    private String taxCode;

    private String businessRegistrationNumber;

    @Size(max = 20, message = "Referral code is too long")
    @Pattern(regexp = "^[A-Z0-9]*$", message = "Referral code must be alphanumeric uppercase")
    private String referralCode;

    @AssertTrue(message = "You must accept the terms and conditions")
    private boolean agreeToTerms;
}
