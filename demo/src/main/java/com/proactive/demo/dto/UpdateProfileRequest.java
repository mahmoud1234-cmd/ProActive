package com.proactive.demo.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class UpdateProfileRequest {

    @Size(min = 2, max = 50, message = "Prénom : 2 à 50 caractères")
    private String firstName;

    @Size(min = 2, max = 50, message = "Nom : 2 à 50 caractères")
    private String lastName;

    @Email(message = "Email invalide")
    private String email;

    /** Mot de passe actuel — requis pour changer le mot de passe */
    private String currentPassword;

    /** Nouveau mot de passe — optionnel */
    @Size(min = 6, message = "Minimum 6 caractères")
    private String newPassword;
}
