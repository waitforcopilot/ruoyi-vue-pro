package cn.iocoder.yudao.module.hrm.service.payroll;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.hrm.enums.ErrorCodeConstants.PAYROLL_TAX_IDENTITY_KEY_MISSING;
import static cn.iocoder.yudao.module.hrm.enums.ErrorCodeConstants.PAYROLL_TAX_INVALID;

import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Base64;
import javax.crypto.*;
import javax.crypto.spec.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/** Tax identifiers never enter normal profile JSON or audit snapshots in clear text. */
@Component
public class HrmPayrollTaxIdentityCodec {
  @Value("${yudao.hrm.tax.identity-encryption-key:}")
  private String encryptionKey;

  @Value("${yudao.hrm.tax.identity-fingerprint-key:}")
  private String fingerprintKey;

  private byte[] key(String value) {
    if (value == null || value.trim().isEmpty()) throw exception(PAYROLL_TAX_IDENTITY_KEY_MISSING);
    try {
      byte[] decoded = Base64.getDecoder().decode(value);
      if (decoded.length != 32) throw exception(PAYROLL_TAX_IDENTITY_KEY_MISSING);
      return decoded;
    } catch (IllegalArgumentException invalid) {
      throw exception(PAYROLL_TAX_IDENTITY_KEY_MISSING);
    }
  }

  public String fingerprint(String value) {
    try {
      Mac mac = Mac.getInstance("HmacSHA256");
      mac.init(new SecretKeySpec(key(fingerprintKey), "HmacSHA256"));
      return Base64.getUrlEncoder()
          .withoutPadding()
          .encodeToString(mac.doFinal(value.getBytes(StandardCharsets.UTF_8)));
    } catch (java.security.GeneralSecurityException failure) {
      throw exception(PAYROLL_TAX_INVALID);
    }
  }

  public String encrypt(String value) {
    try {
      byte[] nonce = new byte[12];
      new SecureRandom().nextBytes(nonce);
      Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
      cipher.init(
          Cipher.ENCRYPT_MODE,
          new SecretKeySpec(key(encryptionKey), "AES"),
          new GCMParameterSpec(128, nonce));
      cipher.updateAAD("hrm-tax-identity:v1".getBytes(StandardCharsets.UTF_8));
      return "v1:"
          + Base64.getEncoder().encodeToString(nonce)
          + ":"
          + Base64.getEncoder()
              .encodeToString(cipher.doFinal(value.getBytes(StandardCharsets.UTF_8)));
    } catch (java.security.GeneralSecurityException failure) {
      throw exception(PAYROLL_TAX_INVALID);
    }
  }

  public String decrypt(String encoded) {
    if (encoded == null) throw exception(PAYROLL_TAX_INVALID);
    try {
      String[] parts = encoded.split(":", -1);
      if (parts.length != 3 || !"v1".equals(parts[0])) throw exception(PAYROLL_TAX_INVALID);
      byte[] nonce = Base64.getDecoder().decode(parts[1]);
      if (nonce.length != 12) throw exception(PAYROLL_TAX_INVALID);
      Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
      cipher.init(
          Cipher.DECRYPT_MODE,
          new SecretKeySpec(key(encryptionKey), "AES"),
          new GCMParameterSpec(128, nonce));
      cipher.updateAAD("hrm-tax-identity:v1".getBytes(StandardCharsets.UTF_8));
      return new String(
          cipher.doFinal(Base64.getDecoder().decode(parts[2])), StandardCharsets.UTF_8);
    } catch (java.security.GeneralSecurityException | IllegalArgumentException failure) {
      throw exception(PAYROLL_TAX_INVALID);
    }
  }
}
