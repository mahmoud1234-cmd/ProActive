package com.proactive.demo.service;

import com.proactive.demo.model.User;
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

    @Async
    public void sendApprovalEmail(User approvedUser, User admin) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(fromEmail, "ProActive");
            helper.setTo(approvedUser.getEmail());
            // Sujet sans emoji (réduit le score spam)
            helper.setSubject("Votre compte ProActive a été activé");

            // Fournir les deux versions : texte brut ET HTML
            String text = buildApprovalText(approvedUser, admin);
            String html = buildApprovalHtml(approvedUser, admin);
            helper.setText(text, html);

            // Headers anti-spam
            message.setHeader("X-Mailer", "ProActive Mailer");
            message.setHeader("X-Priority", "3");

            mailSender.send(message);
            log.info("Email approbation envoyé à {}", approvedUser.getEmail());
        } catch (Exception e) {
            log.error("Echec email approbation {} : {}", approvedUser.getEmail(), e.getMessage());
        }
    }

    @Async
    public void sendRejectionEmail(User rejectedUser, User admin) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(fromEmail, "ProActive");
            helper.setTo(rejectedUser.getEmail());
            helper.setSubject("Votre demande d'inscription ProActive");

            String text = buildRejectionText(rejectedUser, admin);
            String html = buildRejectionHtml(rejectedUser, admin);
            helper.setText(text, html);

            message.setHeader("X-Mailer", "ProActive Mailer");
            message.setHeader("X-Priority", "3");

            mailSender.send(message);
            log.info("Email refus envoyé à {}", rejectedUser.getEmail());
        } catch (Exception e) {
            log.error("Echec email refus {} : {}", rejectedUser.getEmail(), e.getMessage());
        }
    }

    // ── Version texte brut (anti-spam essentiel) ────────────────────────

    private String buildApprovalText(User user, User admin) {
        return """
            Bonjour %s,
            
            Votre compte ProActive a été approuvé et activé par %s (Administrateur).
            
            Informations de votre compte :
            - Email : %s
            - Role  : %s
            - Statut: Actif
            
            Vous pouvez maintenant vous connecter sur la plateforme ProActive.
            
            Cordialement,
            L'équipe ProActive
            
            ---
            Ce message a été envoyé automatiquement par ProActive.
            """.formatted(
                user.getFirstName() + " " + user.getLastName(),
                admin.getFirstName() + " " + admin.getLastName(),
                user.getEmail(),
                getRoleLabel(user.getRole())
            );
    }

    private String buildRejectionText(User user, User admin) {
        return """
            Bonjour %s,
            
            Nous vous informons que votre demande de création de compte ProActive
            n'a pas pu être approuvée pour le moment.
            
            Pour plus d'informations, veuillez contacter votre administrateur.
            
            Décision prise par : %s (Administrateur ProActive)
            
            Cordialement,
            L'équipe ProActive
            """.formatted(
                user.getFirstName() + " " + user.getLastName(),
                admin.getFirstName() + " " + admin.getLastName()
            );
    }

    // ── Templates HTML ──────────────────────────────────────────────────

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
              <title>Compte ProActive activé</title>
            </head>
            <body style="margin:0;padding:0;background-color:#f4f6f9;font-family:Arial,Helvetica,sans-serif;">
              <table width="100%%" cellpadding="0" cellspacing="0" border="0" style="background:#f4f6f9;padding:32px 16px;">
                <tr><td align="center">
                  <table width="580" cellpadding="0" cellspacing="0" border="0"
                         style="max-width:580px;width:100%%;background:#ffffff;border-radius:12px;overflow:hidden;border:1px solid #e2e8f0;">

                    <!-- Header -->
                    <tr>
                      <td style="background:#0b1f3a;padding:32px 32px 24px;text-align:center;">
                        <table cellpadding="0" cellspacing="0" border="0" style="margin:0 auto 14px;">
                          <tr>
                            <td style="background:#1a73e8;border-radius:10px;width:48px;height:48px;text-align:center;vertical-align:middle;">
                              <span style="color:#ffffff;font-size:20px;font-weight:900;">PA</span>
                            </td>
                          </tr>
                        </table>
                        <h1 style="margin:0;color:#ffffff;font-size:22px;font-weight:700;">ProActive</h1>
                        <p style="margin:6px 0 0;color:#94adc8;font-size:13px;">Gestion de Projets</p>
                      </td>
                    </tr>

                    <!-- Bannière -->
                    <tr>
                      <td style="background:#1e8449;padding:14px 32px;text-align:center;">
                        <p style="margin:0;color:#ffffff;font-size:15px;font-weight:600;">
                          Compte activé avec succès
                        </p>
                      </td>
                    </tr>

                    <!-- Corps -->
                    <tr>
                      <td style="padding:32px;">
                        <p style="margin:0 0 16px;color:#1a202c;font-size:17px;font-weight:600;">Bonjour %s,</p>
                        <p style="margin:0 0 20px;color:#4a5568;font-size:14px;line-height:1.7;">
                          Votre compte <strong>ProActive</strong> a été approuvé et activé.
                          Vous pouvez maintenant vous connecter et accéder à la plateforme.
                        </p>

                        <!-- Tableau infos -->
                        <table width="100%%" cellpadding="0" cellspacing="0" border="0"
                               style="background:#f8fafc;border:1px solid #e2e8f0;border-radius:8px;margin:20px 0;">
                          <tr>
                            <td style="padding:16px 20px;">
                              <p style="margin:0 0 10px;color:#718096;font-size:11px;font-weight:700;text-transform:uppercase;letter-spacing:0.05em;">
                                Informations du compte
                              </p>
                              <table width="100%%" cellpadding="0" cellspacing="0" border="0">
                                <tr>
                                  <td style="padding:5px 0;color:#718096;font-size:13px;width:100px;">Email</td>
                                  <td style="padding:5px 0;color:#1a202c;font-size:13px;font-weight:600;">%s</td>
                                </tr>
                                <tr>
                                  <td style="padding:5px 0;color:#718096;font-size:13px;">Role</td>
                                  <td style="padding:5px 0;color:#1a73e8;font-size:13px;font-weight:600;">%s</td>
                                </tr>
                                <tr>
                                  <td style="padding:5px 0;color:#718096;font-size:13px;">Statut</td>
                                  <td style="padding:5px 0;color:#1e8449;font-size:13px;font-weight:600;">Actif</td>
                                </tr>
                              </table>
                            </td>
                          </tr>
                        </table>

                        <p style="margin:0;color:#4a5568;font-size:13px;line-height:1.7;">
                          Pour toute question, contactez votre administrateur.
                        </p>
                      </td>
                    </tr>

                    <!-- Footer -->
                    <tr>
                      <td style="background:#f8fafc;border-top:1px solid #e2e8f0;padding:16px 32px;">
                        <table width="100%%" cellpadding="0" cellspacing="0" border="0">
                          <tr>
                            <td>
                              <p style="margin:0;color:#a0aec0;font-size:12px;">Approuvé par</p>
                              <p style="margin:3px 0 0;color:#1a202c;font-size:13px;font-weight:600;">%s</p>
                              <p style="margin:2px 0 0;color:#718096;font-size:12px;">Administrateur ProActive</p>
                            </td>
                            <td align="right">
                              <p style="margin:0;color:#cbd5e0;font-size:11px;">ProActive Platform</p>
                              <p style="margin:2px 0 0;color:#cbd5e0;font-size:11px;">© 2026</p>
                            </td>
                          </tr>
                        </table>
                      </td>
                    </tr>

                  </table>
                </td></tr>
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
            <head><meta charset="UTF-8"><title>Demande ProActive</title></head>
            <body style="margin:0;padding:0;background:#f4f6f9;font-family:Arial,Helvetica,sans-serif;">
              <table width="100%%" cellpadding="0" cellspacing="0" border="0" style="background:#f4f6f9;padding:32px 16px;">
                <tr><td align="center">
                  <table width="580" cellpadding="0" cellspacing="0" border="0"
                         style="max-width:580px;width:100%%;background:#ffffff;border-radius:12px;overflow:hidden;border:1px solid #e2e8f0;">
                    <tr>
                      <td style="background:#0b1f3a;padding:32px;text-align:center;">
                        <table cellpadding="0" cellspacing="0" border="0" style="margin:0 auto 14px;">
                          <tr>
                            <td style="background:#1a73e8;border-radius:10px;width:48px;height:48px;text-align:center;vertical-align:middle;">
                              <span style="color:#fff;font-size:20px;font-weight:900;">PA</span>
                            </td>
                          </tr>
                        </table>
                        <h1 style="margin:0;color:#fff;font-size:22px;font-weight:700;">ProActive</h1>
                      </td>
                    </tr>
                    <tr>
                      <td style="background:#c0392b;padding:14px 32px;text-align:center;">
                        <p style="margin:0;color:#fff;font-size:15px;font-weight:600;">Demande d'inscription non acceptée</p>
                      </td>
                    </tr>
                    <tr>
                      <td style="padding:32px;">
                        <p style="margin:0 0 16px;color:#1a202c;font-size:17px;font-weight:600;">Bonjour %s,</p>
                        <p style="margin:0 0 16px;color:#4a5568;font-size:14px;line-height:1.7;">
                          Votre demande de création de compte <strong>ProActive</strong>
                          n'a pas pu être approuvée pour le moment.
                        </p>
                        <p style="margin:0;color:#4a5568;font-size:14px;line-height:1.7;">
                          Pour plus d'informations, contactez directement votre administrateur.
                        </p>
                      </td>
                    </tr>
                    <tr>
                      <td style="background:#f8fafc;border-top:1px solid #e2e8f0;padding:16px 32px;">
                        <p style="margin:0;color:#a0aec0;font-size:12px;">Décision prise par</p>
                        <p style="margin:3px 0 0;color:#1a202c;font-size:13px;font-weight:600;">%s</p>
                        <p style="margin:2px 0 0;color:#718096;font-size:12px;">Administrateur ProActive</p>
                      </td>
                    </tr>
                  </table>
                </td></tr>
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
