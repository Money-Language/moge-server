package com.moge.moge.domain.user;

import com.moge.moge.domain.user.model.req.*;
import com.moge.moge.domain.user.model.res.*;
import com.moge.moge.domain.user.service.MailService;
import com.moge.moge.domain.user.service.UserService;
import com.moge.moge.global.common.BaseResponse;
import com.moge.moge.global.config.security.JwtService;
import com.moge.moge.global.exception.BaseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

import static com.moge.moge.global.exception.BaseResponseStatus.*;
import static com.moge.moge.global.util.ValidationRegex.*;

@RestController
@RequestMapping("/app/users")
public class UserController {

    final Logger logger = LoggerFactory.getLogger(this.getClass());

    @Autowired private final UserProvider userProvider;
    @Autowired private final UserService userService;
    @Autowired private final JwtService jwtService;
    @Autowired private final MailService mailService;

    public UserController(UserProvider userProvider, UserService userService, MailService mailService, JwtService jwtService){
        this.userProvider = userProvider;
        this.userService = userService;
        this.mailService = mailService;
        this.jwtService = jwtService;
    }

    /* 회원가입 */
    @ResponseBody
    @PostMapping("/sign-up")
    public BaseResponse<PostUserRes> createUser(@RequestBody PostUserReq postUserReq) {
        try {
            if (postUserReq.getEmail() == null) {
                return new BaseResponse<>(POST_USERS_EMPTY_EMAIL);
            }
            if (postUserReq.getNickname() == null) {
                return new BaseResponse<>(POST_USERS_EMPTY_NICKNAME);
            }
            if (postUserReq.getPassword() == null) {
                return new BaseResponse<>(POST_USERS_EMPTY_PASSWORD);
            }
            if (postUserReq.getRePassword() == null) {
                return new BaseResponse<>(POST_USERS_EMPTY_REPASSWORD);
            }
            if (postUserReq.getContract1() == null && postUserReq.getContract2() == null && postUserReq.getContract3() == null) {
                return new BaseResponse<>(POST_USERS_EMPTY_TERMS);
            }
            if (!isRegexEmail(postUserReq.getEmail())) {
                return new BaseResponse<>(POST_USERS_INVALID_EMAIL);
            }
            if (!isRegexPassword(postUserReq.getPassword())) {
                return new BaseResponse<>(POST_USERS_INVALID_PASSWORD);
            }
            if (!isRegexNickname(postUserReq.getNickname())) {
                return new BaseResponse<>(POST_USERS_INVALID_NICKNAME);
            }
            if (!postUserReq.getPassword().equals(postUserReq.getRePassword())) {
                return new BaseResponse<>(POST_USERS_INVALID_REPASSWORD);
            }

            PostUserRes postUserRes = userService.createUser(postUserReq);
            return new BaseResponse<>(postUserRes);

        } catch (BaseException exception) {
            return new BaseResponse<>((exception.getStatus()));
        }
    }

    /* 이메일 validation */
    @ResponseBody
    @PostMapping("/validate-email")
    public BaseResponse<String> validateEmail(@RequestParam("email") String email) {
        try {
            if (email == null) {
                return new BaseResponse<>(POST_USERS_EMPTY_EMAIL);
            }
            if (!isRegexEmail(email)) {
                return new BaseResponse<>(POST_USERS_INVALID_EMAIL);
            }
            userProvider.checkEmail(email);
            return new BaseResponse<>(SUCCESS_CHECK_EMAIL);
        } catch (BaseException exception) {
            return new BaseResponse<>(exception.getStatus());
        }
    }

    /* 닉네임 중복 체크 및 validation 처리 */
    @ResponseBody
    @PostMapping("/validate-nickname")
    public BaseResponse<String> validateNickname(@RequestParam("nickname") String nickname) {
        try {
            if (nickname == null) {
                return new BaseResponse<>(POST_USERS_EMPTY_NICKNAME);
            }
            if (!isRegexNickname(nickname)) {
                return new BaseResponse<>(POST_USERS_INVALID_NICKNAME);
            }
            userProvider.checkNickname(nickname);
            return new BaseResponse<>(SUCCESS_CHECK_NICKNAME);
        } catch (BaseException exception) {
            return new BaseResponse<>(exception.getStatus());
        }
    }

