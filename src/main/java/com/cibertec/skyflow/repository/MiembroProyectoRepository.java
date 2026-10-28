package com.cibertec.skyflow.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.cibertec.skyflow.entity.MiembroProyecto;


@Repository
public interface MiembroProyectoRepository extends JpaRepository<MiembroProyecto, Long> {

	Optional<MiembroProyecto> findByProyectoIdAndUsuarioId(Long proyectoId, Long usuarioId);

	List<MiembroProyecto> findByProyectoId(Long proyectoId);

	boolean existsByProyectoIdAndUsuarioId(Long proyectoId, Long usuarioId);
}
