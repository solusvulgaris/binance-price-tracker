package com.ak.binancepricetracker.repository;

import com.ak.binancepricetracker.entity.PriceHistory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.Optional;

//JpaRepository уже предоставляет базовые операции — сохранение, поиск по ID, получение всех записей и удаление
public interface PriceHistoryRepository extends JpaRepository<PriceHistory, Long> {

    //	Последняя запись по символу
    Optional<PriceHistory> findFirstBySymbolOrderByFetchedAtDescIdDesc(String symbol);

    //Все записи за период с пагинацией
    Page<PriceHistory> findBySymbolAndFetchedAtBetweenOrderByFetchedAtAscIdAsc(
            String symbol,
            OffsetDateTime from,
            OffsetDateTime to,
            Pageable pageable
    );

    //Первая запись за период
    Optional<PriceHistory> findFirstBySymbolAndFetchedAtBetweenOrderByFetchedAtAscIdAsc(
            String symbol,
            OffsetDateTime from,
            OffsetDateTime to
    );

    //	Последняя запись за период
    Optional<PriceHistory> findFirstBySymbolAndFetchedAtBetweenOrderByFetchedAtDescIdDesc(
            String symbol,
            OffsetDateTime from,
            OffsetDateTime to
    );

    @Query("""
            select avg(p.price)
            from PriceHistory p
            where p.symbol = :symbol
              and p.fetchedAt between :from and :to
            """)
    Optional<BigDecimal> findAveragePriceBySymbolAndFetchedAtBetween(
            @Param("symbol") String symbol,
            @Param("from") OffsetDateTime from,
            @Param("to") OffsetDateTime to
    );
}
