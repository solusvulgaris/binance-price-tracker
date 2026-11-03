package com.ak.binancepricetracker.repository;

import com.ak.binancepricetracker.entity.PriceHistory;
import org.springframework.data.jpa.repository.JpaRepository;

//JpaRepository уже предоставляет базовые операции — сохранение, поиск по ID, получение всех записей и удаление
public interface PriceHistoryRepository extends JpaRepository<PriceHistory, Long> {
}