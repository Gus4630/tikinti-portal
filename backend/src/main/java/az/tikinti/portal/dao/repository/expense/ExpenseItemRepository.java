package az.tikinti.portal.dao.repository.expense;

import az.tikinti.portal.dao.entity.expense.ExpenseItemEntity;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ExpenseItemRepository extends JpaRepository<ExpenseItemEntity, UUID> {
    List<ExpenseItemEntity> findByExpenseIdAndIsActiveTrue(UUID expenseId);
}
