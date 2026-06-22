package az.tikinti.portal.controller.public_;

import az.tikinti.portal.dao.repository.building.BuildingRepository;
import az.tikinti.portal.dao.repository.expense.ExpenseRepository;
import az.tikinti.portal.dao.repository.supplier.SupplierRepository;
import az.tikinti.portal.model.dto.record.PlatformStatsResponse;
import az.tikinti.portal.model.enums.ExpenseStatus;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/public")
public class PublicStatsController {

    private static final Set<ExpenseStatus> MANAGED_STATUSES = Set.of(ExpenseStatus.APPROVED, ExpenseStatus.PAID);

    private final ExpenseRepository expenseRepository;
    private final BuildingRepository buildingRepository;
    private final SupplierRepository supplierRepository;

    @GetMapping("/stats")
    public ResponseEntity<PlatformStatsResponse> getStats() {
        return ResponseEntity.ok(new PlatformStatsResponse(
                expenseRepository.sumAllAmountByStatusIn(MANAGED_STATUSES),
                buildingRepository.countByIsActiveTrue(),
                supplierRepository.countByIsActiveTrue()
        ));
    }
}
