package com.nowcoder.community.controller;

import com.google.code.kaptcha.Producer;
import com.nowcoder.community.annotation.LoginRequired;
import com.nowcoder.community.constant.*;
import com.nowcoder.community.entity.DiscussPost;
import com.nowcoder.community.entity.ReplyInfo;
import com.nowcoder.community.entity.User;
import com.nowcoder.community.exception.CustomizeException;
import com.nowcoder.community.service.*;
import com.nowcoder.community.util.CommonUtil;
import com.nowcoder.community.util.RedisKeyUtil;
import com.nowcoder.community.util.ThreadLocalHolder;
import com.nowcoder.community.vo.PageInfo;
import com.qiniu.util.Auth;
import com.qiniu.util.StringMap;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * 用户管理控制层
 * @author Alex
 * @version 1.0
 * @date 2022/2/2 11:58
 */
@Controller
@RequestMapping("user")
@Slf4j
@Api(tags = "用户相关接口")
public class UserController {

    @Autowired
    private UserService userService;

    @Autowired
    private DiscussPostService discussPostService;

    @Autowired
    private FollowService followService;

    @Autowired
    private CommentService commentService;

    @Autowired
    private LikeService likeService;

    @Autowired
    private Producer kaptchaProducer;

    @Autowired
    private RedisTemplate redisTemplate;

    @Value("${qiniu.key.access}")
    private String accessKey;

    @Value("${qiniu.key.secret}")
    private String secretKey;

    @Value("${qiniu.bucket.header.name}")
    private String headerBucketName;

    @Value("${qiniu.bucket.header.url}")
    private String headerBucketUrl;

    @Value("${server.servlet.context-path}")
    private String contextPath;

    @Value("${community.path.upload}")
    private String uploadPath;

    @Value("${community.path.domain}")
    private String domain;

    @Autowired
    private ThreadLocalHolder<User> userThreadLocalHolder;


    @ModelAttribute
    public void siteAttrbute(Model model){
        User user = userThreadLocalHolder.getCache();
        if (!CommonUtil.isEmtpy(user)) {
            model.addAttribute("userId",user.getId());
        }
    }

    @RequestMapping(path = "loginPage",method = RequestMethod.GET)
    public String loginPage(){
        return "site/login";
    }

    @RequestMapping(path = "registerPage",method = RequestMethod.GET)
    public String registerPage(){
        return "site/register";
    }

    @RequestMapping(path = "forgetPwdPage",method = RequestMethod.GET)
    public String forgetPwdPage(){
        return "/site/forget";
    }

    @RequestMapping(path = "register", method = RequestMethod.POST)
    public String register(Model model, User user){
        Map<String,Object> map = userService.register(user);
        if(map == null || map.isEmpty()){
            model.addAttribute("msg","注册成功，我们已经向您的邮箱发送了一封激活邮件，请尽快激活！");
            model.addAttribute("target","/index");
            return "site/operate-result";
        }else{
            model.addAttribute("usernameMsg",map.get("usernameMsg"));
            model.addAttribute("passwordMsg",map.get("passwordMsg"));
            model.addAttribute("emailMsg",map.get("emailMsg"));
            return "site/register";
        }
    }

    @RequestMapping(path = "login",method = RequestMethod.POST)
    public String login(Model model,/*HttpSession session,*/HttpServletResponse response,
                        String username,String password,String verifyCode,boolean rememberMe,@CookieValue("kaptchaOwner") String kaptchaOwner){
        String kaptcha=null;
        if(StringUtils.isNotBlank(kaptchaOwner)){
            String kaptchaKey = RedisKeyUtil.getKaptchaKey(kaptchaOwner);
            kaptcha = (String) redisTemplate.opsForValue().get(kaptchaKey);
        }
        if(StringUtils.isBlank(kaptcha)||StringUtils.isBlank(verifyCode)||!kaptcha.equalsIgnoreCase(verifyCode)){
            model.addAttribute("codeMsg","验证码不正确");
            return "/site/login";
        }

        // 检查账号，密码
        int expiredSeconds = rememberMe ? LoginConstant.REMEMBERME_EXPIRED_SECONDS.getExpired():LoginConstant.DEFAULT_EXPIRED_SECONDS.getExpired();
        Map<String, Object> map = userService.login(username, password, expiredSeconds);
        if(map.containsKey(SystemConstant.USER_CREDENTIAL_TICKET)){
            Cookie cookie = new Cookie("ticket", map.get("ticket").toString());
            cookie.setPath(contextPath);
            cookie.setMaxAge(expiredSeconds);
            response.addCookie(cookie);
            return "redirect:/index";
        }else{
            model.addAttribute("usernameMsg",map.get("usernameMsg"));
            model.addAttribute("passwordMsg",map.get("passwordMsg"));
            return "site/login";
        }


    }

