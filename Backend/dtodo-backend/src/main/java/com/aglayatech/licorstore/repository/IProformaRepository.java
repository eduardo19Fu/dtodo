package com.aglayatech.licorstore.repository;

import com.aglayatech.licorstore.model.Proforma;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Date;
import java.util.List;

public interface IProformaRepository extends JpaRepository<Proforma, Long> {

    List<Proforma> findAllByFechaEmisionBetween(Date date1, Date date2);

    @Query(value = "{call sp_get_proformas(:date1, :date2);}", nativeQuery = true)
    List<Proforma> findAllProformas(@Param("date1") Date date1, @Param("date2") Date date2);

}
