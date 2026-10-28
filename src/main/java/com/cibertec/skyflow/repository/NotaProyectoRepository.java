package com.cibertec.skyflow.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.cibertec.skyflow.entity.NotaProyecto;


@Repository
public interface NotaProyectoRepository extends JpaRepository<NotaProyecto, Long> {

	List<NotaProyecto> findByProyectoIdOrderByFechaCreacionDesc(Long proyectoId);
}