    @RequestMapping(path = "logout",method = RequestMethod.GET)
    public String logout(@CookieValue("ticket") String ticket){
        if(StringUtils.isBlank(ticket)){
            return "index";
        }
        userService.logout(ticket);
        userThreadLocalHolder.clear();
        SecurityContextHolder.clearContext();
        return "redirect:/user/loginPage";
    }


    @RequestMapping(path = "/activation/{userId}/{code}",method = RequestMethod.GET)
    public String activation(Model model, @PathVariable("userId") int userId,@PathVariable("code") String code){
        int activationResult = userService.activation(userId, code);
        if(activationResult == ActivationStatus.ACTIVATION_SUCCESS.getCode()){
            model.addAttribute("msg","激活成功,您的账号已经可以正常使用了！");
            model.addAttribute("target","/user/loginPage");
        }else if(activationResult == ActivationStatus.ACTIVATION_FAILURE.getCode()){
            model.addAttribute("msg","无效操作，该账号已经激活过了");
            model.addAttribute("target","/index");
        }else{
            model.addAttribute("msg","激活失败,您提供的激活码不正确");
            model.addAttribute("target","/index");
        }

        return "/site/operate-result";
    }

    @RequestMapping(path = "/kaptcha",method = RequestMethod.GET)
    public void getKaptcha(HttpServletResponse response/*, HttpSession session*/){
        //生成验证码
        String text = kaptchaProducer.createText();
        BufferedImage image = kaptchaProducer.createImage(text);


        // 验证码归属者,每次刷新验证码，为客户端用户生成临时凭证返回给用户,其中临时凭证存放在cookie中
        String kaptchaOwner = CommonUtil.generateUUID();
        Cookie cookie = new Cookie("kaptchaOwner", kaptchaOwner);
        cookie.setMaxAge(60);
        cookie.setPath(contextPath);
        response.addCookie(cookie);

        // 将验证码存入redis
        String kaptchaKey = RedisKeyUtil.getKaptchaKey(kaptchaOwner);
        redisTemplate.opsForValue().set(kaptchaKey,text,60, TimeUnit.SECONDS);

        // 将图片输出给浏览器
        response.setContentType("image/png");
        try {
            OutputStream os = response.getOutputStream();
            ImageIO.write(image,"png",os);
        }catch (Exception e){
            log.error("验证码生成失败，{}",e.getMessage());
        }
    }

    @LoginRequired
    @RequestMapping(path = "/settings",method = RequestMethod.GET)
    public String getSettingsPage(Model model){

        // 上传文件名称 -> 目的：服务器缓存无效，上传历史保留
        String fileName = CommonUtil.generateUUID();

        // 设置响应信息
        StringMap policy = new StringMap();
        policy.put("returnBody", CommonUtil.getJsonString(0,"生成成功"));

        // 生成七牛云上传凭证
        Auth auth = Auth.create(accessKey, secretKey);
        String uploadToken = auth.uploadToken(headerBucketName, fileName, 3600, policy);

        model.addAttribute("uploadToken",uploadToken);
        model.addAttribute("fileName",fileName);

        return "site/setting";
    }

    /**
     * 更新头像路径(异步访问)：
     *      更新为七牛云服务器的路径
     * @param fileName
     * @return
     */
    @RequestMapping(path = "/header/url",method = RequestMethod.POST)
    @ResponseBody
    @ApiOperation(value = "更新头像url地址")
    public String updateHeaderUrl(String fileName){
        if (StringUtils.isBlank(fileName)){
            return CommonUtil.getJsonString(1,"文件名不能为空");
        }

        String url = headerBucketUrl + "/" + fileName;

        userService.updateHeader(userThreadLocalHolder.getCache().getId(),url);

        return CommonUtil.getJsonString(0,"success");
    }


