package kuit.project.beering.repository.drink;

import kuit.project.beering.domain.Drink;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface DrinkRepository extends JpaRepository<Drink, Long>, CustomDrinkRepository {

    @Query(value = "select d from Drink d "+
            "right join fetch Favorite f on f.drink.id = d.id " +
            "where f.member.id = ?1")
    Slice<Drink> findByMemberIdAndFavorite(Long memberId, Pageable pageable);
}
