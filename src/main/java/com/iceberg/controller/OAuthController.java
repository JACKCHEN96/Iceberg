package com.iceberg.controller;

import com.iceberg.entity.Privilege;
import com.iceberg.entity.UserInfo;
import com.iceberg.service.PrivilegeService;
import com.iceberg.service.UserInfoService;
import com.iceberg.utils.Config;
import com.iceberg.utils.HttpClientUtils;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

import javax.annotation.Resource;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
public class OAuthController {
  @Resource
  private UserInfoService userInfoService;
  @Resource
  private PrivilegeService privilegeService;

  @RequestMapping("/oauth/github/callback")
  public String githubLogin(String code, HttpServletRequest request, HttpServletResponse httpResponse)
      throws IOException {
    // code is sent by github, need use it to exchange access_token to get userinfo
    // 1. get access_token
    String s3 = "https://github.com/login/oauth/access_token";
    Map<String, String> paramMap = new HashMap<>();
    paramMap.put("client_id", "d644cfff862d6e5d155a");
    paramMap.put("client_secret", "ac4a5c749e66dcb25bc7f9a216a7983b6326dc5a");
    paramMap.put("code", code);
    paramMap.put("redirect_uri", "http://localhost:8080/oauth/github/callback");

    String accessTokenResponse = HttpClientUtils.doPost(s3, paramMap);
    System.out.println(accessTokenResponse);
    String access_token = parseAccessTokenResponse(accessTokenResponse);
    System.out.println(access_token);

    // 2. access_token get user info

    URL url = new URL("https://api.github.com/user");
    HttpURLConnection conn = (HttpURLConnection) url.openConnection();

    conn.setRequestProperty("Authorization", "Bearer " + access_token);
    conn.setRequestProperty("Content-Type", "application/json");
    conn.setRequestMethod("GET");

    BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream(), "UTF-8"));
    String output;
    StringBuffer response = new StringBuffer();
    while ((output = in.readLine()) != null) {
      response.append(output);
    }

    in.close();
    System.out.println("Response:-" + response.toString());
    // 3. down to database, set auth-provider to be github
    Map<String, String> userInfoMap = parseGithubObject(response.toString());
    Integer id = Integer.parseInt(userInfoMap.get("id"));
    System.out.println(id);
    String username = userInfoMap.get("username");
    System.out.println(username);
    String email = userInfoMap.get("email");
    System.out.println(email);
    UserInfo user = new UserInfo(id, username, email);

    // if user is existed
    if (userInfoService.userIsExisted(user)) {
    } else {
      userInfoService.add(user);
    }
    setSessionUserInfo(user, request.getSession());
    // 4. configure user session in order to login in

    return "redirect:http://localhost:8080/";
  }

  public static String parseAccessTokenResponse(String accessTokenResponse) {
    if (accessTokenResponse == null) {
      return null;
    }
    String accessToken = null;
    String[] res = accessTokenResponse.split("&");
    String accessTokenKV = res[0];
    accessToken = accessTokenKV.substring(13);
    System.out.println("accessToken is " + accessToken);
    return accessToken;
  }

  public static Map<String, String> parseGithubObject(String response) {
    if (response == null) {
      return null;
    }
    String email = null;
    String[] res = response.toString().split(",");
    if (res.length >= 10) {
      String emailKV = res[res.length - 10];
      email = emailKV.substring(9, emailKV.length() - 1);
    }
    String username = res[0].substring(1);
    String id = res[1];
    Map<String, String> userMap = new HashMap<>();
    String value = username.substring(9);
    String usernameValue = value.substring(0, value.length() - 1);
    userMap.put("username", usernameValue);
    userMap.put("id", id.substring(5));
    userMap.put("email", email);
    return userMap;

  }

  /**
   * get user privilege info through user info and save it in session
   * 
   * @param userInfo
   * @param session
   * @return
   */
  public UserInfo setSessionUserInfo(UserInfo userInfo, HttpSession session) {
    List<Privilege> privileges = privilegeService.getPrivilegeByRoleid(userInfo.getRoleid());
    userInfo.setPrivileges(privileges);
    session.setAttribute(Config.CURRENT_USERNAME, userInfo);
    return userInfo;

  }
}
