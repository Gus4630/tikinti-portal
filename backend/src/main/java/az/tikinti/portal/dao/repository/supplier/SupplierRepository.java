package az.tikinti.portal.dao.repository.supplier;

import az.tikinti.portal.dao.entity.supplier.SupplierEntity;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

@Repository
public interface SupplierRepository
        extends JpaRepository<SupplierEntity, UUID>, JpaSpecificationExecutor<SupplierEntity> {

    Optional<SupplierEntity> findByNameIgnoreCase(String name);
}
