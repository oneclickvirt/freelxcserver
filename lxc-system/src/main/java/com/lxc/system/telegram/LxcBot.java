package com.lxc.system.telegram;

import com.lxc.common.constant.LxcConstants;
import com.lxc.common.utils.BotUtils;
import com.lxc.common.utils.StringUtils;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.telegram.telegrambots.bots.DefaultBotOptions;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;

@Slf4j
public class LxcBot extends TelegramLongPollingBot {

    private final LxcHandler lxcHandler;

    public LxcBot(DefaultBotOptions defaultBotOptions,LxcHandler lxcHandler) {
        super(defaultBotOptions);
        this.lxcHandler = lxcHandler;
    }

    @Override
    public String getBotUsername() {
        return LxcConstants.BOOT_NAME;
    }

    @Override
    public String getBotToken() {
        return LxcConstants.BOOT_TOKEN;
    }


    @Override
    @SneakyThrows
    public void onUpdateReceived(Update update) {
        Message updateMessage = update.getMessage();
        CallbackQuery callbackQuery = update.getCallbackQuery();
        if (update.hasMessage() && updateMessage.hasText()) {
            lxcHandler.threadPoolTaskExecutor.execute(() -> {
                processMessage(update);
            });
//           System.out.println(BotUtils.isMember("-1001474554890", Long.valueOf("1749475863")));
        } else if (update.hasCallbackQuery()) {
            lxcHandler.threadPoolTaskExecutor.execute(() -> {
                processCallbackQuery(callbackQuery);
            });
        }
    }

    /**
     * 发送文本消息
     *
     * @param text   内容
     * @param chatId 内容ID
     * @param messageId 消息ID
     */
    @SneakyThrows
    @Async
    public void sendTextMsg(String text, String chatId,Integer messageId) {
        SendMessage sendMessage = SendMessage.builder()
            .chatId(chatId)
            .text(text)
            .replyToMessageId(messageId)
//            .protectContent(true)
            .build();
        executeAsync(sendMessage);
    }


    /**
     * 发送文本消息
     *
     * @param chatId 内容ID
     */
    @SneakyThrows
    @Async
    public void sendStartMsg(String chatId) {
        InlineKeyboardButton button1 = InlineKeyboardButton.builder().text("重置密码").callbackData("/repasswd").build();
        InlineKeyboardButton button2 = InlineKeyboardButton.builder().text("获取TGID").callbackData("/myid").build();
        List<InlineKeyboardButton> list1 = new ArrayList<>();
        List<InlineKeyboardButton> list2 = new ArrayList<>();
        list1.add(button1);
        list1.add(button2);
        List<List<InlineKeyboardButton>> rowList = new ArrayList<>();
        Collections.addAll(rowList, list1,list2);
        InlineKeyboardMarkup inlineKeyboardMarkup = InlineKeyboardMarkup.builder().keyboard(rowList).build();
        SendMessage sendMessage = SendMessage.builder()
            .chatId(chatId)
            .text("欢迎使用LXC测试机器人，请选择您需要的操作")
            .replyMarkup(inlineKeyboardMarkup)
//            .protectContent(true)
            .build();
        executeAsync(sendMessage);
    }

    /**
     * 发送文本消息
     *
     * @param update   内容
     */
    @SneakyThrows
    @Async
    public void processMessage(Update update) {
        Long chatId = update.getMessage().getChatId();
        Integer messageId = update.getMessage().getMessageId();
        doHandel(chatId, update.getMessage().getText(),messageId);
    }

    /**
     * 发送文本消息
     *
     * @param callbackQuery   内容
     */
    @SneakyThrows
    @Async
    public void processCallbackQuery(CallbackQuery callbackQuery) {
        Long chatId = callbackQuery.getFrom().getId();
        doHandel(chatId, callbackQuery.getData(), null);
    }

    private void doHandel(Long chatId, String param, Integer messageId) {
        if (BotUtils.isMember(LxcConstants.CHAT_ID,chatId)) {
            String text = lxcHandler.handle(chatId,param);
            if (StringUtils.equals(text,"start")) {
                sendStartMsg(chatId.toString());
            } else {
                sendTextMsg(text,chatId.toString(),messageId);
            }
        } else {
            sendTextMsg("请先订阅频道 https://t.me/VPS_spiders",chatId.toString(),null);
        }
    }
}
