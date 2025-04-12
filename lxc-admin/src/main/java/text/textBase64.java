package text;


import cn.hutool.crypto.symmetric.AES;
import org.junit.Test;

import java.nio.charset.Charset;

public class textBase64 {
    @Test
    public void test01() {
        String originStr = "ZJune147258";
//        String encodePw = Base64.encode(originStr);
//        System.out.println("encode Password:" + encodePw);
////        byte[] decodePw = Base64.decode(encodePw);
////        System.out.println("decode Password:" + decodePw[0]);
//        String decodePw = Base64.decodeStr(encodePw);
//        System.out.println("decode Password:" + decodePw);

        // 使用BCrypt进行密码哈希
//        String hashedPassword = BCrypt.hashpw(originStr, BCrypt.gensalt());
//        System.out.println("BCrypt哈希后的密码: " + hashedPassword);
//
//        // 验证输入的密码是否与哈希后的一致
//        boolean isMatch = BCrypt.checkpw(originStr, hashedPassword);
//        System.out.println("原始密码与哈希后密码匹配: " + isMatch);
//
//        // 测试错误密码的情况
//        boolean wrongPasswordCheck = BCrypt.checkpw("wrongPassword", hashedPassword);
//        System.out.println("错误密码与哈希后密码匹配: " + wrongPasswordCheck);

//        @Value("${aes.key}")
        String aesKey = "lxc_apply";
//        byte[] keyBytes = SecureUtil.generateKey("AES").getEncoded();
//        String encrypt = encrypt(originStr, keyBytes);
//        System.out.println("AES加密后内容：" + encrypt);
//        String decrypt = decrypt(encrypt, keyBytes);
//        System.out.println("AES解密后内容：" + decrypt);

        byte[] bytes = "lxcapplylxcapply".getBytes();
//        String BASE64_ENCODED_KEY = (String) bytes;
        // 要加密的原始密码
        String originalPassword = "ZJune147258";

        // 将Base64编码的密钥转换为字节数组
//        byte[] keyBytes = Base64.decode(BASE64_ENCODED_KEY);
        byte[] keyBytes = bytes;

        // 使用指定的密钥初始化AES加密器
        AES aes = new AES(keyBytes);

        // 加密密码
        String encryptedPassword = aes.encryptBase64(originalPassword);
        System.out.println("加密后的密码: " + encryptedPassword);

        // 解密密码
        String decryptedPassword = aes.decryptStr(encryptedPassword, Charset.defaultCharset());
        System.out.println("解密后的密码: " + decryptedPassword);
    }
    public String encrypt(String data, byte[] aesKey) {
        AES aes = new AES(aesKey);
        return aes.encryptBase64(data);
    }

    public String decrypt(String encryptedData, byte[] aesKey) {
        AES aes = new AES(aesKey);
        return new String(aes.decrypt(encryptedData));
    }
}
