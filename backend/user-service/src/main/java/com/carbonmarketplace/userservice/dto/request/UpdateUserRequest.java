package com.carbonmarketplace.userservice.dto.request;

import com.carbonmarketplace.userservice.validation.ValidPhone;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateUserRequest {

    @Size(min = 2, max = 255, message = "Name must be between 2-255 characters")
    @Pattern(regexp = "^[a-zA-ZÀ-ỹ\\s'-]*$", message = "Name contains invalid characters")
    private String fullName;

    @ValidPhone
    private String phone;

    @Size(max = 500, message = "Bio must not exceed 500 characters")
    private String bio;

    private String avatarUrl;

    // For corporate buyers
    @Size(max = 255, message = "Company name is too long")
    private String companyName;

    @Pattern(regexp = "^[0-9]{10,13}$", message = "Tax code must be 10-13 digits")
    private String taxCode;

    private String businessRegistrationNumber;
}
