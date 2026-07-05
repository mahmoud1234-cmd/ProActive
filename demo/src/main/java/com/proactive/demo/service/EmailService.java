package com.proactive.demo.service;

import com.proactive.demo.model.User;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailService {

    private final JavaMailSender mailSender;

    @Value("${spring.mail.username}")
    private String fromEmail;

    /**
     * Envoie un email de confirmation d'approbation à l'utilisateur.
     * Exécuté en asynchrone pour ne pas bloquer la requête HTTP.
     */
    @Async
    public void sendApprovalEmail(User approvedUser, User admin) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(fromEmail, "ProActive — Administration");
            helper.setTo(approvedUser.getEmail());
            helper.setSubject("✅ Votre compte ProActive a été activé");
            helper.setText(buildApprovalHtml(approvedUser, admin), true);

            mailSender.send(message);
            log.info("Email d'approbation envoyé à {}", approvedUser.getEmail());

        } catch (Exception e) {
            log.error("Échec envoi email à {} : {}", approvedUser.getEmail(), e.getMessage());
        }
    }

    /**
     * Envoie un email de refus à l'utilisateur.
     */
    @Async
    public void sendRejectionEmail(User rejectedUser, User admin) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(fromEmail, "ProActive — Administration");
            helper.setTo(rejectedUser.getEmail());
            helper.setSubject("❌ Votre demande d'inscription ProActive");
            helper.setText(buildRejectionHtml(rejectedUser, admin), true);

            mailSender.send(message);
            log.info("Email de refus envoyé à {}", rejectedUser.getEmail());

        } catch (Exception e) {
            log.error("Échec envoi email à {} : {}", rejectedUser.getEmail(), e.getMessage());
        }
    }

    // ── Templates HTML ────────────────────────────────────────────────────

    private String buildApprovalHtml(User user, User admin) {
        String adminName = admin.getFirstName() + " " + admin.getLastName();
        String userName  = user.getFirstName() + " " + user.getLastName();
        String roleLabel = getRoleLabel(user.getRole());

        return """
            <!DOCTYPE html>
            <html lang="fr">
            <head>
              <meta charset="UTF-8">
              <meta name="viewport" content="width=device-width, initial-scale=1.0">
              <title>Compte activé</title>
            </head>
            <body style="margin:0;padding:0;background-color:#f0f4fa;font-family:'Segoe UI',Tahoma,Geneva,Verdana,sans-serif;">
              <table width="100%%" cellpadding="0" cellspacing="0" style="background-color:#f0f4fa;padding:40px 20px;">
                <tr>
                  <td align="center">
                    <table width="600" cellpadding="0" cellspacing="0" style="max-width:600px;width:100%%;">

                      <!-- Header -->
                      <tr>
                        <td align="center" style="background:linear-gradient(135deg,#0b1f3a,#1a3a5c);border-radius:16px 16px 0 0;padding:40px 30px;">
                          <div style="width:56px;height:56px;background:linear-gradient(145deg,#39b5ff,#4ce6b8);border-radius:14px;display:inline-block;line-height:56px;text-align:center;font-size:24px;font-weight:900;color:#032039;margin-bottom:16px;">PA</div>
                          <h1 style="margin:0;color:#ffffff;font-size:26px;font-weight:700;letter-spacing:-0.5px;">ProActive</h1>
                          <p style="margin:6px 0 0;color:#a8c4e0;font-size:14px;">Gestion de Projets · Suivi Prédictif</p>
                        </td>
                      </tr>

                      <!-- Bannière verte -->
                      <tr>
                        <td style="background:#27ae60;padding:16px 30px;text-align:center;">
                          <p style="margin:0;color:#ffffff;font-size:16px;font-weight:600;">
                            ✅ &nbsp;Votre compte a été activé avec succès
                          </p>
                        </td>
                      </tr>

                      <!-- Corps -->
                      <tr>
                        <td style="background:#ffffff;padding:40px 30px;">
                          <p style="margin:0 0 20px;color:#0b1f3a;font-size:18px;font-weight:600;">
                            Bonjour %s,
                          </p>
                          <p style="margin:0 0 20px;color:#45576f;font-size:15px;line-height:1.7;">
                            Nous avons le plaisir de vous informer que votre compte <strong>ProActive</strong> a été
                            <strong style="color:#27ae60;">approuvé et activé</strong> par l'administrateur.
                          </p>

                          <!-- Carte info compte -->
                          <table width="100%%" cellpadding="0" cellspacing="0"
                                 style="background:#f8fbff;border:1px solid #d2e3fc;border-radius:12px;margin:20px 0;">
                            <tr>
                              <td style="padding:20px 24px;">
                                <p style="margin:0 0 12px;color:#6c7f9a;font-size:12px;font-weight:700;text-transform:uppercase;letter-spacing:0.06em;">
                                  Détails de votre compte
                                </p>
                                <table width="100%%" cellpadding="0" cellspacing="0">
                                  <tr>
                                    <td style="padding:6px 0;color:#45576f;font-size:14px;width:120px;">📧 Email</td>
                                    <td style="padding:6px 0;color:#0b1f3a;font-size:14px;font-weight:600;">%s</td>
                                  </tr>
                                  <tr>
                                    <td style="padding:6px 0;color:#45576f;font-size:14px;">🎭 Rôle</td>
                                    <td style="padding:6px 0;">
                                      <span style="background:#e8f0fe;color:#1a73e8;padding:3px 10px;border-radius:6px;font-size:13px;font-weight:600;">
                                        %s
                                      </span>
                                    </td>
                                  </tr>
                                  <tr>
                                    <td style="padding:6px 0;color:#45576f;font-size:14px;">✅ Statut</td>
                                    <td style="padding:6px 0;">
                                      <span style="background:#e8f8ee;color:#27ae60;padding:3px 10px;border-radius:6px;font-size:13px;font-weight:600;">
                                        Actif
                                      </span>
                                    </td>
                                  </tr>
                                </table>
                              </td>
                            </tr>
                          </table>

                          <!-- Bouton CTA -->
                          <table width="100%%" cellpadding="0" cellspacing="0" style="margin:28px 0;">
                            <tr>
                              <td align="center">
                                <a href="http://localhost:4200/login"
                                   style="display:inline-block;background:linear-gradient(135deg,#1a73e8,#1557b0);color:#ffffff;font-size:15px;font-weight:600;text-decoration:none;padding:14px 36px;border-radius:10px;letter-spacing:0.3px;">
                                  🚀 &nbsp;Se connecter à ProActive
                                </a>
                              </td>
                            </tr>
                          </table>

                          <p style="margin:0;color:#45576f;font-size:14px;line-height:1.7;">
                            Si vous avez des questions, n'hésitez pas à contacter votre administrateur.
                          </p>
                        </td>
                      </tr>

                      <!-- Signature admin -->
                      <tr>
                        <td style="background:#f8fafc;border-top:1px solid #e9edf5;padding:20px 30px;border-radius:0 0 16px 16px;">
                          <table width="100%%" cellpadding="0" cellspacing="0">
                            <tr>
                              <td>
                                <p style="margin:0;color:#8090a8;font-size:13px;">Approuvé par</p>
                                <p style="margin:4px 0 0;color:#0b1f3a;font-size:14px;font-weight:600;">%s</p>
                                <p style="margin:2px 0 0;color:#6c7f9a;font-size:12px;">Administrateur ProActive</p>
                              </td>
                              <td align="right">
                                <p style="margin:0;color:#c0c8d4;font-size:11px;">ProActive Platform</p>
                                <p style="margin:2px 0 0;color:#c0c8d4;font-size:11px;">© 2026 Tous droits réservés</p>
                              </td>
                            </tr>
                          </table>
                        </td>
                      </tr>

                    </table>
                  </td>
                </tr>
              </table>
            </body>
            </html>
            """.formatted(userName, user.getEmail(), roleLabel, adminName);
    }

    private String buildRejectionHtml(User user, User admin) {
        String adminName = admin.getFirstName() + " " + admin.getLastName();
        String userName  = user.getFirstName() + " " + user.getLastName();

        return """
            <!DOCTYPE html>
            <html lang="fr">
            <head><meta charset="UTF-8"><title>Demande refusée</title></head>
            <body style="margin:0;padding:0;background-color:#f0f4fa;font-family:'Segoe UI',Tahoma,Geneva,Verdana,sans-serif;">
              <table width="100%%" cellpadding="0" cellspacing="0" style="background-color:#f0f4fa;padding:40px 20px;">
                <tr>
                  <td align="center">
                    <table width="600" cellpadding="0" cellspacing="0" style="max-width:600px;width:100%%;">
                      <tr>
                        <td align="center" style="background:linear-gradient(135deg,#0b1f3a,#1a3a5c);border-radius:16px 16px 0 0;padding:40px 30px;">
                          <div style="width:56px;height:56px;background:linear-gradient(145deg,#39b5ff,#4ce6b8);border-radius:14px;display:inline-block;line-height:56px;text-align:center;font-size:24px;font-weight:900;color:#032039;margin-bottom:16px;">PA</div>
                          <h1 style="margin:0;color:#ffffff;font-size:26px;font-weight:700;">ProActive</h1>
                          <p style="margin:6px 0 0;color:#a8c4e0;font-size:14px;">Gestion de Projets · Suivi Prédictif</p>
                        </td>
                      </tr>
                      <tr>
                        <td style="background:#e74c3c;padding:16px 30px;text-align:center;">
                          <p style="margin:0;color:#ffffff;font-size:16px;font-weight:600;">
                            ❌ &nbsp;Votre demande d'inscription n'a pas été acceptée
                          </p>
                        </td>
                      </tr>
                      <tr>
                        <td style="background:#ffffff;padding:40px 30px;">
                          <p style="margin:0 0 20px;color:#0b1f3a;font-size:18px;font-weight:600;">Bonjour %s,</p>
                          <p style="margin:0 0 20px;color:#45576f;font-size:15px;line-height:1.7;">
                            Nous vous informons que votre demande de création de compte <strong>ProActive</strong>
                            n'a pas pu être approuvée pour le moment.
                          </p>
                          <p style="margin:0;color:#45576f;font-size:14px;line-height:1.7;">
                            Pour plus d'informations, veuillez contacter directement votre administrateur.
                          </p>
                        </td>
                      </tr>
                      <tr>
                        <td style="background:#f8fafc;border-top:1px solid #e9edf5;padding:20px 30px;border-radius:0 0 16px 16px;">
                          <p style="margin:0;color:#8090a8;font-size:13px;">Décision prise par</p>
                          <p style="margin:4px 0 0;color:#0b1f3a;font-size:14px;font-weight:600;">%s</p>
                          <p style="margin:2px 0 0;color:#6c7f9a;font-size:12px;">Administrateur ProActive</p>
                        </td>
                      </tr>
                    </table>
                  </td>
                </tr>
              </table>
            </body>
            </html>
            """.formatted(userName, adminName);
    }

    private String getRoleLabel(User.Role role) {
        return switch (role) {
            case ADMIN   -> "Administrateur";
            case MANAGER -> "Manager";
            case USER    -> "Utilisateur";
        };
    }
}
