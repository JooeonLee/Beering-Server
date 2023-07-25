package kuit.project.beering.dto.response.drink;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@AllArgsConstructor
@Builder
public class GetDrinkResponse {
    Long beerId;
    String nameKr;
    String nameEn;
    int price;
    float alcohol;
    String description;
    String manufacturer;
    float totalRating;
    int reviewCount;
    boolean isLiked;
    List<ReviewPreview> reviewPreviews;
}
