package com.cibertec.skyflow.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.cibertec.skyflow.entity.Proyecto;

@Repository
public interface ProyectoRepository extends JpaRepository<Proyecto, Long> {

	/** Proyectos en los que el usuario es miembro (lider o subordinado). */
	@Query("SELECT m.proyecto FROM MiembroProyecto m WHERE m.usuario.id = :usuarioId")
	List<Proyecto> buscarPorUsuario(@Param("usuarioId") Long usuarioId);
}
