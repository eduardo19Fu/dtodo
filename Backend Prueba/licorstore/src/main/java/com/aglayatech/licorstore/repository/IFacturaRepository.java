package com.aglayatech.licorstore.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.aglayatech.licorstore.model.Factura;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Date;
import java.util.List;
import java.util.Optional;

public interface IFacturaRepository extends JpaRepository<Factura, Long> {

    @Query(value = "Select get_cant_ventas()", nativeQuery = true)
    Integer getCantidadVentas();

    List<Factura> findByFechaBetween(Date iniDate, Date endDate);

    Optional<Factura> findFacturaByNoFactura(Long noFactura);

    @Query(value = "{call sp_get_facturas(:date1, :date2);}", nativeQuery = true)
    List<Factura> findAllFacturas(@Param("date1") Date date1, @Param("date2") Date date2);
}
