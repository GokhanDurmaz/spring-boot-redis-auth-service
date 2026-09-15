package com.example.demo_app.dto;

import jakarta.validation.constraints.NotBlank;

public class ChangePasswordRequest {
    @NotBlank(message = "Old password cannot be empty!")
    private String oldPassword;

    @NotBlank(message = "New password cannot be empty!")
    private String newPassword;

    public String getOldPassword() {
        return oldPassword;
    }

    public void setOldPassword(String oldPassword) {
        this.oldPassword = oldPassword;
    }

    public String getNewPassword() {
        return newPassword;
    }

    public void setNewPassword(String newPassword) {
        this.newPassword = newPassword;
    }
}