    @LoginRequired
    @RequestMapping(path = "/upload",method = RequestMethod.POST)
    @Deprecated
    public String uploadHeader(MultipartFile headerImg,Model model){
        if(CommonUtil.isEmtpy(headerImg)){
            model.addAttribute("error","您还没有上传头像");
            return "/site/setting";
        }
        String fileName = headerImg.getOriginalFilename();
        String fileSuffix = fileName.substring(fileName.lastIndexOf("."));
        if(StringUtils.isBlank(fileSuffix)){
            model.addAttribute("error","文件格式不正确!");
            return "site/setting";
        }

        // 生成随机文件名
        fileName = CommonUtil.generateUUID() + fileSuffix;
        // 确定文件存放路径
        String filePath = uploadPath + "/" +fileName;
        File dist = new File(filePath);
        if (!dist.getParentFile().exists()){
            dist.getParentFile().mkdirs();
        }
        try {
            // 存储文件
            headerImg.transferTo(dist);
        } catch (IOException e) {
            log.error("上传文件失败:"+e.getMessage());
            throw new RuntimeException("上传文件失败，服务器异常：",e);
        }

        // 更新当前用户头像路径(web访问路径)
        // http://localhost:8088/community/user/header/xxx.png
        User user = userThreadLocalHolder.getCache();
        String headerUrl = domain + contextPath + "/user/header/"+fileName;
        userService.updateHeader(user.getId(),headerUrl);
        return "redirect:/index";
    }

    @RequestMapping(path = "/header/{fileName}",method = RequestMethod.GET)
    @LoginRequired
    public void getHeader(@PathVariable("fileName") String fileName, HttpServletResponse response){
        // 获取用户头像文件存放路径
        File file = new File(uploadPath + "/" + fileName);
        // 文件后缀
        String suffix = fileName.substring(fileName.lastIndexOf("."));

        // 响应图片
        response.setContentType("img/"+suffix);

        try (OutputStream os = response.getOutputStream();
             FileInputStream fis = new FileInputStream(file);){

            byte[] buffer = new byte[1024];
            int offset = 0;
            while ((offset=fis.read(buffer))!=-1){
                os.write(buffer,0,offset);
            }
        } catch (IOException e) {
            log.error("读取头像失败:{}",e.getMessage());
        }
    }

    @RequestMapping(path = "/profile/{userId}",method = RequestMethod.GET)
    @LoginRequired
    public String getProfilePage(Model model,@PathVariable("userId") int userId){
        User user = userService.findUserById(userId);
        if(CommonUtil.isEmtpy(user)){
            throw new RuntimeException("用户登录异常");
        }
        // 用户信息
        model.addAttribute("user",user);

        // 显示正确的点赞数量、关注数量、粉丝数量、用户关注状态
        // 点赞数量
        int userLikeCount = likeService.findUserLikeCount(userId);
        model.addAttribute("userLikeCount",userLikeCount);

        // 关注数量
        long followeeCount = followService.findFolloweeCount(userId, CommentEntityConstant.ENTITY_TYPE_USER.getType());
        model.addAttribute("followeeCount",followeeCount);


        // 粉丝数量
        long followerCount = followService.findFollowerCount(CommentEntityConstant.ENTITY_TYPE_USER.getType(), userId);
        model.addAttribute("followerCount",followerCount);

        // 是否已关注
        boolean hasFollowed = false;
        if(!CommonUtil.isEmtpy(userThreadLocalHolder.getCache())){
            hasFollowed = followService.hasFollowed(userThreadLocalHolder.getCache().getId(),CommentEntityConstant.ENTITY_TYPE_USER.getType(),userId);
        }
        model.addAttribute("hasFollowed",hasFollowed);

        return "/site/profile";
    }

    @RequestMapping(path = "/mypost",method = RequestMethod.GET)
    @LoginRequired
    public String getMyPostPage(Model model, PageInfo pageInfo){
        // 获取当前登录用户信息
        User user = userThreadLocalHolder.getCache();
        int discussPostCount = discussPostService.findDiscussPostCount(user.getId());
        model.addAttribute("discussPostCount",discussPostCount);

        // 设置分页信息
        pageInfo.setPath("/user/mypost");
        pageInfo.setLimit(5);
        pageInfo.setRows(discussPostCount);

        // 查询帖子列表
        List<DiscussPost> discussPosts = discussPostService.findDiscussPostList(user.getId(), pageInfo.getOffset(), pageInfo.getLimit(),0);
        model.addAttribute("discussPosts",discussPosts);
        return "/site/my-post";
    }

    @RequestMapping(path = "/myreply",method = RequestMethod.GET)
    @LoginRequired
    public String getMyReplyPage(PageInfo pageInfo,Model model){

        // 获取当前登录用户信息
        User user = userThreadLocalHolder.getCache();
        int replyInfoCount = commentService.findReplyInfoCount(user.getId());
        model.addAttribute("replyInfoCount",replyInfoCount);

        // 设置分页信息
        pageInfo.setPath("/user/mypost");
        pageInfo.setLimit(5);
        pageInfo.setRows(replyInfoCount);

        // 查询帖子回复列表
        List<ReplyInfo> replyInfoList = commentService.findReplyInfoList(user.getId(), pageInfo.getOffset(), pageInfo.getLimit());
        model.addAttribute("replyInfoList",replyInfoList);

        return "/site/my-reply";
    }

