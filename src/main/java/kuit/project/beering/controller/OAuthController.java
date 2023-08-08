package kuit.project.beering.controller;

import kuit.project.beering.domain.AgreementName;
import kuit.project.beering.domain.OAuthType;
import kuit.project.beering.dto.request.auth.KakaoLoginRequest;
import kuit.project.beering.dto.request.auth.OAuthSignupRequest;
import kuit.project.beering.dto.request.member.AgreementRequest;
import kuit.project.beering.dto.response.SignupNotCompletedResponse;
import kuit.project.beering.dto.response.member.MemberLoginResponse;
import kuit.project.beering.security.auth.oauth.helper.OAuthHelperResolver;
import kuit.project.beering.service.OAuthService;
import kuit.project.beering.util.BaseResponse;
import kuit.project.beering.util.BaseResponseStatus;
import kuit.project.beering.util.exception.AgreementValidationException;
import kuit.project.beering.util.exception.FieldValidationException;
import kuit.project.beering.util.exception.SignupNotCompletedException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.validation.ObjectError;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.List;

@RestController
@Slf4j
@RequiredArgsConstructor
@RequestMapping("/oauth")
public class OAuthController {

    private final OAuthService oauthService;
    private final OAuthHelperResolver oauthHelperResolver;

    @GetMapping("/kakao/callback")
    public BaseResponse<MemberLoginResponse> kakaoOauth(@ModelAttribute KakaoLoginRequest kakaoLoginRequest) {

        if (kakaoLoginRequest.getError() != null) return new BaseResponse<>(BaseResponseStatus.OAUTH_LOGIN_FAILED);

        MemberLoginResponse memberLoginResponse = oauthService.oauth(kakaoLoginRequest.getCode(), oauthHelperResolver.getOauthHelper(OAuthType.KAKAO));

        return new BaseResponse<>(memberLoginResponse);
    }

    @PostMapping("/signup")
    public BaseResponse<Object> signup(@RequestBody @Validated OAuthSignupRequest request,
                                       BindingResult bindingResult) {

        validateAgreement(request, bindingResult);

        if (bindingResult.hasFieldErrors()) throw new FieldValidationException(bindingResult);
        if (bindingResult.hasGlobalErrors()) throw new AgreementValidationException(bindingResult);

        MemberLoginResponse response = oauthService.signup(request, oauthHelperResolver.getOauthHelper(request.getOAuthType()));

        return new BaseResponse<>(response);
    }

    @ExceptionHandler(SignupNotCompletedException.class)
    public BaseResponse<SignupNotCompletedResponse> loginNotCompleted(SignupNotCompletedException ex) {
        return new BaseResponse<>(SignupNotCompletedResponse.builder()
                .isLoginCompleted(false)
                .sub(ex.getSub()).build());
    }


    /**
     * @param request
     * @param bindingResult
     * @Brief Agreement 값 제대로 들어왔는지 확인. 개수, 약관 포함 여부
     */
    private static void validateAgreement(OAuthSignupRequest request, BindingResult bindingResult) {
        List<AgreementRequest> agreements = request.getAgreements();
        List<AgreementName> agreementNames = agreements.stream()
                .map(AgreementRequest::getName).toList();

        /**
         * @Condition 약관 개수와 필요한 약관이 모두 들어왔는지
         */
        if (agreementNames.size() != AgreementName.values().length &&
                !agreementNames.containsAll(Arrays.stream(AgreementName.values()).toList())) {
            bindingResult.addError(new ObjectError
                    ("Agreement", "SERVICE, PERSONAL, MARKETING 을 모두 포함"));
        }

        List<AgreementRequest> agreementRequests = agreements.stream().filter(agreementRequest ->
                AgreementName.SERVICE.equals(agreementRequest.getName()) ||
                        AgreementName.PERSONAL.equals(agreementRequest.getName())).toList();

        agreementRequests.forEach(agreementRequest -> {
            if (!agreementRequest.getIsAgreed())
                bindingResult.addError(new FieldError(
                        "Agreement", "name", agreementRequest.getName(),
                        true, null, null,
                        "SERVICE, PERSONAL 의 isAgreed 값은 반드시 TRUE"));
        });
    }
}