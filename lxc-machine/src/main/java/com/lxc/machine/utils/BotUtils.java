package com.lxc.machine.utils;


import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.lxc.common.constant.LxcConstants;
import com.lxc.common.utils.spring.SpringUtils;
import com.lxc.machine.domain.SysConfig4LxcMachine;
import com.lxc.machine.mapper.SysConfigMapper4LxcMachine;
import lombok.extern.slf4j.Slf4j;
import org.telegram.telegrambots.bots.DefaultAbsSender;
import org.telegram.telegrambots.bots.DefaultBotOptions;
import org.telegram.telegrambots.meta.api.methods.groupadministration.GetChatMember;
import org.telegram.telegrambots.meta.api.objects.chatmember.ChatMember;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

@Slf4j
public class BotUtils {
    /**
     * 获取系统参数设置SysConfig
     * @param key
     * @return
     */
    private static SysConfig4LxcMachine getSysConfig(String key) {
        SysConfig4LxcMachine sysConfig4LxcMachine = SpringUtils.getBean(SysConfigMapper4LxcMachine.class).selectOne(new LambdaQueryWrapper<SysConfig4LxcMachine>().eq(SysConfig4LxcMachine::getConfigKey, key));
        return sysConfig4LxcMachine;
    }

    /**
     * 获取ChatId
     * @param keyOfTGChannelID
     * @return
     */
    public static String getChatId(String keyOfTGChannelID) {
        SysConfig4LxcMachine sysConfig4LxcMachine = getSysConfig(keyOfTGChannelID);
        return sysConfig4LxcMachine != null ? sysConfig4LxcMachine.getConfigValue() : LxcConstants.CHAT_ID;
    }

    /**
     * 获取BotToken
     * @param keyOfTGBotToken
     * @return
     */
    public static String getBotToken(String keyOfTGBotToken) {
        SysConfig4LxcMachine sysConfig4LxcMachine = getSysConfig(keyOfTGBotToken);
        return sysConfig4LxcMachine != null ? sysConfig4LxcMachine.getConfigValue() : LxcConstants.BOOT_TOKEN;
    }

    /**
     * 获取是否开启校验成员
     * @param keyOfTGEnableCheckMember
     * @return
     */
    public static Boolean getEnableCheckMember(String keyOfTGEnableCheckMember) {
        SysConfig4LxcMachine sysConfig4LxcMachine = getSysConfig(keyOfTGEnableCheckMember);
        return sysConfig4LxcMachine != null ? "true".equalsIgnoreCase(sysConfig4LxcMachine.getConfigValue()) : false;
    }

    public static boolean isMember(String ChatId, Long userId) {

        boolean result = false;
        String status = "left";

        DefaultBotOptions botOptions = new DefaultBotOptions();
        botOptions.setProxyHost("127.0.0.1");
        botOptions.setProxyPort(1080);
        botOptions.setProxyType(DefaultBotOptions.ProxyType.SOCKS5);

        DefaultAbsSender sender = new DefaultAbsSender(botOptions) {
            @Override
            public String getBotToken() {
                        return BotUtils.getBotToken(LxcConstants.TG_BOT_TOKEN);
            }
        };



        GetChatMember getChatMember = new GetChatMember();
        getChatMember.setChatId(ChatId);
        getChatMember.setUserId(userId);
        //TODO 不获取用户信息
//      /*  try {
//            ChatMember chatMember = sender.execute(getChatMember);
//            status = chatMember.getStatus();
//        } catch (TelegramApiException e) {
//            ChatMember chatMember = null;
//            try {
//                chatMember = sender.execute(getChatMember);
//                status = chatMember.getStatus();
//            } catch (TelegramApiException ex) {
//                log.error("获取用户状态失败", ex);
//            }
//
//        }*/

        switch (status) {
            case "member":
            case "administrator":
            case "creator":
                result = true;
                break;
            case "left":
                result = false;
                break;
        }
//        return result;
        return true;
    }


    public static String getUserName(String ChatId, Long userId) {

        String userName = null;

        DefaultBotOptions botOptions = new DefaultBotOptions();
        //botOptions.setProxyHost("127.0.0.1");
        //botOptions.setProxyPort(7890);
        //botOptions.setProxyType(DefaultBotOptions.ProxyType.SOCKS5);

        DefaultAbsSender sender = new DefaultAbsSender(botOptions) {
            @Override
            public String getBotToken() {
                return BotUtils.getBotToken(LxcConstants.TG_BOT_TOKEN);
            }
        };

        GetChatMember getChatMember = new GetChatMember();
        getChatMember.setChatId(ChatId);

        getChatMember.setUserId(userId);


        try {
            ChatMember chatMember = sender.execute(getChatMember);
            userName = chatMember.getUser().getUserName();
        } catch (TelegramApiException e) {
            // TODO 预知报错 报错时认为不存在用户
            // throw new RuntimeException(e);
            try {
                ChatMember chatMember = sender.execute(getChatMember);
                userName = chatMember.getUser().getUserName();
            } catch (TelegramApiException ex) {
                ex.printStackTrace();
            }

        }

        return userName;
    }


}