    /* 로그인 */
    @ResponseBody
    @PostMapping("/login")
    public BaseResponse<PostLoginRes> login(@RequestBody PostLoginReq postLoginReq) {
        if (postLoginReq.getEmail() == null) {
            return new BaseResponse<>(POST_USERS_EMPTY_EMAIL);
        }
        if (postLoginReq.getPassword() == null) {
            return new BaseResponse<>(POST_USERS_EMPTY_PASSWORD);
        }
        if (!isRegexEmail(postLoginReq.getEmail())) {
            return new BaseResponse<>(POST_USERS_INVALID_EMAIL);
        }

        try {
            PostLoginRes postLoginRes = userProvider.login(postLoginReq);
            return new BaseResponse<>(postLoginRes);
        } catch (BaseException exception) {
            return new BaseResponse<>(exception.getStatus());
        }
    }

    /* 패스워드 변경 */
    @ResponseBody
    @PatchMapping("/{userIdx}/password")
    public BaseResponse<String> updatePassword(@PathVariable("userIdx") int userIdx,
                                               @RequestBody PatchUserPasswordReq patchUserPasswordReq) {

        if (!isRegexPassword(patchUserPasswordReq.getModPassword())) {
            return new BaseResponse<>(POST_USERS_INVALID_PASSWORD);
        }
        if (!patchUserPasswordReq.getModPassword().equals(patchUserPasswordReq.getReModPassword())) {
            return new BaseResponse<>(POST_USERS_NEW_PASSWORD_NOT_CORRECT);
        }

        try {
            int userIdxByJwt = jwtService.getUserIdx();
            if (userIdxByJwt != userIdx) {
                return new BaseResponse<>(INVALID_USER_JWT);
            }
            userService.updatePassword(userIdx, patchUserPasswordReq);
            return new BaseResponse<>(SUCCESS_UPDATE_PASSWORD);
        } catch(BaseException exception) {
            return new BaseResponse<>(exception.getStatus());
        }
    }

