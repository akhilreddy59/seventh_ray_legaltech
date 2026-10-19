package com.seventhray.contractmanager.repository;

import com.seventhray.contractmanager.model.Contract;
import com.seventhray.contractmanager.model.ContractStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public class ContractRepository {
    private static final RowMapper<Contract> CONTRACT_ROW = (rs, rowNum) -> new Contract(
            rs.getObject("id", UUID.class),
            rs.getString("title"),
            rs.getString("file_name"),
            rs.getString("storage_path"),
            ContractStatus.valueOf(rs.getString("status")),
            rs.getString("extracted_text"),
            rs.getTimestamp("created_at").toInstant().atOffset(ZoneOffset.UTC),
            rs.getTimestamp("updated_at").toInstant().atOffset(ZoneOffset.UTC));

    private final JdbcTemplate jdbc;

    public ContractRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public List<Contract> findAll(UUID ownerId) {
        return jdbc.query("SELECT * FROM contracts WHERE owner_id = ? ORDER BY created_at DESC", CONTRACT_ROW, ownerId);
    }

    public Optional<Contract> findById(UUID id, UUID ownerId) {
        return jdbc.query("SELECT * FROM contracts WHERE id = ? AND owner_id = ?", CONTRACT_ROW, id, ownerId).stream().findFirst();
    }

    @org.springframework.transaction.annotation.Transactional
    public Contract insert(Contract contract, UUID ownerId) {
        jdbc.update("""
                INSERT INTO contracts (id, title, file_name, storage_path, status, extracted_text, owner_id)
                VALUES (?, ?, ?, ?, 'DRAFT', ?, ?)
                """, contract.id(), contract.title(), contract.fileName(), contract.storagePath(), contract.extractedText(), ownerId);
        return findById(contract.id(), ownerId).orElseThrow();
    }

    public ContractStatus findStatusForUpdate(UUID id, UUID ownerId) {
        return jdbc.query("SELECT status FROM contracts WHERE id = ? AND owner_id = ? FOR UPDATE",
                rs -> rs.next() ? ContractStatus.valueOf(rs.getString(1)) : null, id, ownerId);
    }

    public void updateStatus(UUID id, UUID ownerId, ContractStatus status) {
        jdbc.update("UPDATE contracts SET status = ?, updated_at = NOW() WHERE id = ? AND owner_id = ?", status.name(), id, ownerId);
    }
}
