package az.tikinti.portal.dao.repository.expense;

import az.tikinti.portal.dao.entity.expense.ExpenseEntity;
import az.tikinti.portal.model.enums.ExpenseStatus;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface ExpenseRepository
        extends JpaRepository<ExpenseEntity, UUID>, JpaSpecificationExecutor<ExpenseEntity> {

    Optional<ExpenseEntity> findByContentHash(String contentHash);

    @Query("""
            SELECT e.amountBaseCurrency FROM ExpenseEntity e
            WHERE e.category.id = :categoryId AND e.status = :status AND e.isActive = true
            ORDER BY e.amountBaseCurrency
            """)
    List<BigDecimal> findAmountsInBaseCurrencyByCategory(
            @Param("categoryId") UUID categoryId,
            @Param("status") ExpenseStatus status);

    @Query("""
            SELECT COALESCE(SUM(e.amountBaseCurrency), 0) FROM ExpenseEntity e
            WHERE e.building.id = :buildingId AND e.category.id = :categoryId
            AND e.status = :status AND e.isActive = true
            """)
    BigDecimal sumAmountBaseCurrencyByBuildingAndCategory(
            @Param("buildingId") UUID buildingId,
            @Param("categoryId") UUID categoryId,
            @Param("status") ExpenseStatus status);

    @Query("""
            SELECT COALESCE(SUM(e.amountBaseCurrency), 0) FROM ExpenseEntity e
            WHERE e.building.id = :buildingId AND e.status = :status AND e.isActive = true
            """)
    BigDecimal sumAmountBaseCurrencyByBuilding(
            @Param("buildingId") UUID buildingId,
            @Param("status") ExpenseStatus status);

    @Query("""
            SELECT MIN(e.expenseDate) FROM ExpenseEntity e
            WHERE e.building.id = :buildingId AND e.isActive = true
            """)
    LocalDate findFirstExpenseDateByBuilding(@Param("buildingId") UUID buildingId);

    Page<ExpenseEntity> findAllByBuildingIdAndStatusAndIsActiveTrue(
            UUID buildingId, ExpenseStatus status, Pageable pageable);

    @Query("""
            SELECT e.supplier.id, COALESCE(SUM(e.amountBaseCurrency), 0) FROM ExpenseEntity e
            WHERE e.building.id = :buildingId AND e.status = :status
            AND e.isActive = true AND e.supplier IS NOT NULL
            GROUP BY e.supplier.id
            """)
    List<Object[]> sumAmountBaseCurrencyBySupplierAndBuilding(
            @Param("buildingId") UUID buildingId,
            @Param("status") ExpenseStatus status);

    @Query("""
            SELECT e FROM ExpenseEntity e
            WHERE e.building.id = :buildingId
            AND e.expenseDate >= :startDate AND e.expenseDate <= :endDate
            AND e.isActive = true
            ORDER BY e.expenseDate
            """)
    List<ExpenseEntity> findByBuildingAndDateRange(
            @Param("buildingId") UUID buildingId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate);

    @Query(value = """
            SELECT TO_CHAR(e.expense_date, 'YYYY-MM') AS month,
                   COALESCE(SUM(e.amount_base_currency), 0) AS total
            FROM expenses e
            WHERE e.building_id = :buildingId
              AND e.status = :status
              AND e.is_active = true
              AND e.expense_date IS NOT NULL
            GROUP BY TO_CHAR(e.expense_date, 'YYYY-MM')
            ORDER BY TO_CHAR(e.expense_date, 'YYYY-MM')
            """, nativeQuery = true)
    List<Object[]> sumMonthlyByBuilding(
            @Param("buildingId") UUID buildingId,
            @Param("status") String status);
}
