package kuit.project.beering.service;

import kuit.project.beering.domain.*;
import kuit.project.beering.domain.image.ReviewImage;
import kuit.project.beering.dto.request.review.ReviewCreateRequestDto;
import kuit.project.beering.dto.request.selectedOption.SelectedOptionCreateRequestDto;
import kuit.project.beering.dto.response.review.ReviewDetailReadResponseDto;
import kuit.project.beering.dto.response.review.ReviewResponseDto;
import kuit.project.beering.repository.*;
import kuit.project.beering.util.exception.DrinkException;
import kuit.project.beering.util.exception.MemberException;
import kuit.project.beering.util.exception.ReviewException;
import kuit.project.beering.util.s3.AwsS3Uploader;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import static kuit.project.beering.util.BaseResponseStatus.*;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class ReviewService {

    private final ReviewRepository reviewRepository;
    private final SelectedOptionRepository selectedOptionRepository;
    private final ReviewOptionRepository reviewOptionRepository;
    private final DrinkRepository drinkRepository;
    private final MemberRepository memberRepository;
    private final AwsS3Uploader awsS3Uploader;
    private final ReviewImageRepository reviewImageRepository;

    //리뷰 생성
    public ReviewResponseDto save(Long memberId, Long drinkId, ReviewCreateRequestDto requestDto, List<MultipartFile> reviewImages) {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new MemberException(NONE_MEMBER));

        Drink drink = drinkRepository.findById(drinkId)
                .orElseThrow(() -> new DrinkException(NONE_DRINK));
        List<ReviewOption> reviewOptions = reviewOptionRepository.findAllByCategoryId(drink.getCategory().getId());

        Review review = requestDto.toEntity(member, drink);
        reviewRepository.save(review);
        uploadReviewImages(reviewImages, review);

        List<SelectedOptionCreateRequestDto> selectedOptionCreateRequestDtos = requestDto.getSelectedOptions().stream()
                .collect(Collectors.toList());

        List<SelectedOption> selectedOptions = new ArrayList<>();

        for (SelectedOptionCreateRequestDto dto : selectedOptionCreateRequestDtos) {
            for(ReviewOption reviewOption : reviewOptions) {
                if (dto.getReviewOptionId() == reviewOption.getId()) {
                    selectedOptions.add(dto.toEntity(review, reviewOption));
                }
            }
        }

        log.info("ReviewOptionList size = {}", reviewOptions.size());
        log.info("SelectedOptionCreateRequestDtoList size= {}", selectedOptionCreateRequestDtos.size());
        log.info("SelectedOptionList size = {}", selectedOptions.size());

        if(selectedOptionCreateRequestDtos.size() != selectedOptions.size())
            throw new ReviewException(UNMATCHED_OPTION_SIZE);
        selectedOptionRepository.saveAll(selectedOptions);

        log.info("Review created !! memberId={}, drinkId={}", memberId, drinkId);
        return ReviewResponseDto.of(review);
    }

    // 리뷰이미지를 S3에 저장하고 리뷰이미지 엔티티 생성
    private List<ReviewImage> uploadReviewImages(List<MultipartFile> reviewImages, Review review) {

        List<String> uploadNames = reviewImages.stream()
                .map(reviewImage -> reviewImage.getOriginalFilename().toUpperCase())
                .collect(Collectors.toList());
        List<String> urls = reviewImages.stream()
                .map(reviewImage -> awsS3Uploader.upload(reviewImage, "review"))
                .collect(Collectors.toList());
        List<ReviewImage> images = new ArrayList<>();
        for(int i=0; i< urls.size(); i++) {
            ReviewImage image = createPostImage(review, urls.get(i), uploadNames.get(i));
            images.add(image);
        }
        return  images;
    }

    // 리뷰이미지를 S3에 업로드하는 로직
    private ReviewImage createPostImage(Review review, String url, String uploadName) {
        log.info("url = {}, uploadName = {}", url, uploadName);
        review.clearImages();

        return reviewImageRepository.save(ReviewImage.builder()
                .imageUrl(url)
                .uploadName(uploadName)
                .review(review)
                .build());
    }

    // 리뷰 상세 보기
    public ReviewDetailReadResponseDto readReviewDetail(long reviewId) {

        // todo 예외 처리하기!!
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new IllegalArgumentException("해당 리뷰가 존재하지 않습니다"));

        return new ReviewDetailReadResponseDto(review);
    }
}