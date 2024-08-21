package com.lxc.web.controller.machine;


import cn.hutool.http.HttpStatus;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.lxc.common.core.domain.R;
import com.lxc.machine.domain.LxcLxc;
import com.lxc.machine.domain.LxcQueue;
import com.lxc.machine.domain.LxcSuperLxc;
import com.lxc.machine.domain.dto.LxcTextDto;
import com.lxc.machine.mapper.LxcLxcMapper;
import com.lxc.machine.mapper.LxcQueueMapper;
import com.lxc.machine.mapper.LxcSuperLxcMapper;
import lombok.RequiredArgsConstructor;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import javax.validation.Valid;
import javax.validation.constraints.NotBlank;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/admin")
public class AdminApi {

    final LxcLxcMapper lxcMapper;
    final LxcQueueMapper queueMapper;
    final LxcSuperLxcMapper superLxcMapper;


    static class AddLxcDto {
        @NotBlank String name;
        @NotBlank String ip;
        @NotBlank String password;
        @NotBlank String tag;
        @NotBlank String port;
        @NotBlank String portStart;
        @NotBlank String portEnd;
        @NotBlank String superDate;
        @NotBlank String superDead;
        @NotBlank String token;
        @NotBlank String childName;

        @NotBlank String superPassword;

        @NotBlank String superPort;

        public String getChildName() {
            return childName;
        }

        public void setChildName(String childName) {
            this.childName = childName;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getIp() {
            return ip;
        }

        public void setIp(String ip) {
            this.ip = ip;
        }

        public String getPassword() {
            return password;
        }

        public void setPassword(String password) {
            this.password = password;
        }

        public String getTag() {
            return tag;
        }

        public void setTag(String tag) {
            this.tag = tag;
        }

        public String getPort() {
            return port;
        }

        public void setPort(String port) {
            this.port = port;
        }

        public String getPortStart() {
            return portStart;
        }

        public void setPortStart(String portStart) {
            this.portStart = portStart;
        }

        public String getPortEnd() {
            return portEnd;
        }

        public void setPortEnd(String portEnd) {
            this.portEnd = portEnd;
        }

        public String getSuperDate() {
            return superDate;
        }

        public void setSuperDate(String superDate) {
            this.superDate = superDate;
        }

        public String getSuperDead() {
            return superDead;
        }

        public void setSuperDead(String superDead) {
            this.superDead = superDead;
        }

        public String getToken() {
            return token;
        }

        public void setToken(String token) {
            this.token = token;
        }

        public String getSuperPassword() {
            return superPassword;
        }

        public void setSuperPassword(String superPassword) {
            this.superPassword = superPassword;
        }

        public String getSuperPort() {
            return superPort;
        }

        public void setSuperPort(String superPort) {
            this.superPort = superPort;
        }
    }


    @PostMapping("/saveLxc")
    public R saveLxc(@RequestBody @Valid AddLxcDto addLxcDto, HttpServletRequest request) {

        HttpSession session = request.getSession();
        Object sessionCode = session.getAttribute("loginType");

        // 验证当前管理员用户是否登录
        if (sessionCode == null || !Objects.equals("true", sessionCode.toString().toLowerCase()))
            return R.fail(HttpStatus.HTTP_FORBIDDEN, "未登录");
        // 新增小鸡的操作携带的token如果不是指定字符串
        if (!addLxcDto.getToken().equals("spiritlhl")) return R.fail(HttpStatus.HTTP_FORBIDDEN, "不要调皮");
        LxcLxc lxc = new LxcLxc();

        //小鸡储存
        lxc.setState(0);
        lxc.setPortEnd(addLxcDto.getPortEnd());
        lxc.setSshPort(addLxcDto.getPort());
        lxc.setSuperIp(addLxcDto.getIp());
        lxc.setChildName(addLxcDto.getChildName());
        lxc.setTag(addLxcDto.getTag());
        lxc.setPassword(addLxcDto.getPassword());
        lxc.setName(addLxcDto.getName());
        lxc.setSuperDead(addLxcDto.getSuperDead());
        lxc.setSuperDate(addLxcDto.getSuperDate());
        int lxcCode = lxcMapper.insert(lxc);

        //母鸡储存
        /*LxcSuperLxc superLxc = new LxcSuperLxc();
        superLxc.setName(addLxcDto.getName());
        superLxc.setDate(addLxcDto.getSuperDate());
        superLxc.setDead(addLxcDto.getSuperDead());
        superLxc.setIp(addLxcDto.getIp());
        superLxc.setTag(addLxcDto.getTag());
        superLxc.setPassword(addLxcDto.getSuperPassword());
        superLxc.setSshPort(addLxcDto.getSuperPort());
        int superCode = superLxcMapper.insert(superLxc);*/

        if (lxcCode == 1) {
            return R.success("执行完毕");
        } else {
            return R.fail(HttpStatus.HTTP_INTERNAL_ERROR, "数据库错误，请联系站长");
        }

    }


