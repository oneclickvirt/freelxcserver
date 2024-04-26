package com.lxc.common.utils;

import java.security.SecureRandom;
import java.util.Base64;

public class InvitationCodeGenerator {
    private static final int CODE_LENGTH = 32;

    public static String generateInvitationCode() {
        return generateInvitationCode(CODE_LENGTH);
    }

    public static String generateInvitationCode(Integer length) {
        if (length == null || length <= 0) {
            length = CODE_LENGTH;
        }
        SecureRandom secureRandom = new SecureRandom();
        byte[] randomBytes = new byte[length];
        secureRandom.nextBytes(randomBytes);

        Base64.Encoder base64Encoder = Base64.getUrlEncoder().withoutPadding();
        String invitationCode = base64Encoder.encodeToString(randomBytes);

        return invitationCode.substring(0, length);
    }

    public static void main(String[] args) {
        String invitationCode = generateInvitationCode();
        System.out.println("Generated Invitation Code: " + invitationCode);
    }
}