    @RequestMapping(path = "/updatePassword",method = RequestMethod.POST)
    @LoginRequired
    public String updatePassword(String originPassword,String newPassword,String repeatPassword,Model model){
        // 获取当前登录用户信息
        User user = userThreadLocalHolder.getCache();
        model.addAttribute("originPassword",originPassword);
        model.addAttribute("newPassword",newPassword);
        model.addAttribute("repeatPassword",repeatPassword);

        // 密码信息校验
        if(!CommonUtil.md5Encode(originPassword).equals(user.getPassword())){
            model.addAttribute("errorMsg1","原密码错误");
            return "site/setting";
        }

        if(CommonUtil.md5Encode(newPassword).equals(user.getPassword())){
            model.addAttribute("errorMsg2","新密码不能与原密码一样");
            return "site/setting";
        }

        if(!newPassword.equals(repeatPassword)){
            model.addAttribute("errorMsg3","两次输入密码不一致");
            return "site/setting";
        }

        user.setPassword(CommonUtil.md5Encode(newPassword));
        int result = userService.updatePassword(user);
        if(result<=0){
            model.addAttribute("errorMsg","更新密码失败");
            return "site/setting";
        }

        return "redirect:/index";
    }

    @RequestMapping(path = "/forgetPwd",method = RequestMethod.POST)
    public String forgetPwd(Model model,String email,String verifyCode,String newPassword,HttpSession session,@CookieValue String verifyEmail){
        // 非空校验
        if(StringUtils.isBlank(email)){
            model.addAttribute("emailMsg","邮箱为空");
            return "/site/forget";
        }
        if(StringUtils.isBlank(verifyCode)){
            model.addAttribute("verifyCodeMsg","验证码为空");
            return "/site/forget";
        }
        if(StringUtils.isBlank(newPassword)){
            model.addAttribute("passwordMsg","密码为空");
            return "/site/forget";
        }

        model.addAttribute("email",email);
        model.addAttribute("verifyCode",verifyCode);
        model.addAttribute("newPassword",newPassword);

        // 检查验证码
        String emailVerifyCode = (String) session.getAttribute("verifyCode");
        if(StringUtils.isBlank(emailVerifyCode)||StringUtils.isBlank(emailVerifyCode)||!verifyCode.equalsIgnoreCase(emailVerifyCode)){
            model.addAttribute("verifyCodeMsg","验证码不正确");
            return "/site/forget";
        }

        if(CommonUtil.isEmtpy(verifyEmail)){
            model.addAttribute("verifyCodeMsg","验证码超时");
            return "/site/forget";
        }

        //验证邮箱
        User user = userService.findUserByEmail(email);
        if(CommonUtil.isEmtpy(user)){
            model.addAttribute("emailMsg","邮箱不存在");
            return "/site/forget";
        }else{
            // 检查密码
            if(CommonUtil.md5Encode(newPassword).equals(user.getPassword())){
                model.addAttribute("passwordMsg","新密码不能和原密码一致");
                return "/site/forget";
            }else{
                // 执行插入操作
                user.setPassword(CommonUtil.md5Encode(newPassword));
                int result = userService.updatePassword(user);
                if(result<=0){
                    model.addAttribute("errorMsg","重置密码失败!");
                    return "/site/forget";
                }
            }
        }
        return "/site/login";
    }

    @RequestMapping(path = "/emailVerify",method = RequestMethod.POST)
    public void emailVerify(HttpSession session,String email,HttpServletResponse response){
        String verifyCode = CommonUtil.generateUUID().substring(0, 6);
        session.setAttribute("verifyCode",verifyCode);

        if(CommonUtil.isEmtpy(email)){
            throw new CustomizeException(ResultEnum.EMAIL_NOT_EXIST,"emailVerify()");
        }
        // 调用service发送验证码邮件
        userService.sendVerifyEmail(verifyCode,email);

        // 生成一个cookie，用于判断验证码是否失效
        Cookie cookie = new Cookie("verifyEmail", CommonUtil.generateUUID());
        cookie.setPath(contextPath);
        cookie.setDomain(domain);
        cookie.setMaxAge(SysEmailConstant.EMAIL_VERIFYCODE_EXPIRED.getSec());

        response.addCookie(cookie);
    }

}
