package az.tikinti.portal.dao.repository.expense;

import az.tikinti.portal.dao.entity.expense.ExpenseMediaEntity;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ExpenseMediaRepository extends JpaRepository<ExpenseMediaEntity, UUID> {

    List<ExpenseMediaEntity> findAllByExpenseIdAndIsActiveTrueOrderByDisplayOrderAsc(UUID expenseId);
}
