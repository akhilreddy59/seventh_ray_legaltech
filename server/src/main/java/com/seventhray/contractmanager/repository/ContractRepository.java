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

    public List<Contract> findAll() {
        return jdbc.query("SELECT * FROM contracts ORDER BY created_at DESC", CONTRACT_ROW);
    }

    public Optional<Contract> findById(UUID id) {
        return jdbc.query("SELECT * FROM contracts WHERE id = ?", CONTRACT_ROW, id).stream().findFirst();
    }

    @org.springframework.transaction.annotation.Transactional
    public Contract insert(Contract contract) {
        jdbc.update("""
                INSERT INTO contracts (id, title, file_name, storage_path, status, extracted_text)
                VALUES (?, ?, ?, ?, 'DRAFT', ?)
                """, contract.id(), contract.title(), contract.fileName(), contract.storagePath(), contract.extractedText());
        return findById(contract.id()).orElseThrow();
    }

    public ContractStatus findStatusForUpdate(UUID id) {
        return jdbc.query("SELECT status FROM contracts WHERE id = ? FOR UPDATE",
                rs -> rs.next() ? ContractStatus.valueOf(rs.getString(1)) : null, id);
    }

    public void updateStatus(UUID id, ContractStatus status) {
        jdbc.update("UPDATE contracts SET status = ?, updated_at = NOW() WHERE id = ?", status.name(), id);
    }
}