    /* 이메일 인증 메일 발송 */
    @ResponseBody
    @PostMapping("/send-email")
    public BaseResponse<String> sendEmail(@RequestParam("email") String email) {
        if (email != null) {
            if (!isRegexEmail(email)) {
                return new BaseResponse<>(POST_USERS_INVALID_EMAIL);
            }
        }

        try {
            String code = mailService.sendCertifiedMail(email);
            mailService.insertCertifiedCode(email, code);
            return new BaseResponse<>(code);
        } catch (BaseException exception) {
            return new BaseResponse<>(exception.getStatus());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /* 이메일 중복 확인 */
    @ResponseBody
    @PostMapping("/login/check-email")
    public BaseResponse<String> checkEmail(@RequestBody PostEmailCheckReq postEmailCheckReq) throws BaseException {
        if (postEmailCheckReq.getEmail() != null) {
            if (!isRegexEmail(postEmailCheckReq.getEmail())) {
                return new BaseResponse<>(POST_USERS_INVALID_EMAIL);
            }
        }

        if (!isRegexEmailCode(postEmailCheckReq.getCode())) {
            return new BaseResponse<>(POST_USERS_INVALID_EMAIL_CODE);
        }

        if (userProvider.checkCertifiedEmail(postEmailCheckReq.getEmail()) == 0) {
            return new BaseResponse<>(POST_USERS_EMPTY_CERTIFIED_EMAIL);
        }

        int timeDiff = userProvider.checkCertifiedTime(postEmailCheckReq.getEmail());
        if (timeDiff >= 1000) {
            return new BaseResponse<>(FAILED_TO_CERTIFY_TIME);
        }

        if (!(userProvider.checkCertifiedCode(postEmailCheckReq.getEmail(), postEmailCheckReq.getCode()))) {
            return new BaseResponse<>(FAILED_TO_CERTIFY_CODE);
        }

        return new BaseResponse<>(SUCCESS_CHECK_CERTIFY_EMAIL);
    }

    /* 관심 키워드 설정 (5개중 3개) */
    @ResponseBody
    @PostMapping("/{userIdx}/keyword")
    public BaseResponse<String> createUserKeyword(@PathVariable("userIdx") int userIdx, @RequestBody PostUserKeywordReq postUserKeywordReq) {
        try {
            // jwt 토큰 확인
            int userIdxByJwt = jwtService.getUserIdx();
            if (userIdxByJwt != userIdx) {
                return new BaseResponse<>(INVALID_USER_JWT);
            }
            // 3개를 선택하지 않았다면 예외 처리
            if (postUserKeywordReq.getCategoryIdx().size() != 3) {
                return new BaseResponse<>(POST_USERS_CATEGORY_NUM);
            }
            userService.createUserKeyword(userIdx, postUserKeywordReq);
            return new BaseResponse<>(SUCCESS_CREATE_KEYWORD);
        } catch (BaseException exception) {
            return new BaseResponse<>((exception.getStatus()));
        }
    }

    /* 관심 키워드 수정 */
    @ResponseBody
    @PatchMapping("/{userIdx}/keyword")
    public BaseResponse<String> updateUserKeyword(@PathVariable("userIdx") int userIdx, @RequestBody PatchUserKeywordReq patchUserKeywordReq) {
        try {
            // jwt 토큰 확인
            int userIdxByJwt = jwtService.getUserIdx();
            if (userIdxByJwt != userIdx) {
                return new BaseResponse<>(INVALID_USER_JWT);
            }
            // 3개를 선택하지 않았다면 예외 처리
            if (patchUserKeywordReq.getCategoryIdx().size() != 3) {
                return new BaseResponse<>(POST_USERS_CATEGORY_NUM);
            }

            userService.updateUserKeyword(userIdx, patchUserKeywordReq);
            return new BaseResponse<>(SUCCESS_UPDATE_KEYWORD);

        } catch (BaseException exception) {
            return new BaseResponse<>((exception.getStatus()));
        }
    }

    /* 프로필 조회 */
    @ResponseBody
    @GetMapping("/{userIdx}/profile")
    public BaseResponse<GetUserProfileRes> getUserProfile(@PathVariable("userIdx") int userIdx) {
        try {
            int userIdxByJwt = jwtService.getUserIdx();
            if (userIdxByJwt != userIdx) {
                return new BaseResponse<>(INVALID_USER_JWT);
            }
            GetUserProfileRes userProfile = userProvider.getUserProfile(userIdx);
            return new BaseResponse<>(userProfile);

        } catch (BaseException exception) {
            return new BaseResponse<>((exception.getStatus()));
        }
    }


    /* 프로필 사진 + 닉네임 수정 */
    @ResponseBody
    @PatchMapping("/{userIdx}/profile")
    public BaseResponse<String> updateProfile(@PathVariable("userIdx") int userIdx,
                                              @RequestPart(value = "profileImage", required = false) MultipartFile profileImage,
                                              @RequestPart(value = "nickname") String nickname) {
        try {
            int userIdxByJwt = jwtService.getUserIdx();
            if (userIdxByJwt != userIdx) {
                return new BaseResponse<>(INVALID_USER_JWT);
            }
            userService.updateProfile(userIdx, profileImage, nickname);
            return new BaseResponse<>(SUCCESS_UPDATE_PROFILE);

        } catch (BaseException exception) {
            return new BaseResponse<>((exception.getStatus()));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /* 프로필 사진 삭제 */
    @ResponseBody
    @DeleteMapping("/{userIdx}/profile")
    public BaseResponse<String> deleteProfileImage(@PathVariable("userIdx") int userIdx) {
        try {
            int userIdxByJwt = jwtService.getUserIdx();
            if (userIdxByJwt != userIdx) {
                return new BaseResponse<>(INVALID_USER_JWT);
            }
            userService.deleteProfileImage(userIdx);
            return new BaseResponse<>(SUCCESS_DELETE_USER_PROFILE_IMAGE);

        } catch (BaseException exception) {
            return new BaseResponse<>((exception.getStatus()));
        }
    }

    /* 팔로우 등록 + 취소 */
    @ResponseBody
    @PostMapping("/follow")
    public BaseResponse<String> createUserFollow(@RequestBody PostUserFollowReq postUserFollowReq) {
        try {
            userService.createUserFollow(postUserFollowReq.getUserIdx(), postUserFollowReq.getFollowingIdx());
            return new BaseResponse<>(SUCCESS_CREATE_FOLLOW);
        } catch (BaseException exception) {
            return new BaseResponse<>((exception.getStatus()));
        }
    }

    /* 팔로잉 조회 (내가 팔로우 하는 사람) */
    @ResponseBody
    @GetMapping("/{userIdx}/following")
    public BaseResponse<List<GetUserFollowRes>> getUserFollowings(@PathVariable("userIdx") int userIdx,
                                                                  int page) {
        try {
            int userIdxByJwt = jwtService.getUserIdx();
            if (userIdxByJwt != userIdx) {
                return new BaseResponse<>(INVALID_USER_JWT);
            }
            if (page <= 0) {
                return new BaseResponse<>(POST_FOLLOW_INVALID_PAGE);
            }

            List<GetUserFollowRes> getUserFollowingsResList = userProvider.getUserFollowings(userIdx, page);
            return new BaseResponse<>(getUserFollowingsResList);

        } catch (BaseException exception) {
            return new BaseResponse<>((exception.getStatus()));
        }
    }

    /* 팔로워 조회 (나를 팔로우하는 사람) */
    @ResponseBody
    @GetMapping("/{userIdx}/follower")
    public BaseResponse<List<GetUserFollowRes>> getUserFollowers(@PathVariable("userIdx") int userIdx, int page) {
        try {
            int userIdxByJwt = jwtService.getUserIdx();
            if (userIdxByJwt != userIdx) {
                return new BaseResponse<>(INVALID_USER_JWT);
            }
            if (page <= 0) {
                return new BaseResponse<>(POST_FOLLOW_INVALID_PAGE);
            }

            List<GetUserFollowRes> getUserFollowersRes = userProvider.getUserFollowers(userIdx, page);
            return new BaseResponse<>(getUserFollowersRes);
        } catch (BaseException exception) {
            return new BaseResponse<>((exception.getStatus()));
        }
    }

    /* 유저가 좋아요 누른 게시글 조회
    * 카테고리명, 제목, 퀴즈개수 조회수, 좋아요수 */
    @ResponseBody
    @GetMapping("/{userIdx}/board/like")
    public BaseResponse<List<GetUserBoardLikeRes>> getUserBoardLike(@PathVariable("userIdx") int userIdx) {
        try {
            int userIdxByJwt = jwtService.getUserIdx();
            if (userIdxByJwt != userIdx) {
                return new BaseResponse<>(INVALID_USER_JWT);
            }
            List<GetUserBoardLikeRes> userBoardLike = userProvider.getUserBoardLike(userIdx);
            return new BaseResponse<>(userBoardLike);

        } catch (BaseException exception) {
            return new BaseResponse<>((exception.getStatus()));
        }
    }

    /* 유저 탈퇴 */
    @ResponseBody
    @DeleteMapping("/{userIdx}")
    public BaseResponse<String> deleteUser(@PathVariable("userIdx") int userIdx) {
        try {
            int userIdxByJwt = jwtService.getUserIdx();
            if (userIdxByJwt != userIdx) {
                return new BaseResponse<>(INVALID_USER_JWT);
            }
            userService.deleteUser(userIdx);
            return new BaseResponse<>(SUCCESS_DELETE_USER);

        } catch (BaseException exception) {
            return new BaseResponse<>((exception.getStatus()));
        }
    }

}