    static class LxcDto {
        @NotBlank String name;

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getKey() {
            return key;
        }

        public void setKey(String key) {
            this.key = key;
        }

        @NotBlank String key;
    }

    @PostMapping("/deleteLxc")
    public R deleteLxc(@RequestBody @Valid LxcDto lxcDto) {
        QueryWrapper<LxcLxc> lxcQueryWrapper = new QueryWrapper<>();
        lxcQueryWrapper.eq("name", lxcDto.getName());
        LxcLxc lxc = lxcMapper.selectOne(lxcQueryWrapper);
        String tag = lxc.getTag();
        lxcMapper.delete(lxcQueryWrapper);
        QueryWrapper<LxcQueue> lxcQueueWrapper = new QueryWrapper<>();
        QueryWrapper<LxcSuperLxc> lxcSuperLxcWrapper = new QueryWrapper<>();
        queueMapper.delete(lxcQueueWrapper.eq("tag", tag));
        superLxcMapper.delete(lxcSuperLxcWrapper.eq("tag", tag));
        return R.success("执行完毕");
    }

    /**
     * 通过JSON格式来添加小鸡
     * @param lxcTextDto
     * @return
     */
    @PostMapping("/addLxc")
    public R addLxc(@RequestBody @Valid LxcTextDto lxcTextDto) {
        // lxcText中以文本形式记录了小鸡的信息
        String contents = lxcTextDto.getLxcText();
        // 校验内容是否符合规则
        String pattern = "([a-z]+\\d*) (\\d*) ([\\da-z]*) ?(\\d*) ?(\\d*)";
        Pattern r = Pattern.compile(pattern);
        Matcher m = r.matcher(contents);


        String dtoJson = lxcTextDto.getSuperIp();

        while (m.find()) {
            //信息独立截取，以空格分割
            String[] arr = m.group(0).split(" ");
            List<String> lxcList = Arrays.asList(arr);
            String lxcName = lxcList.get(0);
            try {
                JSONArray lxcArray = new JSONArray(dtoJson);
                for (int i = 0; i < lxcArray.length(); i++) {
                    JSONObject lxcJson = lxcArray.getJSONObject(i);
                    String myLxcName = lxcJson.getString("NAME");
                    String lxcDate = lxcJson.getString("DATE");
                    String lxcDead = lxcJson.getString("DEAD");
                    String lxcTag = lxcJson.getString("TAG");
                    String lxcIp = lxcJson.getString("IP");
                    String superPassword = lxcJson.getString("PASSWORD");
                    String superPort = lxcJson.getString("PORT");
                    if (lxcName.contains(myLxcName)) {
                        LxcLxc lxc = new LxcLxc();
                        lxc.setChildName(lxcName);
                        lxc.setName(myLxcName);
                        lxc.setTag(lxcTag);
                        lxc.setPassword(lxcList.get(2));
                        lxc.setSuperIp(lxcIp);
                        lxc.setSshPort(lxcList.get(1));
                        if (lxcList.size() > 3) {
                            lxc.setPortStart(lxcList.get(3));
                            lxc.setPortEnd(lxcList.get(4));
                        }
                        lxc.setSuperDate(lxcDate);
                        lxc.setSuperDead(lxcDead);
                        lxcMapper.insert(lxc);

                        //母鸡储存
                        LxcSuperLxc superLxc = new LxcSuperLxc();
                        superLxc.setName(myLxcName);
                        superLxc.setDate(lxcDate);
                        superLxc.setDead(lxcDead);
                        superLxc.setIp(lxcDate);
                        superLxc.setTag(lxcTag);
                        superLxc.setPassword(superPassword);
                        superLxc.setSshPort(superPort);
                        superLxcMapper.insert(superLxc);

                        i = lxcArray.length();
                    }

                }
            } catch (JSONException e) {
                throw new RuntimeException(e);
            }

        }
        return R.success("执行完毕");

    }


}
